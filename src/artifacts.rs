//! Reproducible observations of a saved model. Each request opens a private copy of the
//! same input; command parameters remain engine-native instead of a second CAD API.
use std::collections::HashSet;
use std::path::{Path, PathBuf};
use std::process::Command;

use font8x8::UnicodeFonts;
use image::{Rgba, RgbaImage};
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};
use serde_json::{Value, json};

fn yes() -> bool {
    true
}
fn version() -> u32 {
    1
}
fn width() -> u32 {
    1000
}
fn height() -> u32 {
    800
}
fn columns() -> u32 {
    2
}

#[derive(Clone, Debug, Deserialize, Serialize, JsonSchema)]
#[serde(deny_unknown_fields)]
pub struct Plan {
    #[serde(default = "version")]
    pub version: u32,
    /// Include the four standard snapshots and a labelled 2×2 sheet. Requests with the
    /// same id replace a default. Explicit requests still run with --no-previews.
    #[serde(default = "yes")]
    pub defaults: bool,
    #[serde(default)]
    pub requests: Vec<Request>,
}
impl Default for Plan {
    fn default() -> Self {
        Self { version: 1, defaults: true, requests: vec![] }
    }
}

#[derive(Clone, Debug, Deserialize, Serialize, JsonSchema)]
pub struct Request {
    /// Unique filename-safe identifier; also the key in the report's artifacts map.
    pub id: String,
    /// Optional request failures are warnings and do not fail the evaluation.
    #[serde(default = "yes")]
    pub required: bool,
    #[serde(flatten)]
    pub action: Action,
}

#[derive(Clone, Debug, Deserialize, Serialize, JsonSchema)]
#[serde(tag = "kind", rename_all = "snake_case", deny_unknown_fields)]
pub enum Action {
    Snapshot {
        #[serde(default)]
        options: Snapshot,
    },
    Sheet {
        views: Vec<Snapshot>,
        #[serde(default = "columns")]
        columns: u32,
    },
    /// Engine file.export options are passed unchanged, except path is managed here.
    Export {
        format: String,
        #[serde(default)]
        params: serde_json::Map<String, Value>,
        #[serde(default)]
        commands: Vec<Value>,
    },
    /// Execute any engine commands and retain their JSON results. If extension is set,
    /// $output names a file the commands must produce. $dir is the request directory.
    Commands {
        commands: Vec<Value>,
        #[serde(default)]
        extension: Option<String>,
    },
    /// Escape hatch for new engine CLI features. Arguments are passed directly, without
    /// a shell. Exact $design, $output and $dir strings are replaced with managed paths.
    Cli {
        args: Vec<String>,
        #[serde(default)]
        extension: Option<String>,
    },
}

#[derive(Clone, Debug, Deserialize, Serialize, JsonSchema)]
#[serde(default, deny_unknown_fields)]
pub struct Snapshot {
    /// Standard or saved view name. Omit both view and camera to use the engine default
    /// or the camera set by commands. Cannot be combined with camera.
    pub view: Option<String>,
    /// Native engine camera: target in mm, yaw/pitch in radians, distance in mm; fov
    /// and other engine-supported fields are forwarded unchanged.
    pub camera: Option<Value>,
    #[schemars(range(min = 16, max = 8192))]
    pub width: u32,
    #[schemars(range(min = 16, max = 8192))]
    pub height: u32,
    pub grid: bool,
    pub axes: bool,
    /// Engine commands applied before capture, e.g. inspect.section or view.apply.
    pub commands: Vec<Value>,
    /// Sheet caption. Defaults to the view name or panel number.
    pub label: Option<String>,
}
impl Default for Snapshot {
    fn default() -> Self {
        Self { view: None, camera: None, width: width(), height: height(), grid: false, axes: false, commands: vec![], label: None }
    }
}

fn safe_name(s: &str) -> bool {
    !s.is_empty() && s.len() <= 80 && s.bytes().all(|b| b.is_ascii_alphanumeric() || b == b'_' || b == b'-')
}
fn validate_commands(commands: &[Value]) -> Result<(), String> {
    for c in commands {
        if c["command"].as_str().is_none_or(str::is_empty) || c.get("params").is_some_and(|p| !p.is_object()) {
            return Err("commands require a nonempty command string and optional params object".into());
        }
    }
    Ok(())
}
fn validate_snapshot(s: &Snapshot) -> Result<(), String> {
    if s.view.is_some() && s.camera.is_some() {
        return Err("give view or camera, not both".into());
    }
    if s.camera.as_ref().is_some_and(|v| !v.is_object()) {
        return Err("camera must be an engine camera object".into());
    }
    if !(16..=8192).contains(&s.width) || !(16..=8192).contains(&s.height) {
        return Err("image width and height must be 16..8192".into());
    }
    validate_commands(&s.commands)
}
impl Plan {
    pub fn load(path: Option<&Path>) -> Result<Self, String> {
        let plan: Self = match path {
            Some(path) => {
                let bytes = if path == Path::new("-") {
                    use std::io::Read;
                    let mut bytes = vec![];
                    std::io::stdin().read_to_end(&mut bytes).map_err(|e| e.to_string())?;
                    bytes
                } else {
                    std::fs::read(path).map_err(|e| e.to_string())?
                };
                serde_json::from_slice(&bytes).map_err(|e| format!("requests: {e}"))?
            }
            None => Self::default(),
        };
        plan.validate()?;
        Ok(plan)
    }
    pub fn validate(&self) -> Result<(), String> {
        if self.version != 1 {
            return Err("unsupported request version; expected 1".into());
        }
        let mut ids = HashSet::new();
        for r in &self.requests {
            if !safe_name(&r.id) || ["source", "design", "request_results"].contains(&r.id.as_str()) || !ids.insert(&r.id) {
                return Err(format!("invalid, reserved or duplicate request id: {}", r.id));
            }
            let result = match &r.action {
                Action::Snapshot { options } => validate_snapshot(options),
                Action::Sheet { views, columns } => {
                    if views.is_empty() || *columns == 0 {
                        return Err(format!("{}: sheet needs views and positive columns", r.id));
                    }
                    for v in views {
                        validate_snapshot(v)?;
                    }
                    sheet_size(views, *columns).map(|_| ())
                }
                Action::Export { format, params, commands } => {
                    if !safe_name(format) || params.contains_key("path") || params.contains_key("format") {
                        return Err(format!("{}: export needs a filename-safe format; path and format cannot be overridden in params", r.id));
                    }
                    validate_commands(commands)
                }
                Action::Commands { commands, extension } => {
                    validate_extension(extension)?;
                    validate_commands(commands)
                }
                Action::Cli { args, extension } => {
                    validate_extension(extension)?;
                    if args.is_empty() { Err("cli args must contain a subcommand".into()) } else { Ok(()) }
                }
            };
            result.map_err(|e| format!("{}: {e}", r.id))?;
        }
        Ok(())
    }
    pub fn expanded(&self, previews: bool) -> Vec<Request> {
        let views: Vec<Snapshot> = ["iso", "front", "top", "right"]
            .iter()
            .map(|v| Snapshot {
                view: Some((*v).into()),
                commands: vec![json!({"command":"browser.visibility","params":{"folder":"construction","visible":false}})],
                ..Snapshot::default()
            })
            .collect();
        let mut requests = Vec::new();
        if self.defaults && previews {
            for (id, options) in ["iso", "front", "top", "right"].into_iter().zip(views.clone()) {
                requests.push(Request { id: id.into(), required: true, action: Action::Snapshot { options } });
            }
            requests.push(Request { id: "overview".into(), required: true, action: Action::Sheet { views, columns: 2 } });
        }
        for r in &self.requests {
            if let Some(old) = requests.iter_mut().find(|old| old.id == r.id) {
                *old = r.clone();
            } else {
                requests.push(r.clone());
            }
        }
        requests
    }
}
fn validate_extension(extension: &Option<String>) -> Result<(), String> {
    if extension.as_ref().is_some_and(|s| !safe_name(s)) { Err("extension must be filename-safe, without a dot".into()) } else { Ok(()) }
}

/// Draft-07 additionalProperties applies within each oneOf branch. Schemars 0.8's
/// flattened tagged enums leave common fields outside those branches; copy them in
/// so the published schema accepts the same request objects as serde.
pub fn request_schema() -> Result<Value, String> {
    let mut schema = serde_json::to_value(schemars::schema_for!(Plan)).map_err(|e| e.to_string())?;
    let request = &mut schema["definitions"]["Request"];
    let common = request["properties"].as_object().cloned().ok_or("missing request properties")?;
    let required = request["required"].as_array().cloned().ok_or("missing request requirements")?;
    for branch in request["oneOf"].as_array_mut().ok_or("missing request alternatives")? {
        branch["properties"].as_object_mut().ok_or("missing variant properties")?.extend(common.clone());
        branch["required"].as_array_mut().ok_or("missing variant requirements")?.extend(required.clone());
    }
    Ok(schema)
}

pub fn capabilities(cli: &str) -> Result<Value, String> {
    let out = Command::new(cli).arg("commands").output().map_err(|e| e.to_string())?;
    if !out.status.success() {
        return Err(String::from_utf8_lossy(&out.stderr).into_owned());
    }
    let commands: Value = serde_json::from_slice(&out.stdout).map_err(|e| e.to_string())?;
    Ok(json!({
        "schema_version": 1, "requests_schema": request_schema()?, "engine_commands": commands,
        "defaults": Plan::default().expanded(true),
        "source_forms": {
            "concise": {"compilation":"engine commands", "coverage":"supported modeling syntax"},
            "native": {"format":"solidcraft-source/1", "compilation":"direct native document loading", "coverage":"all document fields and package entry payloads, including unknown fields", "validation":"engine required for semantic validation"}
        },
        "placeholders": {"$design": "private input design", "$output": "requested output file (requires extension for commands/cli)", "$dir": "request directory"},
        "glb": {"implementation": "binary STL conversion", "units": "metres", "up": "Y", "preserves": ["triangle geometry"], "omits": ["CAD features", "materials", "assembly hierarchy"]},
        "notes": ["Native commands and CLI arguments have the engine's normal filesystem access; request isolation is not a sandbox.", "Snapshot cameras and export parameters are engine-native. Unsupported capabilities return per-request errors."]
    }))
}

fn write_json(path: &Path, v: &impl Serialize) -> Result<(), String> {
    std::fs::write(path, serde_json::to_vec_pretty(v).map_err(|e| e.to_string())?).map_err(|e| e.to_string())
}
struct Context<'a> {
    cli: &'a str,
    dir: PathBuf,
    design: PathBuf,
    output: Option<PathBuf>,
}
impl Context<'_> {
    fn replace(&self, v: &Value) -> Result<Value, String> {
        Ok(match v {
            Value::String(s) => match s.as_str() {
                "$design" => json!(self.design),
                "$dir" => json!(self.dir),
                "$output" => json!(self.output.as_ref().ok_or("$output requires an output extension")?),
                _ => v.clone(),
            },
            Value::Array(a) => Value::Array(a.iter().map(|v| self.replace(v)).collect::<Result<_, _>>()?),
            Value::Object(o) => Value::Object(o.iter().map(|(k, v)| Ok((k.clone(), self.replace(v)?))).collect::<Result<_, String>>()?),
            _ => v.clone(),
        })
    }
    fn run(&self, args: &[String], log: &str) -> Result<Value, String> {
        let args = self.replace(&json!(args))?;
        let args: Vec<String> = serde_json::from_value(args).map_err(|e| e.to_string())?;
        let out = Command::new(self.cli).args(&args).current_dir(&self.dir).output().map_err(|e| format!("{}: {e}", self.cli))?;
        write_json(&self.dir.join(format!("{log}.invocation.json")), &json!({"executable": self.cli, "args": args}))?;
        std::fs::write(self.dir.join(format!("{log}.stdout")), &out.stdout).map_err(|e| e.to_string())?;
        std::fs::write(self.dir.join(format!("{log}.stderr")), &out.stderr).map_err(|e| e.to_string())?;
        if !out.status.success() {
            return Err(format!("engine exited {}: {}", out.status, String::from_utf8_lossy(&out.stderr).trim()));
        }
        Ok(serde_json::from_slice(&out.stdout).unwrap_or_else(|_| json!({"stdout": String::from_utf8_lossy(&out.stdout)})))
    }
    fn script(&self, commands: &[Value], filename: &str) -> Result<PathBuf, String> {
        let path = self.dir.join(filename);
        write_json(&path, &self.replace(&json!({"commands": commands}))?)?;
        Ok(path)
    }
    fn commands(&self, commands: &[Value]) -> Result<Value, String> {
        let script = self.script(commands, "commands.json")?;
        let v = self.run(
            &["run".into(), script.to_string_lossy().into_owned(), "--in".into(), "$design".into(), "--results".into(), "--quiet".into()],
            "engine",
        )?;
        write_json(&self.dir.join("results.json"), &v)?;
        Ok(v)
    }
    fn snapshot(&self, s: &Snapshot, name: &str) -> Result<(PathBuf, Value), String> {
        let script = self.script(&s.commands, &format!("{name}.commands.json"))?;
        let path = self.dir.join(format!("{name}.png"));
        let mut args = vec![
            "snapshot".into(),
            script.to_string_lossy().into_owned(),
            "--in".into(),
            "$design".into(),
            "--out".into(),
            path.to_string_lossy().into_owned(),
            "--width".into(),
            s.width.to_string(),
            "--height".into(),
            s.height.to_string(),
        ];
        if let Some(view) = &s.view {
            args.extend(["--view".into(), view.clone()]);
        }
        if let Some(camera) = &s.camera {
            args.extend(["--camera".into(), camera.to_string()]);
        }
        if !s.grid {
            args.push("--no-grid".into());
        }
        if !s.axes {
            args.push("--no-axes".into());
        }
        let reply = self.run(&args, name)?;
        if !path.is_file() {
            return Err("snapshot succeeded without creating its output".into());
        }
        Ok((path, reply))
    }
}

/// Run every request independently and retain both successful and failed results.
/// Returns diagnostics to merge into the evaluation/observation report.
pub fn execute(cli: &str, design: &Path, dir: &Path, requests: &[Request], report: &mut Value) -> Result<Vec<Value>, String> {
    Plan { version: 1, defaults: false, requests: requests.to_vec() }.validate()?;
    let mut diagnostics = vec![];
    let mut results = vec![];
    let root = dir.join("requests");
    std::fs::create_dir(&root).map_err(|e| e.to_string())?;
    write_json(&dir.join("requests.json"), &requests)?;
    // Resolve relative executable paths before changing working directory for requests.
    let executable = if Path::new(cli).components().count() > 1 {
        std::fs::canonicalize(cli).map_err(|e| e.to_string())?.to_string_lossy().into_owned()
    } else {
        cli.into()
    };
    for request in requests {
        let rd = root.join(&request.id);
        std::fs::create_dir(&rd).map_err(|e| e.to_string())?;
        write_json(&rd.join("request.json"), request)?;
        let input = rd.join("input.solidcraft");
        std::fs::copy(design, &input).map_err(|e| e.to_string())?;
        let ctx = Context { cli: &executable, dir: rd.clone(), design: input, output: None };
        let result = perform(ctx, &request.action);
        let mut entry = json!({"id": request.id, "required": request.required, "request": request, "directory": rd, "ok": result.is_ok()});
        match result {
            Ok((path, metadata)) => {
                report["artifacts"][&request.id] = json!(path);
                entry["path"] = json!(path);
                entry["metadata"] = metadata;
            }
            Err(error) => {
                entry["error"] = json!(error);
                diagnostics.push(
                    json!({"stage":"artifact", "request_id":request.id, "severity":if request.required {"error"} else {"warning"}, "message":error}),
                );
            }
        }
        write_json(&rd.join("result.json"), &entry)?;
        results.push(entry);
    }
    report["request_results"] = json!(results);
    Ok(diagnostics)
}
fn perform(mut ctx: Context<'_>, action: &Action) -> Result<(PathBuf, Value), String> {
    match action {
        Action::Snapshot { options } => ctx.snapshot(options, "image"),
        Action::Sheet { views, columns } => sheet(&ctx, views, *columns),
        Action::Export { format, params, commands } => {
            let glb = format == "glb";
            let extension = if format == "3mf-tagged" { "3mf" } else { format };
            let path = ctx.dir.join(format!("model.{extension}"));
            let intermediate = if glb { ctx.dir.join("mesh.stl") } else { path.clone() };
            let mut params = params.clone();
            params.insert("path".into(), json!(intermediate));
            params.insert("format".into(), json!(if glb { "stl" } else { format }));
            let mut commands = commands.clone();
            commands.push(json!({"command":"file.export", "params":params}));
            ctx.output = Some(path.clone());
            let engine = ctx.commands(&commands)?;
            let conversion = if glb { crate::glb::convert(&intermediate, &path)? } else { Value::Null };
            if !path.is_file() {
                return Err("export succeeded without creating its output".into());
            }
            Ok((path, json!({"engine":engine, "conversion":conversion})))
        }
        Action::Commands { commands, extension } => {
            ctx.output = extension.as_ref().map(|ext| ctx.dir.join(format!("output.{ext}")));
            let reply = ctx.commands(commands)?;
            let path = ctx.output.clone().unwrap_or_else(|| ctx.dir.join("results.json"));
            if !path.is_file() {
                return Err("commands succeeded without creating $output".into());
            }
            Ok((path, json!({"results":reply})))
        }
        Action::Cli { args, extension } => {
            ctx.output = extension.as_ref().map(|ext| ctx.dir.join(format!("output.{ext}")));
            let reply = ctx.run(args, "engine")?;
            write_json(&ctx.dir.join("results.json"), &reply)?;
            let path = ctx.output.clone().unwrap_or_else(|| ctx.dir.join("results.json"));
            if !path.is_file() {
                return Err("CLI succeeded without creating $output".into());
            }
            Ok((path, json!({"results":reply})))
        }
    }
}
fn sheet_size(views: &[Snapshot], columns: u32) -> Result<(u32, u32, u32, u32), String> {
    let cell_w = views.iter().map(|v| v.width).max().ok_or("sheet needs views")?;
    let cell_h = views.iter().map(|v| v.height).max().ok_or("sheet needs views")? + 24;
    let count = u32::try_from(views.len()).map_err(|_| "too many panels")?;
    let cols = columns.min(count).max(1);
    let rows = count.div_ceil(cols);
    let w = cell_w.checked_mul(cols).ok_or("sheet width overflow")?;
    let h = cell_h.checked_mul(rows).ok_or("sheet height overflow")?;
    if u64::from(w) * u64::from(h) > 64_000_000 {
        return Err("sheet exceeds 64 million pixels; reduce panel size or split sheets".into());
    }
    Ok((w, h, cell_w, cell_h))
}
fn sheet(ctx: &Context<'_>, views: &[Snapshot], columns: u32) -> Result<(PathBuf, Value), String> {
    let (w, h, cw, ch) = sheet_size(views, columns)?;
    let cols = columns.min(views.len() as u32);
    let mut canvas = RgbaImage::from_pixel(w, h, Rgba([240, 242, 245, 255]));
    let mut panels = vec![];
    for (i, v) in views.iter().enumerate() {
        // Each panel gets a fresh private design even when its prelude contains file.save.
        let pd = ctx.dir.join(format!("panel-{i}"));
        std::fs::create_dir(&pd).map_err(|e| e.to_string())?;
        let input = pd.join("input.solidcraft");
        std::fs::copy(&ctx.design, &input).map_err(|e| e.to_string())?;
        let pc = Context { cli: ctx.cli, dir: pd, design: input, output: None };
        let (path, reply) = pc.snapshot(v, "image")?;
        let image = image::open(&path).map_err(|e| e.to_string())?.to_rgba8();
        if image.dimensions() != (v.width, v.height) {
            return Err("snapshot dimensions differ from requested panel dimensions".into());
        }
        let x = (i as u32 % cols) * cw;
        let y = (i as u32 / cols) * ch;
        image::imageops::overlay(&mut canvas, &image, i64::from(x), i64::from(y + 24));
        let label = v.label.clone().or_else(|| v.view.clone()).unwrap_or_else(|| format!("View {}", i + 1));
        draw_label(&mut canvas, x + 8, y + 4, cw.saturating_sub(16), &label);
        panels.push(json!({"label":label,"path":path,"rectangle":[x,y+24,v.width,v.height],"options":v,"engine":reply}));
    }
    let path = ctx.dir.join("sheet.png");
    canvas.save(&path).map_err(|e| e.to_string())?;
    Ok((path, json!({"width":w,"height":h,"panels":panels})))
}
fn draw_label(canvas: &mut RgbaImage, x: u32, y: u32, width: u32, label: &str) {
    for (i, c) in label.chars().take((width / 16) as usize).enumerate() {
        if let Some(glyph) = font8x8::BASIC_FONTS.get(c).or_else(|| font8x8::BASIC_FONTS.get('?')) {
            for (gy, row) in glyph.iter().enumerate() {
                for gx in 0..8 {
                    if row & (1 << gx) != 0 {
                        for dy in 0..2 {
                            for dx in 0..2 {
                                canvas.put_pixel(x + i as u32 * 16 + gx * 2 + dx, y + gy as u32 * 2 + dy, Rgba([35, 40, 48, 255]));
                            }
                        }
                    }
                }
            }
        }
    }
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    #[test]
    fn reject_ambiguous_or_unbounded_requests_and_unknown_fields() {
        for request in [
            json!({"id":"../escape","kind":"snapshot"}),
            json!({"id":"design","kind":"snapshot"}),
            json!({"id":"x","kind":"snapshot","options":{"view":"top","camera":{}}}),
            json!({"id":"x","kind":"snapshot","options":{"width":0}}),
            json!({"id":"x","kind":"sheet","columns":0,"views":[]}),
            json!({"id":"x","kind":"sheet","views":[{"width":8192,"height":8192}]}),
            json!({"id":"x","kind":"commands","commands":[{"command":"document.inspect","params":42}]}),
            json!({"id":"x","kind":"export","format":"stl","params":{"path":"overwrite.stl"}}),
            json!({"id":"x","kind":"cli","args":[],"extension":"../out"}),
        ] {
            let plan: Plan = serde_json::from_value(json!({"requests":[request]})).unwrap();
            assert!(plan.validate().is_err(), "{plan:?}");
        }
        for request in [json!({"id":"x","kind":"snapshot","options":{"widht":300}}), json!({"id":"x","kind":"snapshot","widht":300})] {
            assert!(serde_json::from_value::<Plan>(json!({"requests":[request]})).is_err());
        }
        let p: Plan = serde_json::from_value(json!({"requests":[{"id":"x","kind":"snapshot"},{"id":"x","kind":"snapshot"}]})).unwrap();
        assert!(p.validate().is_err());
    }
    #[test]
    fn explicit_requests_override_defaults_and_survive_no_previews() {
        let p: Plan = serde_json::from_value(json!({"requests":[{"id":"iso","kind":"snapshot","options":{"width":256}}]})).unwrap();
        let requests = p.expanded(true);
        assert_eq!(requests.len(), 5);
        let Action::Snapshot { options } = &requests[0].action else { return };
        assert_eq!(options.width, 256);
        assert_eq!(p.expanded(false).len(), 1);
    }
}
