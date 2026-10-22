//! Manufacturing/presentation layouts derived from evaluated geometry. The source document
//! is never edited: evaluated geometry is snapshotted into an independent review model.
use crate::artifacts::Context;
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};
use serde_json::{Value, json};
use solidcraft_expr::{Kind, eval_with};
use std::{
    collections::{BTreeMap, HashMap, HashSet},
    path::PathBuf,
};
type R<T> = Result<T, String>;

#[derive(Clone, Debug, Serialize, Deserialize, JsonSchema)]
#[serde(untagged)]
pub enum Scalar {
    Number(f64),
    Expression(String),
}
impl Default for Scalar {
    fn default() -> Self {
        Self::Number(0.0)
    }
}
fn spacing() -> Scalar {
    Scalar::Number(5.0)
}
fn volume_tolerance() -> f64 {
    1e-4
}
fn yes() -> bool {
    true
}
#[derive(Clone, Debug, Serialize, Deserialize, JsonSchema)]
#[serde(untagged)]
pub enum LayoutRef {
    Named(String),
    Inline(Layout),
}
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Hash, Serialize, Deserialize, JsonSchema)]
#[serde(rename_all = "snake_case")]
pub enum Geometry {
    /// Preserve the export tessellation directly for printing and review.
    #[default]
    Mesh,
    /// Transfer B-rep geometry through STEP; required for STEP output.
    Solid,
}

#[derive(Clone, Debug, Serialize, Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
pub struct Layout {
    #[serde(default)]
    pub geometry: Geometry,
    pub parts: Vec<Part>,
    #[serde(default = "spacing")]
    pub spacing: Scalar,
    /// Optional rectangular bed, mm. Layout fails rather than silently overflowing.
    #[serde(default)]
    pub bed: Option<[Scalar; 2]>,
    /// Maximum relative volume difference allowed across geometry transfer (plus 1e-5 mm³ floor).
    #[serde(default = "volume_tolerance")]
    pub volume_relative_tolerance: f64,
}
#[derive(Clone, Debug, Serialize, Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
pub struct Part {
    pub id: String,
    pub select: Selection,
    /// Successive world-axis rotations about the world origin, before bounding-box placement.
    #[serde(default)]
    pub rotate: Vec<Rotation>,
    /// XY position of the rotated bounding box's lower-left corner; omit for automatic packing.
    #[serde(default)]
    pub at: Option<[Scalar; 2]>,
    #[serde(default = "yes")]
    pub on_bed: bool,
    /// Bottom clearance above the bed, or extra Z translation when on_bed is false.
    #[serde(default)]
    pub lift: Scalar,
}
#[derive(Clone, Debug, Serialize, Deserialize, JsonSchema)]
#[serde(untagged)]
pub enum Selection {
    Bodies(Bodies),
    Instance(Instance),
}
#[derive(Clone, Debug, Serialize, Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
pub struct Bodies {
    pub bodies: Vec<String>,
}
#[derive(Clone, Debug, Serialize, Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
pub struct Instance {
    /// Exact occurrence-name path, from the root. Descendant geometry is included.
    pub instance: Vec<String>,
}
#[derive(Clone, Debug, Serialize, Deserialize, JsonSchema)]
#[serde(deny_unknown_fields)]
pub struct Rotation {
    pub axis: [Scalar; 3],
    pub angle: Scalar,
}
impl Layout {
    pub fn validate(&self) -> R<()> {
        if self.parts.is_empty() || self.parts.len() > 256 {
            return Err("layout needs 1..256 parts".into());
        }
        if !self.volume_relative_tolerance.is_finite() || self.volume_relative_tolerance < 0. {
            return Err("volume_relative_tolerance must be finite and nonnegative".into());
        }
        let mut ids = HashSet::new();
        for part in &self.parts {
            if part.id.is_empty()
                || part.id.len() > 80
                || !part.id.bytes().all(|c| c.is_ascii_alphanumeric() || c == b'_' || c == b'-')
                || !ids.insert(&part.id)
            {
                return Err(format!("invalid or duplicate layout part id `{}`", part.id));
            }
            let names = match &part.select {
                Selection::Bodies(b) => &b.bodies,
                Selection::Instance(i) => &i.instance,
            };
            if names.is_empty() || names.len() > 256 || names.iter().any(|s| s.is_empty()) {
                return Err(format!("{}: selection must contain 1..256 names", part.id));
            }
            if part.rotate.len() > 64 {
                return Err(format!("{}: too many rotations", part.id));
            }
        }
        Ok(())
    }
}
struct Env(BTreeMap<String, solidcraft_expr::Value>);
impl Env {
    fn new(parameters: &Value) -> R<Self> {
        let mut values = BTreeMap::new();
        for p in parameters["parameters"].as_array().ok_or("engine parameter inventory missing")? {
            if !p["error"].is_null() {
                return Err(format!("parameter {}: {}", p["name"], p["error"]));
            }
            let name = p["name"].as_str().ok_or("missing parameter name")?;
            let v = p["value"].as_f64().ok_or("missing parameter value")?;
            let unit = p["unit"].as_str().unwrap_or("");
            let value = eval_with(&format!("{v} {unit}"), &|n| Err(solidcraft_expr::Error(format!("unknown parameter {n}")))).map_err(|e| e.0)?;
            values.insert(name.into(), value);
        }
        Ok(Self(values))
    }
    fn number(&self, s: &Scalar, kind: Kind) -> R<f64> {
        let expr = match s {
            Scalar::Number(n) => n.to_string(),
            Scalar::Expression(e) => e.clone(),
        };
        let v = eval_with(&expr, &|n| self.0.get(n).copied().ok_or_else(|| solidcraft_expr::Error(format!("unknown model parameter `{n}`"))))
            .and_then(|v| v.to_kind(kind))
            .map_err(|e| format!("layout expression `{expr}`: {e}"))?;
        if !v.is_finite() || v.abs() > 1e9 {
            return Err("layout expression exceeds supported numeric range".into());
        }
        Ok(v)
    }
}
fn command(id: &str, params: Value) -> Value {
    json!({"command":id,"params":params})
}
fn phase(ctx: &mut Context<'_>, name: &str, commands: &[Value], save: bool) -> R<Vec<Value>> {
    let script = ctx.script(commands, &format!("{name}.commands.json"))?;
    let mut args = vec!["run".into(), script.to_string_lossy().into_owned(), "--results".into(), "--quiet".into()];
    if commands.first().is_none_or(|c| c["command"] != "file.new") {
        args.extend(["--in".into(), ctx.design.to_string_lossy().into_owned()]);
    }
    let output = ctx.dir.join(format!("{name}.solidcraft"));
    if save {
        args.extend(["--save".into(), output.to_string_lossy().into_owned()]);
    }
    let result = ctx.run(&args, name)?;
    std::fs::write(ctx.dir.join(format!("{name}.results.json")), serde_json::to_vec_pretty(&result).map_err(|e| e.to_string())?)
        .map_err(|e| e.to_string())?;
    let results = result.get("results").unwrap_or(&result).as_array().ok_or("engine results must be an array")?.clone();
    if results.len() != commands.len() {
        return Err("engine returned incomplete layout results".into());
    }
    if save {
        if !output.is_file() {
            return Err("engine did not save prepared layout".into());
        }
        ctx.design = output;
    }
    Ok(results)
}
fn instance_path(inventory: &Value, names: &[String]) -> R<Vec<u64>> {
    let components = inventory["components"].as_array().ok_or("engine component inventory missing")?;
    let (mut parent, mut path) = (0, vec![]);
    for name in names {
        let node = components.iter().find(|c| c["id"].as_u64() == Some(parent)).ok_or("missing parent component")?;
        let children: Vec<_> = node["children"].as_array().into_iter().flatten().collect();
        let mut matches: Vec<_> = children.iter().filter(|o| o["name"].as_str() == Some(name)).collect();
        if matches.is_empty() && parent != 0 {
            let qualified = crate::assembly::qualified(node["name"].as_str().ok_or("missing component name")?, name);
            matches = children.iter().filter(|o| o["name"].as_str() == Some(&qualified)).collect();
        }
        let [o] = matches.as_slice() else { return Err(format!("instance path segment `{name}` is missing or ambiguous")) };
        path.push(o["id"].as_u64().ok_or("missing occurrence id")?);
        parent = o["component"].as_u64().ok_or("missing component id")?;
    }
    Ok(path)
}
#[derive(Clone, Copy, Debug, Serialize)]
struct Bounds {
    min: [f64; 3],
    max: [f64; 3],
}
fn bounds(value: &Value) -> R<Bounds> {
    let read = |key: &str| -> R<[f64; 3]> {
        let a = value[key].as_array().ok_or("missing layout bounds")?;
        if a.len() != 3 {
            return Err("invalid layout bounds".into());
        }
        let mut result = [0.; 3];
        for (slot, n) in result.iter_mut().zip(a) {
            *slot = n.as_f64().filter(|n| n.is_finite() && n.abs() <= 1e9).ok_or("nonfinite or excessive layout bounds")?;
        }
        Ok(result)
    };
    let b = Bounds { min: read("min")?, max: read("max")? };
    if (0..3).any(|i| b.max[i] < b.min[i]) {
        return Err("inverted layout bounds".into());
    }
    Ok(b)
}
fn separated(a: Bounds, b: Bounds, gap: f64) -> bool {
    (0..2).any(|i| a.max[i] + gap <= b.min[i] + 1e-7 || b.max[i] + gap <= a.min[i] + 1e-7)
}
fn placed(b: Bounds, at: [f64; 2], z: f64) -> Bounds {
    Bounds { min: [at[0], at[1], z], max: [at[0] + b.max[0] - b.min[0], at[1] + b.max[1] - b.min[1], z + b.max[2] - b.min[2]] }
}
fn arrange(layout: &Layout, env: &Env, input: &[Bounds]) -> R<Vec<Bounds>> {
    let gap = env.number(&layout.spacing, Kind::Length)?;
    if gap < 0. {
        return Err("layout spacing must be nonnegative".into());
    }
    let bed = layout.bed.as_ref().map(|a| Ok::<_, String>([env.number(&a[0], Kind::Length)?, env.number(&a[1], Kind::Length)?])).transpose()?;
    if bed.is_some_and(|b| b.iter().any(|v| *v <= 0.)) {
        return Err("bed dimensions must be positive".into());
    }
    let area: f64 = input.iter().map(|b| (b.max[0] - b.min[0] + gap) * (b.max[1] - b.min[1] + gap)).sum();
    let widest = input.iter().map(|b| b.max[0] - b.min[0]).fold(0., f64::max);
    let width = bed.map_or(area.sqrt().max(widest), |b| b[0]);
    let mut output: Vec<Option<Bounds>> = vec![None; input.len()];
    let fits = |b: Bounds| bed.is_none_or(|bed| b.min[0] >= -1e-7 && b.min[1] >= -1e-7 && b.max[0] <= bed[0] + 1e-7 && b.max[1] <= bed[1] + 1e-7);
    // Reserve manual placements first, regardless of their order in the recipe.
    for manual in [true, false] {
        for (i, (part, b)) in layout.parts.iter().zip(input).enumerate() {
            if part.at.is_some() != manual {
                continue;
            }
            let lift = env.number(&part.lift, Kind::Length)?;
            if lift < 0. {
                return Err(format!("{}: lift must be nonnegative", part.id));
            }
            let z = if part.on_bed { lift } else { b.min[2] + lift };
            let taken: Vec<_> = output.iter().flatten().copied().collect();
            let target = if let Some(at) = &part.at {
                placed(*b, [env.number(&at[0], Kind::Length)?, env.number(&at[1], Kind::Length)?], z)
            } else {
                let mut rows = vec![0.];
                rows.extend(taken.iter().map(|b| b.max[1] + gap));
                rows.sort_by(f64::total_cmp);
                let mut found = None;
                for y in rows {
                    let mut x = 0.;
                    for _ in 0..=taken.len() {
                        let candidate = placed(*b, [x, y], z);
                        if candidate.max[0] > width + 1e-7 || !fits(candidate) {
                            break;
                        }
                        if let Some(hit) = taken.iter().find(|a| !separated(**a, candidate, gap)) {
                            x = hit.max[0] + gap;
                        } else {
                            found = Some(candidate);
                            break;
                        }
                    }
                    if found.is_some() {
                        break;
                    }
                }
                found.ok_or_else(|| format!("{}: cannot fit part on bed with requested spacing; enlarge the bed or split the layout", part.id))?
            };
            if !fits(target) || taken.iter().any(|a| !separated(*a, target, gap)) {
                return Err(format!("{}: manual placement overlaps another part/spacing or exceeds bed", part.id));
            }
            if target.min.iter().chain(&target.max).any(|v| v.abs() > 1e9) {
                return Err("layout placement exceeds numeric range".into());
            }
            output[i] = Some(target);
        }
    }
    output.into_iter().map(|b| b.ok_or_else(|| "missing layout placement".into())).collect()
}

#[derive(Default)]
pub(crate) struct SourceCache {
    inventory: Option<Vec<Value>>,
    solids: HashMap<(Geometry, Vec<String>), PathBuf>,
}

pub(crate) fn prepare(ctx: &mut Context<'_>, layout: &Layout, cache: &mut SourceCache) -> R<Value> {
    layout.validate()?;
    let inventory = if let Some(inventory) = &cache.inventory {
        inventory.clone()
    } else {
        let inventory = phase(
            ctx,
            "layout-inventory",
            &[command("parameters.list", json!({"filter":"user"})), command("inspect.measure", json!({})), command("component.list", json!({}))],
            false,
        )?;
        cache.inventory = Some(inventory.clone());
        inventory
    };
    let env = Env::new(&inventory[0])?;
    let known: HashSet<_> = inventory[1]["bodies"].as_array().ok_or("missing measured bodies")?.iter().filter_map(|b| b["name"].as_str()).collect();
    let mut probes = vec![];
    for part in &layout.parts {
        if let Selection::Instance(instance) = &part.select {
            let path = instance_path(&inventory[2], &instance.instance)?;
            probes.push(command("occurrence.properties", json!({"occurrence":path.last(),"path":path})));
        }
    }
    let properties = if probes.is_empty() { vec![] } else { phase(ctx, "layout-selections", &probes, false)? };
    let mut properties = properties.iter();
    let mut selections = vec![];
    let (format, import_command) = match layout.geometry {
        Geometry::Mesh => ("3mf", "file.insert_mesh"),
        Geometry::Solid => ("step", "file.insert_step"),
    };
    let mut exports = vec![];
    let mut files: Vec<PathBuf> = vec![];
    let mut extracted = vec![];
    for (i, part) in layout.parts.iter().enumerate() {
        let names: Vec<String> = match &part.select {
            Selection::Bodies(b) => b.bodies.clone(),
            Selection::Instance(_) => properties.next().ok_or("missing instance properties")?["bodies"]
                .as_array()
                .ok_or("missing instance bodies")?
                .iter()
                .map(|b| {
                    b["world_name"].as_str().map(str::to_owned).ok_or_else(|| {
                        "instance selection requires an engine exposing world_name in occurrence.properties; use a current development build".into()
                    })
                })
                .collect::<R<_>>()?,
        };
        if names.is_empty() || names.iter().any(|n| !known.contains(n.as_str())) {
            return Err(format!("{}: selected body is missing or instance is empty", part.id));
        }
        let file = if let Some(file) = cache.solids.get(&(layout.geometry, names.clone())) {
            file.clone()
        } else {
            let file = ctx.dir.join(format!("layout-part-{i}.{format}"));
            exports.push(command("file.export", json!({"format":format,"path":file,"bodies":names})));
            extracted.push((names.clone(), file.clone()));
            file
        };
        selections.push(names);
        files.push(file);
    }
    if !exports.is_empty() {
        phase(ctx, "layout-extract", &exports, false)?;
    }
    for (names, file) in extracted {
        cache.solids.insert((layout.geometry, names), file);
    }
    let mut imports = vec![command("file.new", json!({"name":"Prepared layout"}))];
    for (i, file) in files.iter().enumerate() {
        imports.push(command("component.activate", json!({"component":0})));
        imports.push(command(import_command, json!({"name":format!("layout_{i}"),"path":file})));
    }
    let result = phase(ctx, "layout-import", &imports, true)?;
    let mut occurrences = vec![];
    let mut rotations = vec![];
    for (i, part) in layout.parts.iter().enumerate() {
        let occurrence = result.get(2 + 2 * i).and_then(|r| r["occurrence"].as_u64()).ok_or("import did not return an occurrence")?;
        occurrences.push(occurrence);
        for r in &part.rotate {
            let axis = [env.number(&r.axis[0], Kind::Unitless)?, env.number(&r.axis[1], Kind::Unitless)?, env.number(&r.axis[2], Kind::Unitless)?];
            if axis.iter().map(|v| v * v).sum::<f64>() < 1e-24 {
                return Err(format!("{}: rotation axis must be nonzero", part.id));
            }
            rotations.push(command("occurrence.move", json!({"occurrence":occurrence,"axis":axis,"angle":env.number(&r.angle,Kind::Angle)?})));
        }
    }
    for id in &occurrences {
        rotations.push(command("occurrence.properties", json!({"occurrence":id})));
    }
    let rotated = phase(ctx, "layout-orient", &rotations, true)?;
    let input: Vec<_> = rotated.iter().skip(rotated.len() - occurrences.len()).map(|p| bounds(&p["total"]["bbox"])).collect::<R<_>>()?;
    // Snapshot/import is an evaluated geometry boundary, not source feature history.
    // Refuse a partial import rather than publishing a layout with missing or changed solids.
    let mut transfer = vec![];
    for ((names, measured), file) in selections.iter().zip(rotated.iter().skip(rotated.len() - occurrences.len())).zip(&files) {
        let mut expected = 0.;
        for name in names {
            let body = inventory[1]["bodies"]
                .as_array()
                .into_iter()
                .flatten()
                .find(|b| b["name"].as_str() == Some(name))
                .ok_or("missing selected measurement")?;
            expected += body["volume_mm3"].as_f64().ok_or("selected body has no solid volume")?;
        }
        let actual = measured["total"]["volume_mm3"].as_f64().ok_or("imported body has no volume")?;
        // Compare mesh import to the actual exported triangles, not the B-rep integrator's
        // volume. Tessellation error and transfer error are different quantities.
        let reference = if layout.geometry == Geometry::Mesh { mesh_volume(file)? } else { expected };
        let tolerance = 1e-5_f64.max(reference.abs() * layout.volume_relative_tolerance);
        transfer.push(json!({"bodies":names,"source_volume_mm3":expected,"reference_volume_mm3":reference,
            "tessellation_difference_mm3":reference-expected,"prepared_volume_mm3":actual,
            "difference_mm3":actual-reference,"tolerance_mm3":tolerance}));
        if measured["body_count"].as_u64() != Some(names.len() as u64) || (actual - reference).abs() > tolerance {
            return Err(format!(
                "layout geometry import changed body count or volume: expected {} bodies/{reference} mm³, got {}/{actual} mm³",
                names.len(),
                measured["body_count"]
            ));
        }
    }
    let output = arrange(layout, &env, &input)?;
    let mut moves = vec![];
    let mut placements = vec![];
    for (i, ((part, b), target)) in layout.parts.iter().zip(&input).zip(&output).enumerate() {
        let delta: [f64; 3] = std::array::from_fn(|axis| target.min[axis] - b.min[axis]);
        moves.push(command("occurrence.move", json!({"occurrence":occurrences[i],"translate":delta})));
        placements.push(json!({"id":part.id,"bodies":selections[i],"occurrence":occurrences[i],"translation":delta,"bounds":target}));
    }
    moves.push(command("inspect.measure", json!({})));
    let result = phase(ctx, "layout-prepared", &moves, true)?;
    let report = json!({"design":ctx.design,"placements":placements,"measurements":result.last(),"geometry":layout.geometry,"parameters":inventory[0]["parameters"],"geometry_transfer":transfer});
    std::fs::write(ctx.dir.join("layout.json"), serde_json::to_vec_pretty(&report).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
    Ok(report)
}

/// Read only the flat world-space 3MF snapshots written by the engine. General user 3MF
/// files still go through engine import, so unsupported scene structures are never guessed.
fn mesh_volume(path: &std::path::Path) -> R<f64> {
    use std::io::Read;
    let file = std::fs::File::open(path).map_err(|e| e.to_string())?;
    let mut archive = zip::ZipArchive::new(file).map_err(|e| e.to_string())?;
    let entry = archive.by_name("3D/3dmodel.model").map_err(|e| e.to_string())?;
    let mut xml = String::new();
    entry.take(128 * 1024 * 1024 + 1).read_to_string(&mut xml).map_err(|e| e.to_string())?;
    if xml.len() > 128 * 1024 * 1024 {
        return Err("layout mesh snapshot exceeds 128 MiB XML limit".into());
    }
    let doc = roxmltree::Document::parse(&xml).map_err(|e| format!("invalid engine 3MF snapshot: {e}"))?;
    let root = doc.root_element();
    if root.attribute("unit") != Some("millimeter") {
        return Err("engine mesh snapshot must use millimetres".into());
    }
    let mut volumes = HashMap::new();
    for object in root.descendants().filter(|n| n.has_tag_name("object")) {
        let id = object.attribute("id").ok_or("missing 3MF object id")?;
        let mesh = object.children().find(|n| n.has_tag_name("mesh")).ok_or("engine snapshot must contain flat mesh objects")?;
        let vertices: Vec<[f64; 3]> = mesh
            .descendants()
            .filter(|n| n.has_tag_name("vertex"))
            .map(|n| {
                let mut p = [0.; 3];
                for (i, key) in ["x", "y", "z"].iter().enumerate() {
                    p[i] = n.attribute(*key).and_then(|s| s.parse::<f64>().ok()).filter(|v| v.is_finite()).ok_or("invalid 3MF vertex")?;
                }
                Ok::<_, String>(p)
            })
            .collect::<R<_>>()?;
        if vertices.is_empty() {
            return Err("empty 3MF snapshot mesh".into());
        }
        let mut volume = 0.;
        for triangle in mesh.descendants().filter(|n| n.has_tag_name("triangle")) {
            let mut points = [[0.; 3]; 3];
            for (i, key) in ["v1", "v2", "v3"].iter().enumerate() {
                let p = triangle.attribute(*key).and_then(|s| s.parse::<usize>().ok()).and_then(|i| vertices.get(i)).ok_or("invalid 3MF triangle")?;
                // Match the engine's origin-zero integral. Rebasing an open mesh
                // changes this quantity even when its triangles transfer exactly.
                points[i] = *p;
            }
            let [a, b, c] = points;
            volume += (a[0] * (b[1] * c[2] - b[2] * c[1]) + a[1] * (b[2] * c[0] - b[0] * c[2]) + a[2] * (b[0] * c[1] - b[1] * c[0])) / 6.;
        }
        if !volume.is_finite() || volumes.insert(id, volume.abs()).is_some() {
            return Err("invalid 3MF mesh volume or duplicate object id".into());
        }
    }
    let build = root.children().find(|n| n.has_tag_name("build")).ok_or("missing 3MF build")?;
    let mut total = 0.;
    for item in build.children().filter(|n| n.has_tag_name("item")) {
        if item.attribute("transform").is_some() {
            return Err("engine mesh snapshot must bake world transforms into vertices".into());
        }
        total += volumes.get(item.attribute("objectid").ok_or("missing 3MF object reference")?).ok_or("unknown 3MF object")?;
    }
    if !total.is_finite() {
        return Err("nonfinite mesh volume".into());
    }
    Ok(total)
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    #[test]
    fn mesh_transfer_reference_matches_engine_origin_for_open_surfaces() {
        use std::io::Write;
        let path = std::env::temp_dir().join(format!("layout-volume-{}.3mf", std::process::id()));
        let mut zip = zip::ZipWriter::new(std::fs::File::create(&path).unwrap());
        zip.start_file("3D/3dmodel.model", zip::write::SimpleFileOptions::default()).unwrap();
        zip.write_all(
            br#"<model unit="millimeter"><resources><object id="1"><mesh><vertices>
            <vertex x="1" y="0" z="0"/><vertex x="0" y="2" z="0"/><vertex x="0" y="0" z="3"/>
            </vertices><triangles><triangle v1="0" v2="1" v3="2"/></triangles></mesh></object>
            </resources><build><item objectid="1"/></build></model>"#,
        )
        .unwrap();
        zip.finish().unwrap();
        // The engine reports 1; rebasing to the first vertex incorrectly reports 0.
        assert_eq!(mesh_volume(&path).unwrap(), 1.);
        std::fs::remove_file(path).unwrap();
    }
    #[test]
    fn packing_reserves_manual_placements_before_automatic_parts() {
        let layout: Layout = serde_json::from_value(json!({"bed":[10,10],"spacing":1,"parts":[
            {"id":"automatic","select":{"bodies":["A"]}},
            {"id":"manual","select":{"bodies":["B"]},"at":[0,0]}
        ]}))
        .unwrap();
        let b = Bounds { min: [30., 40., 8.], max: [32., 42., 9.] };
        let result = arrange(&layout, &Env(BTreeMap::new()), &[b, b]).unwrap();
        assert_eq!(result[1].min, [0., 0., 0.]);
        assert_eq!(result[0].min, [3., 0., 0.]);
        let mut overlapping = layout.clone();
        overlapping.parts[0].at = Some([Scalar::Number(0.), Scalar::Number(0.)]);
        assert!(arrange(&overlapping, &Env(BTreeMap::new()), &[b, b]).is_err());
    }
    #[test]
    fn retained_z_lift_and_zero_spacing_have_explicit_semantics() {
        let layout: Layout = serde_json::from_value(json!({"spacing":0,"parts":[
            {"id":"part","select":{"bodies":["A"]},"at":[2,3],"on_bed":false,"lift":"2 mm"}
        ]}))
        .unwrap();
        let b = Bounds { min: [30., 40., 8.], max: [32., 42., 9.] };
        let result = arrange(&layout, &Env(BTreeMap::new()), &[b]).unwrap();
        assert_eq!(result[0].min, [2., 3., 10.]);
        assert_eq!(result[0].max, [4., 5., 11.]);
    }
}
