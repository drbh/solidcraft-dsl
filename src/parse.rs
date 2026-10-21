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
];
const LIST_KEYS: &[&str] = &["targets", "features", "bodies", "tools", "points"];
const IDENT_KEYS: &[&str] = &["from", "body", "axis", "plane", "target", "sketch", "type", "direction", "alignment", "alignment2"];
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
];

const FEATURES: &[&str] = &[
    "extrude", "revolve", "hole", "box", "cylinder", "sphere", "torus", "fillet", "chamfer", "shell", "mirror", "pattern", "combine", "move",
    "remove", "sweep", "loft",
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

pub fn parse(src: &str) -> R<Design> {
    let lines = lex(src)?;
    let mut p = P { lines: &lines, i: 0 };
    let mut d = Design::default();
    while let Some(l) = p.lines.get(p.i) {
        p.i += 1;
        let mut c = Cur::new(l);
        let head = c.ident("a statement")?;
        match head.as_str() {
            "design" => {
                d.name = Some(c.ident("the design name")?);
                if c.eat_ident("units") {
                    d.units = Some(c.ident("a unit")?);
                }
                c.end()?;
            }
            "param" | "let" => {
                let name = c.ident("the parameter name")?;
                let expr = c.rest_after_eq()?;
                d.params.push(Param { name, expr, comment: None, line: l.no, local: head == "let" });
            }
            "sketch" => {
                let s = parse_sketch_head(&mut c, l.no)?;
                let stmts = parse_block(&mut p, l.no, parse_sketch_stmt)?;
                d.items.push(Item::Sketch(Sketch { stmts, ..s }));
            }
            k if FEATURES.contains(&k) => d.items.push(Item::Feature(parse_feature(&mut c, k)?)),
            other => return Err(err(l.no, format!("unknown statement `{other}`"))),
        }
    }
    Ok(d)
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
        } else if k == "at" {
            // `at Sketch`, `at Sketch.a, Sketch.b`, `at (x,y,z)`
            if c.peek() == Some(&Tok::Punct('(')) {
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
