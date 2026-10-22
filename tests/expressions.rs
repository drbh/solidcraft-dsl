#![allow(clippy::unwrap_used)]
use solidcraft_dsl::build;

#[test]
fn forward_helpers_are_symbolic_and_coordinates_are_explicit() {
    let c = build(
        "design Test\nlet half = L/2\nparam L = 4 cm\nsketch S on XY {\npoint seed = (half, max(2 mm, L/4))\nbind point live = (-half, 0)\n}\n",
    )
    .unwrap();
    let commands = c.script["commands"].as_array().unwrap();
    let params: Vec<_> = commands.iter().filter(|c| c["command"] == "parameters.add").collect();
    assert_eq!(params.len(), 1);
    assert_eq!(params[0]["params"]["name"], "L");
    let points: Vec<_> = commands.iter().filter(|c| c["command"] == "sketch.point").collect();
    let seeds = points[0].to_string();
    assert!(seeds.contains("20.0") && seeds.contains("10.0"), "{seeds}");
    let live = points[1].to_string();
    assert!(live.contains('L') && !live.contains("half"), "{live}");
    let sites = c.expressions["sites"].as_array().unwrap();
    assert!(sites.iter().any(|s| s["persistent"] == true));
    assert!(sites.iter().any(|s| s["persistent"] == false));
}

#[test]
fn invalid_expressions_fail_before_engine_execution() {
    for declarations in [
        "param a = b\nparam b = a",
        "param a = missing/2",
        "param a = 1 mm + 2 deg",
        "param a = 1/0",
        "param a = sqrt(-1)",
        "param a = 1 mm^2",
        "param sin = 2",
        "param a = 2\nparam a = 3",
    ] {
        let err = build(&format!("design Invalid\n{declarations}\n")).unwrap_err();
        assert!(err.contains("line"), "{err}");
    }
    for point in ["(1,2,3)", "(1 deg,0)", "(missing,0)"] {
        assert!(build(&format!("design Invalid\nsketch S on XY {{\npoint p = {point}\n}}\n")).is_err());
    }
}

#[test]
fn area_helpers_and_nested_math_use_engine_semantics() {
    let c =
        build("design Math\nparam L = 4 mm\nlet area = L^2\nlet diagonal = sqrt(2*area)\nsketch S on XY {\npoint p = (max(L, diagonal), -L/2)\n}\n")
            .unwrap();
    let parameters = c.expressions["parameters"].as_array().unwrap();
    let area = parameters.iter().find(|p| p["name"] == "area").unwrap();
    assert_eq!(area["value"], 16.0);
    assert_eq!(area["dimensions"]["length"], 2);
    let diag = parameters.iter().find(|p| p["name"] == "diagonal").unwrap();
    assert!((diag["value"].as_f64().unwrap() - 32_f64.sqrt()).abs() < 1e-12);
}

#[test]
fn all_stress_examples_compile_and_fix_accepts_entity_lists() {
    for entry in std::fs::read_dir(concat!(env!("CARGO_MANIFEST_DIR"), "/examples")).unwrap() {
        let path = entry.unwrap().path();
        if path.extension().is_none_or(|ext| ext != "sc") {
            continue;
        }
        let result = build(&std::fs::read_to_string(&path).unwrap());
        assert!(result.is_ok(), "{}: {result:?}", path.display());
    }
}

#[test]
fn sketch_sizes_and_topology_are_evaluated_without_losing_units() {
    let c = build("design Seeds\nparam R = 4 mm\nlet sides = floor(12/2)\nsketch S on XY {\ncircle c = (0,0) radius R\npolygon p = (20,0) radius (R/2) sides sides\n}\n").unwrap();
    let commands = c.script["commands"].as_array().unwrap();
    let circle = commands.iter().find(|c| c["command"] == "sketch.circle.center").unwrap();
    assert_eq!(circle["params"]["radius"], 4.0);
    let polygon = commands.iter().find(|c| c["command"] == "sketch.polygon.inscribed").unwrap();
    assert_eq!(polygon["params"]["radius"], 2.0);
    assert_eq!(polygon["params"]["sides"], 6);
    for sides in ["2", "65", "3.5", "3 mm"] {
        assert!(build(&format!("design Invalid\nsketch S on XY {{\npolygon p = (0,0) radius 2 sides ({sides})\n}}\n")).is_err());
    }
}

#[test]
fn helper_expansion_has_a_resource_bound() {
    let mut src = "design Expansion\nparam L = 1 mm\nlet h0 = L\n".to_owned();
    for i in 1..20 {
        src.push_str(&format!("let h{i} = h{}+h{}\n", i - 1, i - 1));
    }
    src.push_str("box B size h19 1 1\n");
    let error = build(&src).unwrap_err();
    assert!(error.contains("4096"), "{error}");
}
