//! Assembly elaboration: definitions are compiled once, placements share their geometry.
//! All instances start at identity until the graph is complete; copies never inherit the
//! first instance's authored placement. Connector frames are captured before solving joints.
use std::collections::{HashMap, HashSet};

use serde_json::{Value, json};
use solidcraft_expr::Kind;

use crate::{
    Compiled,
    ast::*,
    expressions::{Environment, text},
};

type R<T> = Result<T, String>;
fn error(line: usize, message: impl std::fmt::Display) -> String {
    format!("line {line}: {message}")
}

pub fn present(d: &Design) -> bool {
    !d.components.is_empty() || !d.instances.is_empty() || !d.connectors.is_empty() || !d.grounds.is_empty() || !d.joints.is_empty()
}

// Length-prefix names are injective even when authored names contain punctuation.
pub(crate) fn qualified(scope: &str, name: &str) -> String {
    format!("@{}:{scope}/{name}", scope.len())
}
fn validate_name(name: &str, line: usize) -> R<()> {
    if name.eq_ignore_ascii_case("root") {
        return Err(error(line, "`root` is reserved for the design"));
    }
    if name.is_empty() || name.starts_with('@') || name.parse::<u64>().is_ok() || name.len() > 200 {
        return Err(error(line, "names must be nonempty, nonnumeric, at most 200 bytes, and cannot start with `@`"));
    }
    Ok(())
}
fn unique(seen: &mut HashSet<String>, name: &str, line: usize) -> R<()> {
    validate_name(name, line)?;
    if !seen.insert(name.into()) {
        return Err(error(line, format!("duplicate name `{name}`")));
    }
    Ok(())
}

/// Qualify only geometry symbols, never expression references or sketch-local entity names.
fn geometry(d: &Design, params: &[Param], scope: Option<&str>) -> R<Design> {
    let mut out = Design { params: params.to_vec(), items: d.items.clone(), ..Design::default() };
    crate::check::check_geometry(&out)?;
    let Some(scope) = scope else { return Ok(out) };
    let mut names = HashMap::new();
    for item in &out.items {
        let (name, line) = match item {
            Item::Sketch(s) => (&s.name, s.line),
            Item::Feature(f) => (&f.name, f.line),
        };
        validate_name(name, line)?;
        names.insert(name.clone(), qualified(scope, name));
        if let Item::Feature(f) = item {
            for (k, v) in &f.opts {
                if k == "body"
                    && matches!(f.kind.as_str(), "extrude" | "revolve" | "box" | "cylinder" | "sphere" | "torus")
                    && let Val::Expr(n) = v
                {
                    validate_name(n, f.line)?;
                    names.insert(n.clone(), qualified(scope, n));
                }
            }
        }
    }
    let rename = |name: &mut String| {
        if let Some(n) = names.get(name) {
            *name = n.clone();
        }
    };
    for item in &mut out.items {
        match item {
            Item::Sketch(s) => {
                rename(&mut s.name);
            }
            Item::Feature(f) => {
                rename(&mut f.name);
                for (key, val) in &mut f.opts {
                    match val {
                        Val::Expr(n) if matches!(key.as_str(), "from" | "body" | "target" | "sketch") => {
                            if n.starts_with("Body") && !names.contains_key(n) {
                                return Err(error(
                                    f.line,
                                    "component body references need explicit names (`-> body Name`), not engine-generated BodyN names",
                                ));
                            }
                            rename(n);
                        }
                        Val::List(ns) if matches!(key.as_str(), "targets" | "bodies" | "tools" | "features" | "at" | "points") => {
                            for n in ns {
                                if names.contains_key(n) {
                                    rename(n);
                                } else if matches!(key.as_str(), "at" | "points")
                                    && let Some((base, rest)) = n.split_once('.')
                                {
                                    if let Some(new) = names.get(base) {
                                        *n = format!("{new}.{rest}");
                                    }
                                } else {
                                    if n.starts_with("Body") && !names.contains_key(n) {
                                        return Err(error(f.line, "name component bodies explicitly with `-> body Name`"));
                                    }
                                    rename(n);
                                }
                            }
                        }
                        Val::Ref(r) if !r.parts.is_empty() => rename(&mut r.base),
                        _ => {}
                    }
                }
            }
        }
    }
    Ok(out)
}

#[derive(Clone, Copy)]
struct Transform {
    at: [f64; 3],
    axis: [f64; 3],
    angle: f64,
}
fn dot(a: [f64; 3], b: [f64; 3]) -> f64 {
    a.iter().zip(b).map(|(a, b)| a * b).sum()
}
fn unit(v: [f64; 3], line: usize) -> R<[f64; 3]> {
    let n = dot(v, v).sqrt();
    if n < 1e-12 || !n.is_finite() {
        return Err(error(line, "frame axes must be nonzero finite vectors"));
    }
    Ok(v.map(|x| x / n))
}
impl Transform {
    fn direction(self, v: [f64; 3]) -> [f64; 3] {
        let [x, y, z] = self.axis;
        let cross = [y * v[2] - z * v[1], z * v[0] - x * v[2], x * v[1] - y * v[0]];
        let (s, c) = self.angle.sin_cos();
        std::array::from_fn(|i| v[i] * c + cross[i] * s + self.axis[i] * dot(self.axis, v) * (1.0 - c))
    }
    fn point(self, v: [f64; 3]) -> [f64; 3] {
        let r = self.direction(v);
        std::array::from_fn(|i| r[i] + self.at[i])
    }
}

struct Builder<'a> {
    design: &'a Design,
    definitions: HashMap<&'a str, &'a Component>,
    built: HashSet<String>,
    visiting: Vec<String>,
    env: Environment,
    compiled: Compiled,
    moves: Vec<(String, Transform, usize)>,
    root: HashMap<String, (&'a Component, Transform)>,
}
impl Builder<'_> {
    fn emit(&mut self, command: &str, params: Value, line: usize) {
        if let Some(commands) = self.compiled.script["commands"].as_array_mut() {
            commands.push(json!({"command":command,"params":params}));
        }
        self.compiled.lines.push(line);
    }
    fn scalar(&mut self, expr: &str, kind: Kind, line: usize, role: &str, persistent: bool) -> R<f64> {
        let value = self.env.number(expr, kind, line, role)?;
        if value.abs() > 1e9 {
            return Err(error(line, format!("{role} exceeds supported numeric range")));
        }
        if let Some(sites) = self.compiled.expressions["sites"].as_array_mut() {
            sites.push(json!({"line":line,"role":role,"expression":expr,"value":value,"persistent":persistent,
                "unit":match kind {Kind::Length=>"mm",Kind::Angle=>"rad",Kind::Unitless=>""},"depends_on":solidcraft_expr::references(expr)}));
        }
        Ok(value)
    }
    fn vector(&mut self, v: &[Comp], kind: Kind, line: usize, role: &str) -> R<[f64; 3]> {
        let [a, b, c] = v else { return Err(error(line, format!("{role} needs three coordinates"))) };
        Ok([
            self.scalar(text(a), kind, line, role, false)?,
            self.scalar(text(b), kind, line, role, false)?,
            self.scalar(text(c), kind, line, role, false)?,
        ])
    }
    fn expression(&mut self, expr: &str, kind: Kind, line: usize) -> R<String> {
        self.scalar(expr, kind, line, "joint_expression", true)?;
        let mut result = expr.to_string();
        // Reverse dependency order expands helpers transitively while preserving parameters.
        for i in self.env.order.iter().rev() {
            if let Some(p) = self.design.params.get(*i)
                && p.local
            {
                result = solidcraft_expr::rename_reference(&result, &p.name, &format!("({})", p.expr));
                if result.len() > 4096 {
                    return Err(error(line, "expanded joint expression exceeds 4096 bytes"));
                }
            }
        }
        Ok(result)
    }
    fn scope(&mut self, d: &Design, scope: Option<&str>, depth: usize) -> R<()> {
        if depth >= 64 {
            return Err("assembly exceeds 63 nested component levels".into());
        }
        let mut names = HashSet::new();
        for instance in &d.instances {
            unique(&mut names, &instance.name, instance.line)?;
            let definition = *self
                .definitions
                .get(instance.component.as_str())
                .ok_or_else(|| error(instance.line, format!("unknown component `{}`", instance.component)))?;
            let occurrence = scope.map(|s| qualified(s, &instance.name)).unwrap_or_else(|| instance.name.clone());
            let at = self.vector(&instance.at, Kind::Length, instance.line, "placement")?;
            let axis = unit(self.vector(&instance.axis, Kind::Unitless, instance.line, "placement_axis")?, instance.line)?;
            let angle = self.scalar(&instance.angle, Kind::Angle, instance.line, "placement", false)?;
            let transform = Transform { at, axis, angle };
            if self.visiting.contains(&definition.name) {
                return Err(error(instance.line, format!("cyclic component instances: {} -> {}", self.visiting.join(" -> "), definition.name)));
            }
            if self.built.insert(definition.name.clone()) {
                self.emit(
                    "component.create",
                    json!({"name":definition.name,"occurrence_name":occurrence,"parent":scope.unwrap_or("root")}),
                    instance.line,
                );
                let g = geometry(&definition.design, &self.design.params, Some(&definition.name))?;
                let compiled = crate::compile::compile_geometry(&g)?;
                for (command, line) in compiled.script["commands"].as_array().into_iter().flatten().zip(&compiled.lines) {
                    if command["command"] != "parameters.add" {
                        self.emit(command["command"].as_str().ok_or("missing command id")?, command["params"].clone(), *line);
                    }
                }
                self.compiled.sketches += compiled.sketches;
                self.compiled.features += compiled.features;
                self.compiled.dims.extend(compiled.dims);
                for field in ["sites", "requires"] {
                    if let Some(values) = self.compiled.expressions[field].as_array_mut() {
                        values.extend(compiled.expressions[field].as_array().into_iter().flatten().cloned());
                    }
                }
                self.visiting.push(definition.name.clone());
                self.scope(&definition.design, Some(&definition.name), depth + 1)?;
                self.visiting.pop();
            } else {
                self.emit(
                    "occurrence.copy",
                    json!({"component":definition.name,"occurrence_name":occurrence,"parent":scope.unwrap_or("root")}),
                    instance.line,
                );
            }
            self.moves.push((occurrence, transform, instance.line));
            if scope.is_none() {
                self.root.insert(instance.name.clone(), (definition, transform));
            }
        }
        for (name, line) in &d.grounds {
            if scope.is_some() {
                return Err(error(*line, "ground a top-level instance; nested grounding is not supported by concise assemblies"));
            }
            if !names.contains(name) {
                return Err(error(*line, format!("unknown instance `{name}`")));
            }
        }
        Ok(())
    }
    fn endpoint(&self, r: &Ref, line: usize) -> R<String> {
        let [connector] = r.parts.as_slice() else {
            return Err(error(line, "joint endpoints must be top-level instance.connector; nested placement joints are not supported"));
        };
        let (definition, _) = self.root.get(&r.base).ok_or_else(|| error(line, format!("unknown instance `{}`", r.base)))?;
        if !definition.design.connectors.iter().any(|c| c.name == *connector) {
            return Err(error(line, format!("unknown connector `{}`", r.text())));
        }
        Ok(qualified(&r.base, connector))
    }
}

// Validate the entire definition graph, including unused definitions. Memoized size/depth
// bounds prevent a small source DAG from expanding into an enormous placement tree.
fn validate_graph(d: &Design, definitions: &HashMap<&str, &Component>) -> R<()> {
    fn visit(
        name: &str,
        definitions: &HashMap<&str, &Component>,
        active: &mut HashSet<String>,
        done: &mut HashMap<String, (usize, usize)>,
    ) -> R<(usize, usize)> {
        if let Some(size) = done.get(name) {
            return Ok(*size);
        }
        let c = definitions.get(name).ok_or_else(|| format!("unknown component `{name}`"))?;
        if !active.insert(name.into()) {
            return Err(error(c.line, format!("cyclic component instances involving `{name}`")));
        }
        if active.len() > 63 {
            return Err(error(c.line, "assembly exceeds 63 nested component levels"));
        }
        let (mut size, mut depth) = (1usize, 1usize);
        let mut names = HashSet::new();
        if let Some((_, line)) = c.design.grounds.first() {
            return Err(error(*line, "ground a top-level instance; nested grounding is not supported"));
        }
        for i in &c.design.instances {
            unique(&mut names, &i.name, i.line)?;
            if !definitions.contains_key(i.component.as_str()) {
                return Err(error(i.line, format!("unknown component `{}`", i.component)));
            }
            let (child_size, child_depth) = visit(&i.component, definitions, active, done)?;
            size = size.saturating_add(child_size);
            depth = depth.max(child_depth + 1);
            if size > 100_000 || depth > 63 {
                return Err(error(i.line, "assembly exceeds 100000 placements or 63 nested component levels"));
            }
        }
        active.remove(name);
        done.insert(name.into(), (size, depth));
        Ok((size, depth))
    }
    let mut done = HashMap::new();
    let mut active = HashSet::new();
    for c in &d.components {
        visit(&c.name, definitions, &mut active, &mut done)?;
    }
    let mut total = 0usize;
    for i in &d.instances {
        let (size, _) = done.get(&i.component).ok_or_else(|| error(i.line, format!("unknown component `{}`", i.component)))?;
        total = total.saturating_add(*size);
        if total > 100_000 {
            return Err(error(i.line, "assembly exceeds 100000 placements"));
        }
    }
    Ok(())
}

pub fn compile(d: &Design) -> R<Compiled> {
    let mut names = HashSet::new();
    let mut definitions = HashMap::new();
    for c in &d.components {
        unique(&mut names, &c.name, c.line)?;
        let mut connectors = HashSet::new();
        for connector in &c.design.connectors {
            unique(&mut connectors, &connector.name, connector.line)?;
        }
        // Check even unused definitions; typoed bodies must not silently hide in a library.
        crate::compile::compile_geometry(&geometry(&c.design, &d.params, Some(&c.name))?)?;
        definitions.insert(c.name.as_str(), c);
    }
    validate_graph(d, &definitions)?;
    for item in &d.items {
        let (name, line) = match item {
            Item::Sketch(s) => (&s.name, s.line),
            Item::Feature(f) => (&f.name, f.line),
        };
        validate_name(name, line)?;
    }
    let base = geometry(d, &d.params, None)?;
    let compiled = crate::compile::compile_geometry(&base)?;
    let mut b = Builder {
        design: d,
        definitions,
        built: HashSet::new(),
        visiting: vec![],
        env: Environment::new(d)?,
        compiled,
        moves: vec![],
        root: HashMap::new(),
    };
    let site_count = b.compiled.expressions["sites"].as_array().map_or(0, Vec::len);
    for definition in &d.components {
        for c in &definition.design.connectors {
            b.vector(&c.at, Kind::Length, c.line, "connector")?;
            let z = unit(b.vector(&c.z, Kind::Unitless, c.line, "connector_axis")?, c.line)?;
            let x = unit(b.vector(&c.x, Kind::Unitless, c.line, "connector_axis")?, c.line)?;
            if dot(z, x).abs() > 1e-9 {
                return Err(error(c.line, "connector axis and x direction must be perpendicular"));
            }
        }
        for i in &definition.design.instances {
            b.vector(&i.at, Kind::Length, i.line, "placement")?;
            unit(b.vector(&i.axis, Kind::Unitless, i.line, "placement_axis")?, i.line)?;
            b.scalar(&i.angle, Kind::Angle, i.line, "placement", false)?;
        }
    }
    if let Some(sites) = b.compiled.expressions["sites"].as_array_mut() {
        sites.truncate(site_count);
    }
    b.scope(d, None, 0)?;
    for (name, t, line) in b.moves.clone() {
        b.emit("occurrence.move", json!({"occurrence":name,"translate":t.at,"axis":t.axis,"angle":t.angle,"capture":true}), line);
    }
    for (name, line) in &d.grounds {
        b.emit("occurrence.ground", json!({"occurrence":name,"grounded":true}), *line);
    }
    // Capture all local origins before any joint moves an instance. Deterministic source order.
    for instance in &d.instances {
        let (definition, t) = b.root.get(&instance.name).copied().ok_or("missing instance")?;
        for c in &definition.design.connectors {
            let at = b.vector(&c.at, Kind::Length, c.line, "connector")?;
            let z = unit(b.vector(&c.z, Kind::Unitless, c.line, "connector_axis")?, c.line)?;
            let x = unit(b.vector(&c.x, Kind::Unitless, c.line, "connector_axis")?, c.line)?;
            if dot(z, x).abs() > 1e-9 {
                return Err(error(c.line, "connector axis and x direction must be perpendicular"));
            }
            b.emit("joint.origin",json!({"name":qualified(&instance.name,&c.name),"occurrence":instance.name,"point":t.point(at),"z":t.direction(z),"x":t.direction(x)}),c.line);
        }
    }
    let mut names = HashSet::new();
    for j in &d.joints {
        unique(&mut names, &j.name, j.line)?;
        let kinds: &[Kind] = match j.kind.as_str() {
            "rigid" => &[],
            "revolute" => &[Kind::Angle],
            "slider" => &[Kind::Length],
            "cylindrical" | "pin_slot" => &[Kind::Angle, Kind::Length],
            "planar" => &[Kind::Length, Kind::Length, Kind::Angle],
            "ball" => &[Kind::Angle, Kind::Angle, Kind::Angle],
            _ => {
                return Err(error(
                    j.line,
                    format!("unsupported joint type `{}`; expected rigid, revolute, slider, cylindrical, pin_slot, planar or ball", j.kind),
                ));
            }
        };
        if (!j.values.is_empty() && j.values.len() != kinds.len()) || j.limits.len() > kinds.len() {
            return Err(error(j.line, format!("{} joint takes {} values and at most {} limit pairs", j.kind, kinds.len(), kinds.len())));
        }
        if j.a.base == j.b.base {
            return Err(error(j.line, "a joint must connect two different instances"));
        }
        let mut values = vec![];
        for (v, k) in j.values.iter().zip(kinds) {
            values.push(b.expression(text(v), *k, j.line)?);
        }
        let mut limits = vec![];
        for (pair, k) in j.limits.iter().zip(kinds) {
            let [lo, hi] = pair.as_slice() else { return Err(error(j.line, "each limit needs (min,max)")) };
            let lo = b.scalar(text(lo), *k, j.line, "joint_limit", false)?;
            let hi = b.scalar(text(hi), *k, j.line, "joint_limit", false)?;
            if lo > hi {
                return Err(error(j.line, "joint limit minimum exceeds maximum"));
            }
            let convert = |v: f64| if *k == Kind::Angle { v.to_degrees() } else { v };
            limits.push([convert(lo), convert(hi)]);
        }
        let a = b.endpoint(&j.a, j.line)?;
        let other = b.endpoint(&j.b, j.line)?;
        let offset = b.expression(&j.offset, Kind::Length, j.line)?;
        let angle = b.expression(&j.angle, Kind::Angle, j.line)?;
        let mut params = json!({"name":j.name,"type":j.kind,"a":a,"b":other,"offset":offset,"angle":angle,"flip":j.flip,"limits":limits});
        if !values.is_empty() {
            params["values"] = json!(values);
        }
        b.emit("joint.create", params, j.line);
    }
    b.emit("component.activate", json!({"component":0}), 0);
    b.compiled.expressions["assembly"] = json!({"definitions":b.built.len(),"instances":b.moves.len(),"joints":d.joints.len()});
    if let Some(requires) = b.compiled.expressions["requires"].as_array_mut() {
        requires.push(json!("authored_occurrence_names"));
    }
    Ok(b.compiled)
}

/// Check required command capabilities before mutating a document.
pub fn check_capabilities(c: &Compiled, commands: &Value) -> R<()> {
    for required in c.expressions["requires"].as_array().into_iter().flatten() {
        let specs: &[(&str, &str)] = match required.as_str() {
            Some("persistent_sketch_point_expressions") => &[("sketch.point", "expression")],
            Some("authored_occurrence_names") => &[("component.create", "occurrence_name"), ("occurrence.copy", "occurrence_name")],
            _ => return Err(format!("unknown required engine capability {required}")),
        };
        for (id, field) in specs {
            if !commands.as_array().is_some_and(|a| a.iter().any(|c| c["id"] == *id && c["params"].as_str().is_some_and(|s| s.contains(field)))) {
                return Err(format!(
                    "this design requires engine capability {required} ({id}: {field}); use a compatible development build via SOLIDCRAFT_CLI or update the running app"
                ));
            }
        }
    }
    Ok(())
}
pub fn check_engine(cli: &str, c: &Compiled) -> R<()> {
    if c.expressions["requires"].as_array().is_none_or(|r| r.is_empty()) {
        return Ok(());
    }
    let out = std::process::Command::new(cli).arg("commands").output().map_err(|e| format!("cannot discover engine capabilities: {e}"))?;
    if !out.status.success() {
        return Err(format!("cannot discover engine capabilities: {}", String::from_utf8_lossy(&out.stderr)));
    }
    let commands: Value = serde_json::from_slice(&out.stdout).map_err(|e| e.to_string())?;
    check_capabilities(c, &commands)
}
