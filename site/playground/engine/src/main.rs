//! The playground's engine: one `.sc` build per run, as a WASI command.
//!
//!   sc-playground <design.sc> [--checks]
//!
//! Reads the design (and any parts it `use`s) from the WASI file system, compiles it with
//! solidcraft-dsl, runs the commands in a SolidCraft `Session`, and writes:
//!
//! - `/out/model.stl`: the whole model, binary STL in millimetres, Z up;
//! - `/out/result.json`: `{ok, stage, diagnostics, checks, bodies, timings}`.
//!
//! Every diagnostic carries the source line it came from where one is known. The page (see
//! site/src/lib/engine.worker.js) mounts the examples in memory, runs this once per build and
//! throws the instance away, so each build starts from a clean heap.
use serde_json::{Value, json};
use std::path::Path;
use std::time::Instant;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let result = match args.get(1) {
        Some(path) => run(path, args.iter().any(|a| a == "--checks")),
        None => json!({"ok": false, "stage": "usage", "diagnostics": [diag("usage", "usage: sc-playground <design.sc> [--checks]", None)]}),
    };
    if let Err(e) = std::fs::write("/out/result.json", result.to_string()) {
        eprintln!("cannot write /out/result.json: {e}");
        std::process::exit(2);
    }
}

fn diag(stage: &str, message: impl Into<String>, line: Option<usize>) -> Value {
    json!({"stage": stage, "severity": "error", "message": message.into(), "line": line})
}

/// `line 12: …` / `…:12:4` style positions in compiler messages.
fn message_line(message: &str) -> Option<usize> {
    let lower = message.to_ascii_lowercase();
    let at = lower.find("line ")?;
    lower[at + 5..].chars().take_while(char::is_ascii_digit).collect::<String>().parse().ok()
}

fn ms(t: Instant) -> f64 {
    (t.elapsed().as_secs_f64() * 10_000.0).round() / 10.0
}

fn run(path: &str, with_checks: bool) -> Value {
    let started = Instant::now();
    let fail = |stage: &str, diagnostics: Vec<Value>, timings: Value| json!({"ok": false, "stage": stage, "diagnostics": diagnostics, "timings": timings});

    let source = match std::fs::read_to_string(path) {
        Ok(s) => s,
        Err(e) => return fail("read", vec![diag("read", format!("{path}: {e}"), None)], json!({})),
    };
    let compiled = match solidcraft_dsl::build_file(&source, Path::new(path)) {
        Ok(c) => c,
        Err(e) => return fail("compile", vec![diag("compile", e.clone(), message_line(&e))], json!({"compile_ms": ms(started)})),
    };
    if compiled.native.is_some() {
        return fail("compile", vec![diag("compile", "native documents can't be built in the playground", None)], json!({}));
    }
    if let Some(link) = compiled.links.first() {
        return fail(
            "compile",
            vec![diag("compile", format!("linked parts (`component … from \"{}\"`) need the CLI; use `use` instead", link.source), Some(link.line))],
            json!({}),
        );
    }
    let mut script = compiled.script.clone();
    if let Err(e) = solidcraft_dsl::link::inline_files(&mut script, Path::new(path)) {
        return fail("compile", vec![diag("compile", e.clone(), message_line(&e))], json!({}));
    }
    let compile_ms = ms(started);

    // Run the commands one at a time, so a failing one points at its source line.
    let engine_started = Instant::now();
    let mut session = solidcraft_engine::Session::default();
    let commands = script["commands"].as_array().cloned().unwrap_or_default();
    for (i, cmd) in commands.iter().enumerate() {
        let id = cmd["command"].as_str().unwrap_or_default();
        if let Err(e) = session.execute(id, &cmd["params"]) {
            let mut d = diag("engine", format!("step {} ({id}): {e}", i + 1), compiled.lines.get(i).copied());
            d["command"] = json!(id);
            return fail("engine", vec![d], json!({"compile_ms": compile_ms, "engine_ms": ms(engine_started)}));
        }
    }
    let engine_ms = ms(engine_started);

    let mut diagnostics = Vec::new();
    let line_of_name = |name: &Value| {
        commands.iter().position(|cmd| &cmd["params"]["name"] == name).and_then(|i| compiled.lines.get(i).copied())
    };
    let doc = session.execute("document.inspect", &json!({"measure": false})).unwrap_or(Value::Null);
    for feature in doc["timeline"].as_array().into_iter().flatten() {
        for (field, severity) in [("error", "error"), ("warning", "warning")] {
            if let Some(message) = feature[field].as_str().filter(|s| !s.is_empty()) {
                let mut d = diag("timeline", format!("{}: {message}", feature["name"].as_str().unwrap_or("feature")), line_of_name(&feature["name"]));
                d["severity"] = json!(severity);
                diagnostics.push(d);
            }
        }
    }
    let measures = session.execute("inspect.measure", &json!({})).unwrap_or(Value::Null);
    let bodies = measures["bodies"].as_array().cloned().unwrap_or_default();

    // Checks: the same verification `evaluate` does, with the engine queries run in this session.
    let checks_started = Instant::now();
    let mut check_entries = Vec::new();
    if with_checks && !compiled.checks.is_empty() {
        let exists = |n: &str| bodies.iter().any(|b| b["name"] == n);
        let asked = |k: &solidcraft_dsl::checks::Check| k.needs_engine() && exists(&k.body) && k.other.as_deref().is_none_or(exists);
        let clearances: Vec<Value> = compiled
            .checks
            .iter()
            .filter(|k| k.quantity == "clearance")
            .map(|k| {
                if asked(k) { session.execute("inspect.clearance", &solidcraft_dsl::checks::clearance_command(k)["params"]).unwrap_or(Value::Null) } else { Value::Null }
            })
            .collect();
        // Overhang checks need a per-body mesh; export them up front so the lookup can be `Fn`.
        let mut meshes = std::collections::BTreeMap::new();
        for k in compiled.checks.iter().filter(|k| asked(k) && k.quantity == "overhang") {
            if !meshes.contains_key(&k.body) {
                let stl = export(&mut session, Some(&k.body));
                meshes.insert(k.body.clone(), stl);
            }
        }
        let lookup = |body: &str| -> Result<Vec<u8>, String> { meshes.get(body).cloned().unwrap_or_else(|| Err("the body was not exported".into())) };
        let outcomes = solidcraft_dsl::checks::verify(
            &compiled.checks,
            &solidcraft_dsl::checks::Evidence { bodies: &bodies, clearances: &clearances, mesh: &lookup },
        );
        for o in outcomes {
            let mut entry = o.entry;
            if let Some(message) = &o.failure {
                let line = entry["line"].as_u64().map(|n| n as usize);
                let mut d = diag("check", message.clone(), line);
                if o.approximate {
                    d["severity"] = json!("warning");
                }
                diagnostics.push(d);
            }
            entry["approximate"] = json!(o.approximate);
            check_entries.push(entry);
        }
    }
    let checks_ms = ms(checks_started);

    let export_started = Instant::now();
    let model = export(&mut session, None);
    let (ok, triangles) = match &model {
        Ok(stl) => match std::fs::write("/out/model.stl", stl) {
            Ok(()) => (true, stl.get(80..84).map(|b| u32::from_le_bytes([b[0], b[1], b[2], b[3]])).unwrap_or(0)),
            Err(e) => {
                diagnostics.push(diag("export", format!("writing the mesh: {e}"), None));
                (false, 0)
            }
        },
        Err(e) => {
            diagnostics.push(diag("export", e.clone(), None));
            (false, 0)
        }
    };
    let failed = diagnostics.iter().any(|d| d["severity"] == "error" && d["stage"] != "check");
    json!({
        "ok": ok && !failed,
        "stage": if ok { "done" } else { "export" },
        "diagnostics": diagnostics,
        "checks": check_entries,
        "bodies": bodies.iter().map(|b| json!({"name": b["name"], "volume_mm3": b["volume_mm3"], "bbox": b["bbox"]})).collect::<Vec<_>>(),
        "triangles": triangles,
        "commands": commands.len(),
        "timings": {"compile_ms": compile_ms, "engine_ms": engine_ms, "checks_ms": checks_ms, "export_ms": ms(export_started), "total_ms": ms(started)},
    })
}

/// Binary STL of the model, or of one body. On wasm32 the engine's file layer is in memory, so
/// the export is read back from its writes.
fn export(session: &mut solidcraft_engine::Session, body: Option<&str>) -> Result<Vec<u8>, String> {
    let path = "/out/export.stl";
    let mut params = json!({"path": path, "format": "stl"});
    if let Some(b) = body {
        params["bodies"] = json!([b]);
    }
    session.execute("file.export", &params).map_err(|e| format!("export: {e}"))?;
    solidcraft_io::vfs::take_writes()
        .into_iter()
        .rev()
        .find(|(p, _)| p == path)
        .map(|(_, bytes)| bytes)
        .ok_or_else(|| "the export wrote nothing".to_string())
}
