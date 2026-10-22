#![allow(clippy::unwrap_used, clippy::expect_used)]

use std::path::PathBuf;
use std::process::Command;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

use serde_json::{Value, json};
use solidcraft_dsl::evaluate::{Options, evaluate};

struct Workspace(PathBuf);
static NEXT: AtomicU64 = AtomicU64::new(0);
impl Workspace {
    fn new() -> Self {
        let stamp = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos();
        let path = std::env::temp_dir().join(format!("sc-evaluate-test-{}-{stamp}-{}", std::process::id(), NEXT.fetch_add(1, Ordering::Relaxed)));
        std::fs::create_dir(&path).unwrap();
        Self(path)
    }
    fn options(&self, source: &str, cli: &str) -> Options {
        let path = self.0.join("part.sc");
        std::fs::write(&path, source).unwrap();
        Options { source: path, output: self.0.join("build"), cli: cli.into(), previews: false, expectations: None, requests: None }
    }
}
impl Drop for Workspace {
    fn drop(&mut self) {
        let _ = std::fs::remove_dir_all(&self.0);
    }
}
fn read(path: impl AsRef<std::path::Path>) -> Value {
    serde_json::from_slice(&std::fs::read(path).unwrap()).unwrap()
}
fn cli() -> Option<String> {
    if let Ok(cli) = std::env::var("SOLIDCRAFT_CLI") {
        return Some(cli);
    }
    Command::new("solidcraft-cli").arg("commands").output().ok().filter(|o| o.status.success()).map(|_| "solidcraft-cli".into())
}

#[test]
fn compile_failure_is_structured_and_preserves_source() {
    let w = Workspace::new();
    let source = "design Bad\nbox B size -1 2 3\n";
    let opts = w.options(source, "this-engine-must-not-be-invoked");
    let report = evaluate(&opts).unwrap();
    assert_eq!(report["ok"], false);
    assert_eq!(report["diagnostics"][0]["stage"], "compile");
    assert_eq!(report["diagnostics"][0]["line"], 2);
    assert_eq!(report["diagnostics"][0]["statement"], "box B size -1 2 3");
    assert_eq!(std::fs::read_to_string(&opts.source).unwrap(), source);
    assert_eq!(std::fs::read_to_string(report["artifacts"]["source"].as_str().unwrap()).unwrap(), source);
    assert_eq!(read(opts.output.join("latest.json")), report);
    assert!(!opts.output.join("last-success.json").exists());
}

#[test]
fn unavailable_engine_and_invalid_expectations_are_reported() {
    let w = Workspace::new();
    let mut opts = w.options("design Cube\nbox B size 2 3 4\n", "/nonexistent/solidcraft-cli");
    let report = evaluate(&opts).unwrap();
    assert_eq!(report["ok"], false);
    assert_eq!(report["diagnostics"][0]["stage"], "engine");
    let path = w.0.join("checks.json");
    std::fs::write(&path, r#"[{"pointer":"/measurements/body_count","expected":1,"tolerance":-1}]"#).unwrap();
    opts.expectations = Some(path);
    let report = evaluate(&opts).unwrap();
    assert_eq!(report["diagnostics"][0]["stage"], "expectations");
}

#[test]
fn headless_evaluation_checks_deltas_and_preserves_last_success() {
    let Some(cli) = cli() else {
        eprintln!("no solidcraft-cli: skipped");
        return;
    };
    let w = Workspace::new();
    let mut opts = w.options("design Cube\nbox B size 2 3 4\n", &cli);
    let path = w.0.join("checks.json");
    std::fs::write(
        &path,
        json!([
            {"pointer":"/measurements/body_count","expected":1},
            {"pointer":"/measurements/total/volume_mm3","expected":24,"tolerance":0.01}
        ])
        .to_string(),
    )
    .unwrap();
    opts.expectations = Some(path.clone());
    let first = evaluate(&opts).unwrap();
    assert_eq!(first["ok"], true, "{first}");
    assert_eq!(first["checks"][1]["passed"], true);
    assert!(std::path::Path::new(first["artifacts"]["design"].as_str().unwrap()).is_file());

    std::fs::write(&opts.source, "design Cube\nbox B size 2 3 5\n").unwrap();
    let failed = evaluate(&opts).unwrap();
    assert_eq!(failed["ok"], false);
    assert_eq!(failed["checks"][1]["passed"], false);
    assert_ne!(failed["source_sha256"], first["source_sha256"]);
    assert!((failed["changes"]["volume_mm3_delta"].as_f64().unwrap() - 6.0).abs() < 0.01);
    assert_eq!(read(opts.output.join("last-success.json")), first);

    // A missing measurement must fail, rather than silently passing a numeric check.
    std::fs::write(path, r#"[{"pointer":"/measurements/missing","expected":0}]"#).unwrap();
    assert_eq!(evaluate(&opts).unwrap()["checks"][0]["passed"], false);
    std::fs::write(&opts.source, "design Bad\nbox B size -1 2 3\n").unwrap();
    assert_eq!(evaluate(&opts).unwrap()["ok"], false);
    assert_eq!(read(opts.output.join("last-success.json")), first);
}

#[test]
fn engine_failure_maps_back_to_source_and_cli_exits_nonzero() {
    let Some(cli) = cli() else {
        eprintln!("no solidcraft-cli: skipped");
        return;
    };
    let w = Workspace::new();
    let opts = w.options("design Bad\nsketch Empty on XY {\n}\nextrude Nope from Empty distance 5\n", &cli);
    let report = evaluate(&opts).unwrap();
    assert_eq!(report["ok"], false, "{report}");
    assert_eq!(report["diagnostics"][0]["line"], 4, "{report}");
    assert!(report["artifacts"].get("design").is_none());
    assert!(!opts.output.join("last-success.json").exists());
    let output = Command::new(env!("CARGO_BIN_EXE_solidcraft-dsl"))
        .env("SOLIDCRAFT_CLI", cli)
        .arg("evaluate")
        .arg(&opts.source)
        .arg("--output-dir")
        .arg(&opts.output)
        .args(["--json", "--no-previews"])
        .output()
        .unwrap();
    assert!(!output.status.success());
    let report: Value = serde_json::from_slice(&output.stdout).unwrap();
    assert_eq!(report["ok"], false);
}

#[test]
fn review_images_and_underconstrained_sketch_warning() {
    let Some(cli) = cli() else {
        eprintln!("no solidcraft-cli: skipped");
        return;
    };
    let w = Workspace::new();
    let opts = w.options("design Part\nsketch Profile on XY {\nrect r = (0,0) (10,20)\n}\nextrude Base from Profile distance 3\n", &cli);
    let out = Command::new(env!("CARGO_BIN_EXE_solidcraft-dsl"))
        .env("SOLIDCRAFT_CLI", cli)
        .env("SOLIDCRAFT_GPU", "cpu")
        .arg("evaluate")
        .arg(&opts.source)
        .arg("--output-dir")
        .arg(&opts.output)
        .arg("--json")
        .output()
        .unwrap();
    let report: Value = serde_json::from_slice(&out.stdout).unwrap();
    assert!(out.status.success(), "{report}\n{}", String::from_utf8_lossy(&out.stderr));
    assert!(report["diagnostics"].as_array().unwrap().iter().any(|d| d["stage"] == "sketch" && d["severity"] == "warning"));
    for view in ["iso", "front", "top", "right"] {
        let bytes = std::fs::read(report["artifacts"][view].as_str().unwrap()).unwrap();
        assert_eq!(&bytes[..8], b"\x89PNG\r\n\x1a\n");
    }
}

#[test]
fn custom_requests_export_query_isolate_and_keep_optional_failures() {
    let Some(cli) = cli() else {
        eprintln!("no solidcraft-cli: skipped");
        return;
    };
    let w = Workspace::new();
    let mut opts = w.options("design Cube\nbox B size 10 20 30 -> body Cube\n", &cli);
    let plan = w.0.join("requests.json");
    std::fs::write(
        &plan,
        json!({"defaults":false,"requests":[
            {"id":"mutate","kind":"commands","commands":[{"command":"file.new","params":{}},{"command":"document.inspect","params":{}}]},
            {"id":"measure","kind":"commands","commands":[{"command":"inspect.measure","params":{}}]},
            {"id":"optional","kind":"commands","required":false,"commands":[{"command":"unsupported.command","params":{}}]},
            {"id":"mesh","kind":"export","format":"glb","params":{"bodies":["Cube"]}},
            {"id":"cad","kind":"export","format":"step"},
            {"id":"native","kind":"commands","extension":"stl","commands":[{"command":"file.export","params":{"path":"$output"}}]},
            {"id":"cli","kind":"cli","args":["eval","$design"]}
        ]})
        .to_string(),
    )
    .unwrap();
    opts.requests = Some(plan);
    let checks = w.0.join("checks.json");
    std::fs::write(&checks, r#"[{"pointer":"/request_results/1/metadata/results/0/body_count","expected":1}]"#).unwrap();
    opts.expectations = Some(checks);
    let report = evaluate(&opts).unwrap();
    assert_eq!(report["ok"], true, "{report}");
    assert_eq!(report["checks"][0]["passed"], true, "{report}");
    assert_eq!(report["request_results"][0]["metadata"]["results"][1]["bodies"], json!([]));
    assert_eq!(report["request_results"][1]["metadata"]["results"][0]["body_count"], 1);
    assert_eq!(report["request_results"][2]["ok"], false);
    assert_eq!(report["diagnostics"][0]["severity"], "warning");
    assert!(report["artifacts"].get("optional").is_none());
    let glb = std::fs::read(report["artifacts"]["mesh"].as_str().unwrap()).unwrap();
    assert_eq!(&glb[..4], b"glTF");
    assert!(std::fs::metadata(report["artifacts"]["cad"].as_str().unwrap()).unwrap().len() > 0);
    assert!(std::fs::metadata(report["artifacts"]["native"].as_str().unwrap()).unwrap().len() > 0);
    assert_eq!(report["request_results"][6]["metadata"]["results"]["body_count"], 1);
}

#[test]
fn observe_saved_revision_with_camera_sheet_and_request_failures() {
    let Some(cli) = cli() else {
        eprintln!("no solidcraft-cli: skipped");
        return;
    };
    let w = Workspace::new();
    let opts = w.options("design Cube\nbox B size 10 20 30\n", &cli);
    let first = evaluate(&opts).unwrap();
    assert_eq!(first["ok"], true);
    std::fs::remove_file(&opts.source).unwrap(); // Observe must not depend on editable source.
    let plan = w.0.join("requests.json");
    std::fs::write(
        &plan,
        json!({"defaults":false,"requests":[
            {"id":"close","kind":"snapshot","options":{"width":160,"height":128,"camera":{"target":[5,10,15],"yaw":0.4,"pitch":0.6,"distance":50}}},
            {"id":"sheet","kind":"sheet","columns":2,"views":[
                {"view":"top","width":160,"height":128,"label":"Top"},
                {"view":"front","width":128,"height":128,"label":"Section","commands":[{"command":"inspect.section","params":{"plane":"XY","at":15}}]}
            ]},
            {"id":"fail","kind":"commands","commands":[{"command":"unsupported.command","params":{}}]},
            {"id":"after-failure","kind":"commands","commands":[{"command":"document.inspect","params":{}}]}
        ]})
        .to_string(),
    )
    .unwrap();
    let output = Command::new(env!("CARGO_BIN_EXE_solidcraft-dsl"))
        .env("SOLIDCRAFT_CLI", &cli)
        .env("SOLIDCRAFT_GPU", "cpu")
        .arg("observe")
        .arg(first["report"].as_str().unwrap())
        .arg("--output-dir")
        .arg(&opts.output)
        .arg("--requests")
        .arg(&plan)
        .args(["--json", "--no-previews"])
        .output()
        .unwrap();
    assert!(!output.status.success());
    let report: Value = serde_json::from_slice(&output.stdout).unwrap();
    assert_eq!(report["source_sha256"], first["source_sha256"]);
    assert_eq!(report["request_results"][0]["ok"], true, "{report}");
    assert_eq!(report["request_results"][1]["ok"], true, "{report}");
    assert_eq!(report["request_results"][2]["ok"], false);
    assert_eq!(report["request_results"][3]["ok"], true);
    let sheet = image::open(report["artifacts"]["sheet"].as_str().unwrap()).unwrap();
    assert_eq!((sheet.width(), sheet.height()), (320, 152));
    assert_eq!(read(opts.output.join("last-success.json")), first);
    assert_eq!(read(opts.output.join("latest.json")), first);
    assert_eq!(read(opts.output.join("latest-observation.json")), report);
}

fn supports_bound_points(cli: &str) -> bool {
    let out = Command::new(cli).arg("commands").output().unwrap();
    let commands: Value = serde_json::from_slice(&out.stdout).unwrap();
    commands.as_array().unwrap().iter().any(|c| c["id"] == "sketch.point" && c["params"].as_str().unwrap_or("").contains("expression"))
}

#[test]
fn advanced_cuts_and_joined_pulley_remain_single_solids() {
    let Some(cli) = cli() else { return };
    for name in ["11_phone_stand", "12_spoked_pulley"] {
        let w = Workspace::new();
        let source = std::fs::read_to_string(format!("{}/examples/{name}.sc", env!("CARGO_MANIFEST_DIR"))).unwrap();
        let report = evaluate(&w.options(&source, &cli)).unwrap();
        assert_eq!(report["ok"], true, "{name}: {}", report["diagnostics"]);
        assert_eq!(report["measurements"]["body_count"], 1, "{name}: the hub and spokes must belong to the final solid");
        assert!(report["measurements"]["total"]["volume_mm3"].as_f64().unwrap() > 1000.0, "{name}");
    }
}

#[test]
fn ten_progressive_models_match_analytic_volumes() {
    let Some(cli) = cli() else {
        eprintln!("no engine: skipped stress geometry");
        return;
    };
    let pi = std::f64::consts::PI;
    let cases = [
        ("01_spacer", 432.0 * pi, 1),
        ("02_centered_plate", 3840.0, 1),
        ("03_bolt_plate", 24000.0 - 150.0 * pi, 1),
        ("04_stepped_shaft", 1320.0 * pi, 1),
        ("05_trigonometric_gusset", 4000.0 / 3_f64.sqrt(), 1),
        ("06_bearing_flange", 7540.0 * pi, 1),
        ("07_slotted_bracket", 12000.0 - 45.0 * pi, 1),
        ("08_enclosure_and_lid", 26808.0, 2),
        ("09_heat_sink", 45600.0, 1),
        ("10_rotating_link", 6000.0 - 118.0 * pi, 1),
    ];
    for (name, expected, bodies) in cases {
        if name.starts_with("10_") && !supports_bound_points(&cli) {
            eprintln!("{name}: engine lacks persistent point expressions; requires current development engine");
            continue;
        }
        let w = Workspace::new();
        let source = std::fs::read_to_string(format!("{}/examples/{name}.sc", env!("CARGO_MANIFEST_DIR"))).unwrap();
        let report = evaluate(&w.options(&source, &cli)).unwrap();
        assert_eq!(report["ok"], true, "{name}: {}", report["diagnostics"]);
        assert_eq!(report["measurements"]["body_count"], bodies, "{name}");
        let actual = report["measurements"]["total"]["volume_mm3"].as_f64().unwrap();
        // Curved surfaces use numerical integration; relative error must stay under 0.03%.
        assert!((actual - expected).abs() < expected * 0.0003, "{name}: {actual} vs {expected}");
    }
}

#[test]
fn bound_points_survive_save_reopen_and_native_parameter_edits() {
    let Some(cli) = cli() else { return };
    if !supports_bound_points(&cli) {
        eprintln!("engine lacks persistent point expressions: skipped native binding test");
        return;
    }
    let w = Workspace::new();
    let opts = w.options(include_str!("../examples/10_rotating_link.sc"), &cli);
    let report = evaluate(&opts).unwrap();
    assert_eq!(report["ok"], true, "{}", report["diagnostics"]);
    let design = report["artifacts"]["design"].as_str().unwrap();
    let empty = w.0.join("empty.json");
    std::fs::write(&empty, r#"{"commands":[]}"#).unwrap();
    let json_design = w.0.join("native.json");
    let out = Command::new(&cli).arg("run").arg(&empty).args(["--in", design, "--quiet", "--save"]).arg(&json_design).output().unwrap();
    assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
    let sc = solidcraft_dsl::decompile_readable(&read(json_design)).unwrap();
    assert!(sc.contains("bind point"));
    let roundtrip = Workspace::new();
    let rebuilt = evaluate(&roundtrip.options(&sc, &cli)).unwrap();
    assert_eq!(rebuilt["ok"], true, "{}", rebuilt["diagnostics"]);
    let volume = |r: &Value| r["measurements"]["total"]["volume_mm3"].as_f64().unwrap();
    assert!((volume(&report) - volume(&rebuilt)).abs() < 1e-6);
    std::fs::remove_file(opts.source).unwrap();
    let script = w.0.join("edit.json");
    std::fs::write(
        &script,
        json!({"commands":[
            {"command":"parameters.change","params":{"name":"angle","expression":"135 deg"}},
            {"command":"parameters.change","params":{"name":"L","expression":"80 mm"}},
            {"command":"inspect.measure","params":{}}
        ]})
        .to_string(),
    )
    .unwrap();
    let saved = w.0.join("edited.json");
    let out = Command::new(&cli).arg("run").arg(script).args(["--in", design, "--results", "--quiet", "--save"]).arg(&saved).output().unwrap();
    assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
    let v: Value = serde_json::from_slice(&out.stdout).unwrap();
    let results = v.get("results").unwrap_or(&v).as_array().unwrap();
    let m = results.last().unwrap();
    let actual = m["total"]["volume_mm3"].as_f64().unwrap();
    assert!((actual - (8000.0 - 118.0 * std::f64::consts::PI)).abs() < 1.0, "{m}");
    let bbox = &m["bodies"][0]["bbox"];
    for axis in 0..2 {
        assert!((bbox["max"][axis].as_f64().unwrap() - 50.0 / 2_f64.sqrt()).abs() < 0.01, "{bbox}");
        assert!((bbox["min"][axis].as_f64().unwrap() + 50.0 / 2_f64.sqrt()).abs() < 0.01, "{bbox}");
    }
    assert!(solidcraft_dsl::decompile_readable(&read(saved)).unwrap().contains("bind point"));
}
