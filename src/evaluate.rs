//! Headless evaluation. Each attempt owns an immutable artifact directory; only small
//! report pointers are replaced, so failed attempts cannot overwrite a good design.
use std::path::{Path, PathBuf};
use std::process::Command;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

use serde_json::{Value, json};
use sha2::{Digest, Sha256};

static NEXT_ATTEMPT: AtomicU64 = AtomicU64::new(0);

pub struct Options {
    pub source: PathBuf,
    pub output: PathBuf,
    pub cli: String,
    pub previews: bool,
    pub expectations: Option<PathBuf>,
    pub requests: Option<PathBuf>,
}

fn write(path: &Path, bytes: impl AsRef<[u8]>) -> Result<(), String> {
    std::fs::write(path, bytes).map_err(|e| format!("{}: {e}", path.display()))
}

fn write_json(path: &Path, value: &Value) -> Result<(), String> {
    write(path, serde_json::to_vec_pretty(value).map_err(|e| e.to_string())?)
}

fn read_json(path: &Path) -> Result<Value, String> {
    let bytes = std::fs::read(path).map_err(|e| format!("{}: {e}", path.display()))?;
    serde_json::from_slice(&bytes).map_err(|e| format!("{}: {e}", path.display()))
}

fn publish(root: &Path, name: &str, attempt: &str, value: &Value) -> Result<(), String> {
    let tmp = root.join(format!(".{attempt}-{name}"));
    write_json(&tmp, value)?;
    std::fs::rename(&tmp, root.join(name)).map_err(|e| format!("publishing {name}: {e}"))
}

fn diagnostic(stage: &str, message: impl Into<String>, line: Option<usize>, src: &str) -> Value {
    json!({
        "severity": "error", "stage": stage, "message": message.into(), "line": line,
        "statement": line.and_then(|n| n.checked_sub(1)).and_then(|n| src.lines().nth(n)),
    })
}

fn compile_line(message: &str) -> Option<usize> {
    message.strip_prefix("line ")?.split(':').next()?.parse().ok()
}

fn engine_index(message: &str) -> Option<usize> {
    // SolidCraft's script runner uses one-based step numbers.
    message.split("step ").nth(1)?.split_whitespace().next()?.parse::<usize>().ok()?.checked_sub(1)
}

fn source_for_name(c: &crate::Compiled, name: &Value) -> Option<usize> {
    c.script["commands"].as_array()?.iter().position(|cmd| &cmd["params"]["name"] == name).and_then(|i| c.lines.get(i).copied())
}

fn expectations(path: Option<&Path>) -> Result<Value, String> {
    let Some(path) = path else { return Ok(json!([])) };
    let v = read_json(path)?;
    let checks = v.as_array().ok_or("expectations must be an array of {pointer, expected, tolerance?}")?;
    for check in checks {
        if check["pointer"].as_str().is_none_or(|s| !s.starts_with('/'))
            || check["expected"].as_f64().is_none()
            || check.get("tolerance").is_some_and(|v| v.as_f64().is_none_or(|n| n < 0.0))
        {
            return Err("each expectation needs a JSON pointer, numeric expected value and optional nonnegative tolerance".into());
        }
    }
    Ok(v)
}

/// Returns a report even for compile/build/check failures. I/O failures preventing report
/// creation are returned as Err. Callers should exit unsuccessfully when `ok` is false.
pub fn evaluate(opts: &Options) -> Result<Value, String> {
    let source = std::fs::read_to_string(&opts.source).map_err(|e| format!("{}: {e}", opts.source.display()))?;
    let hash = format!("{:x}", Sha256::digest(source.as_bytes()));
    std::fs::create_dir_all(&opts.output).map_err(|e| e.to_string())?;
    let root = std::fs::canonicalize(&opts.output).map_err(|e| e.to_string())?;
    let time = SystemTime::now().duration_since(UNIX_EPOCH).map_err(|e| e.to_string())?.as_nanos();
    let attempt = format!("{}-{time}-{}-{}", &hash[..12], std::process::id(), NEXT_ATTEMPT.fetch_add(1, Ordering::Relaxed));
    let dir = root.join(&attempt);
    std::fs::create_dir(&dir).map_err(|e| e.to_string())?;
    write(&dir.join("source.sc"), &source)?;
    let previous = read_json(&root.join("last-success.json")).ok();
    let mut report = json!({
        "schema_version": 1, "ok": false, "source": opts.source, "source_sha256": hash,
        "attempt": attempt, "report": dir.join("report.json"),
        "diagnostics": [], "checks": [], "measurements": null, "sketches": [],
        "artifacts": {"source": dir.join("source.sc")},
        "previous_success": previous.as_ref().map(|r| &r["report"]),
        "validity": {"status": "not_checked", "reason": "No independent solid-validity check is performed."}
    });
    let mut diagnostics = Vec::new();
    match expectations(opts.expectations.as_deref()) {
        Err(e) => diagnostics.push(diagnostic("expectations", e, None, &source)),
        Ok(checks) => {
            write_json(&dir.join("expectations.json"), &checks)?;
            match crate::build(&source) {
                Err(e) => diagnostics.push(diagnostic("compile", &e, compile_line(&e), &source)),
                Ok(c) => {
                    if let Err(e) = build(opts, &dir, &source, &c, &checks, &mut report, &mut diagnostics) {
                        diagnostics.push(diagnostic("engine", e, None, &source));
                    }
                }
            }
        }
    }
    if let Some(prev) = &previous {
        report["changes"] = json!({
            "body_count_delta": report["measurements"]["body_count"].as_i64().zip(prev["measurements"]["body_count"].as_i64()).map(|(a,b)| a-b),
            "volume_mm3_delta": report["measurements"]["total"]["volume_mm3"].as_f64().zip(prev["measurements"]["total"]["volume_mm3"].as_f64()).map(|(a,b)| a-b)
        });
    }
    report["ok"] = json!(!diagnostics.iter().any(|d| d["severity"] == "error"));
    report["diagnostics"] = json!(diagnostics);
    write_json(&dir.join("report.json"), &report)?;
    publish(&root, "latest.json", &attempt, &report)?;
    if report["ok"] == true {
        publish(&root, "last-success.json", &attempt, &report)?;
    }
    Ok(report)
}

fn build(
    opts: &Options,
    dir: &Path,
    source: &str,
    c: &crate::Compiled,
    checks: &Value,
    report: &mut Value,
    diagnostics: &mut Vec<Value>,
) -> Result<(), String> {
    let plan = match crate::artifacts::Plan::load(opts.requests.as_deref()) {
        Ok(plan) => plan,
        Err(e) => {
            diagnostics.push(diagnostic("requests", e, None, source));
            return Ok(());
        }
    };
    report["expressions"] = c.expressions.clone();
    if c.expressions["requires"].as_array().is_some_and(|r| !r.is_empty()) {
        let out = Command::new(&opts.cli).arg("commands").output().map_err(|e| e.to_string())?;
        let commands: Value = serde_json::from_slice(&out.stdout).map_err(|e| format!("cannot discover engine capabilities: {e}"))?;
        let supported = commands.as_array().is_some_and(|commands| {
            commands.iter().any(|c| c["id"] == "sketch.point" && c["params"].as_str().is_some_and(|p| p.contains("expression")))
        });
        if !supported {
            return Err("`bind point` requires an engine with persistent sketch point expressions; releases 0.15 and 0.16.2 lack this capability. Set SOLIDCRAFT_CLI to a current development build.".into());
        }
    }
    let mut script = if c.native.is_some() { json!({"commands":[]}) } else { c.script.clone() };
    let cmds = script["commands"].as_array_mut().ok_or("compiler did not emit commands")?;
    let count = cmds.len();
    cmds.push(json!({"command": "inspect.measure", "params": {}}));
    cmds.push(json!({"command": "document.inspect", "params": {"measure": false}}));
    let script_path = dir.join("script.json");
    write_json(&script_path, &script)?;
    write_json(&dir.join("source-map.json"), &json!(c.lines))?;
    let design = dir.join("design.solidcraft");
    // Allow timeline errors only to collect diagnostics. A partial design is never advertised
    // as a successful review artifact and cannot advance last-success.
    let mut command = Command::new(&opts.cli);
    command.arg("run").arg(&script_path);
    if let Some(native) = &c.native {
        let input = dir.join("input.solidcraft");
        native.write(&input)?;
        command.arg("--in").arg(&input);
        report["artifacts"]["native_input"] = json!(input);
    }
    let output = command
        .args(["--results", "--quiet", "--allow-errors", "--save"])
        .arg(&design)
        .output()
        .map_err(|e| format!("cannot run `{}`: {e}", opts.cli))?;
    write(&dir.join("engine.stdout"), &output.stdout)?;
    write(&dir.join("engine.stderr"), &output.stderr)?;
    if !output.status.success() {
        let message = String::from_utf8_lossy(&output.stderr).trim().to_string();
        let i = engine_index(&message);
        let mut d = diagnostic("engine", message, i.and_then(|i| c.lines.get(i).copied()), source);
        d["command_index"] = json!(i);
        d["command"] = i.and_then(|i| c.script["commands"].get(i)).cloned().unwrap_or(Value::Null);
        diagnostics.push(d);
        return Ok(());
    }
    let raw: Value = serde_json::from_slice(&output.stdout).map_err(|e| format!("invalid engine JSON: {e}"))?;
    let results = raw.get("results").unwrap_or(&raw).as_array().ok_or("engine results must be an array")?;
    if results.len() != count + 2 {
        return Err(format!("engine returned {} results; expected {}", results.len(), count + 2));
    }
    let measures = &results[count];
    let doc = &results[count + 1];
    if !measures["bodies"].is_array() || !measures["body_count"].is_number() || !doc["timeline"].is_array() || !doc["sketches"].is_array() {
        return Err("engine inspection response is missing bodies, body_count, timeline or sketches".into());
    }
    report["measurements"] = measures.clone();
    report["sketches"] = doc["sketches"].clone();
    write_json(&dir.join("inspection.json"), doc)?;
    for feature in doc["timeline"].as_array().into_iter().flatten() {
        for (field, severity) in [("error", "error"), ("warning", "warning")] {
            if let Some(message) = feature[field].as_str().filter(|s| !s.is_empty()) {
                let mut d = diagnostic("timeline", message, source_for_name(c, &feature["name"]), source);
                d["severity"] = json!(severity);
                d["feature"] = feature["name"].clone();
                diagnostics.push(d);
            }
        }
    }
    for body in measures["bodies"].as_array().into_iter().flatten() {
        if let Some(e) = body["error"].as_str() {
            diagnostics.push(diagnostic("measurement", e, None, source));
        }
    }
    for sketch in doc["sketches"].as_array().into_iter().flatten() {
        if sketch["dof"].as_u64().is_some_and(|n| n > 0) {
            let mut d = diagnostic(
                "sketch",
                format!("{} has {} degrees of freedom", sketch["name"], sketch["dof"]),
                source_for_name(c, &sketch["name"]),
                source,
            );
            d["severity"] = json!("warning");
            diagnostics.push(d);
        }
    }
    // Geometry errors mean the saved file may contain the last good intermediate body.
    if diagnostics.iter().any(|d| d["severity"] == "error" && d["stage"] != "expectation") {
        return Ok(());
    }
    if !design.is_file() {
        return Err("engine reported success without saving the design".into());
    }
    report["artifacts"]["design"] = json!(design);
    diagnostics.extend(crate::artifacts::execute(&opts.cli, &design, dir, &plan.expanded(opts.previews), report)?);
    let check_results: Vec<Value> = checks.as_array().into_iter().flatten().map(|check| {
        let pointer = check["pointer"].as_str().unwrap_or("");
        let actual = report.pointer(pointer).cloned().unwrap_or(Value::Null);
        let passed = actual.as_f64().zip(check["expected"].as_f64())
            .is_some_and(|(a,e)| (a-e).abs() <= check["tolerance"].as_f64().unwrap_or(0.0));
        json!({"pointer": pointer, "expected": check["expected"], "tolerance": check.get("tolerance").unwrap_or(&json!(0)), "actual": actual, "passed": passed})
    }).collect();
    for check in &check_results {
        if check["passed"] != true {
            diagnostics.push(diagnostic(
                "expectation",
                format!("{}: expected {} ± {}, got {}", check["pointer"], check["expected"], check["tolerance"], check["actual"]),
                None,
                source,
            ));
        }
    }
    report["checks"] = json!(check_results);

    Ok(())
}

/// Gather more evidence from a saved evaluation or native design without reading/recompiling
/// the original source. Observation reports never move evaluation's last-success pointer.
pub fn observe(input: &Path, output: &Path, cli: &str, requests: Option<&Path>, previews: bool) -> Result<Value, String> {
    let input = std::fs::canonicalize(input).map_err(|e| e.to_string())?;
    let parent = if input.extension().is_some_and(|e| e == "json") { Some(read_json(&input)?) } else { None };
    let design = if let Some(parent) = &parent {
        PathBuf::from(parent["artifacts"]["design"].as_str().ok_or("report has no usable design artifact")?)
    } else {
        input.clone()
    };
    let design = std::fs::canonicalize(design).map_err(|e| e.to_string())?;
    let plan = crate::artifacts::Plan::load(requests)?;
    std::fs::create_dir_all(output).map_err(|e| e.to_string())?;
    let root = std::fs::canonicalize(output).map_err(|e| e.to_string())?;
    let time = SystemTime::now().duration_since(UNIX_EPOCH).map_err(|e| e.to_string())?.as_nanos();
    let attempt = format!("observe-{time}-{}-{}", std::process::id(), NEXT_ATTEMPT.fetch_add(1, Ordering::Relaxed));
    let dir = root.join(&attempt);
    std::fs::create_dir(&dir).map_err(|e| e.to_string())?;
    let snapshot = dir.join("design.solidcraft");
    std::fs::copy(&design, &snapshot).map_err(|e| e.to_string())?;
    let hash = format!("{:x}", Sha256::digest(std::fs::read(&snapshot).map_err(|e| e.to_string())?));
    let mut report = json!({
        "schema_version": 1, "kind":"observation", "ok":false, "input":input,
        "source_sha256":parent.as_ref().map(|p| &p["source_sha256"]), "design_sha256":hash,
        "attempt":attempt, "report":dir.join("report.json"), "artifacts":{"design":snapshot},
        "diagnostics":[]
    });
    let diagnostics = match crate::artifacts::execute(cli, &snapshot, &dir, &plan.expanded(previews), &mut report) {
        Ok(d) => d,
        Err(e) => vec![diagnostic("artifact", e, None, "")],
    };
    report["ok"] = json!(!diagnostics.iter().any(|d| d["severity"] == "error"));
    report["diagnostics"] = json!(diagnostics);
    write_json(&dir.join("report.json"), &report)?;
    publish(&root, "latest-observation.json", &attempt, &report)?;
    Ok(report)
}
