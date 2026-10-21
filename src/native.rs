//! Lossless native document transport. Feature payloads are data, never command replay.
//! Native geometry validation and schema migrations belong to the engine's document loader.
use std::collections::{BTreeMap, BTreeSet};
use std::io::{Cursor, Read, Write};
use std::path::Path;

use base64::{Engine, engine::general_purpose::STANDARD};
use serde::{Deserialize, Serialize};
use serde_json::{Value, json};
use sha2::{Digest, Sha256};

type R<T> = Result<T, String>;
const FORMAT: &str = "solidcraft-source/1";
const MAX_FILE: usize = 256 << 20;
const MAX_EXPANDED: usize = 512 << 20;
const MAX_ENTRIES: usize = 10_000;

/// The document is authoritative. Other package entries retain their bytes; none are extracted.
/// Unknown native fields and unknown package entries are deliberately retained.
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct Native {
    pub format: String,
    pub document: Value,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub container: Option<Value>,
    #[serde(default, skip_serializing_if = "BTreeMap::is_empty")]
    pub entries: BTreeMap<String, String>,
}

/// Reject duplicate keys before constructing a Value; silently choosing one is not lossless.
pub fn strict_json(text: &str) -> R<Value> {
    fn validate(text: &str, depth: usize) -> R<()> {
        if depth > 256 {
            return Err("JSON nesting exceeds 256 levels".into());
        }
        let text = text.trim_start();
        if text.starts_with('{') {
            struct Object;
            impl<'de> serde::de::Visitor<'de> for Object {
                type Value = Vec<Box<serde_json::value::RawValue>>;
                fn expecting(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
                    f.write_str("an object with unique keys")
                }
                fn visit_map<A: serde::de::MapAccess<'de>>(self, mut map: A) -> Result<Self::Value, A::Error> {
                    let mut names = BTreeSet::new();
                    let mut values = vec![];
                    while let Some(key) = map.next_key::<String>()? {
                        if !names.insert(key.clone()) {
                            return Err(serde::de::Error::custom(format!("duplicate JSON key `{key}`")));
                        }
                        values.push(map.next_value()?);
                    }
                    Ok(values)
                }
            }
            use serde::de::Deserializer;
            let mut deserializer = serde_json::Deserializer::from_str(text);
            let values = (&mut deserializer).deserialize_map(Object).map_err(|e| e.to_string())?;
            deserializer.end().map_err(|e| e.to_string())?;
            for value in values {
                validate(value.get(), depth + 1)?;
            }
        } else if text.starts_with('[') {
            let values: Vec<Box<serde_json::value::RawValue>> = serde_json::from_str(text).map_err(|e| e.to_string())?;
            for value in values {
                validate(value.get(), depth + 1)?;
            }
        }
        Ok(())
    }
    validate(text, 0)?;
    let mut parser = serde_json::Deserializer::from_str(text);
    parser.disable_recursion_limit();
    let value = Value::deserialize(&mut parser).map_err(|e| e.to_string())?;
    parser.end().map_err(|e| e.to_string())?;
    Ok(value)
}

pub fn is_source(source: &str) -> bool {
    source.trim_start().strip_prefix("native").is_some_and(|s| s.starts_with(char::is_whitespace) || s.starts_with('{'))
}
fn valid_name(name: &str) -> bool {
    !name.is_empty() && !name.contains(['\\', ':', '\0']) && !name.split('/').any(|p| p.is_empty() || p == "." || p == "..")
}
fn json_bytes(bytes: &[u8]) -> R<Value> {
    strict_json(std::str::from_utf8(bytes).map_err(|e| e.to_string())?)
}

impl Native {
    pub fn from_source(source: &str) -> R<Self> {
        if source.len() > (1 << 30) {
            return Err("native source exceeds 1 GiB".into());
        }
        let text = source.trim_start().strip_prefix("native").ok_or("expected `native { … }`")?;
        let native: Self = serde_json::from_value(strict_json(text)?).map_err(|e| e.to_string())?;
        native.validate()?;
        Ok(native)
    }
    pub fn from_document(document: Value) -> R<Self> {
        let native = Self { format: FORMAT.into(), document, container: None, entries: BTreeMap::new() };
        native.validate()?;
        Ok(native)
    }
    pub fn read(path: &Path) -> R<Self> {
        if std::fs::metadata(path).map_err(|e| e.to_string())?.len() > MAX_FILE as u64 {
            return Err("native file exceeds 256 MiB".into());
        }
        Self::from_bytes(&std::fs::read(path).map_err(|e| e.to_string())?)
    }
    pub fn from_bytes(bytes: &[u8]) -> R<Self> {
        if bytes.len() > MAX_FILE {
            return Err("native file exceeds 256 MiB".into());
        }
        if !bytes.starts_with(b"PK\x03\x04") {
            return Self::from_document(json_bytes(bytes)?);
        }
        let mut zip = zip::ZipArchive::new(Cursor::new(bytes)).map_err(|e| e.to_string())?;
        // zip's name index can coalesce duplicate entries. Check the directory count too.
        let start = bytes.len().saturating_sub(65_557);
        let tail = &bytes[start..];
        let end =
            tail.windows(4).rposition(|w| w == b"PK\x05\x06").and_then(|i| tail.get(i..)).filter(|s| s.len() >= 22).ok_or("missing ZIP directory")?;
        let word = |i| usize::from(u16::from_le_bytes([end[i], end[i + 1]]));
        if word(4) != 0 || word(6) != 0 || word(8) != zip.len() || word(10) != zip.len() || end.len() != 22 + word(20) || zip.len() > MAX_ENTRIES {
            return Err("duplicate entries or unsupported ZIP directory".into());
        }
        let mut native = Self { format: FORMAT.into(), document: Value::Null, container: None, entries: BTreeMap::new() };
        let mut total = 0;
        let mut names = BTreeSet::new();
        for i in 0..zip.len() {
            let mut file = zip.by_index(i).map_err(|e| e.to_string())?;
            let name = file.name().to_owned();
            if !valid_name(&name) || !names.insert(name.clone()) {
                return Err(format!("invalid or duplicate package entry `{name}`"));
            }
            if file.size() > MAX_FILE as u64 {
                return Err(format!("entry `{name}` exceeds size limit"));
            }
            let mut data = Vec::new();
            (&mut file).take((MAX_FILE + 1) as u64).read_to_end(&mut data).map_err(|e| e.to_string())?;
            total += data.len();
            if data.len() > MAX_FILE || total > MAX_EXPANDED || data.len() as u64 != file.size() {
                return Err("expanded package exceeds size limit or entry length mismatch".into());
            }
            match name.as_str() {
                "document.json" => native.document = json_bytes(&data)?,
                "container.json" => native.container = Some(json_bytes(&data)?),
                _ => {
                    native.entries.insert(name, STANDARD.encode(data));
                }
            }
        }
        if native.container.is_none() {
            return Err("package is missing container.json".into());
        }
        native.validate()?;
        Ok(native)
    }
    pub fn validate(&self) -> R<()> {
        if self.format != FORMAT {
            return Err(format!("unsupported native source format `{}`", self.format));
        }
        if !self.document["format"].as_str().is_some_and(|f| f.starts_with("solidcraft/") || f.starts_with("solvecraft/")) {
            return Err("expected a native SolidCraft document with a versioned format".into());
        }
        if !self.document["features"].is_array() {
            return Err("native document is missing its features array".into());
        }
        if self.container.as_ref().is_some_and(|c| !c.is_object()) {
            return Err("container manifest must be an object".into());
        }
        if self.container.is_none() && !self.entries.is_empty() {
            return Err("package entries require a container manifest".into());
        }
        if self.entries.len() + 2 > MAX_ENTRIES {
            return Err("too many package entries".into());
        }
        let mut total = serde_json::to_vec(&self.document).map_err(|e| e.to_string())?.len();
        if total > MAX_FILE {
            return Err("document exceeds 256 MiB".into());
        }
        if let Some(container) = &self.container {
            let size = serde_json::to_vec(container).map_err(|e| e.to_string())?.len();
            if size > MAX_FILE {
                return Err("container manifest exceeds 256 MiB".into());
            }
            total += size;
        }
        for (name, encoded) in &self.entries {
            if !valid_name(name) || matches!(name.as_str(), "document.json" | "container.json") {
                return Err(format!("invalid or reserved entry `{name}`"));
            }
            if encoded.len() > MAX_FILE.div_ceil(3) * 4 {
                return Err(format!("entry `{name}` exceeds size limit"));
            }
            let data = STANDARD.decode(encoded).map_err(|e| format!("entry `{name}`: {e}"))?;
            total += data.len();
            if data.len() > MAX_FILE || total > MAX_EXPANDED {
                return Err("expanded package exceeds size limit".into());
            }
        }
        Ok(())
    }
    pub fn to_source(&self) -> R<String> {
        self.validate()?;
        Ok(format!("native {}\n", serde_json::to_string_pretty(self).map_err(|e| e.to_string())?))
    }
    /// No engine evaluation or migrations: preserve the authored native document exactly.
    pub fn to_bytes(&self) -> R<Vec<u8>> {
        self.validate()?;
        let document = serde_json::to_vec(&self.document).map_err(|e| e.to_string())?;
        let Some(container) = &self.container else { return Ok(document) };
        let mut zip = zip::ZipWriter::new(Cursor::new(Vec::new()));
        let options = zip::write::SimpleFileOptions::default().compression_method(zip::CompressionMethod::Deflated);
        let mut part = |name: &str, data: &[u8]| -> R<()> {
            zip.start_file(name, options).map_err(|e| e.to_string())?;
            zip.write_all(data).map_err(|e| e.to_string())
        };
        part("container.json", &serde_json::to_vec(container).map_err(|e| e.to_string())?)?;
        part("document.json", &document)?;
        for (name, encoded) in &self.entries {
            part(name, &STANDARD.decode(encoded).map_err(|e| e.to_string())?)?;
        }
        let bytes = zip.finish().map_err(|e| e.to_string())?.into_inner();
        if bytes.len() > MAX_FILE {
            return Err("encoded package exceeds 256 MiB".into());
        }
        Ok(bytes)
    }
    pub fn write(&self, path: &Path) -> R<()> {
        std::fs::write(path, self.to_bytes()?).map_err(|e| format!("{}: {e}", path.display()))
    }
    pub fn coverage(&self) -> Value {
        let features: Vec<Value> = self.document["features"]
            .as_array()
            .into_iter()
            .flatten()
            .enumerate()
            .map(|(i, f)| {
                json!({
                    "pointer":format!("/document/features/{i}"),"id":f["id"],"name":f["name"],"type":f["type"],"preservation":"native"
                })
            })
            .collect();
        let entries: Vec<Value> = self
            .entries
            .iter()
            .map(|(name, data)| {
                let bytes = STANDARD.decode(data).unwrap_or_default();
                json!({"name":name,"bytes":bytes.len(),"sha256":format!("{:x}",Sha256::digest(&bytes))})
            })
            .collect();
        json!({"representation":"native","source_format":self.format,"document_format":self.document["format"],
            "preservation":"all document fields and package entry payloads","engine_validation":"not_performed",
            "features":features,"document_fields":self.document.as_object().map(|o|o.keys().collect::<Vec<_>>()),"entries":entries,
            "requires":[]})
    }
    pub fn compiled(self) -> crate::Compiled {
        let sketches = self.document["features"].as_array().into_iter().flatten().filter(|f| f["type"] == "sketch").count();
        let features = self.document["features"].as_array().map_or(0, Vec::len).saturating_sub(sketches);
        crate::Compiled { expressions: self.coverage(), native: Some(self), script: Value::Null, sketches, features, lines: vec![], dims: vec![] }
    }
}

/// Exact structured-document comparison plus byte hashes for each opaque entry. JSON key order,
/// whitespace and ZIP compression are not semantic data. No tolerance or omission of unknown fields.
pub fn compare(a: &Native, b: &Native) -> Value {
    fn walk(a: &Value, b: &Value, path: &str, out: &mut Vec<String>) {
        if a == b {
            return;
        }
        match (a, b) {
            (Value::Object(a), Value::Object(b)) => {
                let keys: BTreeSet<_> = a.keys().chain(b.keys()).collect();
                for k in keys {
                    let p = format!("{path}/{}", k.replace('~', "~0").replace('/', "~1"));
                    match (a.get(k), b.get(k)) {
                        (Some(a), Some(b)) => walk(a, b, &p, out),
                        _ => out.push(p),
                    }
                }
            }
            (Value::Array(a), Value::Array(b)) if a.len() == b.len() => {
                for (i, (a, b)) in a.iter().zip(b).enumerate() {
                    walk(a, b, &format!("{path}/{i}"), out)
                }
            }
            _ => out.push(path.into()),
        }
    }
    let mut differences = vec![];
    walk(&a.document, &b.document, "/document", &mut differences);
    walk(&json!(a.container), &json!(b.container), "/container", &mut differences);
    let keys: BTreeSet<_> = a.entries.keys().chain(b.entries.keys()).collect();
    for k in keys {
        if a.entries.get(k) != b.entries.get(k) {
            differences.push(format!("/entries/{}", k.replace('~', "~0").replace('/', "~1")));
        }
    }
    json!({"ok":differences.is_empty(),"comparison":"native document and package entry payloads","differences":differences})
}

/// Export, reconstruct and compare actual files. Optional engine probes run in independent
/// processes on both revisions, preserving the imported source and the reconstruction.
pub fn roundtrip(input: &Path, output: &Path, cli: Option<String>, probe: Option<&Path>) -> R<Value> {
    use std::process::Command;
    use std::time::{SystemTime, UNIX_EPOCH};
    let input = std::fs::canonicalize(input).map_err(|e| e.to_string())?;
    let original = Native::read(&input)?;
    std::fs::create_dir_all(output).map_err(|e| e.to_string())?;
    let root = std::fs::canonicalize(output).map_err(|e| e.to_string())?;
    let stamp = SystemTime::now().duration_since(UNIX_EPOCH).map_err(|e| e.to_string())?.as_nanos();
    let dir = root.join(format!("roundtrip-{stamp}-{}", std::process::id()));
    std::fs::create_dir(&dir).map_err(|e| e.to_string())?;
    let source = dir.join("design.sc");
    let baseline_dir = dir.join("original");
    let reconstructed_dir = dir.join("reconstructed");
    std::fs::create_dir(&baseline_dir).map_err(|e| e.to_string())?;
    std::fs::create_dir(&reconstructed_dir).map_err(|e| e.to_string())?;
    let filename = input.file_name().ok_or("input has no filename")?;
    let baseline = baseline_dir.join(filename);
    std::fs::copy(&input, &baseline).map_err(|e| e.to_string())?;
    let reconstructed = reconstructed_dir.join(filename);
    let text = original.to_source()?;
    std::fs::write(&source, &text).map_err(|e| e.to_string())?;
    let parsed = Native::from_source(&std::fs::read_to_string(&source).map_err(|e| e.to_string())?)?;
    parsed.write(&reconstructed)?;
    let reopened = Native::read(&reconstructed)?;
    let comparison = compare(&original, &reopened);
    let stable = Native::from_source(&text)?.to_source()? == reopened.to_source()?;
    let mut report = json!({"schema_version":1,"ok":comparison["ok"]==true && stable,"input":input,
        "document":comparison,"source_fixed_point":stable,"coverage":original.coverage(),
        "artifacts":{"source":source,"reconstructed":reconstructed},"engine":{"status":"not_requested"},"report":dir.join("report.json")});
    if let Some(cli) = cli {
        let mut script =
            if let Some(probe) = probe { strict_json(&std::fs::read_to_string(probe).map_err(|e| e.to_string())?)? } else { json!({"commands":[]}) };
        let commands = script["commands"].as_array_mut().ok_or("probe must contain a commands array")?;
        commands.push(json!({"command":"inspect.measure","params":{}}));
        commands.push(json!({"command":"document.inspect","params":{"measure":false}}));
        let script_path = dir.join("probe.json");
        std::fs::write(&script_path, serde_json::to_vec_pretty(&script).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
        let mut errors = vec![];
        let mut docs = vec![];
        let mut results = vec![];
        for (label, path) in [("original", &baseline), ("reconstructed", &reconstructed)] {
            let saved = dir.join(label).join("evaluated.json");
            let run =
                Command::new(&cli).arg("run").arg(&script_path).arg("--in").arg(path).args(["--results", "--quiet", "--save"]).arg(&saved).output();
            match run {
                Err(e) => errors.push(format!("{label}: {e}")),
                Ok(out) => {
                    std::fs::write(dir.join(format!("{label}.stdout")), &out.stdout).map_err(|e| e.to_string())?;
                    std::fs::write(dir.join(format!("{label}.stderr")), &out.stderr).map_err(|e| e.to_string())?;
                    if !out.status.success() {
                        errors.push(format!("{label}: {}", String::from_utf8_lossy(&out.stderr)));
                        continue;
                    }
                    match Native::read(&saved) {
                        Ok(d) => docs.push(d),
                        Err(e) => errors.push(format!("{label}: {e}")),
                    }
                    match json_bytes(&out.stdout) {
                        Ok(mut v) => {
                            // document.inspect reports process-local timing and input paths.
                            // Normalize only these documented observation fields, never document data.
                            let values = if v.is_array() { v.as_array_mut() } else { v.get_mut("results").and_then(Value::as_array_mut) };
                            if let Some(values) = values {
                                for (command, value) in script["commands"].as_array().into_iter().flatten().zip(values) {
                                    if command["command"] == "document.inspect" {
                                        if let Some(object) = value.as_object_mut() {
                                            object.remove("path");
                                        }
                                        for entry in value["timeline"].as_array_mut().into_iter().flatten() {
                                            if let Some(object) = entry.as_object_mut() {
                                                object.remove("ms");
                                            }
                                        }
                                    }
                                }
                            }
                            results.push(v)
                        }
                        Err(e) => errors.push(format!("{label}: {e}")),
                    }
                }
            }
        }
        let normalized = if docs.len() == 2 { compare(&docs[0], &docs[1]) } else { json!({"ok":false}) };
        let same_results = results.len() == 2 && results[0] == results[1];
        let passed = errors.is_empty() && normalized["ok"] == true && same_results;
        report["engine"] = json!({"status":if passed{"passed"}else{"failed"},"executable":cli,"normalized_document":normalized,
            "probe_results_equal":same_results,"ignored_observation_fields":["document.inspect.path","document.inspect.timeline[*].ms"],"errors":errors,"probe":script_path,
            "note":"Same-engine paired evaluation, not an independent geometric equivalence proof. Probes exercise only the edits they contain."});
        if !passed {
            report["ok"] = json!(false);
        }
    }
    let bytes = serde_json::to_vec_pretty(&report).map_err(|e| e.to_string())?;
    std::fs::write(dir.join("report.json"), &bytes).map_err(|e| e.to_string())?;
    Ok(report)
}
