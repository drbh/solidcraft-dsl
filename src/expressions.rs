//! Resolve a design's expression graph using the same scalar core as SolidCraft.
//! The lowering boundary separates solver seeds, selection locations, and persistent inputs.
use crate::ast::*;
use serde_json::{Value as Json, json};
use solidcraft_expr::{Kind, ParamDef, Value, eval_params, eval_with, is_reserved, references};
use std::collections::{BTreeMap, HashMap, HashSet};

type R<T> = Result<T, String>;
fn error(line: usize, context: &str, e: impl std::fmt::Display) -> String {
    format!("line {line}: {context}: {e}")
}

#[derive(Debug)]
pub struct Environment {
    pub values: BTreeMap<String, Value>,
    pub order: Vec<usize>,
    pub parameters: Json,
}
impl Environment {
    pub fn new(d: &Design) -> R<Self> {
        let mut names = HashMap::new();
        for (i, p) in d.params.iter().enumerate() {
            if is_reserved(&p.name) {
                return Err(error(p.line, "parameter", format!("`{}` is reserved by the expression language", p.name)));
            }
            if names.insert(p.name.as_str(), i).is_some() {
                return Err(error(p.line, "parameter", format!("`{}` is defined twice", p.name)));
            }
        }
        let defs: Vec<ParamDef> = d.params.iter().map(|p| ParamDef { name: p.name.clone(), expr: p.expr.clone(), unit: "auto".into() }).collect();
        let (values, errors) = eval_params(&defs, 1.0);
        for p in &d.params {
            if let Some(e) = errors.get(&p.name) {
                return Err(error(p.line, &format!("parameter `{}`", p.name), e));
            }
            if !p.local && values.get(&p.name).is_some_and(|v| v.kind().is_none()) {
                return Err(error(
                    p.line,
                    "parameter",
                    "the engine supports length, angle and unitless parameters; use `let` for area/volume helpers",
                ));
            }
        }
        // eval_params has already rejected cycles and excessive depth. Emit parameters in
        // dependency order so engines that validate each parameters.add can accept forward refs.
        fn visit(i: usize, d: &Design, names: &HashMap<&str, usize>, seen: &mut HashSet<usize>, order: &mut Vec<usize>) {
            if !seen.insert(i) {
                return;
            }
            if let Some(p) = d.params.get(i) {
                for name in references(&p.expr) {
                    if let Some(j) = names.get(name.as_str()) {
                        visit(*j, d, names, seen, order);
                    }
                }
            }
            order.push(i);
        }
        let mut order = vec![];
        let mut seen = HashSet::new();
        for i in 0..d.params.len() {
            visit(i, d, &names, &mut seen, &mut order);
        }
        let parameters = Json::Array(
            d.params
                .iter()
                .map(|p| {
                    let v = values.get(&p.name);
                    json!({"name":p.name,"kind":if p.local {"let"}else{"param"},"expression":p.expr,"line":p.line,"value":v.map(|v|v.v),"unit":v.map(unit),"dimensions":v.map(|v|json!({"length":v.len,"angle":v.ang})),"depends_on":references(&p.expr)})
                })
                .collect(),
        );
        Ok(Self { values, order, parameters })
    }
    pub fn eval(&self, text: &str) -> R<Value> {
        eval_with(text, &|n| self.values.get(n).copied().ok_or_else(|| solidcraft_expr::Error(format!("unknown parameter `{n}`")))).map_err(|e| e.0)
    }
    pub fn number(&self, text: &str, kind: Kind, line: usize, context: &str) -> R<f64> {
        self.eval(text).and_then(|v| v.to_kind(kind).map_err(|e| e.0)).map_err(|e| error(line, context, format!("`{text}`: {e}")))
    }
}
pub fn unit(v: &Value) -> &'static str {
    match v.kind() {
        Some(Kind::Length) => "mm",
        Some(Kind::Angle) => "rad",
        _ => "",
    }
}
pub fn text(c: &Comp) -> &str {
    match c {
        Comp::Num(_, s) | Comp::Expr(s) => s,
    }
}

pub struct Lowered {
    pub design: Design,
    pub explanation: Json,
}
/// Evaluate only geometry seeds and selections. Native feature expressions and explicit
/// bound points stay symbolic in the command script and saved engine document.
pub fn lower(d: &Design) -> R<Lowered> {
    let env = Environment::new(d)?;
    let mut design = d.clone();
    design.params = env.order.iter().filter_map(|i| d.params.get(*i).cloned()).collect();
    let mut sites = vec![];
    fn coords(c: &mut [Comp], env: &Environment, line: usize, role: &str, kind: Kind, keep: bool, sites: &mut Vec<Json>) -> R<()> {
        for (axis, c) in c.iter_mut().enumerate() {
            let expression = text(c).to_string();
            let value = env.number(&expression, kind, line, role)?;
            if !value.is_finite() || value.abs() >= 1e9 {
                return Err(error(line, role, "coordinate out of range"));
            }
            if matches!(c, Comp::Expr(_)) || keep {
                sites.push(json!({"line":line,"axis":axis,"role":role,"expression":expression,"value":value,"unit":match kind {Kind::Length=>"mm",Kind::Angle=>"rad",Kind::Unitless=>""},"depends_on":references(&expression),"persistent":keep}));
            }
            if !keep {
                *c = Comp::Num(value, value.to_string());
            }
        }
        Ok(())
    }
    for item in &mut design.items {
        match item {
            Item::Sketch(s) => {
                if let Plane::Face(c) = &mut s.plane {
                    coords(c, &env, s.line, "selection", Kind::Length, false, &mut sites)?;
                }
                for st in &mut s.stmts {
                    let line = st.line;
                    let mut point = |p: &mut Pt| -> R<()> {
                        if let Pt::Xy(c) = p {
                            coords(c, &env, line, "seed", Kind::Length, false, &mut sites)?;
                        }
                        Ok(())
                    };
                    match &mut st.kind {
                        SketchKind::Line { pts, .. } | SketchKind::Polyline { pts, .. } | SketchKind::Spline { pts, .. } => {
                            for p in pts {
                                point(p)?;
                            }
                        }
                        SketchKind::Circle { center, .. } => point(center)?,
                        SketchKind::ArcCenter { center, start, end, .. } => {
                            point(center)?;
                            point(start)?;
                            point(end)?;
                        }
                        SketchKind::ArcThree { start, through, end, .. } => {
                            point(start)?;
                            point(through)?;
                            point(end)?;
                        }
                        SketchKind::Rect { p0, p1, .. } | SketchKind::Slot { p0, p1, .. } => {
                            coords(p0, &env, line, "seed", Kind::Length, false, &mut sites)?;
                            coords(p1, &env, line, "seed", Kind::Length, false, &mut sites)?;
                        }
                        SketchKind::Point { p, bound, .. } => {
                            coords(p, &env, line, if *bound { "binding" } else { "seed" }, Kind::Length, *bound, &mut sites)?
                        }
                        SketchKind::Polygon { center, sides, .. } => {
                            coords(center, &env, line, "seed", Kind::Length, false, &mut sites)?;
                            let n = env.number(sides, Kind::Unitless, line, "sides")?;
                            if n.fract() != 0. || !(3. ..=64.).contains(&n) {
                                return Err(error(line, "sides", "must be an integer from 3 through 64"));
                            }
                            sites.push(
                                json!({"line":line,"role":"topology","expression":sides,"value":n,"persistent":false,"depends_on":references(sides)}),
                            );
                            *sides = (n as usize).to_string();
                        }
                        _ => {}
                    }
                    let seed_size = match &mut st.kind {
                        SketchKind::Circle { size: CircleSize::Radius(e) | CircleSize::Diameter(e), .. }
                        | SketchKind::Polygon { radius: e, .. }
                        | SketchKind::Slot { width: e, .. } => Some(e),
                        _ => None,
                    };
                    if let Some(e) = seed_size {
                        let value = env.number(e, Kind::Length, line, "seed size")?;
                        sites.push(
                            json!({"line":line,"role":"seed","expression":e,"value":value,"unit":"mm","persistent":false,"depends_on":references(e)}),
                        );
                        *e = value.to_string();
                    }
                }
            }
            Item::Feature(f) => {
                for (key, val) in &mut f.opts {
                    if let Val::Tuples(tuples) = val {
                        let direction = matches!(key.as_str(), "dir" | "dir2" | "axis" | "normal");
                        let keep = matches!(f.kind.as_str(), "box" | "sphere" | "cylinder" | "torus")
                            && matches!(key.as_str(), "center" | "corner" | "base");
                        for t in tuples {
                            coords(
                                t,
                                &env,
                                f.line,
                                if keep {
                                    "placement"
                                } else if direction {
                                    "direction"
                                } else {
                                    "selection"
                                },
                                if direction { Kind::Unitless } else { Kind::Length },
                                keep,
                                &mut sites,
                            )?;
                        }
                    }
                    if matches!(key.as_str(), "count" | "count2") {
                        if let Val::Expr(e) = val {
                            let n = env.number(e, Kind::Unitless, f.line, key)?;
                            if n.fract() != 0. || !(1. ..=10000.).contains(&n) {
                                return Err(error(f.line, &format!("`{key}`"), "must be a whole number of at least 1 (maximum 10000)"));
                            }
                            sites.push(
                                json!({"line":f.line,"role":"topology","expression":e,"value":n,"persistent":false,"depends_on":references(e)}),
                            );
                            *e = (n as usize).to_string();
                        }
                    }
                }
            }
        }
    }
    inline_helpers(&mut design)?;
    Ok(Lowered {
        design,
        explanation: json!({"parameters":env.parameters,"sites":sites,"requires":if sites.iter().any(|s|s["role"]=="binding") {vec!["persistent_sketch_point_expressions"]}else{vec![]}}),
    })
}

/// Helpers are expanded as parenthesised expressions, never substituted as evaluated numbers.
/// Thus `let area = L*W` remains responsive to edits of L and W in the saved native model.
fn inline_helpers(d: &mut Design) -> R<()> {
    let mut helpers: Vec<(String, String)> = vec![];
    fn expand(s: &mut String, helpers: &[(String, String)]) -> R<()> {
        for (name, value) in helpers {
            let next = solidcraft_expr::rename_reference(s, name, &format!("({value})"));
            if next.len() > 4096 {
                return Err("helper expansion exceeds the engine's 4096-byte expression limit; split the computation into scalar params".into());
            }
            *s = next;
        }
        Ok(())
    }
    for p in &mut d.params {
        expand(&mut p.expr, &helpers).map_err(|e| error(p.line, "helper", e))?;
        if p.local {
            helpers.push((p.name.clone(), p.expr.clone()));
        }
    }
    d.params.retain(|p| !p.local);
    fn coords(c: &mut [Comp], h: &[(String, String)]) -> R<()> {
        for c in c {
            if let Comp::Expr(e) = c {
                expand(e, h)?;
            }
        }
        Ok(())
    }
    fn point(p: &mut Pt, h: &[(String, String)]) -> R<()> {
        if let Pt::Xy(c) = p {
            coords(c, h)?;
        }
        Ok(())
    }
    for item in &mut d.items {
        match item {
            Item::Sketch(s) => {
                if let Some(e) = &mut s.offset {
                    expand(e, &helpers)?;
                }
                if let Plane::Face(c) = &mut s.plane {
                    coords(c, &helpers)?;
                }
                for st in &mut s.stmts {
                    match &mut st.kind {
                        SketchKind::Line { pts, .. } | SketchKind::Polyline { pts, .. } | SketchKind::Spline { pts, .. } => {
                            for p in pts {
                                point(p, &helpers)?;
                            }
                        }
                        SketchKind::Circle { center, size, .. } => {
                            point(center, &helpers)?;
                            match size {
                                CircleSize::Radius(e) | CircleSize::Diameter(e) => expand(e, &helpers)?,
                            }
                        }
                        SketchKind::ArcCenter { center, start, end, .. } => {
                            point(center, &helpers)?;
                            point(start, &helpers)?;
                            point(end, &helpers)?;
                        }
                        SketchKind::ArcThree { start, through, end, .. } => {
                            point(start, &helpers)?;
                            point(through, &helpers)?;
                            point(end, &helpers)?;
                        }
                        SketchKind::Rect { p0, p1, kind, .. } => {
                            coords(p0, &helpers)?;
                            coords(p1, &helpers)?;
                            if let RectKind::Rounded(e) = kind {
                                expand(e, &helpers)?;
                            }
                        }
                        SketchKind::Slot { p0, p1, width, .. } => {
                            coords(p0, &helpers)?;
                            coords(p1, &helpers)?;
                            expand(width, &helpers)?;
                        }
                        SketchKind::Polygon { center, radius, sides, .. } => {
                            coords(center, &helpers)?;
                            expand(radius, &helpers)?;
                            expand(sides, &helpers)?;
                        }
                        SketchKind::Point { p, .. } => coords(p, &helpers)?,
                        SketchKind::Dim { value, .. } => expand(value, &helpers)?,
                        SketchKind::Fillet { radius, .. } => expand(radius, &helpers)?,
                        _ => {}
                    }
                }
            }
            Item::Feature(f) => {
                for (key, value) in &mut f.opts {
                    match value {
                        Val::Tuples(ts) => {
                            for t in ts {
                                coords(t, &helpers)?;
                            }
                        }
                        Val::Expr(e)
                            if matches!(
                                key.as_str(),
                                "distance"
                                    | "distance2"
                                    | "offset"
                                    | "angle"
                                    | "taper"
                                    | "radius"
                                    | "diameter"
                                    | "height"
                                    | "depth"
                                    | "thickness"
                                    | "width"
                                    | "length"
                                    | "major"
                                    | "minor"
                                    | "spacing"
                                    | "spacing2"
                                    | "count"
                                    | "count2"
                            ) =>
                        {
                            expand(e, &helpers)?
                        }
                        Val::List(values) if matches!(key.as_str(), "size" | "counterbore" | "countersink") => {
                            for e in values {
                                expand(e, &helpers)?;
                            }
                        }
                        _ => {}
                    }
                }
            }
        }
    }
    Ok(())
}
