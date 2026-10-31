//! Parser: lines of tokens into the AST (`ast.rs`). The grammar is in README.md.

use crate::ast::*;
use crate::lex::{Line, Tok, lex};

/// Feature option keys and the kind of value each takes.
const EXPR_KEYS: &[&str] = &[
    "distance",
    "taper",
    "offset",
    "angle",
    "diameter",
    "radius",
    "clearance",
    "thread",
    "depth",
    "height",
    "width",
    "length",
    "major",
    "minor",
    "thickness",
    "count",
    "spacing",
    "count2",
    "spacing2",
    "fit",
    "sides",
    "distance2",
    "size",
    "designation",
    "turns",
    "pitch",
    "section_size",
    "twist",
    "scale",
    "wall",
    "k_factor",
    "bend_radius",
    "start_angle",
    "fraction",
    "factor",
    "gap",
    "margin",
    "top_radius",
    "top_diameter",
];
const TUPLE_KEYS: &[&str] = &[
    "profiles",
    "corner",
    "center",
    "base",
    "edges",
    "faces",
    "dir",
    "dir2",
    "at",
    "origin",
    "normal",
    "to",
    "position",
    "counterbore",
    "countersink",
    "face",
    "through",
    "edge",
    "pull",
    "factors",
    "bends",
    "axis_face",
    "axis_edge",
];
const LIST_KEYS: &[&str] = &["targets", "features", "bodies", "tools", "points", "sections", "between", "curves"];
const IDENT_KEYS: &[&str] = &[
    "from",
    "body",
    "axis",
    "plane",
    "target",
    "sketch",
    "type",
    "direction",
    "alignment",
    "alignment2",
    "rule",
    "section_position",
    "on",
    "neutral",
];
const FLAGS: &[&str] = &[
    "symmetric",
    "negative",
    "positive",
    "through_all",
    "join",
    "cut",
    "intersect",
    "new",
    "auto",
    "inside",
    "outside",
    "both",
    "drilled",
    "simple",
    "reversed",
    "reversed2",
    "identical",
    "copy",
    "keep_tools",
    "flip",
    "chain",
    "modeled",
    "deboss",
    "square",
    "parallel",
    "clockwise",
    "left",
    "internal",
    "external",
    "surface",
];

pub const FEATURES: &[&str] = &[
    "extrude",
    "revolve",
    "hole",
    "box",
    "cylinder",
    "sphere",
    "torus",
    "fillet",
    "chamfer",
    "shell",
    "mirror",
    "pattern",
    "combine",
    "move",
    "remove",
    "sweep",
    "loft",
    "pipe",
    "thread",
    "coil",
    "emboss",
    "sheet_rule",
    "flange",
    "plane",
    "split",
    "rib",
    "web",
    "draft",
    "offset_faces",
    "scale",
    "hem",
    "path_flange",
    "cone",
    "perforate",
    "thicken",
    "convert",
];

struct P<'a> {
    lines: &'a [Line],
    i: usize,
}

type R<T> = Result<T, String>;

fn err(line: usize, msg: impl std::fmt::Display) -> String {
    format!("line {line}: {msg}")
}

/// A cursor over one line's tokens.
struct Cur<'a> {
    no: usize,
    toks: &'a [Tok],
    raw: &'a str,
    i: usize,
}

impl<'a> Cur<'a> {
    fn new(l: &'a Line) -> Cur<'a> {
        Cur { no: l.no, toks: &l.toks, raw: &l.raw, i: 0 }
    }
    fn peek(&self) -> Option<&'a Tok> {
        self.toks.get(self.i)
    }
    fn peek_ident(&self) -> Option<&'a str> {
        match self.peek() {
            Some(Tok::Ident(s)) => Some(s.as_str()),
            _ => None,
        }
    }
    fn next(&mut self) -> Option<&'a Tok> {
        let t = self.toks.get(self.i);
        self.i += 1;
        t
    }
    fn done(&self) -> bool {
        self.i >= self.toks.len()
    }
    fn eat_ident(&mut self, word: &str) -> bool {
        if self.peek_ident() == Some(word) {
            self.i += 1;
            true
        } else {
            false
        }
    }
    fn eat_punct(&mut self, c: char) -> bool {
        if self.peek() == Some(&Tok::Punct(c)) {
            self.i += 1;
            true
        } else {
            false
        }
    }
    fn expect_punct(&mut self, c: char) -> R<()> {
        if self.eat_punct(c) { Ok(()) } else { Err(err(self.no, format!("expected `{c}`, found {}", self.found()))) }
    }
    fn found(&self) -> String {
        self.peek().map(Tok::describe).unwrap_or_else(|| "end of line".into())
    }
    fn ident(&mut self, what: &str) -> R<String> {
        match self.next() {
            Some(Tok::Ident(s)) => Ok(s.clone()),
            Some(Tok::Str(s)) if !s.is_empty() => Ok(s.clone()),
            other => Err(err(self.no, format!("expected {what}, found {}", other.map(Tok::describe).unwrap_or_else(|| "end of line".into())))),
        }
    }
    /// `(x, y)` or a reference to an existing point.
    fn pt(&mut self) -> R<Pt> {
        if self.peek() == Some(&Tok::Punct('(')) { Ok(Pt::Xy(self.point2()?)) } else { Ok(Pt::Ref(self.reference()?)) }
    }
    /// Coordinate expressions are delimited by commas at tuple depth zero. Nested calls
    /// such as min(L/2, W/2) are kept as one component.
    fn point2(&mut self) -> R<[Comp; 2]> {
        self.tuple()?.try_into().map_err(|_| err(self.no, "expected exactly two coordinates"))
    }
    fn tuple(&mut self) -> R<Vec<Comp>> {
        self.expect_punct('(')?;
        let mut out = Vec::new();
        loop {
            let start = self.i;
            let mut depth = 0usize;
            loop {
                match self.peek() {
                    Some(Tok::Punct(',' | ')')) if depth == 0 => break,
                    Some(Tok::Punct('(')) => depth += 1,
                    Some(Tok::Punct(')')) => depth -= 1,
                    None => return Err(err(self.no, "unterminated coordinate tuple")),
                    _ => {}
                }
                self.i += 1;
            }
            let tokens = &self.toks[start..self.i];
            let component = match tokens {
                [] => return Err(err(self.no, "empty coordinate expression")),
                [Tok::Num(n, text)] => Comp::Num(*n, text.clone()),
                _ => Comp::Expr(tokens.iter().map(tok_text).collect::<Vec<_>>().join(" ")),
            };
            out.push(component);
            if self.eat_punct(')') {
                return Ok(out);
            }
            self.expect_punct(',')?;
        }
    }
    /// A single-token expression: a number, a word, a quoted string, or a parenthesised group.
    fn expr1(&mut self, what: &str) -> R<String> {
        match self.next() {
            Some(Tok::Num(_, s)) => {
                // `6 mm`: a number followed by a unit word.
                if let Some(Tok::Ident(u)) = self.peek()
                    && matches!(u.as_str(), "mm" | "cm" | "m" | "in" | "ft" | "deg" | "rad")
                {
                    self.i += 1;
                    return Ok(format!("{s} {u}"));
                }
                Ok(s.clone())
            }
            Some(Tok::Ident(s)) => Ok(s.clone()),
            Some(Tok::Str(s)) => Ok(s.clone()),
            Some(Tok::Punct('(')) => {
                // Up to the matching `)`: rebuild the text from the tokens.
                let mut depth = 1;
                let mut s = String::new();
                loop {
                    match self.next() {
                        Some(Tok::Punct('(')) => {
                            depth += 1;
                            s.push('(');
                        }
                        Some(Tok::Punct(')')) => {
                            depth -= 1;
                            if depth == 0 {
                                break;
                            }
                            s.push(')');
                        }
                        Some(t) => {
                            if !s.is_empty() && !s.ends_with('(') {
                                s.push(' ');
                            }
                            s.push_str(&tok_text(t));
                        }
                        None => return Err(err(self.no, "unterminated `(` in expression")),
                    }
                }
                Ok(s)
            }
            other => Err(err(self.no, format!("expected {what}, found {}", other.map(Tok::describe).unwrap_or_else(|| "end of line".into())))),
        }
    }
    /// The raw text after the first `=` on the line (for `param` and `dim` values).
    fn rest_after_eq(&mut self) -> R<String> {
        self.expect_punct('=')?;
        let s = self.raw.split_once('=').map(|x| x.1.trim()).unwrap_or("");
        if s.is_empty() {
            return Err(err(self.no, "expected an expression after `=`"));
        }
        self.i = self.toks.len();
        Ok(s.to_string())
    }
    /// `name`, `name.part`, `name.3.end`
    fn reference(&mut self) -> R<Ref> {
        let base = self.ident("an entity name")?;
        let mut parts = Vec::new();
        while self.eat_punct('.') {
            match self.next() {
                Some(Tok::Ident(s)) => parts.push(s.clone()),
                Some(Tok::Num(v, s)) if v.fract() == 0.0 && *v >= 1.0 => parts.push(s.clone()),
                other => {
                    return Err(err(
                        self.no,
                        format!("expected a part name after `.`, found {}", other.map(Tok::describe).unwrap_or_else(|| "end of line".into())),
                    ));
                }
            }
        }
        Ok(Ref { base, parts })
    }
    fn end(&self) -> R<()> {
        if self.done() { Ok(()) } else { Err(err(self.no, format!("unexpected {} at the end of the statement", self.found()))) }
    }
}

fn tok_text(t: &Tok) -> String {
    match t {
        Tok::Ident(s) => s.clone(),
        Tok::Num(_, s) => s.clone(),
        Tok::Str(s) => s.clone(),
        Tok::Punct(c) => c.to_string(),
        Tok::Arrow => "->".into(),
    }
}

/// One `use` line (see `ast::Use`); `reuse::expand` replaces it with the other design.
pub fn parse_use(line: &Line) -> R<Use> {
    let mut c = Cur::new(line);
    if c.ident("`use`")? != "use" {
        return Err(err(line.no, "expected `use`"));
    }
    let alias = c.ident("the name for the part's copy")?;
    if !c.eat_ident("from") {
        return Err(err(line.no, "expected `from \"part.sc\"`"));
    }
    let file = match c.next() {
        Some(Tok::Str(s)) if !s.trim().is_empty() => s.clone(),
        _ => return Err(err(line.no, "expected the part file in quotes: `from \"part.sc\"`")),
    };
    let mut u = Use { alias, file, overrides: vec![], rotate: None, at: None, join: None, body: None };
    while !c.done() {
        if c.peek() == Some(&Tok::Arrow) {
            c.i += 1;
            if !c.eat_ident("body") {
                return Err(err(line.no, "expected `-> body <name>`"));
            }
            u.body = Some(c.ident("the body name")?);
            continue;
        }
        match c.ident("a use option: with, rotate, at, join or -> body")?.as_str() {
            "with" => {
                c.expect_punct('(')?;
                loop {
                    let name = c.ident("a parameter of the part")?;
                    c.expect_punct('=')?;
                    // Up to the next top-level `,` or the closing `)`.
                    let mut depth = 0;
                    let mut text = String::new();
                    loop {
                        match c.next() {
                            Some(Tok::Punct('(')) => {
                                depth += 1;
                                text.push('(');
                            }
                            Some(Tok::Punct(')')) if depth > 0 => {
                                depth -= 1;
                                text.push(')');
                            }
                            Some(Tok::Punct(')' | ',')) => {
                                c.i -= 1;
                                break;
                            }
                            Some(t) => {
                                if !text.is_empty() && !text.ends_with('(') {
                                    text.push(' ');
                                }
                                text.push_str(&tok_text(t));
                            }
                            None => return Err(err(line.no, "unterminated `with (…)`")),
                        }
                    }
                    if text.is_empty() {
                        return Err(err(line.no, format!("`{name} =` needs a value")));
                    }
                    u.overrides.push((name, text));
                    if c.eat_punct(')') {
                        break;
                    }
                    c.expect_punct(',')?;
                }
            }
            "rotate" => {
                let axis = parse_axis(&mut c)?;
                u.rotate = Some((axis, c.expr1("the rotation angle")?));
            }
            "at" => u.at = Some(c.tuple()?),
            "join" => {
                if !c.eat_ident("targets") {
                    return Err(err(line.no, "expected `join targets <body>`"));
                }
                let mut targets = vec![c.ident("a body")?];
                while c.eat_punct(',') {
                    targets.push(c.ident("a body")?);
                }
                u.join = Some(targets);
            }
            other => return Err(err(line.no, format!("unknown use option `{other}`"))),
        }
    }
    if u.join.is_some() && u.body.is_some() {
        return Err(err(line.no, "a used part either joins a body or is named with `-> body`, not both"));
    }
    Ok(u)
}

pub fn parse(src: &str) -> R<Design> {
    let lines = lex(src)?;
    let mut p = P { lines: &lines, i: 0 };
    parse_design(&mut p, None)
}

fn parse_design(p: &mut P, open: Option<usize>) -> R<Design> {
    let mut d = Design::default();
    while let Some(l) = p.lines.get(p.i) {
        if l.toks == [Tok::Punct('}')] {
            if open.is_none() {
                return Err(err(l.no, "unexpected `}`"));
            }
            p.i += 1;
            return Ok(d);
        }
        p.i += 1;
        let mut c = Cur::new(l);
        let head = c.ident("a statement")?;
        match head.as_str() {
            "component" => {
                if open.is_some() {
                    return Err(err(l.no, "define components at the top level; nest them with instances"));
                }
                let name = c.ident("the component name")?;
                let source = if c.eat_ident("from") {
                    match c.next() {
                        Some(Tok::Str(s)) if !s.trim().is_empty() => Some(s.clone()),
                        _ => return Err(err(l.no, "expected a quoted part file after `from`, e.g. `from \"wheel.sc\"`")),
                    }
                } else {
                    None
                };
                c.expect_punct('{')?;
                c.end()?;
                let design = parse_design(p, Some(l.no))?;
                if source.is_some() && (!design.items.is_empty() || !design.instances.is_empty()) {
                    return Err(err(l.no, "a component linked `from` a part file holds connectors only; its geometry is the part's"));
                }
                d.components.push(Component { name, design, line: l.no, source });
            }
            "instance" => {
                let name = c.ident("the instance name")?;
                c.expect_punct('=')?;
                let component = c.ident("a component definition")?;
                let mut at = zero3();
                let mut axis = axis3("Z");
                let mut angle = "0 deg".into();
                if c.eat_ident("at") {
                    at = c.tuple()?;
                }
                if c.eat_ident("rotate") {
                    axis = parse_axis(&mut c)?;
                    angle = c.expr1("a rotation angle")?;
                }
                c.end()?;
                d.instances.push(Instance { name, component, at, axis, angle, line: l.no });
            }
            "connector" => {
                if open.is_none() {
                    return Err(err(l.no, "connectors belong inside a component definition"));
                }
                let name = c.ident("the connector name")?;
                if !c.eat_ident("at") {
                    return Err(err(l.no, "expected `at (x,y,z)`"));
                }
                let at = c.tuple()?;
                let z = if c.eat_ident("axis") { parse_axis(&mut c)? } else { axis3("Z") };
                let x = if c.eat_ident("x") { parse_axis(&mut c)? } else { axis3("X") };
                c.end()?;
                d.connectors.push(Connector { name, at, z, x, line: l.no });
            }
            "ground" => {
                let name = c.ident("an instance name")?;
                c.end()?;
                d.grounds.push((name, l.no));
            }
            // `color A, B "#rrggbb"`, `material A, B "PLA"`
            "color" | "material" => {
                let mut bodies = vec![c.ident("a body name")?];
                while c.eat_punct(',') {
                    bodies.push(c.ident("a body name")?);
                }
                let value = c.expr1(if head == "color" { "a colour, \"#rrggbb\"" } else { "a material name" })?;
                let mut opts = vec![("bodies".to_string(), Val::List(bodies)), ("value".to_string(), Val::Expr(value))];
                // `color Body "#rrggbb" faces (x,y,z) …`: only those faces.
                if head == "color" && c.eat_ident("faces") {
                    let mut faces = vec![c.tuple()?];
                    while c.peek() == Some(&Tok::Punct('(')) {
                        faces.push(c.tuple()?);
                    }
                    opts.push(("faces".to_string(), Val::Tuples(faces)));
                }
                c.end()?;
                d.items.push(Item::Feature(Feature { kind: head.clone(), name: format!("{head}@{}", l.no), opts, line: l.no }));
            }
            // `print A, B up X` or `print bed (w, d)`
            "print" => {
                if open.is_some() {
                    return Err(err(l.no, "print setup belongs at the top level"));
                }
                if c.eat_ident("bed") {
                    let bed = c.tuple()?;
                    if bed.len() != 2 {
                        return Err(err(l.no, "`print bed (width, depth)`"));
                    }
                    c.end()?;
                    d.print_bed = Some((bed, l.no));
                } else {
                    let mut bodies = vec![c.ident("a body name")?];
                    while c.eat_punct(',') {
                        bodies.push(c.ident("a body name")?);
                    }
                    if !c.eat_ident("up") {
                        return Err(err(l.no, "expected `up X|Y|Z|(x,y,z)`: the direction that points away from the bed"));
                    }
                    let up = parse_axis(&mut c)?;
                    c.end()?;
                    d.prints.push(Print { bodies, up, line: l.no });
                }
            }
            "for" | "if" => {
                return Err(err(l.no, format!("`{head}` blocks are expanded before parsing: build the design with `build` or from its file")));
            }
            "use" => {
                return Err(err(l.no, "`use` reads another file relative to this one: build the design from its file (`run`, `evaluate`, `watch`)"));
            }
            // `suppress A, B`: build these features, then switch them off (SolidCraft can turn them on).
            "suppress" => {
                let mut features = vec![c.ident("a feature name")?];
                while c.eat_punct(',') {
                    features.push(c.ident("a feature name")?);
                }
                c.end()?;
                let opts = vec![("features".to_string(), Val::List(features))];
                d.items.push(Item::Feature(Feature { kind: "suppress".into(), name: format!("suppress@{}", l.no), opts, line: l.no }));
            }
            "check" => {
                if open.is_some() {
                    return Err(err(l.no, "checks belong at the top level"));
                }
                d.checks.push(parse_check(&mut c, l.no)?);
            }
            "joint" => {
                if open.is_some() {
                    return Err(err(
                        l.no,
                        "joints currently belong at the top level; repeated subassembly joints require placement-aware engine origins",
                    ));
                }
                let name = c.ident("the joint name")?;
                let kind = c.ident("a joint type")?;
                if !c.eat_ident("between") {
                    return Err(err(l.no, "expected `between instance.connector and instance.connector`"));
                }
                let a = c.reference()?;
                if !c.eat_ident("and") {
                    return Err(err(l.no, "expected `and`"));
                }
                let b = c.reference()?;
                let mut joint =
                    Joint { name, kind, a, b, values: vec![], limits: vec![], offset: "0 mm".into(), angle: "0 deg".into(), flip: true, line: l.no };
                let mut seen = std::collections::HashSet::new();
                while !c.done() {
                    let key = c.ident("a joint option")?;
                    let canonical = if key == "value" { "values" } else { &key };
                    if !seen.insert(canonical.to_string()) {
                        return Err(err(l.no, format!("duplicate joint option `{key}`")));
                    }
                    match key.as_str() {
                        "value" => joint.values.push(Comp::Expr(c.expr1("a joint value")?)),
                        "values" => joint.values = c.tuple()?,
                        "limits" => {
                            while c.peek() == Some(&Tok::Punct('(')) {
                                joint.limits.push(c.tuple()?);
                            }
                            if joint.limits.is_empty() {
                                return Err(err(l.no, "expected limits (min,max), one pair per joint value"));
                            }
                        }
                        "offset" => joint.offset = c.expr1("a joint offset")?,
                        "angle" => joint.angle = c.expr1("a frame angle")?,
                        "opposed" => joint.flip = false,
                        _ => return Err(err(l.no, format!("unknown joint option `{key}`"))),
                    }
                }
                d.joints.push(joint);
            }
            "design" => {
                if open.is_some() {
                    return Err(err(l.no, "design declarations belong at the top level"));
                }
                d.name = Some(c.ident("the design name")?);
                if c.eat_ident("units") {
                    d.units = Some(c.ident("a unit")?);
                }
                c.end()?;
            }
            "param" | "let" => {
                if open.is_some() {
                    return Err(err(l.no, "parameters and helpers are global; declare them outside components"));
                }
                let name = c.ident("the parameter name")?;
                let expr = c.rest_after_eq()?;
                d.params.push(Param { name, expr, comment: None, line: l.no, local: head == "let" });
            }
            "sketch" => {
                let s = parse_sketch_head(&mut c, l.no)?;
                let stmts = parse_block(p, l.no, parse_sketch_stmt)?;
                d.items.push(Item::Sketch(Sketch { stmts, ..s }));
            }
            k if FEATURES.contains(&k) => d.items.push(Item::Feature(parse_feature(&mut c, k)?)),
            other => return Err(err(l.no, format!("unknown statement `{other}`"))),
        }
    }
    if let Some(line) = open {
        return Err(err(line, "component block is never closed with `}`"));
    }
    Ok(d)
}

fn zero3() -> Vec<Comp> {
    vec![Comp::Num(0.0, "0".into()); 3]
}
fn axis3(axis: &str) -> Vec<Comp> {
    ["X", "Y", "Z"]
        .iter()
        .map(|a| {
            let n = if *a == axis { 1.0 } else { 0.0 };
            Comp::Num(n, format!("{n}"))
        })
        .collect()
}
fn parse_axis(c: &mut Cur) -> R<Vec<Comp>> {
    if c.peek() == Some(&Tok::Punct('(')) {
        return c.tuple();
    }
    let axis = c.ident("X, Y, Z or (x,y,z)")?;
    if !matches!(axis.as_str(), "X" | "Y" | "Z") {
        return Err(err(c.no, "axis must be X, Y, Z or (x,y,z)"));
    }
    Ok(axis3(&axis))
}

/// `check Body quantity [Other] op value [unit] [up axis]`
fn parse_check(c: &mut Cur, line: usize) -> R<Check> {
    let body = c.ident("a body name")?;
    let mut quantity = c.ident("what to check: shells, volume, area, mass, size.x, clearance or overhang")?;
    if quantity == "size" {
        c.expect_punct('.')?;
        quantity = format!("size.{}", c.ident("x, y or z")?);
    }
    if !matches!(quantity.as_str(), "shells" | "volume" | "area" | "mass" | "size.x" | "size.y" | "size.z" | "clearance" | "overhang") {
        return Err(err(line, format!("cannot check `{quantity}`: use shells, volume, area, mass, size.x|y|z, clearance or overhang")));
    }
    let other = if quantity == "clearance" { Some(c.ident("the body to measure the clearance to")?) } else { None };
    let op = match c.next() {
        Some(Tok::Punct(p @ ('<' | '>' | '='))) => {
            let p = *p;
            if c.eat_punct('=') {
                format!("{p}=")
            } else if p == '=' {
                return Err(err(line, "write `==` to compare for equality"));
            } else {
                p.to_string()
            }
        }
        _ => return Err(err(line, "expected a comparison: ==, <=, >=, < or >")),
    };
    let value = c.expr1("the value to compare with")?;
    let unit = match c.peek() {
        Some(Tok::Ident(u)) if matches!(u.as_str(), "mm2" | "mm3" | "cm3" | "g" | "kg") => {
            let u = u.clone();
            c.i += 1;
            Some(u)
        }
        _ => None,
    };
    let mut up = Vec::new();
    let mut bridge = None;
    loop {
        if c.eat_ident("up") {
            up = parse_axis(c)?;
        } else if c.eat_ident("bridge") {
            bridge = Some(c.expr1("the longest bridge")?);
        } else {
            break;
        }
    }
    if quantity != "overhang" && (bridge.is_some() || !up.is_empty()) {
        return Err(err(line, "`up` and `bridge` belong to overhang checks"));
    }
    c.end()?;
    Ok(Check { body, quantity, other, op, value, unit, up, bridge, line })
}

fn parse_sketch_head(c: &mut Cur, line: usize) -> R<Sketch> {
    let name = c.ident("the sketch name")?;
    if !c.eat_ident("on") {
        return Err(err(line, "expected `on <plane>` after the sketch name"));
    }
    let plane = if c.eat_ident("face") { Plane::Face(c.tuple()?) } else { Plane::Named(c.ident("a plane: XY, XZ, YZ or a construction plane")?) };
    let mut offset = None;
    let mut flip = false;
    let mut explicit = false;
    loop {
        if c.eat_ident("offset") {
            offset = Some(c.expr1("the offset")?);
        } else if c.eat_ident("flip") {
            flip = true;
        } else if c.eat_ident("explicit") {
            explicit = true;
        } else {
            break;
        }
    }
    c.expect_punct('{')?;
    c.end()?;
    Ok(Sketch { name, plane, offset, flip, explicit, stmts: Vec::new(), line })
}

/// Lines up to the matching `}`.
fn parse_block<T>(p: &mut P, open: usize, f: impl Fn(&mut Cur) -> R<T>) -> R<Vec<T>> {
    let mut out = Vec::new();
    loop {
        let Some(l) = p.lines.get(p.i) else { return Err(err(open, "block is never closed with `}`")) };
        p.i += 1;
        if l.toks == [Tok::Punct('}')] {
            return Ok(out);
        }
        let mut c = Cur::new(l);
        out.push(f(&mut c)?);
    }
}

fn parse_points(c: &mut Cur, min: usize) -> R<Vec<Pt>> {
    let mut pts = Vec::new();
    while c.peek() == Some(&Tok::Punct('(')) || matches!(c.peek(), Some(Tok::Ident(w)) if w != "periodic" && w != "closed" && w != "infer") {
        pts.push(c.pt()?);
    }
    if pts.len() < min {
        return Err(err(c.no, format!("expected at least {min} points `(x,y)`")));
    }
    Ok(pts)
}

/// The entities an offset, mirror or pattern copies: references, optionally comma-separated,
/// up to the keyword that follows them.
fn derived_refs(c: &mut Cur, line: usize, until: &[&str]) -> R<Vec<Ref>> {
    let mut refs = Vec::new();
    while !c.peek_ident().is_some_and(|w| until.contains(&w)) {
        if c.done() {
            return Err(err(line, format!("expected `{}` after the entities", until[0])));
        }
        refs.push(c.reference()?);
        c.eat_punct(',');
    }
    c.i += 1;
    if refs.is_empty() {
        return Err(err(line, "name the entities to copy"));
    }
    Ok(refs)
}

fn parse_sketch_stmt(c: &mut Cur) -> R<SketchStmt> {
    let line = c.no;
    let mut construction = false;
    let mut flags: Vec<String> = Vec::new();
    // Leading modifiers: `construction`, `closed`, `infer`, `center`, `rounded`, `periodic`.
    let head = loop {
        let w = c.ident("a sketch statement")?;
        match w.as_str() {
            "construction" => construction = true,
            "bind" | "closed" | "infer" | "center" | "rounded" | "periodic" | "inscribed" | "circumscribed" | "centers" | "ends" => flags.push(w),
            _ => break w,
        }
    };
    // Modifiers may also follow the keyword: `rect center r = …`, `polyline closed infer r = …`.
    loop {
        match c.peek_ident() {
            Some("construction") => {
                construction = true;
                c.i += 1;
            }
            Some(w @ ("closed" | "infer" | "center" | "rounded" | "periodic" | "inscribed" | "circumscribed" | "centers" | "ends"))
                if matches!(head.as_str(), "line" | "polyline" | "rect" | "spline" | "polygon" | "slot") =>
            {
                flags.push(w.to_string());
                c.i += 1;
            }
            _ => break,
        }
    }
    let has = |f: &str| flags.iter().any(|x| x == f);
    if has("bind") && head != "point" {
        return Err(err(line, "bind creates a point; use bound points as references in curves"));
    }
    let kind = match head.as_str() {
        "line" => {
            let name = c.ident("the line name")?;
            c.expect_punct('=')?;
            let pts = parse_points(c, 2)?;
            if pts.len() == 2 {
                SketchKind::Line { name, pts, construction, infer: has("infer") }
            } else {
                SketchKind::Polyline { name, pts, closed: has("closed"), construction, infer: has("infer") }
            }
        }
        "polyline" => {
            let name = c.ident("the polyline name")?;
            c.expect_punct('=')?;
            let pts = parse_points(c, 2)?;
            SketchKind::Polyline { name, pts, closed: has("closed"), construction, infer: has("infer") }
        }
        "rect" => {
            let name = c.ident("the rectangle name")?;
            c.expect_punct('=')?;
            let p0 = c.point2()?;
            let p1 = c.point2()?;
            let kind = if has("center") {
                RectKind::Center
            } else if has("rounded") || c.peek_ident() == Some("radius") {
                if !c.eat_ident("radius") {
                    return Err(err(line, "a rounded rectangle needs `radius <expr>`"));
                }
                RectKind::Rounded(c.expr1("the corner radius")?)
            } else {
                RectKind::TwoPoint
            };
            SketchKind::Rect { name, kind, p0, p1, construction }
        }
        "circle" => {
            let name = c.ident("the circle name")?;
            c.expect_punct('=')?;
            let center = c.pt()?;
            let size = if c.eat_ident("radius") {
                CircleSize::Radius(c.expr1("the radius")?)
            } else if c.eat_ident("diameter") {
                CircleSize::Diameter(c.expr1("the diameter")?)
            } else {
                return Err(err(line, "a circle needs `radius <expr>` or `diameter <expr>`"));
            };
            SketchKind::Circle { name, center, size }
        }
        "arc" => {
            let name = c.ident("the arc name")?;
            c.expect_punct('=')?;
            if c.eat_ident("center") {
                let center = c.pt()?;
                if !c.eat_ident("start") {
                    return Err(err(line, "expected `start (x,y)`"));
                }
                let start = c.pt()?;
                if !c.eat_ident("end") {
                    return Err(err(line, "expected `end (x,y)`"));
                }
                let end = c.pt()?;
                SketchKind::ArcCenter { name, center, start, end }
            } else if c.eat_ident("start") {
                let start = c.pt()?;
                if !c.eat_ident("through") {
                    return Err(err(line, "expected `through (x,y)`"));
                }
                let through = c.pt()?;
                if !c.eat_ident("end") {
                    return Err(err(line, "expected `end (x,y)`"));
                }
                let end = c.pt()?;
                SketchKind::ArcThree { name, start, through, end }
            } else {
                return Err(err(line, "an arc is `center (c) start (s) end (e)` or `start (s) through (t) end (e)`"));
            }
        }
        "point" => {
            let name = c.ident("the point name")?;
            c.expect_punct('=')?;
            SketchKind::Point { name, p: c.point2()?, bound: has("bind") }
        }
        "polygon" => {
            let name = c.ident("the polygon name")?;
            c.expect_punct('=')?;
            let center = c.point2()?;
            if !c.eat_ident("radius") {
                return Err(err(line, "expected `radius <expr>`"));
            }
            let radius = c.expr1("the radius")?;
            if !c.eat_ident("sides") {
                return Err(err(line, "expected `sides <n>`"));
            }
            let sides = c.expr1("the side count")?;
            let circumscribed = has("circumscribed") || c.eat_ident("circumscribed");
            c.eat_ident("inscribed");
            SketchKind::Polygon { name, center, radius, sides, circumscribed }
        }
        "slot" => {
            let name = c.ident("the slot name")?;
            c.expect_punct('=')?;
            let centers = if c.eat_ident("centers") {
                true
            } else if c.eat_ident("ends") {
                false
            } else {
                has("centers") || !has("ends")
            };
            let p0 = c.point2()?;
            let p1 = c.point2()?;
            if !c.eat_ident("width") {
                return Err(err(line, "expected `width <expr>`"));
            }
            SketchKind::Slot { name, centers, p0, p1, width: c.expr1("the width")? }
        }
        "spline" => {
            let name = c.ident("the spline name")?;
            c.expect_punct('=')?;
            let pts = parse_points(c, 2)?;
            let periodic = has("periodic") || c.eat_ident("periodic");
            SketchKind::Spline { name, pts, periodic }
        }
        "project" => {
            let what = c.ident("what to project: face, edge, vertex or body")?;
            let (point, body) = match what.as_str() {
                "face" | "edge" | "vertex" => {
                    let p = c.tuple()?;
                    if p.len() != 3 {
                        return Err(err(line, format!("`project {what}` takes a point (x,y,z) on it")));
                    }
                    let body = if c.eat_ident("body") { Some(c.ident("the body name")?) } else { None };
                    (Some(p), body)
                }
                "body" => (None, Some(c.ident("the body name")?)),
                other => return Err(err(line, format!("`project` takes face, edge, vertex or body, not `{other}`"))),
            };
            SketchKind::Project { what, point, body }
        }
        "svg" | "dxf" => {
            let path = match c.next() {
                Some(Tok::Str(s)) => s.clone(),
                _ => return Err(err(line, format!("expected the file in quotes: `{head} \"drawing.{head}\"`"))),
            };
            let (mut at, mut scale) = (None, None);
            while !c.done() {
                match c.ident("an import option: at or scale")?.as_str() {
                    "at" => at = Some(c.point2()?),
                    "scale" => scale = Some(c.expr1("the scale")?),
                    other => return Err(err(line, format!("unknown {head} option `{other}`"))),
                }
            }
            SketchKind::Import { format: head.clone(), path, at, scale }
        }
        "curve" => {
            let name = c.ident("the curve name")?;
            c.expect_punct('=')?;
            let xy = c.tuple()?;
            let [x, y]: [Comp; 2] = xy.try_into().map_err(|_| err(line, "a curve is `(x(t), y(t))`"))?;
            if !(c.eat_ident("t") && c.eat_ident("from")) {
                return Err(err(line, "expected `t from <start> to <end>` after the curve's `(x(t), y(t))`"));
            }
            let from = c.expr1("the start of t")?;
            if !c.eat_ident("to") {
                return Err(err(line, "expected `to <end>`"));
            }
            let to = c.expr1("the end of t")?;
            let (mut points, mut closed) = (None, false);
            while !c.done() {
                match c.ident("a curve option: points or closed")?.as_str() {
                    "points" => points = Some(c.expr1("the number of points")?),
                    "closed" => closed = true,
                    other => return Err(err(line, format!("unknown curve option `{other}`"))),
                }
            }
            let text = |c: Comp| match c {
                Comp::Num(_, s) | Comp::Expr(s) => s,
            };
            SketchKind::Curve { name, x: text(x), y: text(y), from, to, points, closed }
        }
        "ellipse" => {
            let name = c.ident("the ellipse name")?;
            c.expect_punct('=')?;
            let center = c.point2()?;
            if !c.eat_ident("major") {
                return Err(err(line, "expected `major (x, y)`: the end of the major axis"));
            }
            let major = c.point2()?;
            if !c.eat_ident("minor") {
                return Err(err(line, "expected `minor <radius>`"));
            }
            SketchKind::Ellipse { name, center, major, minor: c.expr1("the minor radius")? }
        }
        "text" => {
            // `text "WORDS"`, `text label` (a text parameter) or `text (label + " - L")`.
            let (text, expression) = match c.next() {
                Some(Tok::Str(s)) => (s.clone(), false),
                Some(Tok::Ident(s)) => (s.clone(), true),
                Some(Tok::Punct('(')) => {
                    let mut e = String::new();
                    loop {
                        match c.next() {
                            Some(Tok::Punct(')')) => break,
                            Some(Tok::Str(s)) => e.push_str(&format!("'{s}'")),
                            Some(t) => e.push_str(&tok_text(t)),
                            None => return Err(err(line, "unterminated `(` in the text expression")),
                        }
                        e.push(' ');
                    }
                    (e.trim().to_string(), true)
                }
                _ => return Err(err(line, "expected the text in quotes: `text \"LABEL\" at (x, y)`, or a text parameter")),
            };
            if !c.eat_ident("at") {
                return Err(err(line, "expected `at (x, y)`: the start of the baseline"));
            }
            let at = c.point2()?;
            let (mut height, mut angle, mut align, mut font, mut bold, mut italic) = (None, None, None, None, false, false);
            while !c.done() {
                match c.ident("a text option")?.as_str() {
                    "height" => height = Some(c.expr1("the height")?),
                    "angle" => angle = Some(c.expr1("the angle")?),
                    "align" => align = Some(c.ident("left, center or right")?),
                    "font" => font = Some(c.expr1("the font")?),
                    "bold" => bold = true,
                    "italic" => italic = true,
                    other => return Err(err(line, format!("unknown text option `{other}`"))),
                }
            }
            SketchKind::Text { text, expression, at, height, angle, align, font, bold, italic }
        }
        "dim" => {
            let kind = match c.peek_ident() {
                Some(k @ ("auto" | "distance" | "horizontal" | "vertical" | "length" | "radius" | "diameter" | "angle")) => {
                    // `dim horizontal = …` would make `horizontal` the entity; require a ref after.
                    let k = k.to_string();
                    c.i += 1;
                    Some(k)
                }
                _ => None,
            };
            let mut refs = vec![c.reference()?];
            if c.peek() == Some(&Tok::Arrow) {
                c.i += 1;
                refs.push(c.reference()?);
            }
            let value = c.rest_after_eq()?;
            SketchKind::Dim { kind, refs, value }
        }
        "offset" => {
            let refs = derived_refs(c, line, &["distance"])?;
            let distance = c.expr1("the offset distance")?;
            let (mut side, mut symmetric) = (None, false);
            while !c.done() {
                match c.ident("an offset option: side or symmetric")?.as_str() {
                    "side" => side = Some(c.point2()?),
                    "symmetric" => symmetric = true,
                    other => return Err(err(line, format!("unknown offset option `{other}`"))),
                }
            }
            SketchKind::Offset { refs, distance, side, symmetric }
        }
        "mirror" => {
            let refs = derived_refs(c, line, &["about"])?;
            SketchKind::Mirror { refs, about: c.reference()? }
        }
        "pattern" => {
            let circular = match c.ident("circular or rectangular")?.as_str() {
                "circular" => true,
                "rectangular" => false,
                other => return Err(err(line, format!("expected `pattern circular` or `pattern rectangular`, found `{other}`"))),
            };
            let refs = derived_refs(c, line, &["count"])?;
            let count = c.expr1("the count")?;
            let (mut center, mut angle, mut spacing, mut dir, mut count2, mut spacing2, mut dir2) = (None, None, None, None, None, None, None);
            while !c.done() {
                match (c.ident("a pattern option")?.as_str(), circular) {
                    ("center", true) => center = Some(if c.peek() == Some(&Tok::Punct('(')) { Pt::Xy(c.point2()?) } else { Pt::Ref(c.reference()?) }),
                    ("angle", true) => angle = Some(c.expr1("the total angle")?),
                    ("spacing", false) => spacing = Some(c.expr1("the spacing")?),
                    ("dir", false) => dir = Some(c.point2()?),
                    ("count2", false) => count2 = Some(c.expr1("the second count")?),
                    ("spacing2", false) => spacing2 = Some(c.expr1("the second spacing")?),
                    ("dir2", false) => dir2 = Some(c.point2()?),
                    (other, _) => {
                        return Err(err(line, format!("unknown {} pattern option `{other}`", if circular { "circular" } else { "rectangular" })));
                    }
                }
            }
            if !circular && (spacing.is_none() || count2.is_some() != spacing2.is_some()) {
                return Err(err(line, "a rectangular pattern needs `spacing`, and `count2` goes with `spacing2`"));
            }
            SketchKind::Pattern { refs, circular, count, center, angle, spacing, dir, count2, spacing2, dir2 }
        }
        "fix" => {
            let mut refs = vec![c.reference()?];
            while !c.done() {
                refs.push(c.reference()?);
            }
            SketchKind::Fix { refs }
        }
        "fillet" => {
            let point = c.reference()?;
            if !c.eat_ident("radius") {
                return Err(err(line, "expected `radius <expr>`"));
            }
            SketchKind::Fillet { point, radius: c.expr1("the radius")? }
        }
        k @ ("horizontal" | "vertical" | "coincident" | "tangent" | "equal" | "parallel" | "perpendicular" | "concentric" | "collinear"
        | "midpoint" | "symmetric" | "curvature") => {
            let mut refs = Vec::new();
            let mut about = None;
            while !c.done() {
                if c.eat_ident("about") {
                    about = Some(c.reference()?);
                    break;
                }
                refs.push(c.reference()?);
            }
            let need = match k {
                "horizontal" | "vertical" => 1,
                _ => 2,
            };
            if refs.len() < need {
                return Err(err(line, format!("`{k}` needs at least {need} entit{}", if need == 1 { "y" } else { "ies" })));
            }
            if k == "symmetric" && about.is_none() {
                return Err(err(line, "`symmetric a b about <line|x|y|point>`"));
            }
            SketchKind::Constraint { kind: k.to_string(), refs, about }
        }
        other => return Err(err(line, format!("unknown sketch statement `{other}`"))),
    };
    c.end()?;
    Ok(SketchStmt { line, kind })
}

fn parse_feature(c: &mut Cur, kind: &str) -> R<Feature> {
    let line = c.no;
    let mut kind = kind.to_string();
    if kind == "pattern" && c.eat_ident("circular") {
        kind = "pattern_circular".into();
    } else if kind == "pattern" {
        c.eat_ident("rectangular");
    }
    let name = c.ident("the feature name")?;
    let mut opts: Vec<(String, Val)> = Vec::new();
    while !c.done() {
        if c.peek() == Some(&Tok::Arrow) {
            // `-> body Name`
            c.i += 1;
            if !c.eat_ident("body") {
                return Err(err(line, "expected `-> body <name>`"));
            }
            opts.push(("body".into(), Val::Expr(c.ident("the body name")?)));
            continue;
        }
        let key = c.ident("an option")?;
        let k = key.as_str();
        let val = if k == "size" {
            // `size a b c`
            let a = c.expr1("length")?;
            let b = c.expr1("width")?;
            let h = c.expr1("height")?;
            Val::List(vec![a, b, h])
        } else if k == "counterbore" || k == "countersink" {
            let a = c.expr1("a diameter")?;
            let b = c.expr1(if k == "counterbore" { "a depth" } else { "an angle" })?;
            Val::List(vec![a, b])
        } else if k == "at" || k == "path" {
            // `at Sketch`, `at Sketch.a, Sketch.b`, `at (x,y,z)`; `path Sketch.a, Sketch.b`
            if k == "at" && c.peek() == Some(&Tok::Punct('(')) {
                Val::Tuples(vec![c.tuple()?])
            } else {
                let mut refs = vec![c.reference()?];
                while c.eat_punct(',') {
                    refs.push(c.reference()?);
                }
                Val::List(refs.iter().map(Ref::text).collect())
            }
        } else if k == "axis" {
            // A world axis `X|Y|Z`, a sketch axis `x|y`, a tuple, or a sketch line `Sketch.l`.
            if c.peek() == Some(&Tok::Punct('(')) { Val::Tuples(vec![c.tuple()?]) } else { Val::Ref(c.reference()?) }
        } else if TUPLE_KEYS.contains(&k) {
            let mut ts = Vec::new();
            while c.peek() == Some(&Tok::Punct('(')) {
                ts.push(c.tuple()?);
            }
            if ts.is_empty() {
                return Err(err(line, format!("`{k}` takes one or more `(x, y[, z])`")));
            }
            Val::Tuples(ts)
        } else if LIST_KEYS.contains(&k) {
            let mut names = vec![c.ident("a name")?];
            while c.eat_punct(',') {
                names.push(c.ident("a name")?);
            }
            Val::List(names)
        } else if IDENT_KEYS.contains(&k) {
            Val::Expr(c.ident(&format!("a value for `{k}`"))?)
        } else if EXPR_KEYS.contains(&k) {
            Val::Expr(c.expr1(&format!("a value for `{k}`"))?)
        } else if FLAGS.contains(&k) {
            Val::Flag
        } else {
            return Err(err(line, format!("unknown option `{k}` for `{kind}`")));
        };
        opts.push((key, val));
    }
    Ok(Feature { kind, name, opts, line })
}
