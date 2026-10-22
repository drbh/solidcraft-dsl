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
}

#[derive(Default)]
struct Counters {
    l: usize,
    c: usize,
    a: usize,
    p: usize,
    s: usize,
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
    for p in &d.params {
        if names.insert(p.name.clone(), p.line).is_some() {
            return Err(err(p.line, format!("parameter `{}` is defined twice", p.name)));
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
                out.push(compile_feature(f, &mut sketches)?);
                lines.push(f.line);
                features += 1;
            }
        }
    }
    Ok(Compiled { native: None, script: json!({"commands": out}), sketches: sketches.len(), features, lines, dims, expressions: lowered.explanation })
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
    for st in &s.stmts {
        let line = st.line;
        match &st.kind {
            SketchKind::Dim { .. } => dims.push(Some(line)),
            SketchKind::Rect { kind: RectKind::Rounded(_), .. } | SketchKind::Fillet { .. } => dims.push(None),
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
            SketchKind::Fix { refs } => {
                let ids: Vec<String> = refs.iter().map(|r| t.resolve(line, r)).collect::<R<_>>()?;
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

fn compile_feature(f: &Feature, sketches: &mut HashMap<String, SketchTable>) -> R<Value> {
    let line = f.line;
    let mut o = Opts::new(f);
    let mut p = json!({});
    let id = match f.kind.as_str() {
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
            p["axis"] = match o.take("axis") {
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
                _ => match (o.tuple("origin")?, o.tuple("dir")?) {
                    (Some(origin), Some(dir)) => json!({"origin": origin, "dir": dir}),
                    _ => return Err(err(line, "`pattern circular` needs `axis X|Y|Z` or `origin (…) dir (…)`")),
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
        other => return Err(err(line, format!("`{other}` is not supported yet"))),
    };
    let params = o.finish(p)?;
    Ok(cmd(id, params))
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
