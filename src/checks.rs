//! `check` statements: properties of the built bodies that `evaluate` verifies, so a design
//! that builds but comes out wrong fails instead of passing silently.
//!
//! ```sc
//! check Holder volume >= 20000 mm3
//! check Cap clearance Bottle >= 0.2 mm
//! check Holder overhang <= 45 deg up X
//! ```
//!
//! Every body also gets an implied `check <body> shells == 1`: a join that doesn't touch the
//! body, or a cut that slices it in two, leaves it in several pieces without an engine error.
//! A body with a sealed void has two shells too, so it states `check Body shells == 2`.
use crate::ast::{Check as Stmt, Comp, Design};
use crate::expressions::{Environment, text};
use serde_json::{Value, json};
use solidcraft_expr::Kind;
use std::collections::HashMap;

/// The longest bridge an overhang check accepts unless it says otherwise: most printers bridge
/// 10 mm cleanly.
pub const DEFAULT_BRIDGE: f64 = 10.0;
/// Faces this close to the bed print as part of the first layer.
const FIRST_LAYER: f64 = 0.3;
/// A ledge this narrow is spanned by the extrusion lines themselves.
const NARROW_LEDGE: f64 = 1.0;
/// A region edge is held when the face beyond it bends back under the region by more than this
/// (sin 1 deg), so near-flat facets of a convex surface don't count.
const HELD_BEND: f64 = 0.0175;
/// Meshes approximate curved and sloped faces; a facet may lean this far past the limit.
const MESH_SLACK_DEG: f64 = 0.5;

#[derive(Debug, Clone)]
pub struct Check {
    pub line: usize,
    pub body: String,
    pub quantity: String,
    pub other: Option<String>,
    pub op: String,
    /// In mm, mm², mm³, g or degrees, by quantity.
    pub value: f64,
    /// Unit length print direction of an overhang check.
    pub up: [f64; 3],
    /// The longest bridge an overhang check accepts, in mm.
    pub bridge: f64,
}

impl Check {
    pub fn unit(&self) -> &'static str {
        match self.quantity.as_str() {
            "volume" => "mm3",
            "area" => "mm2",
            "mass" => "g",
            "overhang" => "deg",
            "shells" => "",
            _ => "mm",
        }
    }
    fn holds(&self, actual: f64) -> bool {
        // Measurements are rounded by the engine and the mesh, so comparisons allow a little:
        // a micron on lengths (the engine's nearest distances are a few hundredths of one off).
        let eps = if self.unit() == "mm" { 1e-3 } else { 1e-6 * self.value.abs().max(1.0) };
        match self.op.as_str() {
            "==" => (actual - self.value).abs() <= eps,
            "<=" => actual <= self.value + eps,
            ">=" => actual >= self.value - eps,
            "<" => actual < self.value,
            _ => actual > self.value,
        }
    }
    /// Needs an engine query after the build: clearance, or a mesh for overhang.
    pub fn needs_engine(&self) -> bool {
        matches!(self.quantity.as_str(), "clearance" | "overhang")
    }
}

/// Evaluate each check's value with the parameters' defaults.
pub fn lower(d: &Design) -> Result<Vec<Check>, String> {
    if d.checks.is_empty() {
        return Ok(vec![]);
    }
    let env = Environment::new(d)?;
    d.checks.iter().map(|s| lower_one(s, &env, d)).collect()
}

fn lower_one(s: &Stmt, env: &Environment, d: &Design) -> Result<Check, String> {
    let fail = |e: String| format!("line {}: check: {e}", s.line);
    let v = env.eval(&s.value).map_err(|e| fail(format!("`{}`: {e}", s.value)))?;
    let wrong = |want: &str| fail(format!("{} takes {want}, not `{}`", s.quantity, s.value));
    // `dims`: the length dimension an expression may have instead of a unit word.
    let scale = |units: &[(&str, f64)], dims: Option<i8>, want: &str| -> Result<f64, String> {
        match &s.unit {
            Some(u) => match units.iter().find(|(n, _)| n == u) {
                Some((_, k)) if v.unitless() => Ok(v.v * k),
                _ => Err(wrong(want)),
            },
            None if v.unitless() || (Some(v.len) == dims && v.ang == 0) => Ok(v.v),
            None => Err(wrong(want)),
        }
    };
    let value = match s.quantity.as_str() {
        "shells" if s.unit.is_none() && v.unitless() && v.v >= 1.0 && v.v.fract() == 0.0 => v.v,
        "shells" => return Err(wrong("a whole number of shells, 1 or more")),
        "volume" => scale(&[("mm3", 1.0), ("cm3", 1000.0)], Some(3), "a volume: mm3, cm3 or a length cubed")?,
        "area" => scale(&[("mm2", 1.0)], Some(2), "an area: mm2 or a length squared")?,
        "mass" => scale(&[("g", 1.0), ("kg", 1000.0)], None, "a mass in g or kg")?,
        "overhang" if s.unit.is_none() => v.to_kind(Kind::Angle).map_err(|_| wrong("an angle"))?.to_degrees(),
        _ if s.unit.is_none() => v.to_kind(Kind::Length).map_err(|_| wrong("a length"))?,
        _ => return Err(wrong("a length")),
    };
    if s.quantity == "overhang" && !matches!(s.op.as_str(), "<=" | "<") {
        return Err(fail("an overhang check sets a limit: `overhang <= 45 deg`".into()));
    }
    // An overhang check without `up` prints the way its body's `print` says, else Z up.
    let up = if !s.up.is_empty() {
        direction(&s.up, env).ok_or_else(|| fail("`up` takes X, Y, Z or a direction (x,y,z)".into()))?
    } else if let Some(p) = d.prints.iter().find(|p| p.bodies.contains(&s.body)) {
        direction(&p.up, env).ok_or_else(|| format!("line {}: print: `up` takes X, Y, Z or a direction (x,y,z)", p.line))?
    } else {
        [0.0, 0.0, 1.0]
    };
    Ok(Check {
        line: s.line,
        body: s.body.clone(),
        quantity: s.quantity.clone(),
        other: s.other.clone(),
        op: s.op.clone(),
        value,
        up,
        bridge: match &s.bridge {
            Some(b) => env.number(b, Kind::Length, s.line, "check: bridge")?,
            None => DEFAULT_BRIDGE,
        },
    })
}

/// A unit direction from `X`/`Y`/`Z` or `(x, y, z)` components, evaluated with the parameters.
pub fn direction(comps: &[Comp], env: &Environment) -> Option<[f64; 3]> {
    if comps.len() != 3 {
        return None;
    }
    let mut v = [0.0; 3];
    for (i, c) in comps.iter().enumerate() {
        v[i] = env.eval(text(c)).ok().filter(|v| v.unitless())?.v;
    }
    let n = v.iter().map(|x| x * x).sum::<f64>().sqrt();
    (n > 1e-9).then(|| v.map(|x| x / n))
}

/// The print layout the design's `print` statements describe: each group turned so its `up`
/// points away from the bed, then arranged automatically.
pub fn print_layout(d: &Design) -> Result<Option<Value>, String> {
    if d.prints.is_empty() {
        return Ok(None);
    }
    let env = Environment::new(d)?;
    let mut parts = Vec::new();
    for p in &d.prints {
        let up = direction(&p.up, &env).ok_or_else(|| format!("line {}: print: `up` takes X, Y, Z or a direction (x,y,z)", p.line))?;
        // The rotation taking `up` to +Z: about up × Z by the angle between them.
        let rotate = if up[2] > 1.0 - 1e-12 {
            vec![]
        } else if up[2] < -1.0 + 1e-12 {
            vec![json!({"axis": [1, 0, 0], "angle": "180 deg"})]
        } else {
            let axis = [up[1], -up[0], 0.0];
            vec![json!({"axis": axis.map(round), "angle": format!("{} deg", round(up[2].acos().to_degrees()))})]
        };
        parts.push(json!({"id": p.bodies.join("-"), "select": {"bodies": p.bodies}, "rotate": rotate}));
    }
    let mut layout = json!({"parts": parts});
    if let Some((bed, line)) = &d.print_bed {
        let mut size = Vec::new();
        for c in bed {
            size.push(env.number(text(c), Kind::Length, *line, "print bed")?);
        }
        layout["bed"] = json!(size);
    }
    Ok(Some(layout))
}

/// The engine query answering a clearance check. Pairs nearer than `clearance` come back with
/// the closest points, to say where a failing check is tight.
pub fn clearance_command(c: &Check) -> Value {
    json!({"command": "inspect.clearance", "params": {"bodies": [c.body], "against": [c.other], "nearest": 1, "clearance": c.value.max(0.0) + 1.0}})
}

/// One check's outcome: the report entry, and a failure message when it fails.
pub struct Outcome {
    pub entry: Value,
    pub failure: Option<String>,
    /// The failure rests on an approximate measurement, so it is reported as a warning.
    pub approximate: bool,
}

/// What the engine told us about the built design.
pub struct Evidence<'a> {
    /// `inspect.measure`'s `bodies`.
    pub bodies: &'a [Value],
    /// `inspect.clearance` replies, one per clearance check in order (null where not asked).
    pub clearances: &'a [Value],
    /// Binary STL of a body, for an overhang check.
    pub mesh: &'a dyn Fn(&str) -> Result<Vec<u8>, String>,
}

/// Verify the design's checks, plus the implied one-piece check for every body that has no
/// explicit `shells` check.
pub fn verify(checks: &[Check], ev: &Evidence) -> Vec<Outcome> {
    let body = |name: &str| ev.bodies.iter().find(|b| b["name"] == name);
    let names = || ev.bodies.iter().filter_map(|b| b["name"].as_str()).collect::<Vec<_>>().join(", ");
    let mut out = Vec::new();
    let mut clearances = ev.clearances.iter();
    for c in checks {
        let statement = describe(c);
        let mut detail = json!({});
        let reply = if c.quantity == "clearance" { clearances.next().cloned().unwrap_or(Value::Null) } else { Value::Null };
        let missing = [Some(c.body.as_str()), c.other.as_deref()].into_iter().flatten().find(|n| body(n).is_none());
        let actual: Result<f64, String> = match (missing, body(&c.body)) {
            (None, Some(b)) => match c.quantity.as_str() {
                "shells" => b["kernel"]["shells"].as_f64().ok_or_else(|| "the engine did not report shells".into()),
                "volume" => b["volume_mm3"].as_f64().ok_or_else(|| "the body is not closed, so it has no volume".into()),
                "area" => b["area_mm2"].as_f64().ok_or_else(|| "the engine did not report an area".into()),
                "mass" => b["mass_g"].as_f64().ok_or_else(|| "the body is not closed, so it has no mass".into()),
                "clearance" => clearance(c, &reply, &mut detail),
                "overhang" => (ev.mesh)(&c.body).and_then(|stl| overhang(&stl, c.up, c.value, c.bridge, &mut detail)),
                q => {
                    let axis = match q {
                        "size.x" => 0,
                        "size.y" => 1,
                        _ => 2,
                    };
                    b["bbox"]["max"][axis]
                        .as_f64()
                        .zip(b["bbox"]["min"][axis].as_f64())
                        .map(|(hi, lo)| hi - lo)
                        .ok_or_else(|| "the engine did not report a bounding box".into())
                }
            },
            (name, _) => Err(format!("there is no body named `{}` (bodies: {})", name.unwrap_or(&c.body), names())),
        };
        out.push(outcome(c, &statement, actual, detail));
    }
    for b in ev.bodies {
        let (Some(name), Some(shells)) = (b["name"].as_str(), b["kernel"]["shells"].as_f64()) else { continue };
        if checks.iter().any(|c| c.body == name && c.quantity == "shells") {
            continue;
        }
        let passed = shells == 1.0;
        out.push(Outcome {
            approximate: false,
            entry: json!({"statement": format!("check {name} shells == 1"), "implied": true, "body": name, "quantity": "shells", "op": "==", "expected": 1, "actual": shells, "passed": passed}),
            failure: (!passed).then(|| {
                format!(
                    "`{name}` came out in {shells} shells: a join that doesn't touch it, a cut that splits it, or a sealed void. \
                     If that is intended, say so with `check {name} shells == {shells}`"
                )
            }),
        });
    }
    out
}

fn describe(c: &Check) -> String {
    let other = c.other.as_ref().map(|o| format!(" {o}")).unwrap_or_default();
    let unit = match c.unit() {
        "" => String::new(),
        u => format!(" {u}"),
    };
    let up = if c.quantity != "overhang" {
        String::new()
    } else if c.bridge == DEFAULT_BRIDGE {
        format!(" up ({}, {}, {})", round(c.up[0]), round(c.up[1]), round(c.up[2]))
    } else {
        format!(" up ({}, {}, {}) bridge {} mm", round(c.up[0]), round(c.up[1]), round(c.up[2]), round(c.bridge))
    };
    format!("check {} {}{other} {} {}{unit}{up}", c.body, c.quantity, c.op, round(c.value))
}

/// `(x, y, z)` to the hundredth of a millimetre.
fn point(v: &Value) -> String {
    let c: Vec<String> = (0..3).map(|i| format!("{}", (v[i].as_f64().unwrap_or(0.0) * 100.0).round() / 100.0)).collect();
    format!("({})", c.join(", "))
}

fn round(x: f64) -> f64 {
    (x * 1000.0).round() / 1000.0
}

fn outcome(c: &Check, statement: &str, actual: Result<f64, String>, mut detail: Value) -> Outcome {
    let (actual, passed, failure) = match actual {
        Err(e) => (Value::Null, false, Some(format!("{statement}: {e}"))),
        Ok(a) => {
            // Overlapping bodies have no clearance; any lower bound fails.
            let overlap = detail["overlap_mm3"].is_number() && matches!(c.op.as_str(), ">=" | ">");
            // An overhang check passes when no unsupported area is left; `a` is the steepest face.
            let passed = if c.quantity == "overhang" { detail["area_mm2"].as_f64() == Some(0.0) } else { !overlap && c.holds(a) };
            let unit = c.unit();
            let why = if overlap {
                format!("the bodies overlap by {:.3} mm3", detail["overlap_mm3"].as_f64().unwrap_or(0.0))
            } else if c.quantity == "overhang" {
                let span = |i: usize| {
                    let (lo, hi) = (detail["at"]["min"][i].as_f64().unwrap_or(0.0), detail["at"]["max"][i].as_f64().unwrap_or(0.0));
                    if (hi - lo).abs() < 0.05 { format!("{}", round(lo)) } else { format!("{}..{}", round(lo), round(hi)) }
                };
                format!(
                    "{:.1} mm2 overhangs more than {} deg (up to {:.1} deg), within x {}, y {}, z {}",
                    detail["area_mm2"].as_f64().unwrap_or(0.0),
                    round(c.value),
                    a,
                    span(0),
                    span(1),
                    span(2)
                )
            } else if c.quantity == "clearance" && detail["from"].is_array() {
                format!("got {} mm, closest between {} and {}", round(a), point(&detail["from"]), point(&detail["to"]))
            } else {
                format!("got {}{}{unit}", round(a), if unit.is_empty() { "" } else { " " })
            };
            (json!(round(a)), passed, (!passed).then(|| format!("{statement}: {why}")))
        }
    };
    detail["statement"] = json!(statement);
    detail["line"] = json!(c.line);
    detail["body"] = json!(c.body);
    detail["quantity"] = json!(c.quantity);
    detail["op"] = json!(c.op);
    detail["expected"] = json!(round(c.value));
    detail["unit"] = json!(c.unit());
    detail["actual"] = actual;
    detail["passed"] = json!(passed);
    // A failure the engine could only measure on the meshes may be the meshes' error.
    let approximate = failure.is_some() && detail["exact"] == false;
    let failure = failure
        .map(|f| if approximate { format!("{f} (measured on the meshes, not exactly: check this fit by hand or with a test print)") } else { f });
    Outcome { entry: detail, failure, approximate }
}

/// Smallest distance between the two bodies; 0 (and `overlap_mm3`) when they overlap.
fn clearance(c: &Check, reply: &Value, detail: &mut Value) -> Result<f64, String> {
    let other = c.other.as_deref().unwrap_or("");
    let pair = |v: &Value| (v["a"] == c.body.as_str() && v["b"] == other) || (v["a"] == other && v["b"] == c.body.as_str());
    if let Some(o) = reply["overlaps"].as_array().into_iter().flatten().find(|v| pair(v)) {
        detail["overlap_mm3"] = o["volume_mm3"].clone();
        detail["at"] = o["center"].clone();
        return Ok(0.0);
    }
    if let Some(u) = reply["unchecked"].as_array().into_iter().flatten().find(|v| pair(v)) {
        return Err(format!("the engine could not check this pair: {}", u["reason"]));
    }
    let near = reply["near"].as_array().into_iter().flatten().find(|v| pair(v));
    let nearest = reply["nearest"][c.body.as_str()].as_array().into_iter().flatten().find(|v| v["body"] == other);
    match near.and_then(|v| v["distance_mm"].as_f64()).or_else(|| nearest.and_then(|v| v["distance_mm"].as_f64())) {
        Some(d) => {
            if let Some(n) = near {
                detail["from"] = n["from"].clone();
                detail["to"] = n["to"].clone();
                // Measured on the meshes when the engine couldn't do it exactly.
                if n["exact"] == false {
                    detail["exact"] = json!(false);
                }
            }
            Ok(d)
        }
        None => Err(format!("the engine reported no distance between `{}` and `{other}`", c.body)),
    }
}

/// Overhang analysis of a body's binary STL printed along `up`. Faces pointing down more than
/// `limit` degrees from vertical need support, except:
/// - faces in the first layer, which the bed holds;
/// - ledges under 1 mm across (twice the area over the perimeter), which the extrusion lines span;
/// - bridges at most `bridge` mm long: a downward region where a straight line through every
///   point reaches a held edge both ways (a window's top, a hole's crown or ceiling, a
///   counterbore's). A region edge is held when it is concave: the face beyond it bends back
///   under the region, as a wall carrying on below it does.
///
/// Returns the steepest face left needing support (or the steepest face, when none is), and
/// records that area, where it is, and the bridges accepted.
fn overhang(stl: &[u8], up: [f64; 3], limit: f64, bridge: f64, detail: &mut Value) -> Result<f64, String> {
    let n = stl.get(80..84).map(|b| u32::from_le_bytes([b[0], b[1], b[2], b[3]]) as usize).ok_or("truncated STL")?;
    if n.checked_mul(50).and_then(|v| v.checked_add(84)) != Some(stl.len()) {
        return Err("the body's mesh is not a valid binary STL".into());
    }
    let f = |i: usize| f32::from_le_bytes([stl[i], stl[i + 1], stl[i + 2], stl[i + 3]]) as f64;
    let dot = |a: [f64; 3], b: [f64; 3]| a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    // Weld the triangles' corners so neighbouring faces share edges.
    let mut ids = HashMap::new();
    let mut points: Vec<[f64; 3]> = Vec::new();
    let tris: Vec<[usize; 3]> = (0..n)
        .map(|t| {
            [0, 1, 2].map(|v| {
                let o = 84 + t * 50 + 12 + v * 12;
                let p = [f(o), f(o + 4), f(o + 8)];
                *ids.entry(p.map(|x| (x * 1e4).round() as i64)).or_insert_with(|| {
                    points.push(p);
                    points.len() - 1
                })
            })
        })
        .collect();
    let height = |i: usize| dot(points[i], up);
    let bed = (0..points.len()).map(height).fold(f64::INFINITY, f64::min);
    // Per triangle: area and how far past vertical it leans down (None: up-facing, first layer or a sliver).
    let lean: Vec<(f64, Option<f64>)> = tris
        .iter()
        .map(|t| {
            let [a, b, c] = t.map(|i| points[i]);
            let (u, v) = ([b[0] - a[0], b[1] - a[1], b[2] - a[2]], [c[0] - a[0], c[1] - a[1], c[2] - a[2]]);
            let cross = [u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]];
            let len = dot(cross, cross).sqrt();
            let down = -dot(cross, up) / len.max(1e-300);
            let first_layer = t.iter().all(|&i| height(i) <= bed + FIRST_LAYER + 1e-3);
            let angle = (len >= 2e-4 && down > 1e-9 && !first_layer).then(|| 90.0 - down.clamp(-1.0, 1.0).acos().to_degrees());
            (len / 2.0, angle)
        })
        .collect();
    let normal = |t: usize| {
        let [a, b, c] = tris[t].map(|i| points[i]);
        let (u, v) = ([b[0] - a[0], b[1] - a[1], b[2] - a[2]], [c[0] - a[0], c[1] - a[1], c[2] - a[2]]);
        let n = [u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]];
        let l = dot(n, n).sqrt().max(1e-300);
        n.map(|x| x / l)
    };
    let steep: Vec<bool> = lean.iter().map(|(_, a)| a.is_some_and(|a| a > limit + MESH_SLACK_DEG)).collect();
    let mut edges: HashMap<(usize, usize), Vec<usize>> = HashMap::new();
    for (t, tri) in tris.iter().enumerate() {
        for k in 0..3 {
            let (a, b) = (tri[k], tri[(k + 1) % 3]);
            edges.entry((a.min(b), a.max(b))).or_default().push(t);
        }
    }
    // Group the steep triangles into connected regions.
    let mut region = vec![usize::MAX; tris.len()];
    let mut regions: Vec<Vec<usize>> = Vec::new();
    for start in (0..tris.len()).filter(|&t| steep[t]) {
        if region[start] != usize::MAX {
            continue;
        }
        let id = regions.len();
        let mut members = vec![start];
        region[start] = id;
        let mut i = 0;
        while let Some(&t) = members.get(i) {
            i += 1;
            for k in 0..3 {
                let (a, b) = (tris[t][k], tris[t][(k + 1) % 3]);
                for &o in &edges[&(a.min(b), a.max(b))] {
                    // Slivers join the region they sit in, so they don't punch holes in it.
                    if (steep[o] || lean[o].0 < 1e-4) && region[o] == usize::MAX {
                        region[o] = id;
                        members.push(o);
                    }
                }
            }
        }
        regions.push(members);
    }
    let sub = |a: [f64; 3], b: [f64; 3]| [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
    let dist = |a: [f64; 3], b: [f64; 3]| dot(sub(a, b), sub(a, b)).sqrt();
    // Two directions across `up`: the build plate seen from above.
    let side = if up[0].abs() < 0.9 { [1.0, 0.0, 0.0] } else { [0.0, 1.0, 0.0] };
    let e1 = {
        let d = dot(side, up);
        let v = [side[0] - d * up[0], side[1] - d * up[1], side[2] - d * up[2]];
        v.map(|x| x / dot(v, v).sqrt())
    };
    let e2 = [up[1] * e1[2] - up[2] * e1[1], up[2] * e1[0] - up[0] * e1[2], up[0] * e1[1] - up[1] * e1[0]];
    let (mut worst, mut area, mut bridges, mut longest) = (0.0f64, 0.0, 0, 0.0f64);
    let (mut lo, mut hi) = ([f64::INFINITY; 3], [f64::NEG_INFINITY; 3]);
    let mut unsupported = Vec::new();
    for (id, members) in regions.iter().enumerate() {
        // The region's boundary: held edges (concave, a wall carries on under the region) and free ones.
        let (mut held, mut free, mut perimeter) = (Vec::new(), Vec::new(), 0.0);
        for &t in members {
            for k in 0..3 {
                let (a, b, c) = (tris[t][k], tris[t][(k + 1) % 3], tris[t][(k + 2) % 3]);
                let others = &edges[&(a.min(b), a.max(b))];
                if others.iter().any(|&o| o != t && region[o] == id) {
                    continue;
                }
                let (pa, pb) = (points[a], points[b]);
                let inward = sub(points[c], [0, 1, 2].map(|i| (pa[i] + pb[i]) / 2.0));
                let inward = inward.map(|x| x / dot(inward, inward).sqrt().max(1e-300));
                perimeter += dist(pa, pb);
                if others.iter().any(|&o| o != t && dot(normal(o), inward) > HELD_BEND) {
                    held.push((pa, pb));
                } else {
                    free.push((pa, pb));
                }
            }
        }
        let region_area: f64 = members.iter().map(|&t| lean[t].0).sum();
        if 2.0 * region_area / perimeter.max(1e-9) <= NARROW_LEDGE {
            continue;
        }
        // A slicer bridges a region with parallel lines in one direction, each ending on held
        // edges both ways (crossing any hole in the region). Seen from above, try 18 directions:
        // one works when every line through the region's sample points does, and its span is the
        // longest such line. The region's span is that of the best direction; with none, it is an
        // overhang.
        let flat = |p: [f64; 3]| [dot(p, e1), dot(p, e2)];
        let walls: Vec<([f64; 2], [f64; 2])> = held.iter().map(|&(a, b)| (flat(a), flat(b))).collect();
        let reach = |p: [f64; 2], d: [f64; 2]| -> Option<f64> {
            walls
                .iter()
                .filter_map(|&(a, b)| {
                    let e = [b[0] - a[0], b[1] - a[1]];
                    let den = d[0] * e[1] - d[1] * e[0];
                    let w = [a[0] - p[0], a[1] - p[1]];
                    let t = (w[0] * e[1] - w[1] * e[0]) / den;
                    let u = (w[0] * d[1] - w[1] * d[0]) / den;
                    (den.abs() > 1e-12 && t > -1e-6 && (-1e-9..=1.0 + 1e-9).contains(&u)).then_some(t.max(0.0))
                })
                .min_by(f64::total_cmp)
        };
        // Each triangle's corners, edge midpoints and centre: the longest lines start at corners.
        let mut samples: Vec<[f64; 2]> = members
            .iter()
            .flat_map(|&t| {
                let [a, b, c] = tris[t].map(|i| points[i]);
                let mid = |p: [f64; 3], q: [f64; 3]| [0, 1, 2].map(|i| (p[i] + q[i]) / 2.0);
                [a, b, c, mid(a, b), mid(b, c), mid(c, a), [0, 1, 2].map(|i| (a[i] + b[i] + c[i]) / 3.0)].map(flat)
            })
            .collect();
        let stride = samples.len().div_ceil(600).max(1);
        samples = samples.into_iter().step_by(stride).collect();
        let span = (0..18)
            .filter_map(|k| {
                let th = std::f64::consts::PI * k as f64 / 18.0;
                let d = [th.cos(), th.sin()];
                samples.iter().try_fold(0.0f64, |longest, &p| Some(longest.max(reach(p, d)? + reach(p, [-d[0], -d[1]])?)))
            })
            .min_by(f64::total_cmp);
        if let Some(s) = span.filter(|&s| s <= bridge + 1e-6) {
            bridges += 1;
            longest = longest.max(s);
            continue;
        }
        let (mut r_lo, mut r_hi) = ([f64::INFINITY; 3], [f64::NEG_INFINITY; 3]);
        for &t in members {
            worst = worst.max(lean[t].1.unwrap_or(0.0));
            for p in tris[t] {
                for i in 0..3 {
                    r_lo[i] = r_lo[i].min(points[p][i]);
                    r_hi[i] = r_hi[i].max(points[p][i]);
                }
            }
        }
        area += region_area;
        for i in 0..3 {
            lo[i] = lo[i].min(r_lo[i]);
            hi[i] = hi[i].max(r_hi[i]);
        }
        let mut entry = json!({"kind": "overhang", "area_mm2": round(region_area), "min": r_lo.map(round), "max": r_hi.map(round)});
        if let Some(s) = span {
            entry["kind"] = json!("bridge too long");
            entry["span_mm"] = json!(round(s));
        }
        unsupported.push(entry);
    }
    unsupported.sort_by(|a, b| b["area_mm2"].as_f64().partial_cmp(&a["area_mm2"].as_f64()).unwrap_or(std::cmp::Ordering::Equal));
    unsupported.truncate(5);
    detail["regions"] = json!(unsupported);
    detail["area_mm2"] = json!(round(area));
    detail["bridges"] = json!({"count": bridges, "longest_mm": round(longest), "limit_mm": round(bridge)});
    if area > 0.0 {
        detail["at"] = json!({"min": lo.map(round), "max": hi.map(round)});
        return Ok(worst);
    }
    // Nothing needs support: report the steepest face outside the bridges.
    Ok(tris.iter().enumerate().filter(|(t, _)| !steep[*t]).filter_map(|(t, _)| lean[t].1).fold(0.0, f64::max))
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;

    fn stl(tris: &[[[f32; 3]; 3]]) -> Vec<u8> {
        let mut b = vec![0u8; 80];
        b.extend((tris.len() as u32).to_le_bytes());
        for t in tris {
            b.extend([0u8; 12]);
            for p in t {
                for x in p {
                    b.extend(x.to_le_bytes());
                }
            }
            b.extend([0u8; 2]);
        }
        b
    }

    #[test]
    fn values_take_their_quantitys_units() {
        let d = crate::parse(
            "design T units mm\nparam L = 10 mm\nbox B size L L L corner (0,0,0) -> body B\n\
             check B volume >= 1 cm3\ncheck B volume <= (L*L*L)\ncheck B mass < 2 kg\ncheck B size.z == L\n\
             check B overhang <= 45 deg up X bridge 5 mm\ncheck B shells == 2\ncheck B clearance B >= 0.2 mm\n",
        )
        .unwrap();
        let c = lower(&d).unwrap();
        let v: Vec<f64> = c.iter().map(|c| c.value).collect();
        assert_eq!(v, [1000.0, 1000.0, 2000.0, 10.0, 45.0, 2.0, 0.2]);
        assert_eq!((c[4].up, c[4].bridge, c[0].bridge), ([1.0, 0.0, 0.0], 5.0, DEFAULT_BRIDGE));
        for bad in ["check B volume >= 5 mm", "check B shells == 1.5", "check B mass <= 5 mm", "check B overhang >= 45 deg", "check B size.z <= 4 g"]
        {
            let d = crate::parse(&format!("design T units mm\n{bad}\n")).unwrap();
            assert!(lower(&d).is_err(), "{bad}");
        }
        for bad in [
            "check B weight <= 5",
            "check B size.w <= 5 mm",
            "check B volume = 5",
            "check B volume 5",
            "check B volume >= 5 mm3 up X",
            "check B size.z <= 5 mm bridge 2 mm",
        ] {
            assert!(crate::parse(&format!("design T units mm\n{bad}\n")).is_err(), "{bad}");
        }
    }

    #[test]
    fn print_statements_orient_the_layout_and_overhang_checks() {
        let d = crate::parse(
            "design T units mm\nbox A size 1 1 1 -> body H\nbox B size 1 1 1 -> body L\nbox C size 1 1 1 -> body F\n\
             print H up X\nprint L up (0, 0, -1)\nprint F up Z\nprint bed (250, 210 mm)\ncheck H overhang <= 45 deg\ncheck F overhang <= 45 deg up Y\n",
        )
        .unwrap();
        let layout = print_layout(&d).unwrap().unwrap();
        assert_eq!(layout["parts"][0], json!({"id": "H", "select": {"bodies": ["H"]}, "rotate": [{"axis": [0.0, -1.0, 0.0], "angle": "90 deg"}]}));
        assert_eq!(layout["parts"][1]["rotate"], json!([{"axis": [1, 0, 0], "angle": "180 deg"}]));
        assert_eq!(layout["parts"][2]["rotate"], json!([]));
        assert_eq!(layout["bed"], json!([250.0, 210.0]));
        let c = lower(&d).unwrap();
        assert_eq!((c[0].up, c[1].up), ([1.0, 0.0, 0.0], [0.0, 1.0, 0.0]));
        let twice = crate::parse("design T units mm\nbox A size 1 1 1 -> body H\nprint H up X\nprint H up Y\n").unwrap();
        assert!(crate::check(&twice).unwrap_err().contains("`H` already has a print orientation (line 3)"));
        assert!(crate::parse("design T units mm\nprint H\n").is_err());
    }

    #[test]
    fn clearances_measured_on_meshes_only_warn() {
        let bodies = [json!({"name": "A", "kernel": {"shells": 1}}), json!({"name": "B", "kernel": {"shells": 1}})];
        let none = |_: &str| -> Result<Vec<u8>, String> { Err("no mesh".into()) };
        let reply = |exact: bool| json!({"overlaps": [], "near": [{"a": "A", "b": "B", "distance_mm": 0.08, "exact": exact, "from": [0, 0, 0], "to": [0, 0, 0.08]}]});
        let d = crate::parse("design T units mm\ncheck A clearance B >= 0.4 mm\n").unwrap();
        let checks = lower(&d).unwrap();
        for (exact, approximate) in [(false, true), (true, false)] {
            let replies = [reply(exact)];
            let out = verify(&checks, &Evidence { bodies: &bodies, clearances: &replies, mesh: &none });
            assert!(out[0].failure.is_some());
            assert_eq!(out[0].approximate, approximate);
            assert_eq!(out[0].failure.as_ref().unwrap().contains("measured on the meshes"), approximate);
        }
    }

    #[test]
    fn implied_one_piece_check_and_explicit_overrides() {
        let bodies = [json!({"name": "A", "kernel": {"shells": 2}, "volume_mm3": 10.0}), json!({"name": "B", "kernel": {"shells": 2}})];
        let none = |_: &str| -> Result<Vec<u8>, String> { Err("no mesh".into()) };
        let ev = Evidence { bodies: &bodies, clearances: &[], mesh: &none };
        let d = crate::parse("design T units mm\ncheck B shells == 2\ncheck A volume >= 5 mm3\ncheck C volume >= 5 mm3\n").unwrap();
        let out = verify(&lower(&d).unwrap(), &ev);
        let failed: Vec<&str> = out.iter().filter_map(|o| o.failure.as_deref()).collect();
        assert_eq!(failed.len(), 2, "{failed:?}");
        assert!(failed[0].contains("no body named `C`"));
        assert!(failed[1].contains("`A` came out in 2 shells"));
    }

    /// The 12 triangles of a box, facing out (or in, for a cavity).
    fn cuboid(lo: [f32; 3], hi: [f32; 3], inward: bool) -> Vec<[[f32; 3]; 3]> {
        let p = |c: [usize; 3]| [0, 1, 2].map(|i| if c[i] == 0 { lo[i] } else { hi[i] });
        let quads = [
            [[0, 0, 0], [0, 1, 0], [1, 1, 0], [1, 0, 0]],
            [[0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]],
            [[0, 0, 0], [1, 0, 0], [1, 0, 1], [0, 0, 1]],
            [[0, 1, 0], [0, 1, 1], [1, 1, 1], [1, 1, 0]],
            [[0, 0, 0], [0, 0, 1], [0, 1, 1], [0, 1, 0]],
            [[1, 0, 0], [1, 1, 0], [1, 1, 1], [1, 0, 1]],
        ];
        quads
            .iter()
            .flat_map(|q| [[q[0], q[1], q[2]], [q[0], q[2], q[3]]])
            .map(|t| if inward { [p(t[0]), p(t[2]), p(t[1])] } else { t.map(p) })
            .collect()
    }

    #[test]
    fn overhang_ignores_the_bed_walls_and_first_layer() {
        // Faces of a 10 mm cube: the floor on the bed, a wall, and a free ceiling at z = 5.
        let floor = [[0., 0., 0.], [0., 10., 0.], [10., 0., 0.]];
        let wall = [[0., 0., 0.], [10., 0., 0.], [0., 0., 10.]];
        let ceiling = [[0., 0., 5.], [0., 10., 5.], [10., 0., 5.]];
        // Sloping down 30 deg from vertical: fine at 45 deg.
        let slope = [[0., 0., 10.], [0., 10. * 30f32.to_radians().tan(), 0.], [10., 0., 10.]];
        // A chamfer inside the first layer.
        let foot = [[0., 0., 0.], [0., 10., 0.2], [10., 0., 0.2]];
        let mut d = json!({});
        assert_eq!(overhang(&stl(&[floor, wall, foot]), [0., 0., 1.], 45.0, 10.0, &mut d).unwrap(), 0.0);
        let a = overhang(&stl(&[floor, wall, slope]), [0., 0., 1.], 45.0, 10.0, &mut d).unwrap();
        assert!((a - 30.0).abs() < 0.01 && d["area_mm2"] == 0.0, "{a} {d}");
        // A ceiling with free edges is a cantilever, not a bridge.
        assert_eq!(overhang(&stl(&[floor, ceiling]), [0., 0., 1.], 45.0, 10.0, &mut d).unwrap(), 90.0);
        assert_eq!(d["area_mm2"], 50.0);
        // Printed on its side, the ceiling is a wall.
        assert_eq!(overhang(&stl(&[floor, ceiling, wall]), [0., 1., 0.], 45.0, 10.0, &mut d).unwrap(), 0.0);
    }

    #[test]
    fn a_cavity_ceiling_is_a_bridge_up_to_its_limit() {
        let mut body = cuboid([0., 0., 0.], [20., 20., 10.], false);
        body.extend(cuboid([5., 5., 2.], [15., 15., 6.], true));
        let mut d = json!({});
        overhang(&stl(&body), [0., 0., 1.], 45.0, 10.0, &mut d).unwrap();
        assert_eq!((d["area_mm2"].clone(), d["bridges"]["count"].clone()), (json!(0.0), json!(1)), "{d}");
        assert_eq!(d["bridges"]["longest_mm"], 10.0, "{d}");
        assert_eq!(overhang(&stl(&body), [0., 0., 1.], 45.0, 5.0, &mut d).unwrap(), 90.0);
        assert_eq!(d["area_mm2"], 100.0);
    }
}
