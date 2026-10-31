//! Compiler: AST to a SolidCraft command script, `{"commands": [{command, params}…]}`.
//!
//! Sketch entity ids are predicted the way the engine allocates them: per sketch, `l`, `c`,
//! `a`, `p`, `e` and `s` counters each start at 1, every creator takes the next ids of its
//! kinds, and the first constraint naming a sketch axis (`x`, `y`) adds one construction line
//! and two points. The rules were measured against solidcraft-cli 0.15 (see README.md).

use std::collections::HashMap;

use serde_json::{Value, json};

use crate::ast::*;

type R<T> = Result<T, String>;

fn err(line: usize, msg: impl std::fmt::Display) -> String {
    format!("line {line}: {msg}")
}

/// What a named sketch entity turned into.
#[derive(Debug, Clone)]
enum Entity {
    Line(String),
    Polyline { lines: Vec<String>, closed: bool },
    Rect { sides: [String; 4], corners: [String; 4], center: Option<String>, arcs: Option<[String; 4]> },
    Circle(String),
    Arc(String),
    Point(String),
    Polygon { lines: Vec<String>, points: Vec<String>, circle: String },
    Slot { lines: [String; 2], arcs: [String; 2], points: [String; 4] },
    Spline { id: String, points: Vec<String> },
    Ellipse { id: String, points: [String; 2] },
}

#[derive(Default)]
struct Counters {
    l: usize,
    c: usize,
    a: usize,
    p: usize,
    s: usize,
    e: usize,
}

impl Counters {
    fn l(&mut self) -> String {
        self.l += 1;
        format!("l{}", self.l)
    }
    fn c(&mut self) -> String {
        self.c += 1;
        format!("c{}", self.c)
    }
    fn a(&mut self) -> String {
        self.a += 1;
        format!("a{}", self.a)
    }
    fn p(&mut self) -> String {
        self.p += 1;
        format!("p{}", self.p)
    }
}

/// A compiled sketch: its entity table, kept so later features can refer to `Sketch.entity`.
struct SketchTable {
    names: HashMap<String, Entity>,
    axes_used: Vec<String>,
    counters: Counters,
}

impl SketchTable {
    fn define(&mut self, line: usize, name: &str, e: Entity) -> R<()> {
        if matches!(name, "origin" | "x" | "y") {
            return Err(err(line, format!("`{name}` is reserved (the sketch origin and axes)")));
        }
        if self.names.insert(name.to_string(), e).is_some() {
            return Err(err(line, format!("`{name}` is already defined in this sketch")));
        }
        Ok(())
    }

    /// The ids of the entities named: every curve of a compound entity named whole, else the one.
    fn curves(&mut self, line: usize, refs: &[Ref]) -> R<Vec<String>> {
        let mut ids = Vec::new();
        for r in refs {
            match self.curves_of(r) {
                Some(curves) => ids.extend(curves),
                None => ids.push(self.resolve(line, r)?),
            }
        }
        Ok(ids)
    }

    /// Every curve of a compound entity named whole (`outline`, `r`, `s`), for `fix`.
    fn curves_of(&self, r: &Ref) -> Option<Vec<String>> {
        if !r.parts.is_empty() {
            return None;
        }
        match self.names.get(&r.base)? {
            Entity::Polyline { lines, .. } | Entity::Polygon { lines, .. } => Some(lines.clone()),
            Entity::Rect { sides, arcs, .. } => Some(sides.iter().chain(arcs.iter().flatten()).cloned().collect()),
            Entity::Slot { lines, arcs, .. } => Some(lines.iter().chain(arcs).cloned().collect()),
            _ => None,
        }
    }

    /// The engine id of a reference. `kind` says what the statement needs (a hint for errors).
    fn resolve(&mut self, line: usize, r: &Ref) -> R<String> {
        let parts: Vec<&str> = r.parts.iter().map(String::as_str).collect();
        match r.base.as_str() {
            "origin" if parts.is_empty() => return Ok("origin".into()),
            ax @ ("x" | "y") if parts.is_empty() => {
                // Each axis named by a constraint becomes a construction line with two points, once.
                if !self.axes_used.iter().any(|a| a == ax) {
                    self.axes_used.push(ax.to_string());
                    self.counters.l();
                    self.counters.p();
                    self.counters.p();
                }
                return Ok(ax.into());
            }
            _ => {}
        }
        let Some(e) = self.names.get(&r.base) else { return Err(err(line, format!("unknown sketch entity `{}`", r.text()))) };
        let bad = || err(line, format!("`{}` is not a part of `{}`", r.text(), r.base));
        let one = |id: &str| -> R<String> {
            match parts.as_slice() {
                [] => Ok(id.to_string()),
                [p @ ("start" | "end" | "center")] => Ok(format!("{id}.{p}")),
                _ => Err(bad()),
            }
        };
        match e {
            Entity::Line(id) => one(id),
            Entity::Circle(id) => match parts.as_slice() {
                [] => Ok(id.clone()),
                ["center"] => Ok(format!("{id}.center")),
                _ => Err(bad()),
            },
            Entity::Arc(id) => one(id),
            Entity::Point(id) => match parts.as_slice() {
                [] => Ok(id.clone()),
                _ => Err(bad()),
            },
            Entity::Polyline { lines, closed } => {
                let n = lines.len();
                let (idx, rest): (usize, &[&str]) = match parts.first().copied() {
                    None => {
                        return Err(err(line, format!("`{}` is a polyline: name a segment (`{}.1`) or an end (`{}.1.end`)", r.base, r.base, r.base)));
                    }
                    Some(s) if s.parse::<usize>().is_ok() => (s.parse::<usize>().unwrap_or(0), &parts[1..]),
                    Some("start") => (1, &["start"]),
                    Some("end") => (n, &["end"]),
                    Some(_) => return Err(bad()),
                };
                if idx == 0 || idx > n {
                    return Err(err(line, format!("`{}` has {n} segments, there is no segment {idx}", r.base)));
                }
                let id = &lines[idx - 1];
                match rest {
                    [] => Ok(id.clone()),
                    ["start"] => Ok(if idx == 1 { format!("{id}.start") } else { format!("{}.end", lines[idx - 2]) }),
                    ["end"] => Ok(if idx == n && *closed { format!("{}.start", lines[0]) } else { format!("{id}.end") }),
                    _ => Err(bad()),
                }
            }
            Entity::Rect { sides, corners, center, arcs } => match parts.as_slice() {
                [] => {
                    Err(err(line, format!("`{}` is a rectangle: name a side (`{}.bottom`) or a corner (`{}.bottom_left`)", r.base, r.base, r.base)))
                }
                ["bottom"] => Ok(sides[0].clone()),
                ["right"] => Ok(sides[1].clone()),
                ["top"] => Ok(sides[2].clone()),
                ["left"] => Ok(sides[3].clone()),
                ["bottom_left"] => Ok(corners[0].clone()),
                ["bottom_right"] => Ok(corners[1].clone()),
                ["top_right"] => Ok(corners[2].clone()),
                ["top_left"] => Ok(corners[3].clone()),
                ["center"] => center.clone().ok_or_else(|| err(line, format!("`{}` has no centre point (only `rect center` has one)", r.base))),
                [a @ ("arc1" | "arc2" | "arc3" | "arc4"), rest @ ..] => {
                    let arcs = arcs.as_ref().ok_or_else(|| err(line, format!("`{}` has no corner arcs (only `rect rounded` has them)", r.base)))?;
                    let id = &arcs[(a.as_bytes()[3] - b'1') as usize];
                    match rest {
                        [] => Ok(id.clone()),
                        [p @ ("start" | "end" | "center")] => Ok(format!("{id}.{p}")),
                        _ => Err(bad()),
                    }
                }
                _ => Err(bad()),
            },
            Entity::Polygon { lines, points, circle } => match parts.as_slice() {
                [] => Err(err(
                    line,
                    format!("`{}` is a polygon: name a side (`{}.1`), a vertex (`{}.v1`) or `{}.circle`", r.base, r.base, r.base, r.base),
                )),
                ["circle"] => Ok(circle.clone()),
                ["circle", "center"] => Ok(format!("{circle}.center")),
                [s] if s.parse::<usize>().is_ok() => lines.get(s.parse::<usize>().unwrap_or(0).wrapping_sub(1)).cloned().ok_or_else(bad),
                [v] if v.starts_with('v') && v[1..].parse::<usize>().is_ok() => {
                    points.get(v[1..].parse::<usize>().unwrap_or(0).wrapping_sub(1)).cloned().ok_or_else(bad)
                }
                _ => Err(bad()),
            },
            Entity::Slot { lines, arcs, points } => match parts.as_slice() {
                [] => Err(err(
                    line,
                    format!("`{}` is a slot: name `{}.line1`, `{}.arc1`, `{}.arc1.center` or `{}.p1`", r.base, r.base, r.base, r.base, r.base),
                )),
                ["line1"] => Ok(lines[0].clone()),
                ["line2"] => Ok(lines[1].clone()),
                ["arc1"] => Ok(arcs[0].clone()),
                ["arc2"] => Ok(arcs[1].clone()),
                ["arc1", p @ ("start" | "end" | "center")] => Ok(format!("{}.{p}", arcs[0])),
                ["arc2", p @ ("start" | "end" | "center")] => Ok(format!("{}.{p}", arcs[1])),
                [p @ ("p1" | "p2" | "p3" | "p4")] => Ok(points[(p.as_bytes()[1] - b'1') as usize].clone()),
                _ => Err(bad()),
            },
            Entity::Spline { id, points } => match parts.as_slice() {
                [] => Ok(id.clone()),
                [s] if s.parse::<usize>().is_ok() => points.get(s.parse::<usize>().unwrap_or(0).wrapping_sub(1)).cloned().ok_or_else(bad),
                ["start"] => points.first().cloned().ok_or_else(bad),
                ["end"] => points.last().cloned().ok_or_else(bad),
                _ => Err(bad()),
            },
            Entity::Ellipse { id, points } => match parts.as_slice() {
                [] => Ok(id.clone()),
                ["center"] => Ok(points[0].clone()),
                _ => Err(bad()),
            },
        }
    }
}

/// The kind of entity an engine id names, from the id alone (`l3` is a line, `l3.end` a point).
#[derive(Debug, Clone, Copy, PartialEq)]
enum Kind {
    Point,
    Line,
    Circle,
    Arc,
    Spline,
    Ellipse,
}

impl Kind {
    fn of(id: &str) -> Kind {
        if id == "origin" || id.ends_with(".start") || id.ends_with(".end") || id.ends_with(".center") {
            return Kind::Point;
        }
        match id.as_bytes().first() {
            Some(b'p') => Kind::Point,
            Some(b'c') => Kind::Circle,
            Some(b'a') => Kind::Arc,
            Some(b's') => Kind::Spline,
            Some(b'e') => Kind::Ellipse,
            // `l…` and the sketch axes `x`, `y`.
            _ => Kind::Line,
        }
    }
    fn name(self) -> &'static str {
        match self {
            Kind::Point => "a point",
            Kind::Line => "a line",
            Kind::Circle => "a circle",
            Kind::Arc => "an arc",
            Kind::Spline => "a spline",
            Kind::Ellipse => "an ellipse",
        }
    }
    fn is_curve(self) -> bool {
        self != Kind::Point
    }
    fn is_round(self) -> bool {
        matches!(self, Kind::Circle | Kind::Arc)
    }
}

/// `what` (a constraint or dimension) given `refs` of these kinds does not fit: the error names
/// what it takes and what each argument is.
fn kind_err(line: usize, what: &str, takes: &str, refs: &[&Ref], kinds: &[Kind]) -> String {
    let got: Vec<String> = refs.iter().zip(kinds).map(|(r, k)| format!("`{}` is {}", r.text(), k.name())).collect();
    err(line, format!("`{what}` takes {takes}; {}", got.join(", ")))
}

/// Does a constraint accept entities of these kinds? `None` when the count is wrong (reported
/// elsewhere); otherwise what it takes, for the error.
fn constraint_fits(kind: &str, k: &[Kind]) -> Result<(), &'static str> {
    use Kind::*;
    let ok = match (kind, k) {
        ("horizontal" | "vertical", [a]) => a == &Line,
        ("horizontal" | "vertical", [a, b]) => a == &Point && b == &Point,
        ("coincident", [a, b]) => *a == Point || *b == Point || (*a == Line && *b == Line),
        ("tangent", [a, b]) => a.is_curve() && b.is_curve() && !(*a == Line && *b == Line),
        ("equal", [a, b]) => (*a == Line && *b == Line) || (a.is_round() && b.is_round()),
        ("parallel" | "perpendicular" | "collinear", [a, b]) => *a == Line && *b == Line,
        ("concentric", [a, b]) => a.is_round() && b.is_round(),
        ("midpoint", [a, b]) => *a == Point && *b == Line,
        ("symmetric", [a, b]) => (*a == Point && *b == Point) || (a.is_curve() && b.is_curve()),
        ("curvature", [a, b]) => a.is_curve() && b.is_curve(),
        _ => true,
    };
    if ok {
        return Ok(());
    }
    Err(match kind {
        "horizontal" | "vertical" => "a line, or two points",
        "coincident" => "two points, a point and a curve, or two lines",
        "tangent" => "two curves, at least one of them round",
        "equal" => "two lines, or two circles or arcs",
        "parallel" | "perpendicular" | "collinear" => "two lines",
        "concentric" => "two circles or arcs",
        "midpoint" => "a point and a line",
        "symmetric" => "two points, or two curves",
        "curvature" => "two curves",
        _ => "other entities",
    })
}

/// Does a dimension of this type measure entities of these kinds?
fn dimension_fits(kind: Option<&str>, k: &[Kind]) -> Result<(), &'static str> {
    use Kind::*;
    let ok = match (kind.unwrap_or("auto"), k) {
        ("radius" | "diameter", [a]) => a.is_round(),
        ("angle", [a, b]) => *a == Line && *b == Line,
        ("length", [a]) => *a == Line,
        ("distance" | "horizontal" | "vertical", [a]) => *a == Line,
        ("distance" | "horizontal" | "vertical", [a, b]) => matches!((a, b), (Point, Point) | (Point, Line) | (Line, Point)),
        ("auto", [a]) => *a == Line || a.is_round(),
        ("auto", [a, b]) => matches!((a, b), (Point, Point) | (Line, Line) | (Point, Line) | (Line, Point)),
        _ => false,
    };
    if ok {
        return Ok(());
    }
    Err(match kind.unwrap_or("auto") {
        "radius" | "diameter" => "one circle or arc",
        "angle" => "two lines",
        "length" => "one line",
        "distance" | "horizontal" | "vertical" => "a line, two points, or a point and a line",
        _ => "a line, a circle or arc, two points, two lines, or a point and a line",
    })
}

fn pt(p: &[Comp; 2]) -> Value {
    comps(p)
}

/// A sketch point: coordinates as given, or the id of the point it refers to.
fn ptv(t: &mut SketchTable, line: usize, p: &Pt) -> R<Value> {
    match p {
        Pt::Xy(xy) => Ok(pt(xy)),
        Pt::Ref(r) => Ok(json!(t.resolve(line, r)?)),
    }
}

fn ptvs(t: &mut SketchTable, line: usize, pts: &[Pt]) -> R<Vec<Value>> {
    pts.iter().map(|p| ptv(t, line, p)).collect()
}

fn comps(v: &[Comp]) -> Value {
    Value::Array(
        v.iter()
            .map(|c| match c {
                Comp::Num(n, _) => json!(n),
                Comp::Expr(e) => json!(e),
            })
            .collect(),
    )
}

fn cmd(id: &str, params: Value) -> Value {
    json!({"command": id, "params": params})
}

/// A number stays a number in the script, anything else is an expression string.
fn expr(s: &str) -> Value {
    match s.parse::<f64>() {
        Ok(n) if n.is_finite() => json!(n),
        _ => json!(s),
    }
}

#[derive(Debug)]
pub struct Compiled {
    /// Native documents bypass command replay and retain their exact identities.
    pub native: Option<crate::native::Native>,
    pub script: Value,
    pub sketches: usize,
    pub features: usize,
    /// Values, dependencies and lowering roles of authored expressions.
    pub expressions: Value,
    /// The source line each command in `script.commands` came from (the source map).
    pub lines: Vec<usize>,
    /// The source line of each dimension-creating statement, in the order the engine numbers
    /// its model parameters `d1`, `d2`, …: `dim` statements, and `None` for a `rect rounded` or
    /// sketch `fillet` (each makes one radius dimension the file holds as an option, not a value).
    pub dims: Vec<Option<usize>>,
    /// Linked parts (`component Name from "part.sc"`), whose `component.insert_design` commands
    /// name the part's source until `link::prepare` builds it and points them at the design.
    pub links: Vec<Link>,
    /// `check` statements, verified by `evaluate` after the build.
    pub checks: Vec<crate::checks::Check>,
    /// The layout `print` statements describe, for `evaluate`'s print requests.
    pub print_layout: Option<Value>,
    /// Left-hand threads: the thread command's index and its body. On an engine without thread
    /// handedness, `assembly::fit_script` cuts them by mirroring (see `left_thread_fallback`).
    pub left_threads: Vec<(usize, String)>,
    /// Path flanges: the command's index and the same chain as edge flanges (or why it can't be
    /// written so). On an engine without path flanges, `assembly::fit_script` uses the chain.
    pub path_flanges: Vec<(usize, Result<Vec<Value>, String>)>,
}

#[derive(Debug, Clone)]
pub struct Link {
    /// Index of the `component.insert_design` command in `script.commands`.
    pub command: usize,
    /// The part file as written in the source.
    pub source: String,
    pub line: usize,
}

pub fn compile(d: &Design) -> R<Compiled> {
    if crate::assembly::present(d) { crate::assembly::compile(d) } else { compile_geometry(d) }
}

pub(crate) fn compile_geometry(d: &Design) -> R<Compiled> {
    let lowered = crate::expressions::lower(d)?;
    let d = &lowered.design;
    let environment = crate::expressions::Environment::new(d)?;
    let mut out: Vec<Value> = Vec::new();
    let mut lines: Vec<usize> = Vec::new();
    let mut dims: Vec<Option<usize>> = Vec::new();
    let mut sketches: HashMap<String, SketchTable> = HashMap::new();
    let mut names: HashMap<String, usize> = HashMap::new();
    let mut features = 0;
    let mut left_threads = Vec::new();
    let mut path_flanges = Vec::new();
    for p in &d.params {
        if names.insert(p.name.clone(), p.line).is_some() {
            return Err(err(p.line, format!("parameter `{}` is defined twice", p.name)));
        }
        if let Some(words) = p.text() {
            out.push(cmd("parameters.add", json!({"name": p.name, "expression": words, "unit": "text"})));
            lines.push(p.line);
            continue;
        }
        let value = environment.values.get(&p.name).ok_or_else(|| err(p.line, "missing evaluated parameter"))?;
        let unit = match value.kind() {
            Some(solidcraft_expr::Kind::Angle) => "deg",
            _ => crate::expressions::unit(value),
        };
        let mut m = json!({"name": p.name, "expression": p.expr, "unit":unit});
        if let Some(c) = &p.comment {
            m["comment"] = json!(c);
        }
        out.push(cmd("parameters.add", m));
        lines.push(p.line);
    }
    for item in &d.items {
        match item {
            Item::Sketch(s) => {
                if let Some(prev) = names.insert(s.name.clone(), s.line) {
                    return Err(err(s.line, format!("`{}` is already the name of what line {prev} defines", s.name)));
                }
                let t = compile_sketch(s, &mut out, &mut lines, &mut dims)?;
                sketches.insert(s.name.clone(), t);
            }
            Item::Feature(f) => {
                if let Some(prev) = names.insert(f.name.clone(), f.line) {
                    return Err(err(f.line, format!("`{}` is already the name of what line {prev} defines", f.name)));
                }
                for mut c in compile_feature(f, &mut sketches)? {
                    // A cone's profile sketch makes model dimensions, like a drawn sketch's.
                    if c["command"] == "sketch.dimension" {
                        dims.push(None);
                    }
                    if let Some(body) = c["params"].as_object_mut().and_then(|p| p.remove("_left_body")) {
                        left_threads.push((out.len(), body.as_str().unwrap_or("").to_string()));
                    }
                    out.push(c);
                    lines.push(f.line);
                }
                if f.kind == "path_flange" {
                    let chain = path_flange_chain(f, d, &environment);
                    // Slots for the chain's other flanges, filled only on an engine that needs them.
                    for _ in 1..chain.as_ref().map_or(1, Vec::len) {
                        out.push(placeholder());
                        lines.push(f.line);
                    }
                    path_flanges.push((out.len() - chain.as_ref().map_or(1, Vec::len), chain));
                }
                features += 1;
            }
        }
    }
    Ok(Compiled {
        native: None,
        script: json!({"commands": out}),
        sketches: sketches.len(),
        features,
        lines,
        dims,
        expressions: {
            let mut e = lowered.explanation;
            // Primitives newer engines add, refused up front by older ones.
            let uses = |kind: &str, flag: Option<&str>| {
                d.items.iter().any(|i| matches!(i, Item::Feature(f) if f.kind == kind && flag.is_none_or(|k| f.opts.iter().any(|(o, _)| o == k))))
            };
            if let Some(r) = e["requires"].as_array_mut() {
                if uses("thread", Some("left")) {
                    r.push(json!("thread_handedness"));
                }
                let text_expression = d
                    .items
                    .iter()
                    .any(|i| matches!(i, Item::Sketch(s) if s.stmts.iter().any(|st| matches!(st.kind, SketchKind::Text { expression: true, .. }))));
                if uses("path_flange", None) {
                    r.push(json!("sheet_path_flange"));
                }
                if uses("revolve", Some("axis_face"))
                    || uses("revolve", Some("axis_edge"))
                    || uses("pattern_circular", Some("axis_face"))
                    || uses("pattern_circular", Some("axis_edge"))
                {
                    r.push(json!("model_axes"));
                }
                // A circular pattern about a construction axis's name (engine 0.27+).
                let named_axis = d.items.iter().any(|i| {
                    matches!(i, Item::Feature(f) if f.kind == "pattern_circular"
                        && f.opts.iter().any(|(k, v)| k == "axis" && matches!(v, Val::Ref(r) if !matches!(r.base.as_str(), "X" | "Y" | "Z"))))
                });
                if named_axis {
                    r.push(json!("construction_axes"));
                }
                if text_expression || d.params.iter().any(|p| p.text().is_some()) {
                    r.push(json!("text_parameters"));
                }
            }
            e
        },
        links: vec![],
        checks: vec![],
        print_layout: None,
        left_threads,
        path_flanges,
    })
}

fn compile_sketch(s: &Sketch, out: &mut Vec<Value>, lines: &mut Vec<usize>, dims: &mut Vec<Option<usize>>) -> R<SketchTable> {
    let mut create = json!({"name": s.name});
    create["plane"] = match &s.plane {
        Plane::Named(n) => json!(n),
        Plane::Face(c) => json!({"face": comps(c)}),
    };
    if let Some(o) = &s.offset {
        create["offset"] = expr(o);
    }
    if s.flip {
        create["flip"] = json!(true);
    }
    out.push(cmd("sketch.create", create));
    lines.push(s.line);
    let mut t = SketchTable { names: HashMap::new(), axes_used: Vec::new(), counters: Counters::default() };
    let first = out.len();
    // Text, offsets, mirrors and patterns make curves whose number depends on what they copy, so
    // only more of them, and constraints, may follow: nothing whose ids must be predicted.
    let mut tail: Option<(usize, &str)> = None;
    for st in &s.stmts {
        let line = st.line;
        let derived = match &st.kind {
            SketchKind::Text { .. } => Some("text"),
            SketchKind::Offset { .. } => Some("an offset"),
            SketchKind::Mirror { .. } => Some("a mirror"),
            SketchKind::Pattern { .. } => Some("a pattern"),
            SketchKind::Import { .. } => Some("an import"),
            SketchKind::Project { .. } => Some("a projection"),
            SketchKind::Engine { .. } => Some("an engine command"),
            _ => None,
        };
        let constraint = matches!(st.kind, SketchKind::Dim { .. } | SketchKind::Constraint { .. } | SketchKind::Fix { .. });
        if let Some((at, what)) = tail
            && derived.is_none()
            && !constraint
        {
            return Err(err(
                line,
                format!("{what} (line {at}) must come after the sketch's drawn entities: the engine numbers the curves it makes by what it copies"),
            ));
        }
        if tail.is_none()
            && let Some(what) = derived
        {
            tail = Some((line, what));
        }
        match &st.kind {
            SketchKind::Dim { .. } => dims.push(Some(line)),
            SketchKind::Rect { kind: RectKind::Rounded(_), .. } | SketchKind::Fillet { .. } | SketchKind::Offset { .. } => dims.push(None),
            _ => {}
        }
        let before = out.len();
        match &st.kind {
            SketchKind::Line { name, pts, construction, infer } => {
                let id = t.counters.l();
                let points = ptvs(&mut t, line, pts)?;
                t.define(line, name, Entity::Line(id))?;
                let mut p = json!({"points": points});
                if *construction {
                    p["construction"] = json!(true);
                }
                if *infer {
                    p["infer"] = json!(true);
                }
                out.push(cmd("sketch.line", p));
            }
            SketchKind::Polyline { name, pts, closed, construction, infer } => {
                let n = if *closed { pts.len() } else { pts.len() - 1 };
                if *closed && pts.len() < 3 {
                    return Err(err(line, "a closed polyline needs at least 3 points"));
                }
                let lines: Vec<String> = (0..n).map(|_| t.counters.l()).collect();
                let points = ptvs(&mut t, line, pts)?;
                t.define(line, name, Entity::Polyline { lines, closed: *closed })?;
                let mut p = json!({"points": points});
                if *closed {
                    p["closed"] = json!(true);
                }
                if *construction {
                    p["construction"] = json!(true);
                }
                if *infer {
                    p["infer"] = json!(true);
                }
                out.push(cmd("sketch.line", p));
            }
            SketchKind::Rect { name, kind, p0, p1, construction } => {
                let sides = [t.counters.l(), t.counters.l(), t.counters.l(), t.counters.l()];
                let (command, params, center, arcs, corners) = match kind {
                    RectKind::TwoPoint => {
                        let corners = [t.counters.p(), t.counters.p(), t.counters.p(), t.counters.p()];
                        ("sketch.rectangle.two_point", json!({"p0": pt(p0), "p1": pt(p1)}), None, None, corners)
                    }
                    RectKind::Center => {
                        // Two construction diagonals, then the four corners and the centre.
                        t.counters.l();
                        t.counters.l();
                        let corners = [t.counters.p(), t.counters.p(), t.counters.p(), t.counters.p()];
                        let center = t.counters.p();
                        ("sketch.rectangle.center", json!({"center": pt(p0), "corner": pt(p1)}), Some(center), None, corners)
                    }
                    RectKind::Rounded(r) => {
                        // 4 arcs, each with 2 end points; no sharp corners.
                        let arcs = [t.counters.a(), t.counters.a(), t.counters.a(), t.counters.a()];
                        for _ in 0..8 {
                            t.counters.p();
                        }
                        let none = [String::new(), String::new(), String::new(), String::new()];
                        ("sketch.rectangle.rounded", json!({"p0": pt(p0), "p1": pt(p1), "radius": expr(r)}), None, Some(arcs), none)
                    }
                };
                let mut params = params;
                if *construction {
                    params["construction"] = json!(true);
                }
                t.define(line, name, Entity::Rect { sides, corners, center, arcs })?;
                out.push(cmd(command, params));
            }
            SketchKind::Circle { name, center, size } => {
                let id = t.counters.c();
                let center = ptv(&mut t, line, center)?;
                t.define(line, name, Entity::Circle(id))?;
                let mut p = json!({"center": center});
                match size {
                    CircleSize::Radius(r) => p["radius"] = expr(r),
                    CircleSize::Diameter(d) => p["diameter"] = expr(d),
                }
                out.push(cmd("sketch.circle.center", p));
            }
            SketchKind::ArcCenter { name, center, start, end } => {
                let id = t.counters.a();
                let (center, start, end) = (ptv(&mut t, line, center)?, ptv(&mut t, line, start)?, ptv(&mut t, line, end)?);
                t.define(line, name, Entity::Arc(id))?;
                out.push(cmd("sketch.arc.center_point", json!({"center": center, "start": start, "end": end})));
            }
            SketchKind::ArcThree { name, start, through, end } => {
                let id = t.counters.a();
                let (start, through, end) = (ptv(&mut t, line, start)?, ptv(&mut t, line, through)?, ptv(&mut t, line, end)?);
                t.define(line, name, Entity::Arc(id))?;
                out.push(cmd("sketch.arc.three_point", json!({"start": start, "through": through, "end": end})));
            }
            SketchKind::Point { name, p, bound } => {
                let id = t.counters.p();
                t.define(line, name, Entity::Point(id.clone()))?;
                let point = if *bound { Value::Array(p.iter().map(|c| json!(crate::expressions::text(c))).collect()) } else { pt(p) };
                let mut params = json!({"point":point});
                if *bound {
                    params["connect"] = json!(false);
                }
                out.push(cmd("sketch.point", params));
                if *bound && p.iter().all(|c| solidcraft_expr::references(crate::expressions::text(c)).is_empty()) {
                    out.push(cmd("sketch.constraint.fix", json!({"entities":[id],"fixed":true})));
                }
            }
            SketchKind::Polygon { name, center, radius, sides, circumscribed } => {
                let sides: usize = sides.parse().map_err(|_| err(line, "invalid polygon sides after expression lowering"))?;
                let lines: Vec<String> = (0..sides).map(|_| t.counters.l()).collect();
                let points: Vec<String> = (0..sides).map(|_| t.counters.p()).collect();
                let circle = t.counters.c();
                t.define(line, name, Entity::Polygon { lines, points, circle })?;
                let id = if *circumscribed { "sketch.polygon.circumscribed" } else { "sketch.polygon.inscribed" };
                out.push(cmd(id, json!({"center": pt(center), "radius": expr(radius), "sides": sides})));
            }
            SketchKind::Slot { name, centers, p0, p1, width } => {
                let l1 = t.counters.l();
                let a1 = t.counters.a();
                let l2 = t.counters.l();
                let a2 = t.counters.a();
                let points = [t.counters.p(), t.counters.p(), t.counters.p(), t.counters.p()];
                t.define(line, name, Entity::Slot { lines: [l1, l2], arcs: [a1, a2], points })?;
                let id = if *centers { "sketch.slot.center_to_center" } else { "sketch.slot.overall" };
                out.push(cmd(id, json!({"p0": pt(p0), "p1": pt(p1), "width": expr(width)})));
            }
            SketchKind::Spline { name, pts, periodic } => {
                t.counters.s += 1;
                let id = format!("s{}", t.counters.s);
                let values = ptvs(&mut t, line, pts)?;
                // Only points drawn by coordinates are new points of the spline.
                let points: Vec<String> = pts.iter().filter(|p| matches!(p, Pt::Xy(_))).map(|_| t.counters.p()).collect();
                t.define(line, name, Entity::Spline { id, points })?;
                let mut p = json!({"points": values});
                if *periodic {
                    p["periodic"] = json!(true);
                }
                out.push(cmd("sketch.spline.fit_point", p));
            }
            SketchKind::Curve { .. } => return Err(err(line, "internal: an equation curve reached the compiler unsampled")),
            SketchKind::Ellipse { name, center, major, minor } => {
                t.counters.e += 1;
                let id = format!("e{}", t.counters.e);
                let points = [t.counters.p(), t.counters.p()];
                t.define(line, name, Entity::Ellipse { id, points })?;
                out.push(cmd("sketch.ellipse", json!({"center": pt(center), "major": pt(major), "minor_radius": expr(minor)})));
            }
            SketchKind::Text { text, expression, at, height, angle, align, font, bold, italic } => {
                let mut p = json!({"at": pt(at)});
                p[if *expression { "text_expression" } else { "text" }] = json!(text);
                for (k, v) in [("height", height), ("angle", angle), ("font", font)] {
                    if let Some(v) = v {
                        p[k] = expr(v);
                    }
                }
                if let Some(a) = align {
                    if !matches!(a.as_str(), "left" | "center" | "right") {
                        return Err(err(line, format!("`align` is left, center or right, not `{a}`")));
                    }
                    p["align"] = json!(a);
                }
                if *bold {
                    p["bold"] = json!(true);
                }
                if *italic {
                    p["italic"] = json!(true);
                }
                out.push(cmd("sketch.text", p));
            }
            SketchKind::Dim { kind, refs, value } => {
                let ids: Vec<String> = refs.iter().map(|r| t.resolve(line, r)).collect::<R<_>>()?;
                let kinds: Vec<Kind> = ids.iter().map(|i| Kind::of(i)).collect();
                if let Err(takes) = dimension_fits(kind.as_deref(), &kinds) {
                    let what = format!("dim {}", kind.as_deref().unwrap_or("auto"));
                    let refs: Vec<&Ref> = refs.iter().collect();
                    return Err(kind_err(line, &what, takes, &refs, &kinds));
                }
                let mut p = json!({"entities": ids, "value": expr(value)});
                if let Some(k) = kind {
                    p["type"] = json!(k);
                }
                out.push(cmd("sketch.dimension", p));
            }
            SketchKind::Constraint { kind, refs, about } => {
                let ids: Vec<String> = refs.iter().map(|r| t.resolve(line, r)).collect::<R<_>>()?;
                let kinds: Vec<Kind> = ids.iter().map(|i| Kind::of(i)).collect();
                if let Err(takes) = constraint_fits(kind, &kinds) {
                    let refs: Vec<&Ref> = refs.iter().collect();
                    return Err(kind_err(line, kind, takes, &refs, &kinds));
                }
                let two = |ids: &[String]| -> R<(String, String)> {
                    match ids {
                        [a, b] => Ok((a.clone(), b.clone())),
                        _ => Err(err(line, format!("`{kind}` takes exactly two entities"))),
                    }
                };
                let c = match kind.as_str() {
                    "horizontal" | "vertical" => match ids.as_slice() {
                        [l] => cmd("sketch.constraint.horizontal_vertical", json!({"line": l, "mode": kind})),
                        [a, b] => cmd("sketch.constraint.horizontal_vertical", json!({"points": [a, b], "mode": kind})),
                        _ => return Err(err(line, format!("`{kind}` takes a line or two points"))),
                    },
                    "midpoint" => {
                        let (p, l) = two(&ids)?;
                        cmd("sketch.constraint.midpoint", json!({"point": p, "line": l}))
                    }
                    "symmetric" => {
                        let (a, b) = two(&ids)?;
                        let about = about.as_ref().ok_or_else(|| err(line, "`symmetric` needs `about`"))?;
                        let about_id = t.resolve(line, about)?;
                        if !matches!(Kind::of(&about_id), Kind::Line | Kind::Point) {
                            return Err(err(
                                line,
                                format!("`symmetric … about` takes a line or a point; `{}` is {}", about.text(), Kind::of(&about_id).name()),
                            ));
                        }
                        cmd("sketch.constraint.symmetry", json!({"a": a, "b": b, "line": about_id}))
                    }
                    k => {
                        let (a, b) = two(&ids)?;
                        cmd(&format!("sketch.constraint.{k}"), json!({"a": a, "b": b}))
                    }
                };
                out.push(c);
            }
            SketchKind::Offset { refs, distance, side, symmetric } => {
                let mut p = json!({"curves": t.curves(line, refs)?, "distance": expr(distance)});
                if let Some(side) = side {
                    p["side"] = pt(side);
                }
                if *symmetric {
                    p["direction"] = json!("symmetric");
                }
                out.push(cmd("sketch.offset", p));
            }
            SketchKind::Mirror { refs, about } => {
                let line_id = t.resolve(line, about)?;
                if Kind::of(&line_id) != Kind::Line {
                    return Err(err(line, format!("mirror about a line, not {}", Kind::of(&line_id).name())));
                }
                out.push(cmd("sketch.mirror", json!({"entities": t.curves(line, refs)?, "line": line_id})));
            }
            SketchKind::Pattern { refs, circular, count, center, angle, spacing, dir, count2, spacing2, dir2 } => {
                let mut p = json!({"entities": t.curves(line, refs)?, "count": expr(count)});
                if *circular {
                    p["center"] = match center {
                        Some(c) => ptv(&mut t, line, c)?,
                        None => json!([0.0, 0.0]),
                    };
                    if let Some(a) = angle {
                        p["angle"] = expr(a);
                    }
                    out.push(cmd("sketch.pattern.circular", p));
                } else {
                    for (k, v) in [("spacing", spacing), ("count2", count2), ("spacing2", spacing2)] {
                        if let Some(v) = v {
                            p[k] = expr(v);
                        }
                    }
                    for (k, v) in [("dir", dir), ("dir2", dir2)] {
                        if let Some(v) = v {
                            p[k] = pt(v);
                        }
                    }
                    out.push(cmd("sketch.pattern.rectangular", p));
                }
            }
            SketchKind::Engine { command, params } => {
                out.push(cmd(command, serde_json::from_str(params).map_err(|e| err(line, e))?));
            }
            SketchKind::Project { what, point, body } => {
                let mut r = json!({});
                match point {
                    Some(p) => r[what.as_str()] = comps(p),
                    None => r["body"] = json!(body),
                }
                if let (Some(b), Some(_)) = (body, point) {
                    r["body"] = json!(b);
                }
                out.push(cmd("sketch.project", json!({"refs": [r]})));
            }
            SketchKind::Import { format, path, at, scale } => {
                // `run`, `evaluate` and `watch` replace the path with the file's text (link::inline_files).
                let mut p = json!({"path": path});
                if let Some(at) = at {
                    p["at"] = pt(at);
                }
                if let Some(k) = scale {
                    p["scale"] = expr(k);
                }
                out.push(cmd(&format!("sketch.insert_{format}"), p));
            }
            SketchKind::Fix { refs } => {
                // A whole polyline, rectangle, slot or polygon fixes all of its curves.
                let ids = t.curves(line, refs)?;
                out.push(cmd("sketch.constraint.fix", json!({"entities": ids, "fixed": true})));
            }
            SketchKind::Fillet { point, radius } => {
                let id = t.resolve(line, point)?;
                if Kind::of(&id) != Kind::Point {
                    return Err(err(
                        line,
                        format!("`fillet` takes a corner point such as `outline.2.end`; `{}` is {}", point.text(), Kind::of(&id).name()),
                    ));
                }
                // The engine replaces the corner with an arc (and its two end points).
                t.counters.a();
                t.counters.p();
                t.counters.p();
                out.push(cmd("sketch.fillet", json!({"point": id, "radius": expr(radius)})));
            }
            SketchKind::Chamfer { point, distance, distance2, angle } => {
                let id = t.resolve(line, point)?;
                if Kind::of(&id) != Kind::Point {
                    return Err(err(
                        line,
                        format!("`chamfer` takes a corner point such as `outline.2.end`; `{}` is {}", point.text(), Kind::of(&id).name()),
                    ));
                }
                // The engine cuts the corner with a line (and its two end points).
                t.counters.l();
                t.counters.p();
                t.counters.p();
                let mut p = json!({"point": id, "distance": expr(distance)});
                let id = match (distance2, angle) {
                    (Some(d2), _) => {
                        p["distance2"] = expr(d2);
                        "sketch.chamfer.two_distance"
                    }
                    (None, Some(a)) => {
                        p["angle"] = expr(a);
                        "sketch.chamfer.distance_angle"
                    }
                    (None, None) => "sketch.chamfer.equal_distance",
                };
                out.push(cmd(id, p));
            }
        }
        lines.extend(std::iter::repeat_n(line, out.len() - before));
    }
    if s.explicit {
        const CREATORS: &[&str] = &[
            "sketch.line",
            "sketch.point",
            "sketch.circle.",
            "sketch.arc.",
            "sketch.rectangle.",
            "sketch.polygon.",
            "sketch.slot.",
            "sketch.spline.",
        ];
        for c in &mut out[first..] {
            let is_creator = c["command"].as_str().is_some_and(|id| CREATORS.iter().any(|p| id.starts_with(p)));
            if is_creator {
                c["params"]["connect"] = json!(false);
            }
        }
    }
    out.push(cmd("sketch.finish", json!({})));
    lines.push(s.line);
    Ok(t)
}

struct Opts<'a> {
    f: &'a Feature,
    used: Vec<bool>,
}

impl<'a> Opts<'a> {
    fn new(f: &'a Feature) -> Opts<'a> {
        Opts { f, used: vec![false; f.opts.len()] }
    }
    fn take(&mut self, key: &str) -> Option<&'a Val> {
        let i = self.f.opts.iter().position(|(k, _)| k == key)?;
        self.used[i] = true;
        Some(&self.f.opts[i].1)
    }
    fn expr(&mut self, key: &str) -> R<Option<Value>> {
        match self.take(key) {
            None => Ok(None),
            Some(Val::Expr(e)) => Ok(Some(expr(e))),
            Some(_) => Err(err(self.f.line, format!("`{key}` takes an expression"))),
        }
    }
    fn ident(&mut self, key: &str) -> R<Option<String>> {
        match self.take(key) {
            None => Ok(None),
            Some(Val::Expr(e)) => Ok(Some(e.clone())),
            Some(_) => Err(err(self.f.line, format!("`{key}` takes a name"))),
        }
    }
    fn tuples(&mut self, key: &str) -> R<Option<Vec<Value>>> {
        match self.take(key) {
            None => Ok(None),
            Some(Val::Tuples(ts)) => Ok(Some(ts.iter().map(|t| comps(t)).collect())),
            Some(_) => Err(err(self.f.line, format!("`{key}` takes `(x, y, z)` tuples"))),
        }
    }
    fn tuple(&mut self, key: &str) -> R<Option<Value>> {
        let Some(mut ts) = self.tuples(key)? else { return Ok(None) };
        if ts.len() != 1 {
            return Err(err(self.f.line, format!("`{key}` takes one tuple")));
        }
        Ok(ts.pop())
    }
    fn list(&mut self, key: &str) -> R<Option<Vec<String>>> {
        match self.take(key) {
            None => Ok(None),
            Some(Val::List(l)) => Ok(Some(l.clone())),
            Some(Val::Expr(e)) => Ok(Some(vec![e.clone()])),
            Some(_) => Err(err(self.f.line, format!("`{key}` takes names separated by commas"))),
        }
    }
    fn flag(&mut self, key: &str) -> bool {
        self.take(key).is_some()
    }
    /// The first of `keys` given as a flag, e.g. the boolean operation.
    fn one_of(&mut self, keys: &[&str]) -> R<Option<String>> {
        let mut found = None;
        for k in keys {
            if self.flag(k) {
                if found.is_some() {
                    return Err(err(self.f.line, format!("give one of {}", keys.join(", "))));
                }
                found = Some(k.to_string());
            }
        }
        Ok(found)
    }
    fn require(&mut self, key: &str) -> R<Value> {
        self.expr(key)?.ok_or_else(|| err(self.f.line, format!("`{}` needs `{key} <expr>`", self.f.kind)))
    }
    fn finish(self, mut params: Value) -> R<Value> {
        for (i, (k, _)) in self.f.opts.iter().enumerate() {
            if !self.used[i] {
                return Err(err(self.f.line, format!("`{k}` does not apply to `{}`", self.f.kind)));
            }
        }
        params["name"] = json!(self.f.name);
        Ok(params)
    }
}

fn operation(o: &mut Opts, p: &mut Value) -> R<()> {
    if let Some(op) = o.one_of(&["new", "join", "cut", "intersect", "auto"])? {
        p["operation"] = json!(op);
    }
    if let Some(t) = o.list("targets")? {
        p["targets"] = json!(t);
    }
    if let Some(b) = o.ident("body")? {
        p["body_name"] = json!(b);
    }
    Ok(())
}

fn profiles(o: &mut Opts, p: &mut Value) -> R<()> {
    if let Some(ts) = o.tuples("profiles")? {
        let picks: Vec<Value> = ts.into_iter().map(|t| json!({"point": t})).collect();
        p["profiles"] = json!(picks);
    }
    Ok(())
}

/// `Sketch.entity` (or `entity` within `default_sketch`) to its engine id.
fn sketch_ref(r: &Ref, default_sketch: Option<&str>, sketches: &mut HashMap<String, SketchTable>, line: usize) -> R<(String, String)> {
    let (sketch, inner) = if sketches.contains_key(&r.base) {
        let Some((first, rest)) = r.parts.split_first() else {
            return Err(err(line, format!("`{}` names a sketch: add the entity, `{}.<entity>`", r.base, r.base)));
        };
        (r.base.clone(), Ref { base: first.clone(), parts: rest.to_vec() })
    } else if let Some(s) = default_sketch {
        (s.to_string(), r.clone())
    } else {
        return Err(err(line, format!("`{}` is not a sketch; write `Sketch.entity`", r.base)));
    };
    let t = sketches.get_mut(&sketch).ok_or_else(|| err(line, format!("unknown sketch `{sketch}`")))?;
    let id = t.resolve(line, &inner)?;
    Ok((sketch, id))
}

fn compile_feature(f: &Feature, sketches: &mut HashMap<String, SketchTable>) -> R<Vec<Value>> {
    let line = f.line;
    let mut o = Opts::new(f);
    let mut p = json!({});
    let surface = matches!(f.kind.as_str(), "extrude" | "revolve" | "loft" | "sweep") && o.flag("surface");
    let id = match f.kind.as_str() {
        // A surface body thickened into a solid: new, or joined to / cut from targets.
        "thicken" => {
            p["bodies"] = json!(o.list("bodies")?.ok_or_else(|| err(line, "`thicken` needs `bodies A, B` (surface bodies)"))?);
            // Along the surface's normal (out of a closed loft or revolve); `flip`: against it.
            let t = o.require("thickness")?;
            p["thickness"] = if o.flag("flip") {
                match t {
                    Value::String(e) => json!(format!("-({e})")),
                    v => json!(-v.as_f64().unwrap_or(0.0)),
                }
            } else {
                t
            };
            if o.flag("symmetric") {
                p["symmetric"] = json!(true);
            }
            operation(&mut o, &mut p)?;
            "surface.thicken"
        }
        "extrude" => {
            let sketch = o.ident("from")?.ok_or_else(|| err(line, "`extrude` needs `from <sketch>`"))?;
            if !sketches.contains_key(&sketch) {
                return Err(err(line, format!("unknown sketch `{sketch}`")));
            }
            p["sketch"] = json!(sketch);
            if o.flag("through_all") {
                p["through_all"] = json!(true);
            } else {
                p["distance"] = o.require("distance")?;
            }
            if let Some(d) = o.one_of(&["symmetric", "negative", "positive"])? {
                p["direction"] = json!(d);
            }
            for (k, pk) in [("taper", "taper"), ("offset", "start_offset"), ("distance2", "distance2")] {
                if let Some(v) = o.expr(k)? {
                    p[pk] = v;
                }
            }
            profiles(&mut o, &mut p)?;
            operation(&mut o, &mut p)?;
            "solid.extrude"
        }
        "revolve" => {
            let sketch = o.ident("from")?.ok_or_else(|| err(line, "`revolve` needs `from <sketch>`"))?;
            if !sketches.contains_key(&sketch) {
                return Err(err(line, format!("unknown sketch `{sketch}`")));
            }
            p["sketch"] = json!(sketch);
            let modelled = model_axis(&mut o)?;
            p["axis"] = match o.take("axis") {
                None if modelled.is_some() => modelled.unwrap_or_default(),
                Some(Val::Ref(r)) if r.parts.is_empty() && matches!(r.base.as_str(), "X" | "Y" | "Z" | "x" | "y") => json!(r.base),
                Some(Val::Ref(r)) => {
                    let id = sketch_ref(r, Some(&sketch), sketches, line)?.1;
                    if Kind::of(&id) != Kind::Line {
                        return Err(err(line, format!("`axis` takes a sketch line; `{}` is {}", r.text(), Kind::of(&id).name())));
                    }
                    json!(id)
                }
                Some(_) => return Err(err(line, "`axis` is X, Y, Z, x, y or a sketch line")),
                None => return Err(err(line, "`revolve` needs `axis`")),
            };
            if let Some(a) = o.expr("angle")? {
                p["angle"] = a;
            }
            if o.flag("symmetric") {
                p["direction"] = json!("symmetric");
            }
            profiles(&mut o, &mut p)?;
            operation(&mut o, &mut p)?;
            "solid.revolve"
        }
        "hole" => {
            match o.take("at") {
                Some(Val::Tuples(ts)) if ts.len() == 1 => p["position"] = comps(&ts[0]),
                Some(Val::List(refs)) => {
                    let mut sketch = None;
                    let mut points = Vec::new();
                    for r in refs {
                        let (base, parts) = match r.split_once('.') {
                            Some((b, rest)) => (b.to_string(), rest.split('.').map(str::to_string).collect()),
                            None => (r.clone(), Vec::new()),
                        };
                        if parts.is_empty() {
                            if !sketches.contains_key(&base) {
                                return Err(err(line, format!("unknown sketch `{base}`")));
                            }
                            sketch = Some(base);
                        } else {
                            let r = Ref { base, parts };
                            let (s, id) = sketch_ref(&r, None, sketches, line)?;
                            if Kind::of(&id) != Kind::Point {
                                return Err(err(line, format!("`at` takes sketch points; `{}` is {}", r.text(), Kind::of(&id).name())));
                            }
                            if sketch.as_ref().is_some_and(|x| *x != s) {
                                return Err(err(line, "all hole points must be in one sketch"));
                            }
                            sketch = Some(s);
                            points.push(id);
                        }
                    }
                    p["sketch"] = json!(sketch);
                    if !points.is_empty() {
                        p["points"] = json!(points);
                    }
                }
                _ => return Err(err(line, "`hole` needs `at <Sketch>`, `at Sketch.a, Sketch.b` or `at (x,y,z)`")),
            }
            let mut size = 0;
            for k in ["diameter", "clearance", "thread"] {
                if let Some(v) = o.expr(k)? {
                    p[k] = v;
                    size += 1;
                }
            }
            if size != 1 {
                return Err(err(line, "`hole` needs exactly one of `diameter`, `clearance` or `thread`"));
            }
            for k in ["depth", "fit"] {
                if let Some(v) = o.expr(k)? {
                    p[k] = v;
                }
            }
            if let Some(Val::List(v)) = o.take("counterbore") {
                p["type"] = json!("counterbore");
                p["cb_diameter"] = expr(&v[0]);
                p["cb_depth"] = expr(&v[1]);
            }
            if let Some(Val::List(v)) = o.take("countersink") {
                p["type"] = json!("countersink");
                p["cs_diameter"] = expr(&v[0]);
                p["cs_angle"] = expr(&v[1]);
            }
            if o.flag("drilled") {
                p["type"] = json!("drilled");
            }
            if o.flag("flip") {
                p["flip"] = json!(true);
            }
            if let Some(t) = o.tuple("to")? {
                p["to"] = t;
            }
            "solid.hole"
        }
        "box" => {
            match o.take("size") {
                Some(Val::List(v)) if v.len() == 3 => {
                    p["length"] = expr(&v[0]);
                    p["width"] = expr(&v[1]);
                    p["height"] = expr(&v[2]);
                }
                _ => return Err(err(line, "`box` needs `size <length> <width> <height>`")),
            }
            for k in ["corner", "center"] {
                if let Some(t) = o.tuple(k)? {
                    p[k] = t;
                }
            }
            operation(&mut o, &mut p)?;
            "solid.box"
        }
        "cylinder" => {
            match (o.expr("radius")?, o.expr("diameter")?) {
                (Some(r), None) => p["radius"] = r,
                (None, Some(d)) => p["diameter"] = d,
                _ => return Err(err(line, "`cylinder` needs `radius` or `diameter`")),
            }
            p["height"] = o.require("height")?;
            if let Some(t) = o.tuple("base")? {
                p["base"] = t;
            }
            match o.take("axis") {
                Some(Val::Ref(r)) if r.parts.is_empty() => p["axis"] = json!(r.base),
                Some(Val::Tuples(t)) if t.len() == 1 => p["axis"] = comps(&t[0]),
                Some(_) => return Err(err(line, "`axis` is X, Y, Z or `(x,y,z)`")),
                None => {}
            }
            operation(&mut o, &mut p)?;
            "solid.cylinder"
        }
        // A revolved profile, so it builds on every engine: a right trapezoid (or a triangle, for a
        // point) beside the axis, its radius, top radius and height live sketch dimensions.
        "cone" => {
            let half = |e: Value| match e {
                Value::String(s) => json!(format!("({s}) / 2")),
                Value::Number(n) => json!(n.as_f64().unwrap_or(0.0) / 2.0),
                v => v,
            };
            let r = match (o.expr("radius")?, o.expr("diameter")?) {
                (Some(r), None) => r,
                (None, Some(d)) => half(d),
                _ => return Err(err(line, "`cone` needs `radius` or `diameter` (the base)")),
            };
            let rt = match (o.expr("top_radius")?, o.expr("top_diameter")?) {
                (Some(r), None) => Some(r),
                (None, Some(d)) => Some(half(d)),
                (None, None) => None,
                _ => return Err(err(line, "give `top_radius` or `top_diameter`, not both")),
            };
            let h = o.require("height")?;
            let seed = |o: &mut Opts, k: &str| -> f64 { o.expr(k).ok().flatten().and_then(|v| v.as_f64()).unwrap_or(0.0) };
            let (sr, srt, sh) = (seed(&mut o, "_r"), seed(&mut o, "_rt"), seed(&mut o, "_h"));
            let rt = rt.filter(|_| srt > 1e-9);
            let base: Vec<f64> = match o.tuple("base")? {
                Some(Value::Array(b)) => b.iter().map(|v| v.as_f64().unwrap_or(0.0)).collect(),
                _ => vec![0.0; 3],
            };
            let axis = match o.take("axis") {
                None => "Z".to_string(),
                Some(Val::Ref(r)) if r.parts.is_empty() && matches!(r.base.as_str(), "X" | "Y" | "Z") => r.base.clone(),
                Some(_) => return Err(err(line, "a cone's `axis` is X, Y or Z")),
            };
            // (radial, axial) to sketch coordinates; whether the axis runs vertically in the sketch.
            let (plane, offset, vertical): (&str, f64, bool) = match axis.as_str() {
                "Z" => ("XZ", base[1], true),
                "X" => ("XY", base[2], false),
                _ => ("XY", base[2], true),
            };
            let at = |radial: f64, axial: f64| -> Value {
                match axis.as_str() {
                    "Z" => json!([base[0] + radial, -(base[2] + axial)]),
                    "X" => json!([base[0] + axial, base[1] + radial]),
                    _ => json!([base[0] + radial, base[1] + axial]),
                }
            };
            let sketch = format!("{}Profile", f.name);
            let mut out = vec![cmd("sketch.create", json!({"name": sketch, "plane": plane, "offset": offset}))];
            let mode = if vertical { "vertical" } else { "horizontal" };
            let axis_line = if let Some(rt) = &rt {
                out.push(cmd("sketch.line", json!({"points": [at(0.0, 0.0), at(sr, 0.0), at(srt, sh), at(0.0, sh)], "closed": true})));
                out.push(cmd("sketch.constraint.parallel", json!({"a": "l3", "b": "l1"})));
                out.push(cmd("sketch.dimension", json!({"entities": ["l3"], "value": rt})));
                "l4"
            } else {
                out.push(cmd("sketch.line", json!({"points": [at(0.0, 0.0), at(sr, 0.0), at(0.0, sh)], "closed": true})));
                "l3"
            };
            out.push(cmd("sketch.constraint.fix", json!({"entities": ["l1.start"], "fixed": true})));
            out.push(cmd("sketch.constraint.horizontal_vertical", json!({"line": axis_line, "mode": mode})));
            out.push(cmd("sketch.constraint.perpendicular", json!({"a": "l1", "b": axis_line})));
            out.push(cmd("sketch.dimension", json!({"entities": ["l1"], "value": r})));
            out.push(cmd("sketch.dimension", json!({"entities": [axis_line], "value": h})));
            out.push(cmd("sketch.finish", json!({})));
            p["sketch"] = json!(sketch);
            p["axis"] = json!(axis_line);
            operation(&mut o, &mut p)?;
            out.push(cmd("solid.revolve", o.finish(p)?));
            return Ok(out);
        }
        "sphere" => {
            match (o.expr("radius")?, o.expr("diameter")?) {
                (Some(r), None) => p["radius"] = r,
                (None, Some(d)) => p["diameter"] = d,
                _ => return Err(err(line, "`sphere` needs `radius` or `diameter`")),
            }
            if let Some(t) = o.tuple("center")? {
                p["center"] = t;
            }
            operation(&mut o, &mut p)?;
            "solid.sphere"
        }
        "torus" => {
            p["major"] = o.require("major")?;
            p["minor"] = o.require("minor")?;
            if let Some(t) = o.tuple("center")? {
                p["center"] = t;
            }
            operation(&mut o, &mut p)?;
            "solid.torus"
        }
        "fillet" | "chamfer" => {
            p["edges"] = json!(o.tuples("edges")?.ok_or_else(|| err(line, format!("`{}` needs `edges (x,y,z) …`", f.kind)))?);
            if f.kind == "fillet" {
                p["radius"] = o.require("radius")?;
            } else {
                p["distance"] = o.require("distance")?;
                if let Some(v) = o.expr("distance2")? {
                    p["distance2"] = v;
                }
                if let Some(v) = o.expr("angle")? {
                    p["angle"] = v;
                }
            }
            if let Some(b) = o.ident("body")? {
                p["body"] = json!(b);
            }
            if f.kind == "fillet" { "solid.fillet" } else { "solid.chamfer" }
        }
        "shell" => {
            p["faces"] = json!(o.tuples("faces")?.ok_or_else(|| err(line, "`shell` needs `faces (x,y,z) …`"))?);
            p["thickness"] = o.require("thickness")?;
            if let Some(d) = o.one_of(&["inside", "outside", "both"])? {
                p["direction"] = json!(d);
            }
            if let Some(b) = o.ident("body")? {
                p["body"] = json!(b);
            }
            "solid.shell"
        }
        "mirror" => {
            if let Some(v) = o.list("features")? {
                p["features"] = json!(v);
            } else if let Some(v) = o.list("bodies")? {
                p["bodies"] = json!(v);
            } else {
                return Err(err(line, "`mirror` needs `features A, B` or `bodies A, B`"));
            }
            p["plane"] = match (o.ident("plane")?, o.tuple("origin")?, o.tuple("normal")?) {
                (Some(n), None, None) => json!(n),
                (None, Some(origin), Some(normal)) => json!({"origin": origin, "normal": normal}),
                _ => return Err(err(line, "`mirror` needs `plane XY|XZ|YZ|<name>` or `origin (…) normal (…)`")),
            };
            "solid.mirror"
        }
        "pattern" => {
            if let Some(v) = o.list("features")? {
                p["features"] = json!(v);
            } else if let Some(v) = o.list("bodies")? {
                p["bodies"] = json!(v);
            } else {
                return Err(err(line, "`pattern` needs `features A, B` or `bodies A, B`"));
            }
            // Along a path: `path Sketch.a, Sketch.b count n spacing s|distance d [identical] [flip]`.
            if let Some(refs) = o.list("path")? {
                let mut path_sketch: Option<String> = None;
                let mut curves = Vec::new();
                for r in refs {
                    let Some((base, rest)) = r.split_once('.') else {
                        return Err(err(line, format!("`path` takes sketch curves, `Sketch.curve`; `{r}` names no curve")));
                    };
                    let r = Ref { base: base.to_string(), parts: rest.split('.').map(str::to_string).collect() };
                    let (s, id) = sketch_ref(&r, None, sketches, line)?;
                    if path_sketch.as_ref().is_some_and(|x| *x != s) {
                        return Err(err(line, "all path curves must be in one sketch"));
                    }
                    path_sketch = Some(s);
                    curves.push(id);
                }
                p["path_sketch"] = json!(path_sketch);
                p["path"] = json!(curves);
                p["count"] = o.require("count")?;
                match (o.expr("spacing")?, o.expr("distance")?) {
                    (Some(s), None) => p["spacing"] = s,
                    (None, Some(d)) => p["distance"] = d,
                    _ => return Err(err(line, "a path pattern needs `spacing` (between copies) or `distance` (first to last)")),
                }
                p["orientation"] = json!(if o.flag("identical") { "identical" } else { "path" });
                if o.flag("flip") {
                    p["flip"] = json!(true);
                }
                return Ok(vec![cmd("solid.pattern.path", o.finish(p)?)]);
            }
            p["dir1"] = o.tuple("dir")?.ok_or_else(|| err(line, "`pattern` needs `dir (x,y,z)`"))?;
            p["count1"] = o.require("count")?;
            p["spacing1"] = o.require("spacing")?;
            if let Some(d) = o.tuple("dir2")? {
                p["dir2"] = d;
                p["count2"] = o.require("count2")?;
                p["spacing2"] = o.require("spacing2")?;
            }
            for k in ["reversed", "reversed2"] {
                if o.flag(k) {
                    p[k] = json!(true);
                }
            }
            "solid.pattern.rectangular"
        }
        "pattern_circular" => {
            if let Some(v) = o.list("features")? {
                p["features"] = json!(v);
            } else if let Some(v) = o.list("bodies")? {
                p["bodies"] = json!(v);
            } else {
                return Err(err(line, "`pattern circular` needs `features A, B` or `bodies A, B`"));
            }
            p["axis"] = match o.take("axis") {
                Some(Val::Ref(r)) if r.parts.is_empty() => json!(r.base),
                _ => match (model_axis(&mut o)?, o.tuple("origin")?, o.tuple("dir")?) {
                    (Some(a), ..) => a,
                    (None, Some(origin), Some(dir)) => json!({"origin": origin, "dir": dir}),
                    _ => return Err(err(line, "`pattern circular` needs `axis X|Y|Z`, `origin (…) dir (…)`, or `axis_face`/`axis_edge (x,y,z)`")),
                },
            };
            p["count"] = o.require("count")?;
            if let Some(a) = o.expr("angle")? {
                p["angle"] = a;
            }
            "solid.pattern.circular"
        }
        "combine" => {
            p["target"] = json!(o.ident("target")?.ok_or_else(|| err(line, "`combine` needs `target <body>`"))?);
            p["tools"] = json!(o.list("tools")?.ok_or_else(|| err(line, "`combine` needs `tools A, B`"))?);
            if let Some(op) = o.one_of(&["join", "cut", "intersect"])? {
                p["operation"] = json!(op);
            }
            if o.flag("keep_tools") {
                p["keep_tools"] = json!(true);
            }
            "solid.combine"
        }
        "move" => {
            p["bodies"] = json!(o.list("bodies")?.ok_or_else(|| err(line, "`move` needs `bodies A, B`"))?);
            if let Some(t) = o.tuple("to")? {
                p["translate"] = t;
            }
            if let Some(a) = o.tuple("axis")? {
                p["axis"] = a;
            }
            if let Some(a) = o.expr("angle")? {
                p["angle"] = a;
            }
            if o.flag("copy") {
                p["copy"] = json!(true);
            }
            "solid.move"
        }
        "remove" => {
            p["bodies"] = json!(o.list("bodies")?.ok_or_else(|| err(line, "`remove` needs `bodies A, B`"))?);
            "solid.remove"
        }
        "loft" => {
            let sections = o.list("sections")?.ok_or_else(|| err(line, "`loft` needs `sections A, B …` (sketch names, in order)"))?;
            if sections.len() < 2 {
                return Err(err(line, "`loft` needs at least two sections"));
            }
            for s in &sections {
                if !sketches.contains_key(s) {
                    return Err(err(line, format!("unknown sketch `{s}`")));
                }
            }
            p["sections"] = json!(sections.iter().map(|s| json!({"sketch": s})).collect::<Vec<_>>());
            operation(&mut o, &mut p)?;
            "solid.loft"
        }
        "sweep" | "pipe" => {
            if f.kind == "sweep" {
                let sketch = o.ident("from")?.ok_or_else(|| err(line, "`sweep` needs `from <profile sketch>`"))?;
                if !sketches.contains_key(&sketch) {
                    return Err(err(line, format!("unknown sketch `{sketch}`")));
                }
                p["sketch"] = json!(sketch);
                profiles(&mut o, &mut p)?;
                for k in ["twist", "scale"] {
                    if let Some(v) = o.expr(k)? {
                        p[k] = v;
                    }
                }
                if o.flag("parallel") {
                    p["orientation"] = json!("parallel");
                }
            } else {
                p["diameter"] = o.require("diameter")?;
                if let Some(v) = o.expr("wall")? {
                    p["wall"] = v;
                }
            }
            let refs = o.list("path")?.ok_or_else(|| err(line, format!("`{}` needs `path Sketch.a, Sketch.b …` (curves in order)", f.kind)))?;
            let mut path_sketch: Option<String> = None;
            let mut curves = Vec::new();
            for r in refs {
                let Some((base, rest)) = r.split_once('.') else {
                    return Err(err(line, format!("`path` takes sketch curves, `Sketch.curve`; `{r}` names no curve")));
                };
                let r = Ref { base: base.to_string(), parts: rest.split('.').map(str::to_string).collect() };
                let (s, id) = sketch_ref(&r, None, sketches, line)?;
                if !Kind::of(&id).is_curve() {
                    return Err(err(line, format!("`path` takes curves; `{}` is {}", r.text(), Kind::of(&id).name())));
                }
                if path_sketch.as_ref().is_some_and(|x| *x != s) {
                    return Err(err(line, "all path curves must be in one sketch"));
                }
                path_sketch = Some(s);
                curves.push(id);
            }
            p["path_sketch"] = json!(path_sketch);
            p["path"] = json!(curves);
            operation(&mut o, &mut p)?;
            if f.kind == "sweep" { "solid.sweep" } else { "solid.pipe" }
        }
        // A construction plane, for sketches (`sketch S on Name`) and features that take a plane.
        "plane" => {
            let base = |o: &mut Opts| -> R<Value> {
                match (o.ident("on")?, o.tuple("face")?) {
                    (Some(b), None) => Ok(json!(b)),
                    (None, Some(f)) => Ok(json!({"face": f})),
                    (None, None) => Ok(json!("XY")),
                    _ => Err(err(line, "give the base plane `on <plane>` or `face (x,y,z)`, not both")),
                }
            };
            if let Some(ts) = o.tuples("through")? {
                let [a, b, c]: [Value; 3] = ts.try_into().map_err(|_| err(line, "`through` takes three points"))?;
                for (k, v) in [("a", a), ("b", b), ("c", c)] {
                    p[k] = json!({"point": v});
                }
                "construct.plane.three_points"
            } else if let Some(planes) = o.list("between")? {
                let [a, b]: [String; 2] = planes.try_into().map_err(|_| err(line, "`between` takes two planes"))?;
                (p["a"], p["b"]) = (json!(a), json!(b));
                "construct.plane.midplane"
            } else if let Some(refs) = o.list("path")? {
                let [r]: [String; 1] = refs.try_into().map_err(|_| err(line, "`path` takes one sketch curve"))?;
                let Some((sk, rest)) = r.split_once('.') else {
                    return Err(err(line, format!("`path` takes a sketch curve, `Sketch.curve`; `{r}` names no curve")));
                };
                let r = Ref { base: sk.to_string(), parts: rest.split('.').map(str::to_string).collect() };
                let (sketch, curve) = sketch_ref(&r, None, sketches, line)?;
                p["path"] = json!({"sketch": sketch, "curve": curve});
                if let Some(t) = o.expr("fraction")? {
                    p["t"] = t;
                }
                "construct.plane.along_path"
            } else if let Some(angle) = o.expr("angle")? {
                p["base"] = base(&mut o)?;
                p["angle"] = angle;
                p["axis"] = match o.take("axis") {
                    Some(Val::Ref(r)) if r.parts.is_empty() && matches!(r.base.as_str(), "X" | "Y" | "Z") => json!(r.base),
                    Some(Val::Tuples(t)) if t.len() == 1 => json!({"origin": o.tuple("origin")?.unwrap_or(json!([0, 0, 0])), "dir": comps(&t[0])}),
                    _ => return Err(err(line, "a plane at an angle needs `axis X|Y|Z` or `axis (x,y,z)`, the hinge line")),
                };
                "construct.plane.at_angle"
            } else {
                p["base"] = base(&mut o)?;
                p["offset"] = o.expr("offset")?.unwrap_or(json!(0));
                "construct.plane.offset"
            }
        }
        "split" => {
            let target = o.ident("target")?.ok_or_else(|| err(line, "`split` needs `target <body>`"))?;
            p["body"] = json!(target);
            p["plane"] = match (o.ident("plane")?, o.tuple("origin")?, o.tuple("normal")?) {
                (Some(pl), None, None) => json!(pl),
                (None, Some(origin), Some(normal)) => json!({"origin": origin, "normal": normal}),
                _ => return Err(err(line, "`split` needs `plane <plane>` or `origin (x,y,z) normal (x,y,z)`")),
            };
            // The piece behind the plane (against its normal) becomes a new body, `<target> (1)`.
            let piece = o.ident("body")?;
            let params = o.finish(p)?;
            let mut out = vec![cmd("solid.split_body", params)];
            if let Some(piece) = piece {
                out.push(cmd("body.rename", json!({"body": format!("{target} (1)"), "name": piece})));
            }
            return Ok(out);
        }
        "rib" | "web" => {
            let sketch = o.ident("from")?.ok_or_else(|| err(line, format!("`{}` needs `from <sketch>`", f.kind)))?;
            p["curves"] = json!(sketch_curves(&mut o, "curves", &sketch, sketches)?);
            p["sketch"] = json!(sketch);
            p["thickness"] = o.require("thickness")?;
            if let Some(d) = o.expr("depth")? {
                p["depth"] = d;
            }
            if o.flag("flip") {
                p["flip"] = json!(true);
            }
            if let Some(d) = o.ident("direction")? {
                if !matches!(d.as_str(), "in_plane" | "normal") {
                    return Err(err(line, "`direction` is in_plane or normal"));
                }
                p["direction"] = json!(d);
            }
            if f.kind == "rib" { "solid.rib" } else { "solid.web" }
        }
        "draft" => {
            p["faces"] = json!(o.tuples("faces")?.ok_or_else(|| err(line, "`draft` needs `faces (x,y,z) …`"))?);
            p["angle"] = o.require("angle")?;
            p["neutral"] = json!(o.ident("neutral")?.unwrap_or_else(|| "XY".into()));
            if let Some(pull) = o.tuple("pull")? {
                p["pull"] = pull;
            }
            if let Some(b) = o.ident("body")? {
                p["body"] = json!(b);
            }
            "solid.draft"
        }
        "offset_faces" => {
            p["faces"] = json!(o.tuples("faces")?.ok_or_else(|| err(line, "`offset_faces` needs `faces (x,y,z) …`"))?);
            p["distance"] = o.require("distance")?;
            if let Some(b) = o.ident("body")? {
                p["body"] = json!(b);
            }
            "solid.offset_face"
        }
        "scale" => {
            p["bodies"] = json!(o.list("bodies")?.ok_or_else(|| err(line, "`scale` needs `bodies A, B`"))?);
            match (o.expr("factor")?, o.tuple("factors")?) {
                (Some(k), None) => p["factor"] = k,
                (None, Some(ks)) => p["factors"] = ks,
                _ => return Err(err(line, "`scale` needs `factor k` or `factors (kx, ky, kz)`")),
            }
            if let Some(origin) = o.tuple("origin")? {
                p["origin"] = origin;
            }
            "solid.scale"
        }
        "engine" => {
            let raw = |k: &str| match f.opts.iter().find(|(o, _)| o == k) {
                Some((_, Val::Raw(s))) => Ok(s.clone()),
                _ => Err(err(line, "internal: engine statement without its command")),
            };
            let params: Value = serde_json::from_str(&raw("params")?).map_err(|e| err(line, e))?;
            return Ok(vec![cmd(&raw("command")?, params)]);
        }
        "suppress" => {
            let features = o.list("features")?.unwrap_or_default();
            o.finish(json!({}))?;
            return Ok(vec![cmd("timeline.suppress", json!({"features": features, "suppressed": true}))]);
        }
        // `color Body "#rrggbb"` and `material Body Name`: properties, not timeline features.
        "color" | "material" => {
            p["bodies"] = json!(o.list("bodies")?.unwrap_or_default());
            let value = o.ident("value")?.unwrap_or_default();
            let id = if f.kind == "color" {
                let hex = value.strip_prefix('#').filter(|h| h.len() == 6 && h.chars().all(|c| c.is_ascii_hexdigit()));
                if hex.is_none() {
                    return Err(err(line, format!("a colour is \"#rrggbb\", not `{value}`")));
                }
                p["color"] = json!(value);
                if let Some(faces) = o.tuples("faces")? {
                    // Faces of one body; the points pick them.
                    let Some([body]) = p["bodies"].as_array().map(Vec::as_slice) else {
                        return Err(err(line, "color the faces of one body at a time"));
                    };
                    p["body"] = body.clone();
                    p["faces"] = json!(faces);
                    if let Some(m) = p.as_object_mut() {
                        m.remove("bodies");
                    }
                }
                "appearance.assign"
            } else {
                p["material"] = json!(value);
                "material.assign"
            };
            let mut params = o.finish(p)?;
            if let Some(m) = params.as_object_mut() {
                m.remove("name");
            }
            return Ok(vec![cmd(id, params)]);
        }
        "thread" => {
            p["face"] = o.tuple("face")?.ok_or_else(|| err(line, "`thread` needs `face (x,y,z)` on a cylindrical face"))?;
            if let Some(d) = o.ident("designation")? {
                p["designation"] = json!(d);
            }
            if let Some(v) = o.expr("length")? {
                p["length"] = v;
            }
            if o.flag("modeled") {
                p["modeled"] = json!(true);
            }
            // Left-handed: native on engines with thread handedness (0.27+); on others, cut as
            // the mirror image of a right-hand thread, which needs the body and five more slots.
            let left = o.flag("left");
            if left {
                p["handedness"] = json!("left");
            }
            // `clearance c internal|external axis A origin (…) body B`: the engine cuts the basic
            // profile, so open the fit by scaling the threaded body about the thread's axis,
            // out for an internal thread and in for an external one, by c at the major radius.
            if let Some(c) = o.expr("clearance")? {
                let side = o.one_of(&["internal", "external"])?.ok_or_else(|| err(line, "a thread clearance needs `internal` or `external`"))?;
                let designation =
                    p["designation"].as_str().ok_or_else(|| err(line, "a thread clearance needs the `designation` (for its diameter)"))?;
                let major: f64 = designation
                    .trim_start_matches(['M', 'm'])
                    .split(['x', 'X'])
                    .next()
                    .and_then(|d| d.parse().ok())
                    .ok_or_else(|| err(line, format!("cannot read a diameter from `{designation}`")))?;
                let body = o.ident("body")?.ok_or_else(|| err(line, "a thread clearance needs `body <name>`, the part to open"))?;
                let axis = match o.take("axis") {
                    Some(Val::Ref(r)) if r.parts.is_empty() && matches!(r.base.as_str(), "X" | "Y" | "Z") => r.base.clone(),
                    _ => return Err(err(line, "a thread clearance needs the thread's `axis X|Y|Z`")),
                };
                let origin = o.tuple("origin")?.ok_or_else(|| err(line, "a thread clearance needs `origin (x,y,z)` on the thread's axis"))?;
                let c = match &c {
                    Value::String(s) => s.clone(),
                    v => format!("{v} mm"),
                };
                let k = format!("1 {} ({c}) / ({} mm)", if side == "internal" { "+" } else { "-" }, major / 2.0);
                let factors: Vec<Value> = ["X", "Y", "Z"].iter().map(|a| if *a == axis { json!(1) } else { json!(k) }).collect();
                if left {
                    p["_left_body"] = json!(body);
                }
                let thread = o.finish(p)?;
                let name = format!("{}Clearance", f.name);
                let mut out = vec![cmd("solid.thread", thread)];
                if left {
                    out.extend(std::iter::repeat_n(placeholder(), 5));
                }
                out.push(cmd("solid.scale", json!({"bodies": [body], "factors": factors, "origin": origin, "name": name})));
                return Ok(out);
            }
            if left {
                let body = o.ident("body")?.ok_or_else(|| err(line, "a left-hand thread needs `body <name>`, the threaded part"))?;
                p["_left_body"] = json!(body);
                let mut out = vec![cmd("solid.thread", o.finish(p)?)];
                out.extend(std::iter::repeat_n(placeholder(), 5));
                return Ok(out);
            }
            "solid.thread"
        }
        "coil" => {
            p["diameter"] = o.require("diameter")?;
            let mut given = 0;
            for k in ["turns", "height", "pitch"] {
                if let Some(v) = o.expr(k)? {
                    p[k] = v;
                    given += 1;
                }
            }
            if given != 2 {
                return Err(err(line, "`coil` needs two of `turns`, `height` and `pitch`"));
            }
            p["section_size"] = o.require("section_size")?;
            if o.flag("square") {
                p["section"] = json!("square");
            }
            if let Some(v) = o.ident("section_position")? {
                p["section_position"] = json!(v);
            }
            if let Some(t) = o.tuple("base")? {
                p["base"] = t;
            }
            match o.take("axis") {
                Some(Val::Ref(r)) if r.parts.is_empty() => p["axis"] = json!(r.base),
                Some(Val::Tuples(t)) if t.len() == 1 => p["axis"] = comps(&t[0]),
                Some(_) => return Err(err(line, "`axis` is X, Y, Z or `(x,y,z)`")),
                None => {}
            }
            if let Some(v) = o.expr("start_angle")? {
                p["start_angle"] = v;
            }
            if o.flag("clockwise") {
                p["clockwise"] = json!(true);
            }
            operation(&mut o, &mut p)?;
            "solid.coil"
        }
        "emboss" => {
            let sketch = o.ident("from")?.ok_or_else(|| err(line, "`emboss` needs `from <sketch>`"))?;
            if !sketches.contains_key(&sketch) {
                return Err(err(line, format!("unknown sketch `{sketch}`")));
            }
            p["sketch"] = json!(sketch);
            p["depth"] = o.require("depth")?;
            p["mode"] = json!(if o.flag("deboss") { "deboss" } else { "emboss" });
            profiles(&mut o, &mut p)?;
            if let Some(t) = o.list("targets")? {
                p["targets"] = json!(t);
            }
            if let Some(t) = o.tuple("face")? {
                p["face"] = t;
            }
            "solid.emboss"
        }
        "sheet_rule" => {
            for k in ["thickness", "bend_radius", "k_factor"] {
                if let Some(v) = o.expr(k)? {
                    p[k] = v;
                }
            }
            p["active"] = json!(true);
            let params = o.finish(p)?;
            return Ok(vec![cmd("sheet.manage_rules", params)]);
        }
        "flange" => {
            if let Some(rule) = o.ident("rule")? {
                p["rule"] = json!(rule);
            }
            if let Some(sketch) = o.ident("from")? {
                // Base flange: a sheet from a sketch profile, its thickness from the rule.
                if !sketches.contains_key(&sketch) {
                    return Err(err(line, format!("unknown sketch `{sketch}`")));
                }
                p["type"] = json!("base");
                p["sketch"] = json!(sketch);
                profiles(&mut o, &mut p)?;
                if o.flag("flip") {
                    p["flip"] = json!(true);
                }
                if let Some(b) = o.ident("body")? {
                    p["body_name"] = json!(b);
                }
            } else {
                // Edge flanges: bent up from the picked edges of a sheet body.
                p["type"] = json!("edge");
                p["edges"] = json!(o.tuples("edges")?.ok_or_else(|| err(line, "`flange` needs `from <sketch>` or `edges (x,y,z) …`"))?);
                p["height"] = o.require("height")?;
                if let Some(v) = o.expr("angle")? {
                    p["angle"] = v;
                }
                if let Some(v) = o.expr("radius")? {
                    p["radius"] = v;
                }
                if let Some(pos) = o.one_of(&["inside", "outside"])? {
                    p["position"] = json!(pos);
                }
                if o.flag("flip") {
                    p["flip"] = json!(true);
                }
                if let Some(b) = o.ident("body")? {
                    p["body"] = json!(b);
                }
            }
            "sheet.flange"
        }
        // Flanges along an open chain of sketch lines, bent from a sheet edge (engine 0.27+).
        "path_flange" => {
            p["edge"] = o.tuple("edge")?.ok_or_else(|| err(line, "`path_flange` needs `edge (x,y,z)` on a top or bottom edge of the sheet"))?;
            let refs =
                o.list("path")?.ok_or_else(|| err(line, "`path_flange` needs `path Sketch.a, Sketch.b` (lines from the edge out, in order)"))?;
            let mut sketch: Option<String> = None;
            let mut curves = Vec::new();
            for r in refs {
                let Some((base, rest)) = r.split_once('.') else {
                    return Err(err(line, format!("`path` takes sketch lines, `Sketch.line`; `{r}` names no line")));
                };
                let r = Ref { base: base.to_string(), parts: rest.split('.').map(str::to_string).collect() };
                let (s, id) = sketch_ref(&r, None, sketches, line)?;
                if Kind::of(&id) != Kind::Line {
                    return Err(err(line, format!("`path` takes lines; `{}` is {}", r.text(), Kind::of(&id).name())));
                }
                if sketch.as_ref().is_some_and(|x| *x != s) {
                    return Err(err(line, "all path lines must be in one sketch"));
                }
                sketch = Some(s);
                curves.push(id);
            }
            p["sketch"] = json!(sketch);
            p["curves"] = json!(curves);
            if let Some(r) = o.expr("radius")? {
                p["radius"] = r;
            }
            if let Some(b) = o.ident("body")? {
                p["body"] = json!(b);
            }
            "sheet.path_flange"
        }
        // A solid or shelled body made into sheet metal: `face` is the fixed face; the flat faces
        // reached from it across the `bends` edges become the sheet, the other edges between them
        // are cut open with the rule's gap.
        "convert" => {
            p["body"] = json!(o.ident("body")?.ok_or_else(|| err(line, "`convert` needs `body <name>`"))?);
            p["face"] = o.tuple("face")?.ok_or_else(|| err(line, "`convert` needs `face (x,y,z)` on the body's fixed flat face"))?;
            if let Some(r) = o.ident("rule")? {
                p["rule"] = json!(r);
            }
            if let Some(b) = o.tuples("bends")? {
                p["bends"] = json!(b);
            }
            "sheet.convert"
        }
        // A surface filling a closed loop: planar regions of a sketch (`from`), or a loop of a
        // body's edges (`edges … of B`), smooth when the loop isn't flat.
        "patch" => {
            if let Some(sketch) = o.ident("from")? {
                if !sketches.contains_key(&sketch) {
                    return Err(err(line, format!("unknown sketch `{sketch}`")));
                }
                p["sketch"] = json!(sketch);
                profiles(&mut o, &mut p)?;
            } else {
                p["edges"] =
                    json!(o.tuples("edges")?.ok_or_else(|| err(line, "`patch` needs `from <sketch>` or `edges (x,y,z) …` on a closed loop"))?);
                if let Some(b) = o.ident("of")? {
                    p["body"] = json!(b);
                }
                if let Some(g) = o.ident("continuity")? {
                    if !matches!(g.as_str(), "g0" | "g1" | "g2") {
                        return Err(err(line, format!("`continuity` is g0, g1 or g2, not `{g}`")));
                    }
                    p["continuity"] = json!(g);
                }
                if let Some(c) = o.expr("crown")? {
                    p["crown"] = c;
                }
                if let Some(t) = o.tuples("through")? {
                    p["points"] = json!(t);
                }
            }
            p["body_name"] = json!(o.ident("body")?.ok_or_else(|| err(line, "a patch is a surface body: give it `-> body <name>`"))?);
            "surface.patch"
        }
        // A face pushed (positive, adding material) or pulled (negative, cutting) along its normal.
        "press_pull" => {
            p["face"] = o.tuple("face")?.ok_or_else(|| err(line, "`press_pull` needs `face (x,y,z)` on the face to move"))?;
            p["distance"] = o.require("distance")?;
            "solid.press_pull"
        }
        // Faces split in two by a plane, without changing the solid: one part can then be moved.
        "split_face" => {
            p["faces"] = json!(o.tuples("faces")?.ok_or_else(|| err(line, "`split_face` needs `faces (x,y,z) …`"))?);
            if let Some(b) = o.ident("of")? {
                p["body"] = json!(b);
            }
            p["plane"] = match (o.ident("plane")?, o.tuple("origin")?, o.tuple("normal")?) {
                (Some(n), None, None) => json!(n),
                (None, Some(origin), Some(normal)) => json!({"origin": origin, "normal": normal}),
                _ => return Err(err(line, "`split_face` needs `plane XY|XZ|YZ|<name>` or `origin (…) normal (…)`")),
            };
            "solid.split_face"
        }
        // Faces copied along their normals as a new surface body (planes and cylinders exactly).
        "offset_surface" => {
            p["faces"] = json!(o.tuples("faces")?.ok_or_else(|| err(line, "`offset_surface` needs `faces (x,y,z) …`"))?);
            if let Some(b) = o.ident("of")? {
                p["body"] = json!(b);
            }
            p["distance"] = o.require("distance")?;
            if o.flag("chain") {
                p["chain"] = json!(true);
            }
            p["body_name"] = json!(o.ident("body")?.ok_or_else(|| err(line, "an offset surface is a body of its own: give it `-> body <name>`"))?);
            "surface.offset"
        }
        // Surfaces sewn into the first body; a solid when they close.
        "stitch" => {
            p["bodies"] = json!(o.list("bodies")?.ok_or_else(|| err(line, "`stitch` needs `bodies A, B …` (sewn into the first)"))?);
            "surface.stitch"
        }
        // A sheet edge folded back on itself.
        "hem" => {
            p["edges"] = json!(o.tuples("edges")?.ok_or_else(|| err(line, "`hem` needs `edges (x,y,z) …` on sheet edges"))?);
            p["length"] = o.require("length")?;
            if let Some(g) = o.expr("gap")? {
                p["gap"] = g;
            }
            if o.flag("flip") {
                p["flip"] = json!(true);
            }
            if let Some(b) = o.ident("body")? {
                p["body"] = json!(b);
            }
            "sheet.hem"
        }
        // A hole field over a flat sheet: in the flat pattern, the DXF and the mass, not the solid.
        "perforate" => {
            p["diameter"] = o.require("diameter")?;
            p["pitch"] = o.require("pitch")?;
            if let Some(m) = o.expr("margin")? {
                p["margin"] = m;
            }
            if o.flag("square") {
                p["pattern"] = json!("square");
            }
            if let Some(b) = o.ident("body")? {
                p["body"] = json!(b);
            }
            "sheet.perforate"
        }
        other => return Err(err(line, format!("`{other}` is not supported yet"))),
    };
    let id = if surface { surface_command(&mut o, &mut p, id, sketches)? } else { id };
    let params = o.finish(p)?;
    Ok(vec![cmd(id, params)])
}

/// `axis_face (x,y,z)` or `axis_edge (x,y,z)`: the axis of a cylinder, cone or torus face, or of a
/// straight or circular edge, followed as the model changes (engine 0.27+).
fn model_axis(o: &mut Opts) -> R<Option<Value>> {
    if let Some(p) = o.tuple("axis_face")? {
        return Ok(Some(json!({"face": p})));
    }
    Ok(o.tuple("axis_edge")?.map(|p| json!({"edge": p})))
}

/// `surface`: the extrude, revolve, loft or sweep makes a surface body (walls only, no caps)
/// instead of a solid. Its profile may be an open chain of sketch `curves`. `thicken` turns it
/// into a solid.
fn surface_command(o: &mut Opts, p: &mut Value, id: &str, sketches: &mut HashMap<String, SketchTable>) -> R<&'static str> {
    let line = o.f.line;
    if p.get("operation").is_some_and(|op| op != "new") || p.get("targets").is_some() {
        return Err(err(line, "a surface is a body of its own: give it `-> body <name>`, then `thicken` it to join or cut"));
    }
    if p.get("body_name").is_none() {
        return Err(err(line, "a surface needs `-> body <name>`"));
    }
    for k in ["taper", "through_all"] {
        if p.get(k).is_some() {
            return Err(err(line, format!("a surface extrude takes no `{k}`")));
        }
    }
    let Some(m) = p.as_object_mut() else { return Err(err(line, "internal: feature params are not an object")) };
    m.remove("operation");
    if id == "solid.revolve" && m.get("direction").is_some_and(|d| d == "symmetric") {
        m.remove("direction");
        m.insert("symmetric".into(), json!(true));
    }
    if let Some(sketch) = p["sketch"].as_str().map(str::to_string)
        && o.f.opts.iter().any(|(k, _)| k == "curves")
    {
        p["curves"] = json!(sketch_curves(o, "curves", &sketch, sketches)?);
    }
    Ok(match id {
        "solid.extrude" => "surface.extrude",
        "solid.revolve" => "surface.revolve",
        "solid.loft" => "surface.loft",
        _ => "surface.sweep",
    })
}

/// A path flange written as edge flanges, each bent from the free edge of the one before: its
/// line's length high, at the line's start, flipped by the rule in docs/shape-features.md
/// (checked against engine 0.27's path flange). Heights stay live expressions; which edge each
/// flange is bent from and which way it folds are decided with the parameters' current values.
fn path_flange_chain(f: &Feature, d: &Design, env: &crate::expressions::Environment) -> R<Vec<Value>> {
    let line = f.line;
    let opt = |k: &str| f.opts.iter().find(|(o, _)| o == k).map(|(_, v)| v);
    let length = |t: &str| env.number(t, solidcraft_expr::Kind::Length, line, "path flange");
    // A coordinate as a length expression: bare numbers are millimetres.
    let mm = |c: &Comp| match c {
        Comp::Num(v, _) => format!("{v} mm"),
        Comp::Expr(e) => e.clone(),
    };
    let sketch = |name: &str| {
        d.items.iter().find_map(|i| match i {
            Item::Sketch(s) if s.name == name => Some(s),
            _ => None,
        })
    };
    let body = match opt("body") {
        Some(Val::Expr(b)) => b.clone(),
        _ => return Err(err(line, "a path flange needs `body <name>` to be written as edge flanges")),
    };
    // The sheet's top face: the normal of the sketch its base flange came from.
    let base = d
        .items
        .iter()
        .find_map(|i| match i {
            Item::Feature(b) if b.kind == "flange" && b.opts.iter().any(|(k, v)| k == "body" && matches!(v, Val::Expr(n) if *n == body)) => {
                b.opts.iter().find_map(|(k, v)| match (k.as_str(), v) {
                    ("from", Val::Expr(s) | Val::Ref(Ref { base: s, .. })) => sketch(s),
                    _ => None,
                })
            }
            _ => None,
        })
        .ok_or_else(|| err(line, format!("no base flange of `{body}` drawn from a sketch")))?;
    // A plane's origin and sketch axes, as 3D expressions.
    let frame = |s: &Sketch| -> R<([f64; 3], [f64; 3], [f64; 3])> {
        if s.flip {
            return Err(err(line, format!("sketch `{}` is flipped; draw it unflipped to write the path flange as edge flanges", s.name)));
        }
        let o = s.offset.as_deref().map_or(Ok(0.0), length)?;
        Ok(match &s.plane {
            Plane::Named(n) if n == "XY" => ([0., 0., o], [1., 0., 0.], [0., 1., 0.]),
            Plane::Named(n) if n == "XZ" => ([0., o, 0.], [1., 0., 0.], [0., 0., -1.]),
            Plane::Named(n) if n == "YZ" => ([o, 0., 0.], [0., 1., 0.], [0., 0., 1.]),
            _ => return Err(err(line, format!("sketch `{}` must be on XY, XZ or YZ to write the path flange as edge flanges", s.name))),
        })
    };
    // A sketch point in 3D, at the parameters' current values.
    let place = |s: &Sketch, p: &[Comp; 2]| -> R<[f64; 3]> {
        let (o, u, v) = frame(s)?;
        let (a, b) = (length(&mm(&p[0]))?, length(&mm(&p[1]))?);
        Ok([0, 1, 2].map(|k| o[k] + u[k] * a + v[k] * b))
    };
    let path = match opt("path") {
        Some(Val::List(l)) => l.clone(),
        Some(Val::Expr(e)) => vec![e.clone()],
        _ => return Err(err(line, "`path_flange` needs `path Sketch.a, Sketch.b`")),
    };
    let mut points: Vec<[f64; 3]> = Vec::new();
    let mut lengths = Vec::new();
    let mut plane = None;
    for r in &path {
        let (sk, name) = r.split_once('.').ok_or_else(|| err(line, format!("`{r}` names no sketch line")))?;
        let s = sketch(sk).ok_or_else(|| err(line, format!("no sketch `{sk}`")))?;
        let pts = s
            .stmts
            .iter()
            .find_map(|st| match &st.kind {
                SketchKind::Line { name: n, pts, .. } if n == name => Some(pts),
                _ => None,
            })
            .ok_or_else(|| err(line, format!("`{r}` must be a `line` to write the path flange as edge flanges")))?;
        let [Pt::Xy(a), Pt::Xy(b)] = pts.as_slice() else {
            return Err(err(line, format!("`{r}` needs coordinates for both ends to write the path flange as edge flanges")));
        };
        let (start, end) = (place(s, a)?, place(s, b)?);
        if points.is_empty() {
            points.push(start);
        }
        points.push(end);
        let t = |k: usize| (mm(&a[k]), mm(&b[k]));
        lengths.push(format!("sqrt((({}) - ({}))^2 + (({}) - ({}))^2)", t(0).1, t(0).0, t(1).1, t(1).0));
        plane = Some(frame(s)?);
    }
    let (_, pu, pv) = plane.ok_or_else(|| err(line, "`path` names no lines"))?;
    let (_, bu, bv) = frame(base)?;
    let cross = |a: [f64; 3], b: [f64; 3]| [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
    let dot = |a: [f64; 3], b: [f64; 3]| a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    let sub = |a: [f64; 3], b: [f64; 3]| [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
    let top = cross(bu, bv);
    // The plate runs toward the edge: across the path's plane, away from the blank's middle.
    let mut corners = Vec::new();
    for st in &base.stmts {
        match &st.kind {
            SketchKind::Rect { kind: RectKind::Center, p0, .. } => corners.push(place(base, p0)?),
            SketchKind::Rect { p0, p1, .. } => {
                corners.push(place(base, p0)?);
                corners.push(place(base, p1)?);
            }
            SketchKind::Line { pts, .. } | SketchKind::Polyline { pts, .. } => {
                for p in pts {
                    if let Pt::Xy(p) = p {
                        corners.push(place(base, p)?);
                    }
                }
            }
            _ => {}
        }
    }
    if corners.is_empty() {
        return Err(err(line, format!("sketch `{}` needs drawn lines or rectangles to write the path flange as edge flanges", base.name)));
    }
    let middle = corners.iter().fold([0.0; 3], |m, c| [m[0] + c[0], m[1] + c[1], m[2] + c[2]]).map(|v| v / corners.len() as f64);
    let edge = match opt("edge") {
        Some(Val::Tuples(t)) if t.len() == 1 && t[0].len() == 3 => t[0].clone(),
        _ => return Err(err(line, "`path_flange` needs `edge (x,y,z)`")),
    };
    let at: [f64; 3] = [length(&mm(&edge[0]))?, length(&mm(&edge[1]))?, length(&mm(&edge[2]))?];
    let mut plate = cross(top, cross(pu, pv));
    if dot(plate, sub(at, middle)) < 0.0 {
        plate = plate.map(|v| -v);
    }
    let steps: Vec<[f64; 3]> = points.windows(2).map(|w| sub(w[1], w[0])).collect();
    let radius = match opt("radius") {
        Some(Val::Expr(r)) => Some(r.clone()),
        _ => None,
    };
    let mut chain = Vec::new();
    for (k, step) in steps.iter().enumerate() {
        let flip = match k {
            0 => dot(*step, top) > 0.0,
            1 => dot(*step, plate) < 0.0,
            _ => dot(*step, steps[k - 2]) < 0.0,
        };
        // Points only pick the edge, so they are numbers, like other flanges' edge points.
        let point = if k == 0 { at } else { points[k] };
        let mut p = json!({"edges": [point], "height": lengths[k], "body": body, "name": format!("{}{}", f.name, k + 1), "type": "edge"});
        if flip {
            p["flip"] = json!(true);
        }
        if let Some(r) = &radius {
            p["radius"] = json!(r);
        }
        chain.push(cmd("sheet.flange", p));
    }
    Ok(chain)
}

/// A read-only command holding a place in the script, for a fallback to fill (see `left_threads`).
fn placeholder() -> Value {
    cmd("document.inspect", json!({"measure": false}))
}

/// Sketch curves named in `key` (`curves a, b`), in the feature's `from` sketch.
fn sketch_curves(o: &mut Opts, key: &str, sketch: &str, sketches: &mut HashMap<String, SketchTable>) -> R<Vec<String>> {
    let line = o.f.line;
    let names = o.list(key)?.ok_or_else(|| err(line, format!("`{}` needs `{key} a, b` (curves of `{sketch}`)", o.f.kind)))?;
    names
        .iter()
        .map(|n| {
            let r = Ref { base: n.clone(), parts: vec![] };
            let (_, id) = sketch_ref(&r, Some(sketch), sketches, line)?;
            if !Kind::of(&id).is_curve() {
                return Err(err(line, format!("`{key}` takes curves; `{n}` is {}", Kind::of(&id).name())));
            }
            Ok(id)
        })
        .collect()
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    use crate::parse::parse;

    fn script(src: &str) -> Vec<Value> {
        let d = parse(src).unwrap();
        compile(&d).unwrap().script["commands"].as_array().unwrap().clone()
    }

    fn fails(src: &str) -> String {
        compile(&parse(src).unwrap()).unwrap_err()
    }

    #[test]
    fn projections_into_sketches() {
        let cmds = script(
            "param H = 30 mm\nbox B size 80 50 H -> body Box\nsketch S on XY offset (H + 2 mm) {\n line l = (0,0) (5,0)\n \
             project face (40, 25, H) body Box\n project body Box\n}\n",
        );
        let p: Vec<&Value> = cmds.iter().filter(|c| c["command"] == "sketch.project").map(|c| &c["params"]["refs"][0]).collect();
        assert_eq!(p, [&json!({"face": [40.0, 25.0, 30.0], "body": "Box"}), &json!({"body": "Box"})]);
        let e = fails("box B size 8 5 3 -> body Box\nsketch S on XY {\n project body Box\n line l = (0,0) (5,0)\n}\n");
        assert!(e.contains("a projection"), "{e}");
    }

    #[test]
    fn converting_a_body_to_sheet_metal() {
        let cmds = script(
            "sheet_rule S thickness 1 mm bend_radius 1 mm k_factor 0.4\nbox B size 50 40 20 -> body Tray\n\
             shell O faces (25,20,20) thickness 1 mm body Tray\nconvert Sheet body Tray face (25,20,0) rule S bends (25,0,0) (0,20,0)\n",
        );
        let c = cmds.iter().find(|c| c["command"] == "sheet.convert").unwrap();
        assert_eq!(
            c["params"],
            json!({"body": "Tray", "face": [25.0, 20.0, 0.0], "rule": "S", "bends": [[25.0, 0.0, 0.0], [0.0, 20.0, 0.0]], "name": "Sheet"})
        );
    }

    #[test]
    fn patterns_and_revolves_about_model_axes() {
        let c = compile(
            &parse(
                "box P size 100 60 5 -> body Part\ncylinder H radius 2 height 7 base (44,0,-1) cut targets Part\n\
                 pattern circular Ring features H axis_face (38,0,12) count 4 angle 360 deg\n\
                 sketch S on XZ {\n rect r = (50,0) (60,-5)\n}\nrevolve R from S axis_edge (40,0,0) angle 90 deg new -> body Q\n",
            )
            .unwrap(),
        )
        .unwrap();
        let cmds = c.script["commands"].as_array().unwrap();
        let by = |id: &str| cmds.iter().find(|c| c["command"] == id).unwrap()["params"]["axis"].clone();
        assert_eq!(by("solid.pattern.circular"), json!({"face": [38.0, 0.0, 12.0]}));
        assert_eq!(by("solid.revolve"), json!({"edge": [40.0, 0.0, 0.0]}));
        assert_eq!(c.expressions["requires"], json!(["model_axes"]));
    }

    #[test]
    fn sketch_chamfers() {
        let cmds = script(
            "param c = 3 mm\nsketch S on XY {\n polyline closed o = (0,0) (40,0) (40,20) (0,20)\n chamfer o.1.end distance c\n \
             chamfer o.2.end distance 3 mm distance2 5 mm\n chamfer o.3.end distance 4 mm angle 45 deg\n line e = (10,5) (20,5)\n dim e = 12 mm\n}\n",
        );
        let ch: Vec<(&str, &Value)> = cmds
            .iter()
            .filter(|c| c["command"].as_str().unwrap().starts_with("sketch.chamfer"))
            .map(|c| (c["command"].as_str().unwrap(), &c["params"]))
            .collect();
        assert_eq!(ch[0], ("sketch.chamfer.equal_distance", &json!({"point": "l1.end", "distance": 3.0})));
        assert_eq!(ch[1], ("sketch.chamfer.two_distance", &json!({"point": "l2.end", "distance": 3.0, "distance2": 5.0})));
        assert_eq!(ch[2], ("sketch.chamfer.distance_angle", &json!({"point": "l3.end", "distance": 4.0, "angle": 45.0})));
        // Each chamfer adds a line: the next line drawn is l8.
        assert_eq!(cmds.iter().find(|c| c["command"] == "sketch.dimension").unwrap()["params"]["entities"], json!(["l8"]));
    }

    #[test]
    fn patches_and_stitching() {
        let cmds = script(
            "param h = 6 mm\nsketch B on XY {\n circle c = (0,0) radius 20\n}\nsketch R on XY offset 18 {\n circle c = (0,0) radius 16\n}\n\
             loft S sections B, R surface -> body K\n\
             patch D edges (16,0,18) (-16,0,18) of K continuity g1 through (0,0,18 mm + h) crown 0.2 -> body T\n\
             patch F from B -> body U\nstitch C bodies K, T, U\n",
        );
        let by = |n: &str| cmds.iter().find(|c| c["params"]["name"] == n).unwrap();
        assert_eq!(
            by("D")["params"],
            json!({"edges": [[16.0, 0.0, 18.0], [-16.0, 0.0, 18.0]], "body": "K", "continuity": "g1", "points": [[0.0, 0.0, 24.0]], "crown": 0.2, "body_name": "T", "name": "D"})
        );
        assert_eq!(by("F")["params"], json!({"sketch": "B", "body_name": "U", "name": "F"}));
        assert_eq!((by("C")["command"].clone(), by("C")["params"]["bodies"].clone()), (json!("surface.stitch"), json!(["K", "T", "U"])));
    }

    #[test]
    fn engine_pass_through() {
        let src = "box P size 10 10 10 -> body Part\n\
                   engine solid.bounding_solid {\"bodies\": [\"Part\"], \"margin\": \"2 mm\", \"body_name\": \"Box\"}  // its own body\n\
                   combine J target Box tools Part join\n";
        let cmds = script(src);
        assert_eq!(cmds[1], json!({"command": "solid.bounding_solid", "params": {"bodies": ["Part"], "margin": "2 mm", "body_name": "Box"}}));
        assert!(crate::parse::parse("engine solid.box [1, 2]\n").unwrap_err().contains("JSON object"));
        assert!(crate::parse::parse("engine nodot {}\n").unwrap_err().contains("command.id"));
        // Inside a sketch: a sketch command in that sketch, after the drawn entities.
        let cmds = script("sketch S on XY {\n point a = (0,0)\n engine sketch.circle.three_point {\"p0\": [0,0], \"p1\": [2,0], \"p2\": [1,1]}\n}\n");
        assert_eq!(cmds[2], json!({"command": "sketch.circle.three_point", "params": {"p0": [0, 0], "p1": [2, 0], "p2": [1, 1]}}));
        assert!(fails("sketch S on XY {\n engine sketch.circle.three_point {}\n point a = (0,0)\n}\n").contains("an engine command"));
    }

    #[test]
    fn direct_face_edits() {
        let cmds = script(
            "param d = 5 mm\nbox B size 60 40 20 -> body Block\ncylinder P radius 4 height 30 base (80,20,0) -> body Pin\n\
             split_face S faces (30,20,20) of Block origin (20,0,0) normal (1,0,0)\nsplit_face T faces (30,20,20) plane XY\n\
             press_pull C face (30,20,20) distance (-d)\noffset_surface O faces (84,20,15) (76,20,15) of Pin distance 0.3 mm chain -> body Skin\n",
        );
        let by = |n: &str| cmds.iter().find(|c| c["params"]["name"] == n).unwrap().clone();
        assert_eq!(
            by("S"),
            json!({"command": "solid.split_face", "params": {"faces": [[30.0, 20.0, 20.0]], "body": "Block", "plane": {"origin": [20.0, 0.0, 0.0], "normal": [1.0, 0.0, 0.0]}, "name": "S"}})
        );
        assert_eq!(by("T")["params"]["plane"], json!("XY"));
        assert_eq!(by("C"), json!({"command": "solid.press_pull", "params": {"face": [30.0, 20.0, 20.0], "distance": "- d", "name": "C"}}));
        assert_eq!(
            by("O")["params"],
            json!({"faces": [[84.0, 20.0, 15.0], [76.0, 20.0, 15.0]], "body": "Pin", "distance": "0.3 mm", "chain": true, "body_name": "Skin", "name": "O"})
        );
    }

    #[test]
    fn surfaces_and_thickening() {
        let cmds = script(
            "param wall = 1.6 mm\nsketch P on XZ {\n spline s = (35,0) (50,-60) (36,-150)\n}\n\
             revolve Skin from P axis Z curves s symmetric angle 90 deg surface -> body Skin\n\
             thicken Wall bodies Skin thickness wall flip new -> body Vase\n",
        );
        let n = cmds.len();
        assert_eq!(cmds[n - 2]["command"], "surface.revolve");
        let rev = &cmds[n - 2]["params"];
        assert_eq!((rev["curves"].clone(), rev["symmetric"].clone(), rev["body_name"].clone()), (json!(["s1"]), json!(true), json!("Skin")));
        assert!(rev.get("operation").is_none() && rev.get("direction").is_none(), "{rev}");
        assert_eq!(cmds[n - 1]["command"], "surface.thicken");
        assert_eq!(cmds[n - 1]["params"]["thickness"], json!("-(wall)"));
        assert_eq!(cmds[n - 1]["params"]["body_name"], json!("Vase"));
        let loft = "sketch A on XY {\n circle c = (0,0) radius 5\n}\nsketch B on XY offset 10 {\n circle c = (0,0) radius 8\n}\n";
        assert!(fails(&format!("{loft}loft L sections A, B surface join targets X\n")).contains("a body of its own"));
        assert!(fails(&format!("{loft}loft L sections A, B surface\n")).contains("needs `-> body <name>`"));
    }

    #[test]
    fn constraint_and_dimension_kinds() {
        let head = "sketch S on XY {\npoint a = (1,1)\nline l = (0,0) (5,0)\ncircle c = (9,9) radius 2\narc k = center (0,0) start (3,0) end (0,3)\n";
        let e = fails(&format!("{head}tangent a l\n}}"));
        assert!(e.contains("line 6") && e.contains("`tangent` takes two curves, at least one of them round; `a` is a point, `l` is a line"), "{e}");
        let e = fails(&format!("{head}tangent l l\n}}"));
        assert!(e.contains("`tangent` takes two curves"), "{e}");
        let e = fails(&format!("{head}midpoint l a\n}}"));
        assert!(e.contains("`midpoint` takes a point and a line; `l` is a line, `a` is a point"), "{e}");
        let e = fails(&format!("{head}concentric c l\n}}"));
        assert!(e.contains("`concentric` takes two circles or arcs"), "{e}");
        let e = fails(&format!("{head}equal l c\n}}"));
        assert!(e.contains("`equal` takes two lines, or two circles or arcs"), "{e}");
        let e = fails(&format!("{head}horizontal c\n}}"));
        assert!(e.contains("`horizontal` takes a line, or two points; `c` is a circle"), "{e}");
        let e = fails(&format!("{head}symmetric a l about y\n}}"));
        assert!(e.contains("`symmetric` takes two points, or two curves"), "{e}");
        let e = fails(&format!("{head}symmetric a l.start about c\n}}"));
        assert!(e.contains("`symmetric … about` takes a line or a point; `c` is a circle"), "{e}");
        let e = fails(&format!("{head}dim radius l = 3\n}}"));
        assert!(e.contains("`dim radius` takes one circle or arc; `l` is a line"), "{e}");
        let e = fails(&format!("{head}dim angle l -> a = 30 deg\n}}"));
        assert!(e.contains("`dim angle` takes two lines"), "{e}");
        let e = fails(&format!("{head}dim c -> l = 3\n}}"));
        assert!(e.contains("`dim auto` takes a line, a circle or arc, two points, two lines, or a point and a line; `c` is a circle"), "{e}");
        let e = fails(&format!("{head}fillet l radius 1\n}}"));
        assert!(e.contains("`fillet` takes a corner point") && e.contains("`l` is a line"), "{e}");
        // What the engine accepts compiles.
        let d = parse(&format!(
            "{head}coincident a l\ncoincident a c\ncoincident l.end k.start\ntangent l c\ntangent c k\nconcentric c k\nequal c k\nmidpoint a l\nsymmetric a l.start about y\nsymmetric c k about l\ndim radius c = 2\ndim diameter k = 6\ndim angle l -> l = 0\ndim a -> l = 1\ndim c = 2\ndim horizontal origin -> a = 1\ndim length l = 5\nhorizontal a l.end\nvertical l\n}}"
        ))
        .unwrap();
        compile(&d).unwrap();
        let e = fails("sketch S on XY {\npoint a = (1,1)\ncircle c = (9,9) radius 2\n}\nhole H at S.c diameter 3");
        assert!(e.contains("`at` takes sketch points; `S.c` is a circle"), "{e}");
        let e = fails("sketch S on XY {\npoint a = (1,1)\ncircle c = (9,9) radius 2\n}\nrevolve R from S axis a");
        assert!(e.contains("`axis` takes a sketch line; `a` is a point"), "{e}");
    }

    #[test]
    fn bracket_ids_match_the_engine() {
        let src = include_str!("../examples/bracket.sc");
        let cmds = script(src);
        let dims: Vec<&Value> = cmds.iter().filter(|c| c["command"] == "sketch.dimension").collect();
        assert_eq!(dims[0]["params"]["entities"], json!(["l1"]));
        assert_eq!(dims[3]["params"]["entities"], json!(["origin", "l3.end"]));
        assert_eq!(dims[3]["params"]["type"], json!("horizontal"));
        assert_eq!(dims[4]["params"]["entities"], json!(["origin", "p1"]));
        assert_eq!(dims[6]["params"]["entities"], json!(["origin", "p2"]));
        let hole = cmds.iter().find(|c| c["command"] == "solid.hole").unwrap();
        assert_eq!(hole["params"]["sketch"], json!("HolePts"));
        assert_eq!(hole["params"]["diameter"], json!("hole_d"));
        let ext = cmds.iter().find(|c| c["command"] == "solid.extrude").unwrap();
        assert_eq!(ext["params"]["direction"], json!("symmetric"));
        assert_eq!(ext["params"]["body_name"], json!("Bracket"));
    }

    #[test]
    fn closed_polyline_end_wraps_and_axes_shift_counters() {
        let cmds = script(
            "sketch S on XY {\n polyline closed tri = (0,0) (10,0) (0,10)\n dim tri.3.end -> tri.2.start = 5\n circle a = (-5,5) radius 1\n circle b = (5,5) radius 1\n symmetric a b about y\n point q = (0,20)\n dim vertical origin -> q = 20\n}\n",
        );
        assert_eq!(cmds[2]["params"]["entities"], json!(["l1.start", "l1.end"]));
        // The `y` axis took l4, p1, p2; the point is p3.
        assert_eq!(cmds[7]["params"]["entities"], json!(["origin", "p3"]));
        assert_eq!(cmds[5]["params"], json!({"a": "c1", "b": "c2", "line": "y"}));
    }

    #[test]
    fn loft_sweep_and_sheet_features() {
        let cmds = script(
            "sketch A on XY {\n circle a = (0,0) radius 5\n}\nsketch B on XY offset 10 {\n rect center b = (0,0) (4,4)\n}\nloft L sections A, B -> body Lb\n\
             sketch P on XZ {\n line a = (0,0) (0,-20)\n arc k = start a.end through (2,-25) end (8,-28)\n}\n\
             sweep S from A path P.a, P.k new -> body Sb\npipe Q path P.a, P.k diameter 3 wall 0.5 new -> body Qb\n\
             thread T face (5,0,5) designation M10 modeled\ncoil C diameter 10 turns 3 pitch 2 section_size 1 square new -> body Cb\n\
             sheet_rule Steel thickness 1.5 bend_radius 1\nsketch F on XY {\n rect f = (0,0) (20,10)\n}\nflange Base from F rule Steel -> body Sheet\n\
             flange Up edges (10,0,0) height 15 flip body Sheet\n",
        );
        let find = |c: &str| cmds.iter().find(|x| x["command"] == c).unwrap()["params"].clone();
        assert_eq!(find("solid.loft")["sections"], json!([{"sketch": "A"}, {"sketch": "B"}]));
        let sweep = find("solid.sweep");
        assert_eq!((sweep["sketch"].clone(), sweep["path_sketch"].clone(), sweep["path"].clone()), (json!("A"), json!("P"), json!(["l1", "a1"])));
        assert_eq!(find("solid.pipe")["wall"], json!(0.5));
        assert_eq!(find("solid.thread")["designation"], json!("M10"));
        assert_eq!(find("solid.coil")["section"], json!("square"));
        assert_eq!(find("sheet.manage_rules")["active"], json!(true));
        let flanges: Vec<&Value> = cmds.iter().filter(|c| c["command"] == "sheet.flange").collect();
        assert_eq!(flanges[0]["params"]["type"], json!("base"));
        assert_eq!(flanges[1]["params"]["body"], json!("Sheet"));
        let e = fails("sketch P on XY {\n point p = (0,0)\n}\npipe Q path P.p diameter 3");
        assert!(e.contains("`path` takes curves; `P.p` is a point"), "{e}");
        let e = fails("sketch A on XY {\n circle a = (0,0) radius 5\n}\nloft L sections A");
        assert!(e.contains("at least two sections"), "{e}");
    }

    #[test]
    fn ellipse_ids_and_text_comes_last() {
        let cmds = script(
            "sketch S on XY {\n ellipse e = (0,0) major (10,0) minor 4\n line l = (20,0) (30,0)\n dim e.center -> l.start = 20\n text \"HI\" at (0,-10) height 5 align center\n}\n",
        );
        assert_eq!(cmds[3]["params"]["entities"], json!(["p1", "l1.start"]));
        assert_eq!(cmds[4]["params"]["align"], json!("center"));
        let cmds = script("sketch S on XY {\n slot s = centers (0,0) (10,0) width 4\n polyline closed t = (0,0) (5,0) (0,5)\n fix s t\n}\n");
        assert_eq!(cmds[3]["params"]["entities"], json!(["l1", "l2", "a1", "a2", "l3", "l4", "l5"]));
        let e = fails("sketch S on XY {\n text \"HI\" at (0,0)\n circle c = (0,0) radius 1\n}\n");
        assert!(e.contains("line 3") && e.contains("must come after the sketch's drawn entities"), "{e}");
    }

    #[test]
    fn offset_mirror_and_pattern_copy_whole_entities() {
        let cmds = script(
            "param gap = 2 mm\nlet hole_r = 1 mm\nsketch S on XY {\n polyline closed o = (0,0) (20,0) (20,10) (0,10)\n circle c = (30,5) radius hole_r\n \
             construction line m = (50,-10) (50,20)\n offset o distance (gap + 1 mm) side (10,5)\n mirror c about m\n \
             pattern circular c count 4 center (40,5) angle 180 deg\n pattern rectangular c count 3 spacing (2*gap) dir (0,1) count2 2 spacing2 6 mm\n fix o\n}\n",
        );
        let by = |id: &str| cmds.iter().find(|c| c["command"] == id).unwrap()["params"].clone();
        assert_eq!(by("sketch.offset"), json!({"curves": ["l1", "l2", "l3", "l4"], "distance": "gap + 1 mm", "side": [10.0, 5.0]}));
        assert_eq!(by("sketch.mirror"), json!({"entities": ["c1"], "line": "l5"}));
        assert_eq!(by("sketch.pattern.circular"), json!({"entities": ["c1"], "count": 4.0, "center": [40.0, 5.0], "angle": 180.0}));
        assert_eq!(
            by("sketch.pattern.rectangular"),
            json!({"entities": ["c1"], "count": 3.0, "spacing": 4.0, "count2": 2.0, "spacing2": 6.0, "dir": [0.0, 1.0]})
        );
        let e = fails("sketch S on XY {\n circle c = (0,0) radius 1\n offset c distance 1\n circle d = (5,0) radius 1\n}\n");
        assert!(e.contains("line 4") && e.contains("an offset (line 3) must come after"), "{e}");
        let e = fails("sketch S on XY {\n circle c = (0,0) radius 1\n mirror c about c\n}\n");
        assert!(e.contains("mirror about a line, not a circle"), "{e}");
        assert!(fails("sketch S on XY {\n circle c = (0,0) radius 1\n pattern circular c count 1\n}\n").contains("2 through 500"));
    }

    #[test]
    fn shaping_splitting_and_properties() {
        let cmds = script(
            "box B size 100 40 30 corner (0,0,0) -> body Tray\nsketch C on YZ offset 20 mm {\n line g = (10,2) (2,10)\n}\n\
             rib Gusset from C curves g thickness 2 mm direction normal flip\nweb Walls from C curves g thickness 1 mm depth 5 mm\n\
             draft D faces (0,20,15) angle (-3 deg) neutral XY pull (0,0,1) body Tray\noffset_faces O faces (100,20,15) distance 1 mm body Tray\n\
             scale S bodies Tray factors (1, 1, 2)\nsketch P on XY {\n arc a = center (0,0) start (50,0) end (0,50)\n}\n\
             pattern Along features O path P.a count 3 spacing 10 mm identical\nsplit Halves target Tray plane YZ -> body Left\n\
             color Tray, Left \"#cc2222\"\nmaterial Left PLA\n",
        );
        let by = |id: &str| cmds.iter().find(|c| c["command"] == id).unwrap()["params"].clone();
        assert_eq!(
            by("solid.rib"),
            json!({"curves": ["l1"], "sketch": "C", "thickness": "2 mm", "flip": true, "direction": "normal", "name": "Gusset"})
        );
        assert_eq!(by("solid.web")["depth"], json!("5 mm"));
        assert_eq!(
            by("solid.draft"),
            json!({"faces": [[0.0, 20.0, 15.0]], "angle": "-3 deg", "neutral": "XY", "pull": [0.0, 0.0, 1.0], "body": "Tray", "name": "D"})
        );
        assert_eq!(by("solid.scale")["factors"], json!([1.0, 1.0, 2.0]));
        assert_eq!(
            by("solid.pattern.path"),
            json!({"features": ["O"], "path_sketch": "P", "path": ["a1"], "count": 3.0, "spacing": "10 mm", "orientation": "identical", "name": "Along"})
        );
        let split = cmds.iter().position(|c| c["command"] == "solid.split_body").unwrap();
        assert_eq!(cmds[split]["params"], json!({"body": "Tray", "plane": "YZ", "name": "Halves"}));
        assert_eq!(cmds[split + 1], json!({"command": "body.rename", "params": {"body": "Tray (1)", "name": "Left"}}));
        assert_eq!(by("appearance.assign"), json!({"bodies": ["Tray", "Left"], "color": "#cc2222"}));
        assert_eq!(by("material.assign"), json!({"bodies": ["Left"], "material": "PLA"}));
        assert!(fails("box B size 1 1 1 -> body T\ncolor T red\n").contains("a colour is \"#rrggbb\""));
        assert!(
            fails("box B size 1 1 1 -> body T\nsketch C on XY {\n point q = (0,0)\n}\nrib R from C curves q thickness 1\n").contains("takes curves")
        );
        // The piece a split names is a body afterwards.
        crate::check(&parse("box B size 10 10 10 -> body T\nsplit S target T plane YZ -> body U\ncolor U \"#000000\"\n").unwrap()).unwrap();
    }

    #[test]
    fn equation_curves_imports_and_sheet_finishing() {
        let cmds = script(
            "param r = 10 mm\nsketch S on XY {\n curve c = (r*cos(t), r*sin(t)) t from 0 deg to 90 deg points 3\n \
             curve o = (r*cos(t), r*sin(t)) t from 0 deg to 360 deg points 4 closed\n line l = (0,0) c.start\n line m = c.end (0,0)\n \
             svg \"art/logo.svg\" at (1,2) scale 0.5\n}\nhem H edges (1,0,15) length 5 mm gap 0.5 mm body G\n\
             perforate P diameter 4 mm pitch 6 mm margin 8 mm square body G\n",
        );
        let splines: Vec<&Value> = cmds.iter().filter(|c| c["command"] == "sketch.spline.fit_point").collect();
        let pts = |i: usize| -> Vec<(i64, i64)> {
            splines[i]["params"]["points"]
                .as_array()
                .unwrap()
                .iter()
                .map(|p| ((p[0].as_f64().unwrap() * 1e6).round() as i64, (p[1].as_f64().unwrap() * 1e6).round() as i64))
                .collect()
        };
        // Three points from 0 to 90 deg; a closed curve leaves out its end (= its start).
        let k = (10e6 * std::f64::consts::FRAC_1_SQRT_2).round() as i64;
        assert_eq!(pts(0), [(10_000_000, 0), (k, k), (0, 10_000_000)]);
        assert_eq!(pts(1), [(10_000_000, 0), (0, 10_000_000), (-10_000_000, 0), (0, -10_000_000)]);
        assert_eq!(splines[1]["params"]["periodic"], json!(true));
        // c's fit points are p1..p3 (o's p4..p7): the lines join its ends.
        let lines: Vec<&Value> = cmds.iter().filter(|c| c["command"] == "sketch.line").collect();
        assert_eq!((lines[0]["params"]["points"][1].clone(), lines[1]["params"]["points"][0].clone()), (json!("p1"), json!("p3")));
        let by = |id: &str| cmds.iter().find(|c| c["command"] == id).unwrap()["params"].clone();
        assert_eq!(by("sketch.insert_svg"), json!({"path": "art/logo.svg", "at": [1.0, 2.0], "scale": 0.5}));
        assert_eq!(by("sheet.hem"), json!({"edges": [[1.0, 0.0, 15.0]], "length": "5 mm", "gap": "0.5 mm", "body": "G", "name": "H"}));
        assert_eq!(by("sheet.perforate")["pattern"], json!("square"));
        let e = fails("sketch S on XY {\n dxf \"a.dxf\"\n circle c = (0,0) radius 1\n}\n");
        assert!(e.contains("an import (line 2) must come after"), "{e}");
        // Bare numbers are millimetres; an angle is not a coordinate.
        assert!(fails("sketch S on XY {\n curve c = (t, t) t from 0 deg to 90 deg\n}\n").contains("must be a length"));
        assert!(fails("sketch S on XY {\n curve c = (t*1 mm, 0) t from 0 to 1 points 2\n}\n").contains("3 through 200"));
    }

    #[test]
    fn cones_are_revolved_profiles_with_live_dimensions_and_thread_options() {
        let d = parse("param r = 10 mm\ncone Tip diameter (2*r) top_radius 2 mm height 20 mm base (1,2,3) axis X -> body Tip\n").unwrap();
        let c = compile(&d).unwrap();
        let cmds = c.script["commands"].as_array().unwrap();
        let ids: Vec<&str> = cmds.iter().map(|c| c["command"].as_str().unwrap()).collect();
        assert_eq!(
            ids,
            [
                "parameters.add",
                "sketch.create",
                "sketch.line",
                "sketch.constraint.parallel",
                "sketch.dimension",
                "sketch.constraint.fix",
                "sketch.constraint.horizontal_vertical",
                "sketch.constraint.perpendicular",
                "sketch.dimension",
                "sketch.dimension",
                "sketch.finish",
                "solid.revolve"
            ]
        );
        // Axis X: the profile is on XY at the base's z, the axis edge horizontal; seeds are numbers.
        assert_eq!(cmds[1]["params"], json!({"name": "TipProfile", "plane": "XY", "offset": 3.0}));
        assert_eq!(cmds[2]["params"]["points"], json!([[1.0, 2.0], [1.0, 12.0], [21.0, 4.0], [21.0, 2.0]]));
        let dims: Vec<&Value> = cmds.iter().filter(|c| c["command"] == "sketch.dimension").map(|c| &c["params"]["value"]).collect();
        assert_eq!(dims, [&json!("2 mm"), &json!("(2 * r) / 2"), &json!("20 mm")]);
        assert_eq!(cmds[11]["params"], json!({"sketch": "TipProfile", "axis": "l4", "body_name": "Tip", "name": "Tip"}));
        assert_eq!(c.expressions["requires"], json!([]));
        assert_eq!(c.dims.len(), 3);
        // A thread clearance scales the part about the thread's axis, by a live expression.
        let fit = compile(&parse("param play = 0.2 mm\ncylinder N radius 15 height 10 -> body Nut\nthread T face (13,0,5) designation M28x2 modeled clearance play internal axis Z origin (0,0,0) body Nut\n").unwrap()).unwrap();
        let scale = fit.script["commands"].as_array().unwrap().last().unwrap().clone();
        assert_eq!(scale["params"]["factors"], json!(["1 + (play) / (14 mm)", "1 + (play) / (14 mm)", 1]));
        assert!(
            fails(
                "cylinder N radius 15 height 10 -> body Nut\nthread T face (13,0,5) designation M28x2 clearance 1 mm axis Z origin (0,0,0) body Nut\n"
            )
            .contains("`internal` or `external`")
        );
        assert!(fails("cone C height 2\n").contains("needs `radius` or `diameter`"));
        let left =
            compile(&parse("cylinder S radius 4 height 20 -> body B\nthread T face (4,0,10) designation M8 modeled left body B\n").unwrap()).unwrap();
        assert_eq!(
            left.script["commands"][1]["params"],
            json!({"face": [4.0, 0.0, 10.0], "designation": "M8", "modeled": true, "handedness": "left", "name": "T"})
        );
        assert_eq!(left.expressions["requires"], json!(["thread_handedness"]));
        assert_eq!(left.left_threads, [(1, "B".to_string())]);
        // Five read-only slots follow, for the fallback; every command keeps its line.
        assert!(left.script["commands"].as_array().unwrap()[2..7].iter().all(|c| c["command"] == "document.inspect"));
        assert_eq!(left.lines.len(), 7);
        // Without handedness, the slots become mirror, remove, right-hand thread, mirror back, remove, rename.
        let mut script = left.script.clone();
        crate::assembly::fit_script(&left, &mut script, &json!([])).unwrap();
        let ids: Vec<&str> = script["commands"].as_array().unwrap()[1..].iter().map(|c| c["command"].as_str().unwrap()).collect();
        assert_eq!(ids, ["solid.mirror", "solid.remove", "solid.thread", "solid.mirror", "solid.remove", "body.rename"]);
        assert!(script["commands"][3]["params"].get("handedness").is_none());
        assert_eq!(script["commands"][6]["params"], json!({"body": "B (2) (2)", "name": "B"}));
        assert!(fails("cylinder S radius 4 height 20 -> body B\nthread T face (4,0,10) designation M8 left\n").contains("needs `body <name>`"));
        let right = compile(&parse("cylinder S radius 4 height 20 -> body B\nthread T face (4,0,10) designation M8\n").unwrap()).unwrap();
        assert_eq!(right.expressions["requires"], json!([]));
    }

    #[test]
    fn face_colours_text_parameters_path_flanges_and_suppression() {
        let c = compile(
            &parse(
                "param who = \"ADA\"\nbox B size 20 20 10 -> body Cube\ncolor Cube \"#ff3300\" faces (10,10,10) (20,10,5)\n\
                 sketch L on XY offset 10 mm {\n text who at (1,1)\n text (who + \" - L\") at (1,8)\n}\n\
                 sketch Z on YZ offset 10 mm {\n line up = (20,0) (20,15)\n line out = (20,15) (40,15)\n}\n\
                 path_flange F edge (10,20,0) path Z.up, Z.out radius 1 mm body Cube\nsuppress F\n",
            )
            .unwrap(),
        )
        .unwrap();
        let cmds = c.script["commands"].as_array().unwrap();
        let by = |id: &str| cmds.iter().filter(|c| c["command"] == id).map(|c| c["params"].clone()).collect::<Vec<_>>();
        assert_eq!(by("parameters.add")[0], json!({"name": "who", "expression": "ADA", "unit": "text"}));
        assert_eq!(by("appearance.assign")[0], json!({"color": "#ff3300", "body": "Cube", "faces": [[10.0, 10.0, 10.0], [20.0, 10.0, 5.0]]}));
        let texts = by("sketch.text");
        assert_eq!((texts[0]["text_expression"].clone(), texts[1]["text_expression"].clone()), (json!("who"), json!("who + ' - L'")));
        assert_eq!(
            by("sheet.path_flange")[0],
            json!({"edge": [10.0, 20.0, 0.0], "sketch": "Z", "curves": ["l1", "l2"], "radius": "1 mm", "body": "Cube", "name": "F"})
        );
        assert_eq!(by("timeline.suppress")[0], json!({"features": ["F"], "suppressed": true}));
        assert_eq!(c.expressions["requires"], json!(["sheet_path_flange", "text_parameters"]));
        assert!(
            fails("box B size 1 1 1 -> body A\nbox C size 1 1 1 -> body D\ncolor A, D \"#000000\" faces (0,0,0)\n").contains("one body at a time")
        );
        assert!(compile(&parse("let w = \"X\"\n").unwrap()).unwrap_err().contains("text belongs in a `param`"));
    }

    #[test]
    fn construction_planes() {
        let cmds = script(
            "plane Up on XY offset 20 mm\nplane Tilt on Up angle 30 deg axis X\nplane Hinge angle 10 deg axis (1,1,0) origin (5,0,0)\n\
             plane Slope through (0,0,0) (10,0,5) (0,10,0)\nplane Mid between Up, Tilt\nsketch P on XZ {\n line l = (0,0) (10,-10)\n}\n\
             plane Station path P.l fraction 0.25\nsketch S on Tilt {\n circle c = (0,0) radius 2\n}\n",
        );
        let p = |i: usize| (cmds[i]["command"].as_str().unwrap().to_string(), cmds[i]["params"].clone());
        assert_eq!(p(0), ("construct.plane.offset".into(), json!({"base": "XY", "offset": "20 mm", "name": "Up"})));
        assert_eq!(p(1), ("construct.plane.at_angle".into(), json!({"base": "Up", "angle": "30 deg", "axis": "X", "name": "Tilt"})));
        assert_eq!(p(2).1["axis"], json!({"origin": [5.0, 0.0, 0.0], "dir": [1.0, 1.0, 0.0]}));
        assert_eq!(p(3).1["b"], json!({"point": [10.0, 0.0, 5.0]}));
        assert_eq!(p(4), ("construct.plane.midplane".into(), json!({"a": "Up", "b": "Tilt", "name": "Mid"})));
        assert_eq!(p(8), ("construct.plane.along_path".into(), json!({"path": {"sketch": "P", "curve": "l1"}, "t": 0.25, "name": "Station"})));
        assert_eq!(p(9).1["plane"], json!("Tilt"));
        let checked = |src: &str| crate::check(&parse(src).unwrap()).unwrap_err();
        let e = checked("sketch S on Tilt {\n circle c = (0,0) radius 2\n}\n");
        assert!(e.contains("unknown plane `Tilt`; planes so far: XY, XZ, YZ"), "{e}");
        assert!(checked("plane A on Nope offset 2 mm\n").contains("unknown plane `Nope`"));
    }

    #[test]
    fn rect_parts_and_errors() {
        let cmds =
            script("sketch S on XY {\n rect center r = (0,0) (10,5)\n dim r.bottom = 20\n dim r.top_left -> r.center = 3\n point z = (1,1)\n}\n");
        assert_eq!(cmds[2]["params"]["entities"], json!(["l1"]));
        assert_eq!(cmds[3]["params"]["entities"], json!(["p4", "p5"]));
        assert_eq!(cmds[4]["params"]["point"], json!([1.0, 1.0]));
        let e = compile(&parse("sketch S on XY {\n rect r = (0,0) (1,1)\n dim r = 3\n}\n").unwrap()).unwrap_err();
        assert!(e.contains("line 3") && e.contains("rectangle"), "{e}");
        let e = compile(&parse("sketch S on XY {\n}\nextrude E from Nope distance 3\n").unwrap()).unwrap_err();
        assert!(e.contains("unknown sketch `Nope`"), "{e}");
        let e = compile(&parse("sketch S on XY {\n}\nextrude E from S distance 3 radius 2\n").unwrap()).unwrap_err();
        assert!(e.contains("`radius` does not apply"), "{e}");
    }
}
