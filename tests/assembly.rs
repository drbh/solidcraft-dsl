#![allow(clippy::unwrap_used)]
use serde_json::{Value, json};
use solidcraft_dsl::build;
const HINGE: &str = include_str!("../examples/assembly_hinge.sc");

#[test]
fn definitions_are_shared_and_all_placements_precede_origins_and_joints() {
    let c = build(HINGE).unwrap();
    let commands = c.script["commands"].as_array().unwrap();
    assert_eq!(commands.iter().filter(|c| c["command"] == "solid.box").count(), 1);
    assert_eq!(commands.iter().filter(|c| c["command"] == "component.create").count(), 1);
    assert_eq!(commands.iter().filter(|c| c["command"] == "occurrence.copy").count(), 1);
    let index = |id| commands.iter().position(|c| c["command"] == id).unwrap();
    assert!(index("occurrence.copy") < index("occurrence.move"));
    assert!(index("occurrence.move") < index("joint.origin"));
    assert!(index("joint.origin") < index("joint.create"));
    assert_eq!(commands[index("joint.create")]["params"]["values"], json!(["opening"]));
    assert_eq!(commands.len(), c.lines.len());
    assert!(c.expressions["sites"].as_array().unwrap().iter().any(|s| s["role"] == "placement" && s["persistent"] == false));
    assert!(c.expressions["sites"].as_array().unwrap().iter().any(|s| s["role"] == "joint_expression" && s["persistent"] == true));
}

#[test]
fn scope_names_do_not_rewrite_expressions_or_sketch_entities() {
    let src = "param S = 3 mm\ncomponent A {\nsketch Outline on XY {\nrect r = (0,0) (S,2)\n}\nextrude Solid from Outline distance S -> body Result\n}\ncomponent B {\nsketch Outline on XY {\nrect r = (0,0) (S,2)\n}\nextrude Solid from Outline distance S -> body Result\n}\ninstance a = A\ninstance b = B";
    let c = build(src).unwrap();
    let extrusions: Vec<_> = c.script["commands"].as_array().unwrap().iter().filter(|c| c["command"] == "solid.extrude").collect();
    assert_eq!(extrusions.len(), 2);
    assert_ne!(extrusions[0]["params"]["sketch"], extrusions[1]["params"]["sketch"]);
    assert_eq!(extrusions[0]["params"]["distance"], "S");
}

#[test]
fn assembly_rejects_ambiguous_or_invalid_input_before_execution() {
    for (src, message) in [
        ("component C {\ninstance child = C\n}\ninstance root_part = C", "cyclic"),
        ("instance a = Missing", "unknown component"),
        ("component Unused {\ninstance a = Missing\n}", "unknown component"),
        ("component C {\ninstance a = C\n}", "cyclic"),
        ("component C {\n}\ninstance ROOT = C", "reserved"),
        ("component C {\nbox B size 2 2 2\nfillet F body Body1 edges (0,0,0) radius 1\n}\ninstance a = C", "explicit names"),
        ("component C {\n}\ninstance a = C\ninstance a = C", "duplicate"),
        ("component C {\n}\ninstance a = C at (0,1)", "three coordinates"),
        ("component C {\n}\ninstance a = C rotate (0,0,0) 90 deg", "nonzero"),
        ("component C {\nconnector p at (0,0,0) axis X\n}\ninstance a = C", "perpendicular"),
        ("component C {\nparam p = 1\n}", "global"),
        ("ground missing", "unknown instance"),
    ] {
        let e = build(src).unwrap_err();
        assert!(e.contains(message), "{src}: {e}");
    }
    for (tail, message) in [
        ("joint j rigid between fixed_leaf.pivot and fixed_leaf.pivot", "different instances"),
        ("joint j rigid between fixed_leaf.child.pivot and moving_leaf.pivot", "nested"),
        ("joint j rigid between fixed_leaf.typo and moving_leaf.pivot", "unknown connector"),
        ("joint j rigid between fixed_leaf.pivot and moving_leaf.pivot value 3", "0 values"),
        ("joint j revolute between fixed_leaf.pivot and moving_leaf.pivot limits (90,0)", "minimum"),
        ("joint j revolute between fixed_leaf.pivot and moving_leaf.pivot value 2 mm", "angle"),
        ("joint j revolute between fixed_leaf.pivot and moving_leaf.pivot value 1 value 2", "duplicate"),
    ] {
        let e = build(&format!("{HINGE}\n{tail}")).unwrap_err();
        assert!(e.contains(message), "{e}");
    }
}

#[test]
fn helpers_remain_symbolic_and_multidof_values_are_typed() {
    let src = HINGE.replace("value opening", "value twice") + "\nlet twice = doubled\nlet doubled = opening*2\n";
    let c = build(&src).unwrap();
    let joint = c.script["commands"].as_array().unwrap().iter().find(|c| c["command"] == "joint.create").unwrap();
    assert!(joint["params"]["values"][0].as_str().unwrap().contains("opening"));
    assert!(!joint.to_string().contains("doubled"));
    for (kind, values) in
        [("cylindrical", "(opening,3 mm)"), ("pin_slot", "(opening,3 mm)"), ("planar", "(2 mm,3 mm,opening)"), ("ball", "(opening,0 deg,0 deg)")]
    {
        build(
            &HINGE
                .replace("revolute", "PLACEHOLDER")
                .replace("PLACEHOLDER", kind)
                .replace("value opening offset 3 mm limits (0 deg,160 deg)", &format!("values {values}")),
        )
        .unwrap();
    }
}

#[test]
fn missing_capabilities_are_explicit() {
    let c = build(HINGE).unwrap();
    assert!(solidcraft_dsl::assembly::check_capabilities(&c, &json!([])).unwrap_err().contains("authored_occurrence_names"));
}

fn engine() -> Option<String> {
    let cli = std::env::var("SOLIDCRAFT_CLI").unwrap_or_else(|_| "solidcraft-cli".into());
    let out = std::process::Command::new(&cli).arg("commands").output().ok()?;
    let commands: Value = serde_json::from_slice(&out.stdout).ok()?;
    if solidcraft_dsl::assembly::check_capabilities(&build(HINGE).unwrap(), &commands).is_err() {
        eprintln!("installed engine lacks authored occurrence names: skipped engine test");
        return None;
    }
    Some(cli)
}

#[test]
fn engine_shared_instances_joint_parameter_edits_and_native_roundtrip() {
    let Some(cli) = engine() else { return };
    let dir = std::env::temp_dir().join(format!("sc-assembly-test-{}", std::process::id()));
    std::fs::create_dir_all(&dir).unwrap();
    let source = dir.join("hinge.sc");
    std::fs::write(&source, HINGE).unwrap();
    let report = solidcraft_dsl::evaluate::evaluate(&solidcraft_dsl::evaluate::Options {
        source,
        output: dir.join("review"),
        cli: cli.clone(),
        previews: false,
        expectations: None,
        requests: None,
    })
    .unwrap();
    assert_eq!(report["ok"], true, "{report}");
    assert_eq!(report["assembly"]["joints"][0]["values"][0]["value"], 35.0);
    assert_eq!(report["assembly"]["joints"][0]["frame_a"]["origin"], json!([10.0, 20.0, 0.0]));
    assert_eq!(report["assembly"]["joints"][0]["frame_b"]["origin"], json!([10.0, 20.0, 3.0]));
    assert_eq!(report["measurements"]["body_count"], 2);
    let input = report["artifacts"]["design"].as_str().unwrap();
    let script = dir.join("edit.json");
    std::fs::write(
        &script,
        json!({"commands":[
            {"command":"parameters.change","params":{"name":"opening","expression":"70 deg"}},
            {"command":"parameters.change","params":{"name":"length","expression":"40 mm"}},
            {"command":"joint.list","params":{}},
            {"command":"component.list","params":{}},
            {"command":"inspect.measure","params":{}}
        ]})
        .to_string(),
    )
    .unwrap();
    let saved = dir.join("edited.json");
    let out = std::process::Command::new(&cli)
        .arg("run")
        .arg(&script)
        .args(["--in", input, "--results", "--quiet", "--save"])
        .arg(&saved)
        .output()
        .unwrap();
    assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
    let results: Value = serde_json::from_slice(&out.stdout).unwrap();
    let results = results.get("results").unwrap_or(&results);
    assert_eq!(results[2]["joints"][0]["values"][0]["value"], 70.0, "{results}");
    assert_eq!(results[2]["conflicts"], json!([]));
    for body in results[4]["bodies"].as_array().unwrap() {
        assert!((body["volume_mm3"].as_f64().unwrap() - 1440.0).abs() < 1e-6);
    }
    let native = solidcraft_dsl::native::Native::read(&saved).unwrap();
    let document = &native.document;
    assert_eq!(document["occurrences"].as_array().unwrap().len(), 2);
    assert_eq!(document["occurrences"][0]["component"], document["occurrences"][1]["component"]);
    let report = solidcraft_dsl::native::roundtrip(&saved, &dir.join("roundtrip"), Some(cli.clone()), None).unwrap();
    assert_eq!(report["ok"], true, "{report}");
    // Repeated subassemblies share the child occurrence definitions, with distinct world placements.
    let mut c = build(include_str!("../examples/assembly_nested.sc")).unwrap();
    c.script["commands"].as_array_mut().unwrap().push(json!({"command":"inspect.measure","params":{}}));
    std::fs::write(&script, c.script.to_string()).unwrap();
    let out = std::process::Command::new(&cli).arg("run").arg(&script).args(["--quiet", "--results", "--save"]).arg(&saved).output().unwrap();
    assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
    let document = solidcraft_dsl::native::Native::read(&saved).unwrap().document;
    assert_eq!(document["occurrences"].as_array().unwrap().len(), 4);
    let output: Value = serde_json::from_slice(&out.stdout).unwrap();
    let results = output.get("results").unwrap_or(&output).as_array().unwrap();
    let measured = results.last().unwrap();
    assert_eq!(measured["body_count"], 4);
    let bodies = measured["bodies"].as_array().unwrap();
    for expected in [[6.0, 24.0, 4.0], [6.0, 54.0, 4.0], [84.0, 24.0, 4.0], [114.0, 24.0, 4.0]] {
        assert!(
            bodies.iter().any(|body| body["center_of_mass"]
                .as_array()
                .unwrap()
                .iter()
                .zip(expected)
                .all(|(a, b)| (a.as_f64().unwrap() - b).abs() < 1e-6)),
            "{measured}"
        );
    }
    let source = dir.join("diagnostics.sc");
    std::fs::write(&source, format!("{HINGE}\ninstance free_a = Leaf at (100,0,0)\ninstance free_b = Leaf at (140,0,0)\njoint free rigid between free_a.pivot and free_b.pivot\n")).unwrap();
    let options = solidcraft_dsl::evaluate::Options {
        source: source.clone(),
        output: dir.join("diagnostics"),
        cli: cli.clone(),
        previews: false,
        expectations: None,
        requests: None,
    };
    let report = solidcraft_dsl::evaluate::evaluate(&options).unwrap();
    assert_eq!(report["ok"], true, "{report}");
    assert!(report["diagnostics"].as_array().unwrap().iter().any(|d| d["stage"] == "assembly" && d["severity"] == "warning"));
    std::fs::write(&source, HINGE.replace("ground fixed_leaf", "ground fixed_leaf\nground moving_leaf")).unwrap();
    let report = solidcraft_dsl::evaluate::evaluate(&options).unwrap();
    assert_eq!(report["ok"], false, "conflicting grounded hinge must fail: {report}");
    let _ = std::fs::remove_dir_all(dir);
}
