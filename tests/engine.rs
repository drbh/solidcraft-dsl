//! Builds the examples with a real `solidcraft-cli` when one is available (`SOLIDCRAFT_CLI`, or
//! on PATH) and checks the measured volumes against the analytic values. Skipped otherwise.

#![allow(clippy::unwrap_used, clippy::expect_used)]

use std::process::Command;

fn cli() -> Option<String> {
    if let Ok(c) = std::env::var("SOLIDCRAFT_CLI") {
        return Some(c);
    }
    Command::new("solidcraft-cli").arg("--version").output().ok().filter(|o| o.status.success()).map(|_| "solidcraft-cli".to_string())
}

/// Build `src`: the single body's volume and face count, the sketches' degrees of freedom, and the
/// design saved as JSON.
fn build(cli: &str, src: &str) -> (f64, u64, Vec<u64>, serde_json::Value) {
    let dir = std::env::temp_dir().join(format!("solidcraft-dsl-test-{}-{}", std::process::id(), src.len()));
    std::fs::create_dir_all(&dir).unwrap();
    let c = solidcraft_dsl::build(src).unwrap();
    let mut script = c.script;
    let cmds = script["commands"].as_array_mut().unwrap();
    cmds.push(serde_json::json!({"command": "inspect.measure", "params": {}}));
    cmds.push(serde_json::json!({"command": "document.inspect", "params": {"measure": false}}));
    let path = dir.join("script.json");
    let saved = dir.join("design.json");
    std::fs::write(&path, script.to_string()).unwrap();
    let out = Command::new(cli).args(["run", path.to_str().unwrap(), "--results", "--quiet", "--save", saved.to_str().unwrap()]).output().unwrap();
    assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
    let v: serde_json::Value = serde_json::from_slice(&out.stdout).unwrap();
    let results = v.get("results").cloned().unwrap_or(v);
    let r = results.as_array().unwrap();
    let m = &r[r.len() - 2];
    let d = &r[r.len() - 1];
    let body = &m["bodies"][0];
    let dof: Vec<u64> = d["sketches"].as_array().unwrap().iter().map(|s| s["dof"].as_u64().unwrap()).collect();
    let design: serde_json::Value = serde_json::from_slice(&std::fs::read(&saved).unwrap()).unwrap();
    (body["volume_mm3"].as_f64().unwrap(), body["faces"].as_u64().unwrap(), dof, design)
}

#[test]
fn examples_build_to_their_analytic_volumes() {
    let Some(cli) = cli() else {
        eprintln!("no solidcraft-cli: skipped");
        return;
    };
    let (v, faces, dof, _) = build(&cli, include_str!("../examples/bracket.sc"));
    let expected = 60.0 * 40.0 * 6.0 + 6.0 * 40.0 * 34.0 - 2.0 * std::f64::consts::PI * 3.2 * 3.2 * 6.0;
    assert!((v - expected).abs() < 0.5, "bracket volume {v} vs {expected}");
    assert_eq!(faces, 10);
    assert_eq!(dof, [0, 0]);

    let (v, faces, dof, _) = build(&cli, include_str!("../examples/plate.sc"));
    assert!((v - 23335.2).abs() < 1.0, "plate volume {v}");
    assert_eq!(faces, 22);
    assert_eq!(dof, [0, 0]);
}

/// compile → build → save → decompile → compile → build gives the same part, with the sketches
/// still fully constrained, and the decompiled text is a fixed point (decompiling its own build
/// gives the same text).
#[test]
fn decompile_round_trips() {
    let Some(cli) = cli() else {
        eprintln!("no solidcraft-cli: skipped");
        return;
    };
    for (what, src) in [("bracket", include_str!("../examples/bracket.sc")), ("plate", include_str!("../examples/plate.sc"))] {
        let (v1, f1, dof1, design) = build(&cli, src);
        let sc = solidcraft_dsl::decompile_readable(&design).unwrap();
        let (v2, f2, dof2, design2) = build(&cli, &sc);
        assert!((v1 - v2).abs() < 1e-6, "{what}: volume {v1} vs {v2} after round trip:\n{sc}");
        assert_eq!(f1, f2, "{what}: faces after round trip:\n{sc}");
        assert_eq!(dof1, dof2, "{what}: sketch dof after round trip:\n{sc}");
        let sc2 = solidcraft_dsl::decompile_readable(&design2).unwrap();
        assert_eq!(sc, sc2, "{what}: decompiled text is not a fixed point");
    }
}
