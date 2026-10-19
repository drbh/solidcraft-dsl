//! Decompiler: a `.solidcraft` design saved as JSON back to `.sc` text.
//!
//! Sketch entities keep the engine's ids as their names (`line l1`, `point p3`), emitted in id
//! order per kind so that compiling the text allocates the same ids again. Sketches are
//! `explicit`: creators never auto-connect, and every coincidence in the file is written as a
//! constraint, so the compiled design is the saved one, constraint for constraint.

use std::collections::{BTreeMap, HashMap, HashSet};
use std::fmt::Write as _;

use serde_json::Value;

type R<T> = Result<T, String>;

fn num(v: &Value) -> String {
    match v.as_f64() {
        Some(f) => fmt_f(f),
        None => v.to_string(),
    }
}

/// Shortest round-trip text for a number (`6`, `-6.5`, `0.1`).
fn fmt_f(f: f64) -> String {
    if f == f.trunc() && f.abs() < 1e15 {
        format!("{}", f as i64)
    } else {
        let s = format!("{f}");
        s
    }
}

/// Quote a name that is not an identifier.
fn name(s: &str) -> String {
    let ident =
        !s.is_empty() && s.chars().next().is_some_and(|c| c.is_alphabetic() || c == '_') && s.chars().all(|c| c.is_alphanumeric() || c == '_');
    if ident { s.to_string() } else { format!("\"{}\"", s.replace('"', "'")) }
}

/// An expression as a single feature-option token: bare when it is one word or number, else parenthesised.
fn expr_tok(e: &str) -> String {
    let e = e.trim();
    let words: Vec<&str> = e.split_whitespace().collect();
    let simple = matches!(words.as_slice(), [w] if !w.contains(['(', ')']));
    let with_unit = matches!(words.as_slice(), [_, u] if matches!(*u, "mm" | "cm" | "m" | "in" | "deg" | "rad"));
    if simple || with_unit { e.to_string() } else { format!("({e})") }
}

fn vec3(v: &Value) -> R<String> {
    let a = v.as_array().filter(|a| a.len() == 3).ok_or("expected [x, y, z]")?;
    Ok(format!("({}, {}, {})", num(&a[0]), num(&a[1]), num(&a[2])))
}

fn vec2(v: &Value) -> R<String> {
    let a = v.as_array().filter(|a| a.len() == 2).ok_or("expected [x, y]")?;
    Ok(format!("({}, {})", num(&a[0]), num(&a[1])))
}

fn s_of<'a>(v: &'a Value, k: &str) -> R<&'a str> {
    v.get(k).and_then(Value::as_str).ok_or_else(|| format!("missing `{k}`"))
}

fn is_zero(v: &Value) -> bool {
    v.as_array().is_some_and(|a| a.iter().all(|x| x.as_f64() == Some(0.0)))
}

fn world_axis(v: &Value) -> Option<&'static str> {
    let a = v.as_array()?;
    let f = |i: usize| a.get(i)?.as_f64();
    match (f(0)?, f(1)?, f(2)?) {
        (1.0, 0.0, 0.0) => Some("X"),
        (0.0, 1.0, 0.0) => Some("Y"),
        (0.0, 0.0, 1.0) => Some("Z"),
        _ => None,
    }
}

/// Decompile a design (the JSON form of a `.solidcraft` file) to `.sc` source.
pub fn decompile(design: &Value) -> R<String> {
    let format = design.get("format").and_then(Value::as_str).unwrap_or("");
    if !format.starts_with("solidcraft/") {
        return Err(format!(
            "not a SolidCraft design (format `{format}`); save it as JSON first (`solidcraft-cli run design.solidcraft --save design.json`)"
        ));
    }
    let mut out = String::new();
    let dname = design.get("name").and_then(Value::as_str).unwrap_or("Design");
    let units = design.get("units").and_then(Value::as_str).unwrap_or("mm");
    let _ = writeln!(out, "design {} units {units}", name(dname));
    let _ = writeln!(out);

    // Parameters: the user's (model: false); dimension parameters (`d7`) are re-created by the dimensions.
    let params = design.get("params").and_then(Value::as_array).cloned().unwrap_or_default();
    let mut dims: HashMap<String, String> = HashMap::new();
    let mut any_param = false;
    for p in &params {
        let n = s_of(p, "name")?;
        let e = s_of(p, "expr")?;
        if p.get("model").and_then(Value::as_bool).unwrap_or(false) {
            dims.insert(n.to_string(), e.to_string());
            continue;
        }
        any_param = true;
        let _ = write!(out, "param {} = {e}", name(n));
        if let Some(c) = p.get("comment").and_then(Value::as_str).filter(|c| !c.is_empty()) {
            let _ = write!(out, " // {c}");
        }
        let _ = writeln!(out);
    }
    if any_param {
        let _ = writeln!(out);
    }

    let features = design.get("features").and_then(Value::as_array).ok_or("missing `features`")?;
    // Feature id → name (sketch references are by id).
    let names: HashMap<u64, String> = features.iter().filter_map(|f| Some((f.get("id")?.as_u64()?, f.get("name")?.as_str()?.to_string()))).collect();
    for f in features {
        let fname = s_of(f, "name")?;
        let kind = s_of(f, "type")?;
        if f.get("suppressed").and_then(Value::as_bool).unwrap_or(false) {
            return Err(format!("feature `{fname}`: suppressed features are not supported yet"));
        }
        let r = match kind {
            "sketch" => {
                // A blank line before each sketch block, except at the top.
                let gap = if out.ends_with("\n\n") || out.is_empty() { "" } else { "\n" };
                sketch(f, &dims).map(|s| format!("{gap}{s}\n"))
            }
            _ => feature(f, kind, &names),
        };
        out.push_str(&r.map_err(|e| format!("feature `{fname}` ({kind}): {e}"))?);
    }
    Ok(out)
}

fn plane(p: &Value) -> R<String> {
    match s_of(p, "type")? {
        "origin" => Ok(s_of(p, "name")?.to_string()),
        "construction" => Ok(name(s_of(p, "name")?)),
        "offset" => {
            let base = p.get("base").ok_or("offset plane without `base`")?;
            Ok(format!("{} offset {}", plane(base)?, expr_tok(s_of(p, "distance")?)))
        }
        "face" => Ok(format!("face {}", vec3(p.get("at").ok_or("face plane without `at`")?)?)),
        other => Err(format!("plane type `{other}` is not supported yet")),
    }
}

/// `(l, 3)` for `l3`; `("origin", 0)` for the origin.
fn split_id(id: &str) -> (String, usize) {
    let kind: String = id.chars().take_while(|c| c.is_alphabetic()).collect();
    let n = id[kind.len()..].parse().unwrap_or(0);
    (kind, n)
}

fn sketch(f: &Value, dims: &HashMap<String, String>) -> R<String> {
    let mut out = String::new();
    let sk = f.get("sketch").ok_or("missing `sketch`")?;
    let _ = writeln!(out, "sketch {} on {} explicit {{", name(s_of(f, "name")?), plane(f.get("plane").ok_or("missing `plane`")?)?);
    let points = sk.get("points").and_then(Value::as_array).cloned().unwrap_or_default();
    let curves = sk.get("curves").and_then(Value::as_array).cloned().unwrap_or_default();
    let constraints = sk.get("constraints").and_then(Value::as_array).cloned().unwrap_or_default();
    let pid = |i: usize| -> R<String> {
        points.get(i).and_then(|p| p.get("id")).and_then(Value::as_str).map(str::to_string).ok_or_else(|| format!("point {i} does not exist"))
    };
    let cid = |i: usize| -> R<String> {
        curves.get(i).and_then(|c| c.get("id")).and_then(Value::as_str).map(str::to_string).ok_or_else(|| format!("curve {i} does not exist"))
    };
    let pos = |i: usize| -> R<String> { vec2(points.get(i).and_then(|p| p.get("pos")).ok_or_else(|| format!("point {i} without `pos`"))?) };
    // Which curve owns each point id (`l1.start` belongs to `l1`).
    let owner = |id: &str| -> Option<String> { id.split_once('.').map(|(o, _)| o.to_string()) };

    // Standalone points first (each kind's counter is independent of the others).
    let mut fixed_points: Vec<String> = Vec::new();
    let mut standalone: BTreeMap<usize, (String, String)> = BTreeMap::new();
    for (i, p) in points.iter().enumerate() {
        let id = pid(i)?;
        if p.get("fixed").and_then(Value::as_bool).unwrap_or(false) && id != "origin" {
            fixed_points.push(id.clone());
        }
        if let ("p", n) = split_id(&id).as_tuple()
            && n > 0
        {
            standalone.insert(n, (id, pos(i)?));
        }
    }
    let expected: Vec<usize> = (1..=standalone.len()).collect();
    if standalone.keys().copied().collect::<Vec<_>>() != expected {
        return Err("the sketch's `p` points are not numbered 1..n (points were deleted); not supported yet".into());
    }
    for (id, p) in standalone.values() {
        let _ = writeln!(out, "  point {id} = {p}");
    }

    // Curves: in id order within each kind; a curve that uses another curve's point must come
    // after it, so pick the smallest ready curve each time.
    let mut by_kind: BTreeMap<String, Vec<(usize, usize)>> = BTreeMap::new();
    for i in 0..curves.len() {
        let (k, n) = split_id(&cid(i)?);
        by_kind.entry(k).or_default().push((n, i));
    }
    for v in by_kind.values_mut() {
        v.sort();
        let ns: Vec<usize> = v.iter().map(|(n, _)| *n).collect();
        if ns != (1..=ns.len()).collect::<Vec<_>>() {
            return Err(format!(
                "curves `{}…` are not numbered 1..n (curves were deleted); not supported yet",
                ns.first().map(|n| n.to_string()).unwrap_or_default()
            ));
        }
    }
    let point_refs = |c: &Value| -> Vec<usize> {
        let mut v = Vec::new();
        for k in ["a", "b", "c", "m", "apex"] {
            if let Some(i) = c.get(k).and_then(Value::as_u64) {
                v.push(i as usize);
            }
        }
        if let Some(pts) = c.get("pts").and_then(Value::as_array) {
            v.extend(pts.iter().filter_map(Value::as_u64).map(|i| i as usize));
        }
        v
    };
    let mut emitted: HashSet<String> = HashSet::new();
    let mut next: BTreeMap<String, usize> = by_kind.keys().map(|k| (k.clone(), 0)).collect();
    let total = curves.len();
    let mut done = 0;
    while done < total {
        let mut progressed = false;
        for (k, list) in &by_kind {
            let Some(&(_, ci)) = list.get(next[k]) else { continue };
            let c = &curves[ci];
            let id = cid(ci)?;
            // Ready when every point it uses is its own, standalone, the origin, or already emitted.
            let ready = point_refs(c).iter().all(|&pi| {
                let p = pid(pi).unwrap_or_default();
                p == "origin" || p.starts_with('p') || owner(&p).is_none_or(|o| o == id || emitted.contains(&o))
            });
            if !ready {
                continue;
            }
            let p_of = |pi: usize| -> R<String> {
                let p = pid(pi)?;
                if owner(&p).as_deref() == Some(id.as_str()) { pos(pi) } else { Ok(p) }
            };
            let construction = if c.get("construction").and_then(Value::as_bool).unwrap_or(false) { "construction " } else { "" };
            match s_of(c, "type")? {
                "line" => {
                    let a = c.get("a").and_then(Value::as_u64).ok_or("line without `a`")? as usize;
                    let b = c.get("b").and_then(Value::as_u64).ok_or("line without `b`")? as usize;
                    let _ = writeln!(out, "  {construction}line {id} = {} {}", p_of(a)?, p_of(b)?);
                }
                "circle" => {
                    let cc = c.get("c").and_then(Value::as_u64).ok_or("circle without `c`")? as usize;
                    let _ = writeln!(out, "  {construction}circle {id} = {} radius {}", p_of(cc)?, num(c.get("r").ok_or("circle without `r`")?));
                }
                "arc" => {
                    let cc = c.get("c").and_then(Value::as_u64).ok_or("arc without `c`")? as usize;
                    let a = c.get("a").and_then(Value::as_u64).ok_or("arc without `a`")? as usize;
                    let b = c.get("b").and_then(Value::as_u64).ok_or("arc without `b`")? as usize;
                    if c.get("reversed").and_then(Value::as_bool).unwrap_or(false) {
                        return Err(format!("arc `{id}` is drawn clockwise (reversed); not supported yet"));
                    }
                    let _ = writeln!(out, "  {construction}arc {id} = center {} start {} end {}", p_of(cc)?, p_of(a)?, p_of(b)?);
                }
                "spline" => {
                    if c.get("control").and_then(Value::as_bool).unwrap_or(false) {
                        return Err(format!("spline `{id}` is a control-point spline; not supported yet"));
                    }
                    let pts = c.get("pts").and_then(Value::as_array).ok_or("spline without `pts`")?;
                    let mut s = String::new();
                    for p in pts {
                        let pi = p.as_u64().ok_or("bad spline point")? as usize;
                        s.push(' ');
                        s.push_str(&p_of(pi)?);
                    }
                    let periodic = if c.get("periodic").and_then(Value::as_bool).unwrap_or(false) { " periodic" } else { "" };
                    let _ = writeln!(out, "  {construction}spline {id} ={s}{periodic}");
                }
                other => return Err(format!("curve `{id}`: `{other}` curves are not supported yet")),
            }
            emitted.insert(id);
            *next.get_mut(k).ok_or("kind")? += 1;
            done += 1;
            progressed = true;
        }
        if !progressed {
            return Err("the sketch's curves refer to each other's points in a cycle; not supported yet".into());
        }
    }

    // Constraints and dimensions.
    let mut fixed_by_constraint: HashSet<String> = HashSet::new();
    for k in &constraints {
        let t = s_of(k, "type")?;
        let p = |key: &str| -> R<String> { pid(k.get(key).and_then(Value::as_u64).ok_or_else(|| format!("`{t}` without `{key}`"))? as usize) };
        let c = |key: &str| -> R<String> { cid(k.get(key).and_then(Value::as_u64).ok_or_else(|| format!("`{t}` without `{key}`"))? as usize) };
        // A dimension's value: its parameter's expression, else the stored number.
        let value = |deg: bool| -> R<String> {
            if let Some(d) = k.get("param").and_then(Value::as_str)
                && let Some(e) = dims.get(d)
            {
                return Ok(e.clone());
            }
            let v = k.get("value").and_then(Value::as_f64).ok_or_else(|| format!("`{t}` without `value`"))?;
            Ok(if deg { format!("{} deg", fmt_f((v.to_degrees() * 1e9).round() / 1e9)) } else { fmt_f(v) })
        };
        let line = match t {
            "coincident" => format!("coincident {} {}", p("p")?, p("q")?),
            "point_on_curve" => format!("coincident {} {}", p("p")?, c("c")?),
            "horizontal" => format!("horizontal {}", c("l")?),
            "vertical" => format!("vertical {}", c("l")?),
            "horizontal_points" => format!("horizontal {} {}", p("p")?, p("q")?),
            "vertical_points" => format!("vertical {} {}", p("p")?, p("q")?),
            "parallel" | "perpendicular" | "collinear" | "tangent" | "equal" | "concentric" => format!("{t} {} {}", c("a")?, c("b")?),
            "smooth" => format!("curvature {} {}", c("a")?, c("b")?),
            "midpoint" => format!("midpoint {} {}", p("p")?, c("l")?),
            "symmetric" => format!("symmetric {} {} about {}", p("p")?, p("q")?, c("l")?),
            "fix" => {
                let id = p("p")?;
                fixed_by_constraint.insert(id.clone());
                format!("fix {id}")
            }
            "distance" => format!("dim distance {} -> {} = {}", p("p")?, p("q")?, value(false)?),
            "distance_x" => format!("dim horizontal {} -> {} = {}", p("p")?, p("q")?, value(false)?),
            "distance_y" => format!("dim vertical {} -> {} = {}", p("p")?, p("q")?, value(false)?),
            "point_line_distance" => format!("dim distance {} -> {} = {}", p("p")?, c("l")?, value(false)?),
            "length" => format!("dim length {} = {}", c("l")?, value(false)?),
            "radius" => format!("dim radius {} = {}", c("c")?, value(false)?),
            "diameter" => format!("dim diameter {} = {}", c("c")?, value(false)?),
            "angle" => {
                if k.get("flip").and_then(Value::as_bool).unwrap_or(false) {
                    return Err("a flipped angle dimension is not supported yet".into());
                }
                format!("dim angle {} -> {} = {}", c("a")?, c("b")?, value(true)?)
            }
            other => return Err(format!("constraint `{other}` is not supported yet")),
        };
        let _ = writeln!(out, "  {line}");
    }
    for id in fixed_points {
        if !fixed_by_constraint.contains(&id) {
            let _ = writeln!(out, "  fix {id}");
        }
    }
    out.push_str("}\n");
    Ok(out)
}

trait AsTuple {
    fn as_tuple(&self) -> (&str, usize);
}
impl AsTuple for (String, usize) {
    fn as_tuple(&self) -> (&str, usize) {
        (self.0.as_str(), self.1)
    }
}

fn operation(f: &Value, out: &mut String) {
    match f.get("operation").and_then(Value::as_str) {
        Some("join") => out.push_str(" join"),
        Some("cut") => out.push_str(" cut"),
        Some("intersect") => out.push_str(" intersect"),
        _ => {}
    }
    if let Some(t) = f.get("targets").and_then(Value::as_array).filter(|t| !t.is_empty()) {
        let names: Vec<String> = t.iter().filter_map(Value::as_str).map(name).collect();
        let _ = write!(out, " targets {}", names.join(", "));
    }
}

fn body_name(f: &Value) -> R<String> {
    let b = f.get("body_names").and_then(Value::as_array).cloned().unwrap_or_default();
    match b.as_slice() {
        [] => Ok(String::new()),
        [one] => Ok(format!(" -> body {}", name(one.as_str().unwrap_or("")))),
        _ => Err("features that name several bodies are not supported yet".into()),
    }
}

fn profiles(f: &Value, out: &mut String) -> R<()> {
    let Some(p) = f.get("profiles") else { return Ok(()) };
    match s_of(p, "type")? {
        "all" => Ok(()),
        "points" => {
            let pts = p.get("points").and_then(Value::as_array).ok_or("profiles without `points`")?;
            out.push_str(" profiles");
            for q in pts {
                out.push(' ');
                out.push_str(&vec2(q)?);
            }
            Ok(())
        }
        other => Err(format!("profile selection `{other}` is not supported yet (only all and points)")),
    }
}

fn sketch_name(id: &Value, names: &HashMap<u64, String>) -> R<String> {
    let id = id.as_u64().ok_or("sketch id is not a number")?;
    names.get(&id).map(|n| name(n)).ok_or_else(|| format!("sketch feature {id} does not exist"))
}

fn names_list(v: Option<&Value>) -> String {
    v.and_then(Value::as_array).map(|a| a.iter().filter_map(Value::as_str).map(name).collect::<Vec<_>>().join(", ")).unwrap_or_default()
}

fn feature(f: &Value, kind: &str, names: &HashMap<u64, String>) -> R<String> {
    let fname = name(s_of(f, "name")?);
    let mut out = String::new();
    match kind {
        "extrude" => {
            let ext = f.get("extent").ok_or("missing `extent`")?;
            let _ = write!(out, "extrude {fname} from {}", sketch_name(f.get("sketch").ok_or("missing `sketch`")?, names)?);
            if ext.get("through_all").and_then(Value::as_bool).unwrap_or(false) {
                out.push_str(" through_all");
            } else {
                let _ = write!(out, " distance {}", expr_tok(s_of(ext, "distance")?));
            }
            match ext.get("direction").and_then(Value::as_str) {
                Some("symmetric") => out.push_str(" symmetric"),
                Some("negative") => out.push_str(" negative"),
                _ => {}
            }
            if ext.get("convention").and_then(Value::as_str).is_some_and(|c| c != "total_distance") {
                return Err("legacy symmetric extrudes (`legacy_per_side`) are not supported yet".into());
            }
            for (k, w) in [("taper", "taper"), ("start_offset", "offset"), ("distance2", "distance2")] {
                if let Some(v) = ext.get(k).and_then(Value::as_str) {
                    let _ = write!(out, " {w} {}", expr_tok(v));
                }
            }
            if ext.get("to").is_some() {
                return Err("extrude To Object is not supported yet".into());
            }
            if f.get("thin").is_some() {
                return Err("thin extrudes are not supported yet".into());
            }
            profiles(f, &mut out)?;
            operation(f, &mut out);
            out.push_str(&body_name(f)?);
        }
        "revolve" => {
            let _ = write!(out, "revolve {fname} from {}", sketch_name(f.get("sketch").ok_or("missing `sketch`")?, names)?);
            let axis = f.get("axis").ok_or("missing `axis`")?;
            let a = match s_of(axis, "type")? {
                "sketch_line" => s_of(axis, "curve")?.to_string(),
                "sketch_axis" => s_of(axis, "axis")?.to_string(),
                "world" => s_of(axis, "axis")?.to_string(),
                other => return Err(format!("revolve axis `{other}` is not supported yet")),
            };
            let _ = write!(out, " axis {a} angle {}", expr_tok(s_of(f, "angle")?));
            if f.get("symmetric").and_then(Value::as_bool).unwrap_or(false) {
                out.push_str(" symmetric");
            }
            if f.get("angle2").is_some() || f.get("to").is_some() {
                return Err("two-sided or To Object revolves are not supported yet".into());
            }
            profiles(f, &mut out)?;
            operation(f, &mut out);
            out.push_str(&body_name(f)?);
        }
        "hole" => {
            let _ = write!(out, "hole {fname} at");
            match f.get("points") {
                Some(pts) => {
                    let s = sketch_name(pts.get("sketch").ok_or("hole points without `sketch`")?, names)?;
                    let ids: Vec<String> = pts
                        .get("ids")
                        .and_then(Value::as_array)
                        .map(|a| a.iter().filter_map(Value::as_str).map(|i| format!("{s}.{i}")).collect())
                        .unwrap_or_default();
                    let _ = write!(out, " {}", ids.join(", "));
                    if pts.get("flip").and_then(Value::as_bool).unwrap_or(false) {
                        out.push_str(" flip");
                    }
                }
                None => {
                    let _ = write!(out, " {}", vec3(f.get("position").ok_or("hole without `position`")?)?);
                }
            }
            let _ = write!(out, " diameter {}", expr_tok(s_of(f, "diameter")?));
            if let Some(d) = f.get("depth").and_then(Value::as_str) {
                let _ = write!(out, " depth {}", expr_tok(d));
            }
            if let Some(h) = f.get("hole") {
                match h.get("type").and_then(Value::as_str).unwrap_or("simple") {
                    "simple" => {}
                    "counterbore" => {
                        let _ = write!(out, " counterbore {} {}", expr_tok(s_of(h, "cb_diameter")?), expr_tok(s_of(h, "cb_depth")?));
                    }
                    "countersink" => {
                        let _ = write!(out, " countersink {} {}", expr_tok(s_of(h, "cs_diameter")?), expr_tok(s_of(h, "cs_angle")?));
                    }
                    "drilled" => out.push_str(" drilled"),
                    other => return Err(format!("hole type `{other}` is not supported yet")),
                }
            }
            if f.get("thread").is_some() || f.get("to").is_some() {
                return Err("threaded or To Object holes are not supported yet".into());
            }
        }
        "fillet" | "chamfer" => {
            let _ = write!(out, "{kind} {fname} edges");
            for e in f.get("edges").and_then(Value::as_array).ok_or("missing `edges`")? {
                let _ = write!(out, " {}", vec3(e)?);
            }
            if kind == "fillet" {
                if f.get("style").is_some() || f.get("fit").is_some() {
                    return Err("fillet styles and fits other than constant/exact are not supported yet".into());
                }
                let _ = write!(out, " radius {}", expr_tok(s_of(f, "radius")?));
            } else {
                let _ = write!(out, " distance {}", expr_tok(s_of(f, "distance")?));
                if let Some(v) = f.get("distance2").and_then(Value::as_str) {
                    let _ = write!(out, " distance2 {}", expr_tok(v));
                }
                if let Some(v) = f.get("angle").and_then(Value::as_str) {
                    let _ = write!(out, " angle {}", expr_tok(v));
                }
                if f.get("flip").and_then(Value::as_bool).unwrap_or(false) {
                    out.push_str(" flip");
                }
            }
            if let Some(b) = f.get("body").and_then(Value::as_str) {
                let _ = write!(out, " body {}", name(b));
            }
        }
        "box" => {
            let _ = write!(out, "box {fname} size {} {} {}", expr_tok(s_of(f, "length")?), expr_tok(s_of(f, "width")?), expr_tok(s_of(f, "height")?));
            let corner = f.get("corner").ok_or("missing `corner`")?;
            if !is_zero(corner) {
                let _ = write!(out, " corner {}", vec3(corner)?);
            }
            operation(f, &mut out);
            out.push_str(&body_name(f)?);
        }
        "cylinder" => {
            let _ = write!(out, "cylinder {fname} radius {} height {}", expr_tok(s_of(f, "radius")?), expr_tok(s_of(f, "height")?));
            let base = f.get("base").ok_or("missing `base`")?;
            if !is_zero(base) {
                let _ = write!(out, " base {}", vec3(base)?);
            }
            if let Some(a) = f.get("axis") {
                match world_axis(a) {
                    Some("Z") => {}
                    Some(w) => {
                        let _ = write!(out, " axis {w}");
                    }
                    None => {
                        let _ = write!(out, " axis {}", vec3(a)?);
                    }
                }
            }
            operation(f, &mut out);
            out.push_str(&body_name(f)?);
        }
        "sphere" => {
            let _ = write!(out, "sphere {fname} radius {}", expr_tok(s_of(f, "radius")?));
            let c = f.get("center").ok_or("missing `center`")?;
            if !is_zero(c) {
                let _ = write!(out, " center {}", vec3(c)?);
            }
            operation(f, &mut out);
            out.push_str(&body_name(f)?);
        }
        "torus" => {
            let _ = write!(out, "torus {fname} major {} minor {}", expr_tok(s_of(f, "major")?), expr_tok(s_of(f, "minor")?));
            let c = f.get("center").ok_or("missing `center`")?;
            if !is_zero(c) {
                let _ = write!(out, " center {}", vec3(c)?);
            }
            operation(f, &mut out);
            out.push_str(&body_name(f)?);
        }
        "combine" => {
            let _ = write!(out, "combine {fname} target {} tools {}", name(s_of(f, "target")?), names_list(f.get("tools")));
            operation(f, &mut out);
            if f.get("keep_tools").and_then(Value::as_bool).unwrap_or(false) {
                out.push_str(" keep_tools");
            }
        }
        "shell" => {
            let _ = write!(out, "shell {fname} faces");
            for e in f.get("faces").and_then(Value::as_array).ok_or("missing `faces`")? {
                let _ = write!(out, " {}", vec3(e)?);
            }
            let _ = write!(out, " thickness {}", expr_tok(s_of(f, "thickness")?));
            if let Some(d @ ("inside" | "outside" | "both")) = f.get("direction").and_then(Value::as_str) {
                let _ = write!(out, " {d}");
            }
            if let Some(b) = f.get("body").and_then(Value::as_str) {
                let _ = write!(out, " body {}", name(b));
            }
            if f.get("tangent_chain").and_then(Value::as_bool).unwrap_or(false) {
                return Err("shell tangent chains are not supported yet".into());
            }
        }
        "mirror" => {
            let bodies = f.get("bodies").and_then(Value::as_array).filter(|b| !b.is_empty());
            let _ = write!(out, "mirror {fname}");
            match bodies {
                Some(_) => {
                    let _ = write!(out, " bodies {}", names_list(f.get("bodies")));
                }
                None => {
                    let _ = write!(out, " features {}", names_list(f.get("features")));
                }
            }
            let p = f.get("plane").ok_or("missing `plane`")?;
            match s_of(p, "type")? {
                "origin" => {
                    let _ = write!(out, " plane {}", s_of(p, "name")?);
                }
                "construction" => {
                    let _ = write!(out, " plane {}", name(s_of(p, "name")?));
                }
                other => return Err(format!("mirror plane `{other}` is not supported yet")),
            }
            if f.get("combine").and_then(Value::as_bool).unwrap_or(false) {
                return Err("mirror with combine is not supported yet".into());
            }
        }
        "pattern" => {
            let p = f.get("pattern").ok_or("missing `pattern`")?;
            let bodies = f.get("bodies").and_then(Value::as_array).filter(|b| !b.is_empty());
            let what = match bodies {
                Some(_) => format!("bodies {}", names_list(f.get("bodies"))),
                None => format!("features {}", names_list(f.get("features"))),
            };
            match s_of(p, "type")? {
                "rectangular" => {
                    let _ = write!(
                        out,
                        "pattern {fname} {what} dir {} count {} spacing {}",
                        vec3(p.get("dir1").ok_or("missing `dir1`")?)?,
                        expr_tok(s_of(p, "count1")?),
                        expr_tok(s_of(p, "spacing1")?)
                    );
                    if let Some(d2) = p.get("dir2") {
                        let _ =
                            write!(out, " dir2 {} count2 {} spacing2 {}", vec3(d2)?, expr_tok(s_of(p, "count2")?), expr_tok(s_of(p, "spacing2")?));
                    }
                }
                "circular" => {
                    let _ = write!(out, "pattern circular {fname} {what}");
                    let origin = p.get("origin").ok_or("missing `origin`")?;
                    let axis = p.get("axis").ok_or("missing `axis`")?;
                    match world_axis(axis) {
                        Some(w) if is_zero(origin) => {
                            let _ = write!(out, " axis {w}");
                        }
                        _ => {
                            let _ = write!(out, " origin {} dir {}", vec3(origin)?, vec3(axis)?);
                        }
                    }
                    let _ = write!(out, " count {} angle {}", expr_tok(s_of(p, "count")?), expr_tok(s_of(p, "angle")?));
                }
                other => return Err(format!("`{other}` patterns are not supported yet")),
            }
        }
        "move" => {
            let _ = write!(out, "move {fname} bodies {}", names_list(f.get("bodies")));
            let t = f.get("translate").and_then(Value::as_array).ok_or("missing `translate`")?;
            let parts: Vec<String> = t.iter().map(|x| x.as_str().map(str::to_string).unwrap_or_else(|| num(x))).collect();
            if parts.iter().any(|p| p != "0") {
                let _ = write!(out, " to ({})", parts.join(", "));
            }
            if let (Some(a), Some(ang)) = (f.get("rotate_axis"), f.get("angle").and_then(Value::as_str)) {
                let _ = write!(out, " axis {} angle {}", vec3(a)?, expr_tok(ang));
            }
            if f.get("copy").and_then(Value::as_bool).unwrap_or(false) {
                out.push_str(" copy");
            }
        }
        "remove" => {
            let _ = write!(out, "remove {fname} bodies {}", names_list(f.get("bodies")));
        }
        other => return Err(format!("`{other}` features are not supported yet")),
    }
    out.push('\n');
    Ok(out)
}
