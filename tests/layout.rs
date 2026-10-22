#![allow(clippy::unwrap_used)]
use serde_json::{Value, json};
use solidcraft_dsl::{
    artifacts::Plan,
    evaluate::{Options, evaluate},
};
use std::{
    path::PathBuf,
    process::Command,
    sync::atomic::{AtomicU64, Ordering},
};
static NEXT: AtomicU64 = AtomicU64::new(0);
struct Workspace(PathBuf);
impl Workspace {
    fn new() -> Self {
        let path = std::env::temp_dir().join(format!("sc-layout-{}-{}", std::process::id(), NEXT.fetch_add(1, Ordering::Relaxed)));
        std::fs::create_dir_all(&path).unwrap();
        Self(path)
    }
    fn evaluate(&self, cli: &str, source: &str, requests: Value) -> Value {
        let file = self.0.join("part.sc");
        let recipe = self.0.join("recipe.json");
        std::fs::write(&file, source).unwrap();
        std::fs::write(&recipe, requests.to_string()).unwrap();
        evaluate(&Options {
            source: file,
            output: self.0.join("review"),
            cli: cli.into(),
            previews: false,
            expectations: None,
            requests: Some(recipe),
        })
        .unwrap()
    }
}
impl Drop for Workspace {
    fn drop(&mut self) {
        let _ = std::fs::remove_dir_all(&self.0);
    }
}
fn cli() -> Option<String> {
    let cli = std::env::var("SOLIDCRAFT_CLI").unwrap_or_else(|_| "solidcraft-cli".into());
    let out = Command::new(&cli).arg("commands").output().ok()?;
    let v: Value = serde_json::from_slice(&out.stdout).ok()?;
    if !v.as_array()?.iter().any(|c| c["id"] == "occurrence.properties") {
        eprintln!("engine lacks occurrence properties: skipping layouts");
        return None;
    }
    Some(cli)
}
fn plan() -> Value {
    json!({"defaults":false,"layouts":{"plate":{"spacing":"W/4","bed":[100,100],"parts":[
 {"id":"a","select":{"bodies":["A"]},"rotate":[{"axis":[1,0,0],"angle":"180 deg"}]},
 {"id":"b","select":{"bodies":["B"]}}
 ]}},"requests":[{"id":"print","layout":"plate","kind":"export","format":"3mf"},
 {"id":"verify","layout":"plate","kind":"commands","commands":[{"command":"inspect.measure","params":{}}]},
 {"id":"glb","layout":"plate","kind":"export","format":"glb"}]})
}
const SOURCE: &str = "param W = 20 mm\nbox First size W 10 4 corner (30,40,6) -> body A\nbox Second size 8 5 3 corner (0,0,20) -> body B\n";
#[test]
fn named_layouts_validate_expand_and_schema_describes_them() {
    let p: Plan = serde_json::from_value(plan()).unwrap();
    p.validate().unwrap();
    assert!(matches!(p.expanded(false)[0].layout, Some(solidcraft_dsl::layout::LayoutRef::Inline(_))));
    let schema = solidcraft_dsl::artifacts::request_schema().unwrap();
    assert!(schema["properties"]["layouts"].is_object());
    for branch in schema["definitions"]["Request"]["oneOf"].as_array().unwrap() {
        assert!(branch["properties"]["layout"].is_object());
    }
    {
        let mut v = plan();
        v["requests"][0]["layout"] = json!("missing");
        assert!(serde_json::from_value::<Plan>(v).unwrap().validate().is_err());
    }
    let mut v = plan();
    v["layouts"]["plate"]["parts"][0]["rotat"] = json!([]);
    assert!(serde_json::from_value::<Plan>(v).is_err());
    let mut v = plan();
    v["layouts"]["plate"]["parts"][1]["id"] = json!("a");
    assert!(serde_json::from_value::<Plan>(v).unwrap().validate().is_err());
}
#[test]
fn measured_layout_exports_reuses_geometry_and_tracks_parameter_changes() {
    let Some(cli) = cli() else { return };
    let w = Workspace::new();
    for size in [20., 40.] {
        let report = w.evaluate(&cli, &SOURCE.replace("20 mm", &format!("{size} mm")), plan());
        assert_eq!(report["ok"], true, "{report}");
        let layout = &report["request_results"][0]["metadata"]["layout"];
        let parts = layout["placements"].as_array().unwrap();
        assert_eq!(parts.len(), 2);
        for p in parts {
            assert!(p["bounds"]["min"][2].as_f64().unwrap().abs() < 1e-6);
        }
        assert!((parts[0]["bounds"]["max"][0].as_f64().unwrap() - size).abs() < 1e-6);
        let a = &parts[0]["bounds"];
        let b = &parts[1]["bounds"];
        assert!((0..2).any(|i| a["max"][i].as_f64().unwrap() + size / 4. <= b["min"][i].as_f64().unwrap() + 1e-6
            || b["max"][i].as_f64().unwrap() + size / 4. <= a["min"][i].as_f64().unwrap() + 1e-6));
        // Original assembly measurements remain at their authored locations.
        assert_eq!(report["measurements"]["bodies"][0]["bbox"]["min"], json!([30., 40., 6.]));
        let reused = PathBuf::from(report["request_results"][1]["directory"].as_str().unwrap());
        assert!(reused.join("prepared.solidcraft").is_file());
        assert!(!reused.join("layout-extract.commands.json").exists());
        let bytes = std::fs::read(report["artifacts"]["glb"].as_str().unwrap()).unwrap();
        assert_eq!(&bytes[..4], b"glTF");
        // The engine's 3MF writer emits world-space mesh vertices. Check the delivered
        // plate actually rests on the bed, independently of the layout manifest.
        let file = std::fs::File::open(report["artifacts"]["print"].as_str().unwrap()).unwrap();
        let mut archive = zip::ZipArchive::new(file).unwrap();
        let mut model = String::new();
        std::io::Read::read_to_string(&mut archive.by_name("3D/3dmodel.model").unwrap(), &mut model).unwrap();
        assert!(model.contains("unit=\"millimeter\""));
        let zs: Vec<f64> =
            model.split("<vertex ").skip(1).map(|v| v.split("z=\"").nth(1).unwrap().split('"').next().unwrap().parse().unwrap()).collect();
        assert!(!zs.is_empty());
        assert!(zs.iter().all(|z| *z >= -1e-6));
        assert!(zs.iter().any(|z| z.abs() < 1e-6));
        assert!(zs.iter().all(|z| *z <= 4. + 1e-6));
        // Reopen the exported 3MF through the engine to verify the actual delivered artifact.
        let script = w.0.join("mesh.json");
        std::fs::write(
            &script,
            json!({"commands":[{"command":"doc.open","params":{"path":report["artifacts"]["print"]}},{"command":"document.inspect","params":{}}]})
                .to_string(),
        )
        .unwrap();
        let out = Command::new(&cli).arg("run").arg(&script).args(["--quiet", "--results"]).output().unwrap();
        assert!(out.status.success(), "{}", String::from_utf8_lossy(&out.stderr));
    }
}
#[test]
fn invalid_selection_dimensions_and_overflow_fail_the_request() {
    let Some(cli) = cli() else { return };
    let w = Workspace::new();
    for (pointer, value) in [
        ("/layouts/plate/parts/0/select/bodies", json!(["Missing"])),
        ("/layouts/plate/spacing", json!("4 deg")),
        ("/layouts/plate/spacing", json!(-1)),
        ("/layouts/plate/bed", json!([1, 1])),
        ("/layouts/plate/parts/0/rotate/0/axis", json!([0, 0, 0])),
        ("/layouts/plate/parts/0/at", json!([200, 0])),
    ] {
        let mut p = plan();
        p["layouts"]["plate"]["parts"][0]["at"] = Value::Null;
        p["requests"].as_array_mut().unwrap().truncate(1);
        *p.pointer_mut(pointer).unwrap() = value;
        let r = w.evaluate(&cli, SOURCE, p);
        assert_eq!(r["ok"], false, "{pointer}: {r}");
    }
}
#[test]
fn repeated_instances_are_selected_by_named_path_without_modifying_the_assembly() {
    let Some(cli) = cli() else { return };
    let commands = Command::new(&cli).arg("commands").output().unwrap();
    if !String::from_utf8_lossy(&commands.stdout).contains("world_name") {
        eprintln!("engine lacks exact occurrence export names: skipped");
        return;
    }
    let w = Workspace::new();
    let source = "component Cube {\nbox Solid size 4 5 6\n}\ncomponent Pair {\ninstance left = Cube\ninstance right = Cube at (20,0,0)\n}\ninstance first = Pair\ninstance second = Pair at (80,0,0)\n";
    let request = json!({"defaults":false,"requests":[{"id":"selected","kind":"export","format":"3mf","layout":{"parts":[{"id":"one","select":{"instance":["first","right"]}},{"id":"two","select":{"instance":["second","right"]},"rotate":[{"axis":[1,0,0],"angle":90}]}]}}]});
    let r = w.evaluate(&cli, source, request);
    assert_eq!(r["ok"], true, "{r}");
    assert_eq!(r["measurements"]["body_count"], 4);
    let layout = &r["request_results"][0]["metadata"]["layout"];
    assert_eq!(layout["measurements"]["body_count"], 2);
    assert!((layout["measurements"]["total"]["volume_mm3"].as_f64().unwrap() - 240.).abs() < 1e-5);
    assert_ne!(layout["placements"][0]["bodies"], layout["placements"][1]["bodies"]);
}

#[test]
fn groups_preserve_relative_geometry_and_reuse_extraction_across_layouts() {
    let Some(cli) = cli() else { return };
    let w = Workspace::new();
    let part = json!({"id":"group","select":{"bodies":["A","B"]},"at":[5,10]});
    let mut turned = part.clone();
    turned["rotate"] = json!([{"axis":[0,0,1],"angle":90}]);
    let recipe = json!({"defaults":false,"requests":[
        {"id":"first","kind":"export","format":"3mf","layout":{"parts":[part]}},
        {"id":"second","kind":"export","format":"3mf","layout":{"parts":[turned]}}
    ]});
    let report = w.evaluate(&cli, SOURCE, recipe);
    assert_eq!(report["ok"], true, "{report}");
    for request in report["request_results"].as_array().unwrap() {
        let layout = &request["metadata"]["layout"];
        assert_eq!(layout["measurements"]["body_count"], 2);
        assert!((layout["measurements"]["total"]["volume_mm3"].as_f64().unwrap() - 920.).abs() < 1e-5);
        assert_eq!(layout["placements"][0]["bounds"]["min"], json!([5., 10., 0.]));
        assert!((layout["placements"][0]["bounds"]["max"][2].as_f64().unwrap() - 17.).abs() < 1e-6);
    }
    let dir = PathBuf::from(report["request_results"][1]["directory"].as_str().unwrap());
    assert!(!dir.join("layout-inventory.commands.json").exists());
    assert!(!dir.join("layout-extract.commands.json").exists());
    let invocation: Value = serde_json::from_slice(&std::fs::read(dir.join("layout-import.invocation.json")).unwrap()).unwrap();
    assert!(!invocation["args"].as_array().unwrap().iter().any(|arg| arg == "--in"));
}

#[test]
fn solid_preparation_is_explicit_for_step_exports() {
    let Some(cli) = cli() else { return };
    let w = Workspace::new();
    let mut recipe = json!({"defaults":false,"requests":[{"id":"solid","kind":"export","format":"step","layout":{"parts":[{"id":"a","select":{"bodies":["A"]}}]}}]});
    assert!(serde_json::from_value::<Plan>(recipe.clone()).unwrap().validate().unwrap_err().contains("geometry: solid"));
    recipe["requests"][0]["layout"]["geometry"] = json!("solid");
    let report = w.evaluate(&cli, SOURCE, recipe);
    assert_eq!(report["ok"], true, "{report}");
    assert_eq!(report["request_results"][0]["metadata"]["layout"]["geometry"], "solid");
    let step = std::fs::read_to_string(report["artifacts"]["solid"].as_str().unwrap()).unwrap();
    assert!(step.contains("ISO-10303-21"));
}
