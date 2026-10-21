#![allow(clippy::unwrap_used, clippy::expect_used)]
use base64::{Engine, engine::general_purpose::STANDARD};
use serde_json::{Value, json};
use solidcraft_dsl::native::{Native, compare};
use std::{
    collections::BTreeMap,
    path::PathBuf,
    process::Command,
    sync::atomic::{AtomicU64, Ordering},
};

struct Workspace(PathBuf);
impl Workspace {
    fn new() -> Self {
        static NEXT: AtomicU64 = AtomicU64::new(0);
        let p = std::env::temp_dir().join(format!("sc-native-{}-{}", std::process::id(), NEXT.fetch_add(1, Ordering::Relaxed)));
        std::fs::create_dir_all(&p).unwrap();
        Self(p)
    }
}
impl Drop for Workspace {
    fn drop(&mut self) {
        let _ = std::fs::remove_dir_all(&self.0);
    }
}
fn document() -> Value {
    json!({"format":"solidcraft/999","name":"Every field","units":"in","next_id":813,
        "features":[{"id":7,"name":"Sketch","type":"sketch","sketch":{"points":[{"id":43,"pos":[1,2],"exprs":{"x":"L/2"}}]}},
            {"id":811,"type":"future_feature","suppressed":true,"settings":{"unknown":[false,null,"text",{"ref":43}]}}],
        "params":[{"name":"L","unit":"in","expr":"2","model":false,"comment":"keep"}],
        "marker":1,"components":[{"id":40,"parent":0}],"occurrences":[{"id":90,"component":40}],
        "metadata":{"arbitrary":{"custom":[1,2,3]}},"future_root":{"a/b~c":"preserve"},
        "assembly":{"joints":[{"id":30,"reference":{"path":[90]}}]},"links":{"40":{"path":"../external.solidcraft"}},
        "materials":{"Body1":"steel"},"configs":{"rows":[{"name":"large","params":{"L":"4 in"}}]}})
}
#[test]
fn every_field_identity_unknown_variant_and_asset_survives() {
    let mut n = Native::from_document(document()).unwrap();
    n.container = Some(json!({"format":"solidcraft-container/1","future_manifest_key":[1,2]}));
    n.entries = BTreeMap::from([
        ("blobs/image.png".into(), STANDARD.encode([0, 1, 255, 0, 7])),
        ("blobs/import.step".into(), STANDARD.encode(b"ISO-10303-21;\nEND-ISO-10303-21;")),
        ("future/extension.bin".into(), STANDARD.encode([7, 9, 13])),
        ("cache/preview.json".into(), STANDARD.encode(b"{\"old\":true}")),
    ]);
    let input = n.to_bytes().unwrap();
    let source = Native::from_bytes(&input).unwrap().to_source().unwrap();
    assert!(source.contains("future_feature") && source.contains("future_root"));
    let c = solidcraft_dsl::build(&source).unwrap();
    let native = c.native.unwrap();
    assert_eq!(c.features, 1);
    assert_eq!(c.sketches, 1);
    let restored = Native::from_bytes(&native.to_bytes().unwrap()).unwrap();
    assert_eq!(compare(&n, &restored)["ok"], true);
    assert_eq!(source, restored.to_source().unwrap());
    let mut edited = restored.clone();
    edited.document["params"][0]["expr"] = json!("4 in");
    let delta = compare(&restored, &edited);
    assert_eq!(delta["differences"], json!(["/document/params/0/expr"]));
    assert_eq!(edited.entries, restored.entries);
}
#[test]
fn numbers_are_not_rounded_and_duplicates_are_rejected() {
    let source = r#"native {"format":"solidcraft-source/1","document":{"format":"solidcraft/99","features":[],"big":184467440737095516160001,"decimal":0.12345678901234567890123456789}}"#;
    let native = Native::from_source(source).unwrap();
    assert!(native.to_source().unwrap().contains("184467440737095516160001"));
    assert!(native.to_source().unwrap().contains("0.12345678901234567890123456789"));
    for data in [r#"{"a":1,"a":2}"#, r#"{"a":[{"x":1,"x":2}]}"#] {
        assert!(solidcraft_dsl::native::strict_json(data).unwrap_err().contains("duplicate"));
    }
}
#[test]
fn invalid_envelopes_and_package_entries_fail_before_output() {
    let mut n = Native::from_document(document()).unwrap();
    n.entries.insert("../escape".into(), STANDARD.encode(b"bad"));
    assert!(n.to_bytes().is_err());
    n.container = Some(json!({"format":"solidcraft-container/1"}));
    assert!(n.to_bytes().is_err());
    n.entries.clear();
    n.entries.insert("document.json".into(), STANDARD.encode(b"bad"));
    assert!(n.to_bytes().is_err());
    n.entries.clear();
    n.entries.insert("blobs/bad".into(), "invalid base64 !".into());
    assert!(n.to_bytes().is_err());
    assert!(Native::from_bytes(b"PK\x03\x04junk").is_err());
    assert!(Native::from_source("native {}").is_err());
}
#[test]
fn cli_lossless_conversion_pack_and_roundtrip_need_no_engine() {
    let w = Workspace::new();
    let input = w.0.join("original.solidcraft");
    Native::from_document(document()).unwrap().write(&input).unwrap();
    let source = w.0.join("part.sc");
    let packed = w.0.join("part.solidcraft");
    let run = |args: &[&std::ffi::OsStr]| {
        let out = Command::new(env!("CARGO_BIN_EXE_solidcraft-dsl")).env("SOLIDCRAFT_CLI", "no-engine-required").args(args).output().unwrap();
        assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
        out
    };
    run(&["decompile".as_ref(), input.as_os_str(), "-o".as_ref(), source.as_os_str()]);
    run(&["check".as_ref(), source.as_os_str()]);
    run(&["pack".as_ref(), source.as_os_str(), "-o".as_ref(), packed.as_os_str()]);
    assert_eq!(compare(&Native::read(&input).unwrap(), &Native::read(&packed).unwrap())["ok"], true);
    let out = run(&["roundtrip".as_ref(), input.as_os_str(), "--output-dir".as_ref(), w.0.join("review").as_os_str(), "--json".as_ref()]);
    let report: Value = serde_json::from_slice(&out.stdout).unwrap();
    assert_eq!(report["ok"], true);
    assert_eq!(report["engine"]["status"], "not_requested");
}

#[test]
fn native_engine_loading_and_parameter_probe_preserve_behavior() {
    let cli = std::env::var("SOLIDCRAFT_CLI").unwrap_or_else(|_| "solidcraft-cli".into());
    if !Command::new(&cli).arg("--version").output().is_ok_and(|o| o.status.success()) {
        eprintln!("no engine: skipped native behavior probe");
        return;
    }
    let w = Workspace::new();
    let script = w.0.join("build.json");
    let input = w.0.join("original.solidcraft");
    let compiled = solidcraft_dsl::build(include_str!("../examples/plate.sc")).unwrap();
    std::fs::write(&script, compiled.script.to_string()).unwrap();
    let out = Command::new(&cli).arg("run").arg(&script).args(["--quiet", "--save"]).arg(&input).output().unwrap();
    assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
    let source = w.0.join("imported.sc");
    std::fs::write(&source, Native::read(&input).unwrap().to_source().unwrap()).unwrap();
    let report = solidcraft_dsl::evaluate::evaluate(&solidcraft_dsl::evaluate::Options {
        source,
        output: w.0.join("evaluation"),
        cli: cli.clone(),
        previews: false,
        expectations: None,
        requests: None,
    })
    .unwrap();
    assert_eq!(report["ok"], true, "{}", report["diagnostics"]);
    assert!((report["measurements"]["total"]["volume_mm3"].as_f64().unwrap() - 23335.2).abs() < 1.0);
    let mut edited = Native::read(&input).unwrap();
    let param = edited.document["params"].as_array_mut().unwrap().iter_mut().find(|p| p["name"] == "L").unwrap();
    param["expr"] = json!("120 mm");
    let edited_source = w.0.join("edited.sc");
    std::fs::write(&edited_source, edited.to_source().unwrap()).unwrap();
    let edited_report = solidcraft_dsl::evaluate::evaluate(&solidcraft_dsl::evaluate::Options {
        source: edited_source,
        output: w.0.join("edited-evaluation"),
        cli: cli.clone(),
        previews: false,
        expectations: None,
        requests: None,
    })
    .unwrap();
    assert_eq!(edited_report["ok"], true, "{}", edited_report["diagnostics"]);
    assert!((edited_report["measurements"]["total"]["volume_mm3"].as_f64().unwrap() - 35335.2).abs() < 1.0);
    let probe = w.0.join("edit.json");
    std::fs::write(&probe, json!({"commands":[{"command":"parameters.change","params":{"name":"L","expression":"120 mm"}}]}).to_string()).unwrap();
    let r = solidcraft_dsl::native::roundtrip(&input, &w.0.join("roundtrip"), Some(cli), Some(&probe)).unwrap();
    assert_eq!(r["ok"], true, "{r}");
    let result_path = PathBuf::from(r["report"].as_str().unwrap()).parent().unwrap().join("reconstructed.stdout");
    let output: Value = serde_json::from_slice(&std::fs::read(result_path).unwrap()).unwrap();
    let results = output.get("results").unwrap_or(&output).as_array().unwrap();
    assert!((results[results.len() - 2]["total"]["volume_mm3"].as_f64().unwrap() - 35335.2).abs() < 1.0);
}

#[test]
fn historical_documents_roundtrip_every_field_and_reach_a_text_fixed_point() {
    let dir = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("tests/fixtures/native-corpus");
    let mut count = 0;
    for entry in std::fs::read_dir(dir).unwrap() {
        let path = entry.unwrap().path();
        if path.extension().is_none_or(|e| e != "solidcraft") {
            continue;
        }
        let original = Native::read(&path).unwrap();
        let source = original.to_source().unwrap();
        let compiled = solidcraft_dsl::build(&source).unwrap();
        assert!(compiled.script.is_null(), "native builds must never masquerade as empty command scripts");
        let rebuilt = Native::from_bytes(&compiled.native.unwrap().to_bytes().unwrap()).unwrap();
        assert_eq!(compare(&original, &rebuilt)["ok"], true, "{}", path.display());
        assert_eq!(source, rebuilt.to_source().unwrap(), "{}", path.display());
        count += 1;
    }
    assert_eq!(count, 12);
}

#[test]
fn historical_documents_have_equal_engine_migrations_geometry_and_inspection() {
    let cli = std::env::var("SOLIDCRAFT_CLI").unwrap_or_else(|_| "solidcraft-cli".into());
    if !Command::new(&cli).arg("--version").output().is_ok_and(|o| o.status.success()) {
        eprintln!("no engine: skipped historical paired evaluation");
        return;
    }
    let fixtures = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("tests/fixtures/native-corpus");
    let w = Workspace::new();
    for entry in std::fs::read_dir(fixtures).unwrap() {
        let path = entry.unwrap().path();
        if path.extension().is_none_or(|e| e != "solidcraft") {
            continue;
        }
        let report = solidcraft_dsl::native::roundtrip(&path, &w.0, Some(cli.clone()), None).unwrap();
        assert_eq!(report["ok"], true, "{}: {report}", path.display());
    }
}

#[test]
fn real_step_mesh_and_canvas_payloads_survive_without_original_asset_files() {
    let cli = std::env::var("SOLIDCRAFT_CLI").unwrap_or_else(|_| "solidcraft-cli".into());
    if !Command::new(&cli).arg("--version").output().is_ok_and(|o| o.status.success()) {
        eprintln!("no engine: skipped native embedded-asset test");
        return;
    }
    let w = Workspace::new();
    let step = w.0.join("solid.step");
    let mesh = w.0.join("mesh.stl");
    let image = w.0.join("reference.png");
    image::RgbaImage::from_pixel(2, 2, image::Rgba([100, 120, 140, 255])).save(&image).unwrap();
    let mut script = solidcraft_dsl::build("design Assets\nbox B size 2 3 4\n").unwrap().script;
    let commands = script["commands"].as_array_mut().unwrap();
    commands.extend([
        json!({"command":"file.export","params":{"path":step,"format":"step"}}),
        json!({"command":"file.export","params":{"path":mesh,"format":"stl"}}),
        json!({"command":"file.new","params":{}}),
        json!({"command":"file.insert_step","params":{"path":step}}),
        json!({"command":"file.insert_mesh","params":{"path":mesh}}),
        json!({"command":"canvas.insert","params":{"path":image,"width":10,"name":"Reference"}}),
    ]);
    let script_path = w.0.join("build.json");
    let input = w.0.join("assets.solidcraft");
    std::fs::write(&script_path, script.to_string()).unwrap();
    let out = Command::new(&cli).arg("run").arg(&script_path).args(["--quiet", "--save"]).arg(&input).output().unwrap();
    assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
    let native = Native::read(&input).unwrap();
    assert!(native.container.is_some(), "engine should save a package");
    assert!(native.entries.keys().any(|n| n.ends_with(".step")));
    assert!(native.entries.keys().any(|n| n.ends_with(".mesh")));
    assert!(native.entries.keys().any(|n| n.ends_with(".png")));
    for p in [step, mesh, image] {
        std::fs::remove_file(p).unwrap();
    }
    let report = solidcraft_dsl::native::roundtrip(&input, &w.0.join("review"), Some(cli), None).unwrap();
    assert_eq!(report["ok"], true, "{report}");
}
