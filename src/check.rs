//! Static checks that run after parsing and before compiling: every name refers to the right
//! kind of thing, every expression has the unit class its slot expects, and literal values are
//! in range. The engine would refuse all of these too, but late, per command and without a
//! line number; here they come back as `line N: …` before anything runs.
//!
//! The checks on sketch entities (a `tangent` between two curves, a `midpoint` of a point and a
//! line) live in the compiler, next to the name resolution that tells the kinds apart.

use std::collections::HashMap;

use crate::ast::*;
use crate::units::{Class, classify, literal};

type R<T> = Result<T, String>;

fn err(line: usize, msg: impl std::fmt::Display) -> String {
    format!("line {line}: {msg}")
}

/// Feature options that take a length.
const LENGTH_KEYS: &[&str] = &[
    "distance",
    "distance2",
    "offset",
    "diameter",
    "radius",
    "depth",
    "height",
    "width",
    "length",
    "major",
    "minor",
    "thickness",
    "spacing",
    "spacing2",
];
/// Feature options that take an angle.
const ANGLE_KEYS: &[&str] = &["angle", "taper"];
/// Feature options that take a whole number, with their minimum.
const COUNT_KEYS: &[(&str, f64)] = &[("count", 1.0), ("count2", 1.0), ("sides", 3.0)];
/// Options whose literal value must be positive.
const POSITIVE_KEYS: &[&str] = &["diameter", "radius", "depth", "height", "width", "length", "major", "minor", "thickness", "spacing", "spacing2"];
/// Features whose `body` option names the body they create (`-> body NAME`), not an existing one.
const CREATORS: &[&str] = &["extrude", "revolve", "box", "cylinder", "sphere", "torus"];

/// What a top-level name denotes, for error messages when the wrong kind is named.
#[derive(Debug, Clone, Copy, PartialEq)]
enum Named {
    Param,
    Sketch,
    Feature,
    Body,
}

struct Checker {
    params: HashMap<String, Class>,
    names: HashMap<String, Named>,
    bodies: Vec<String>,
}

impl Checker {
    /// `expr` must fit `want`; `what` names the slot in errors.
    fn expect(&self, line: usize, what: &str, expr: &str, want: &Class) -> R<Class> {
        let c = classify(expr, &self.params).map_err(|e| err(line, format!("{what}: {e}")))?;
        if !c.fits(want) {
            return Err(err(line, format!("{what} needs {}, `{expr}` is {}", want.describe(), c.describe())));
        }
        Ok(c)
    }

    /// `expr` is a length or an angle (an `auto` dimension).
    fn expect_measure(&self, line: usize, what: &str, expr: &str) -> R<()> {
        let c = classify(expr, &self.params).map_err(|e| err(line, format!("{what}: {e}")))?;
        if !(c.fits(&Class::Length) || c.fits(&Class::Angle)) {
            return Err(err(line, format!("{what} needs a length or an angle, `{expr}` is {}", c.describe())));
        }
        Ok(())
    }

    fn positive(line: usize, what: &str, expr: &str) -> R<()> {
        if let Some(n) = literal(expr)
            && !(n > 0.0 && n.is_finite())
        {
            return Err(err(line, format!("{what} must be positive, not {expr}")));
        }
        Ok(())
    }

    fn count(line: usize, what: &str, expr: &str, min: f64) -> R<()> {
        if let Some(n) = literal(expr)
            && !(n.is_finite() && n.fract() == 0.0 && n >= min)
        {
            return Err(err(line, format!("{what} must be a whole number of at least {min}, not {expr}")));
        }
        Ok(())
    }

    fn sketch(&self, s: &Sketch) -> R<()> {
        let line = s.line;
        if let Some(o) = &s.offset {
            self.expect(line, "`offset`", o, &Class::Length)?;
        }
        for st in &s.stmts {
            let line = st.line;
            let len = |what: &str, e: &str| -> R<()> {
                self.expect(line, what, e, &Class::Length)?;
                Self::positive(line, what, e)
            };
            match &st.kind {
                SketchKind::Circle { size, .. } => match size {
                    CircleSize::Radius(e) => len("`radius`", e)?,
                    CircleSize::Diameter(e) => len("`diameter`", e)?,
                },
                SketchKind::Rect { kind: RectKind::Rounded(r), .. } => len("`radius`", r)?,
                SketchKind::Polygon { radius, .. } => len("`radius`", radius)?,
                SketchKind::Slot { width, .. } => len("`width`", width)?,
                SketchKind::Fillet { radius, .. } => len("`radius`", radius)?,
                SketchKind::Dim { kind, value, .. } => match kind.as_deref() {
                    Some("angle") => {
                        self.expect(line, "an angle dimension", value, &Class::Angle)?;
                    }
                    Some("radius" | "diameter" | "length" | "distance" | "horizontal" | "vertical") => {
                        let k = kind.as_deref().unwrap_or("distance");
                        self.expect(line, &format!("a {k} dimension"), value, &Class::Length)?;
                        if matches!(k, "radius" | "diameter" | "length") {
                            Self::positive(line, &format!("a {k} dimension"), value)?;
                        }
                    }
                    _ => self.expect_measure(line, "a dimension", value)?,
                },
                _ => {}
            }
        }
        Ok(())
    }

    /// A name given where a body is expected.
    fn body_ref(&self, line: usize, key: &str, name: &str) -> R<()> {
        if self.bodies.iter().any(|b| b == name) || is_default_body(name) {
            return Ok(());
        }
        let hint = match self.names.get(name) {
            Some(Named::Feature) => {
                format!("`{name}` is a feature, not a body: name the body it creates (`-> body {name}` on that feature)")
            }
            Some(Named::Sketch) => format!("`{name}` is a sketch, not a body"),
            Some(Named::Param) => format!("`{name}` is a parameter, not a body"),
            _ if self.bodies.is_empty() => {
                format!("unknown body `{name}`: no feature names a body yet (`-> body {name}`), and the engine's own names are `Body1`, `Body2`, …")
            }
            _ => format!("unknown body `{name}`; bodies so far: {}", self.bodies.join(", ")),
        };
        Err(err(line, format!("`{key}`: {hint}")))
    }

    /// A name given where an earlier feature is expected.
    fn feature_ref(&self, line: usize, key: &str, name: &str, this: &str) -> R<()> {
        match self.names.get(name) {
            Some(Named::Feature) => Ok(()),
            Some(Named::Body) => Err(err(line, format!("`{key}`: `{name}` is a body, not a feature; use `bodies {name}`"))),
            Some(Named::Sketch) => Err(err(line, format!("`{key}`: `{name}` is a sketch, not a feature"))),
            Some(Named::Param) => Err(err(line, format!("`{key}`: `{name}` is a parameter, not a feature"))),
            None if name == this => Err(err(line, format!("`{key}`: `{name}` is this feature itself"))),
            None => Err(err(line, format!("`{key}`: unknown feature `{name}` (features are patterned after they are defined)"))),
        }
    }

    fn feature(&mut self, f: &Feature) -> R<()> {
        let line = f.line;
        for (k, v) in &f.opts {
            match v {
                Val::Expr(e) => {
                    let key = k.as_str();
                    if LENGTH_KEYS.contains(&key) {
                        self.expect(line, &format!("`{key}`"), e, &Class::Length)?;
                        if POSITIVE_KEYS.contains(&key) {
                            Self::positive(line, &format!("`{key}`"), e)?;
                        }
                    } else if ANGLE_KEYS.contains(&key) {
                        self.expect(line, &format!("`{key}`"), e, &Class::Angle)?;
                    } else if let Some((_, min)) = COUNT_KEYS.iter().find(|(c, _)| *c == key) {
                        self.expect(line, &format!("`{key}`"), e, &Class::Number)?;
                        Self::count(line, &format!("`{key}`"), e, *min)?;
                    } else if key == "target" || (key == "body" && !CREATORS.contains(&f.kind.as_str())) {
                        self.body_ref(line, key, e)?;
                    }
                }
                Val::List(items) => match k.as_str() {
                    "features" => {
                        for n in items {
                            self.feature_ref(line, k, n, &f.name)?;
                        }
                    }
                    "bodies" | "targets" | "tools" => {
                        for n in items {
                            self.body_ref(line, k, n)?;
                        }
                    }
                    "size" => {
                        for e in items {
                            self.expect(line, "`size`", e, &Class::Length)?;
                            Self::positive(line, "`size`", e)?;
                        }
                    }
                    "counterbore" | "countersink" => {
                        if let Some(d) = items.first() {
                            self.expect(line, &format!("`{k}` diameter"), d, &Class::Length)?;
                            Self::positive(line, &format!("`{k}` diameter"), d)?;
                        }
                        if let Some(second) = items.get(1) {
                            if k == "counterbore" {
                                self.expect(line, "`counterbore` depth", second, &Class::Length)?;
                            } else {
                                self.expect(line, "`countersink` angle", second, &Class::Angle)?;
                            }
                        }
                    }
                    _ => {}
                },
                Val::Tuples(ts) => {
                    for t in ts {
                        for c in t {
                            if let Comp::Expr(e) = c {
                                // Positions are lengths; directions are plain numbers, and both fit.
                                self.expect(line, &format!("`{k}`"), e, &Class::Length)?;
                            }
                        }
                    }
                }
                Val::Ref(_) | Val::Flag => {}
            }
        }
        // The body a creator names.
        if CREATORS.contains(&f.kind.as_str())
            && let Some((_, Val::Expr(b))) = f.opts.iter().find(|(k, _)| k == "body")
        {
            if self.bodies.iter().any(|x| x == b) {
                return Err(err(line, format!("body `{b}` is already created by an earlier feature")));
            }
            if let Some(kind) = self.names.get(b)
                && *kind != Named::Feature
            {
                return Err(err(line, format!("`{b}` is already the name of a {}", describe(*kind))));
            }
            self.bodies.push(b.clone());
            self.names.entry(b.clone()).or_insert(Named::Body);
        }
        Ok(())
    }
}

fn describe(n: Named) -> &'static str {
    match n {
        Named::Param => "parameter",
        Named::Sketch => "sketch",
        Named::Feature => "feature",
        Named::Body => "body",
    }
}

/// `Body1`, `Body2`, …: the engine's name for a body no feature named.
fn is_default_body(name: &str) -> bool {
    name.strip_prefix("Body").is_some_and(|n| !n.is_empty() && n.bytes().all(|b| b.is_ascii_digit()))
}

/// Check a parsed design. Duplicate names are left to the compiler, which reports them with
/// both lines.
pub fn check(d: &Design) -> R<()> {
    let mut c = Checker { params: HashMap::new(), names: HashMap::new(), bodies: Vec::new() };
    for p in &d.params {
        let class = classify(&p.expr, &c.params).map_err(|e| err(p.line, format!("parameter `{}`: {e}", p.name)))?;
        c.params.insert(p.name.clone(), class);
        c.names.insert(p.name.clone(), Named::Param);
    }
    for item in &d.items {
        match item {
            Item::Sketch(s) => {
                c.sketch(s)?;
                c.names.insert(s.name.clone(), Named::Sketch);
            }
            Item::Feature(f) => {
                c.feature(f)?;
                c.names.insert(f.name.clone(), Named::Feature);
            }
        }
    }
    Ok(())
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    use crate::parse::parse;

    fn fails(src: &str) -> String {
        check(&parse(src).unwrap()).unwrap_err()
    }
    fn ok(src: &str) {
        check(&parse(src).unwrap()).unwrap();
    }

    #[test]
    fn examples_pass() {
        ok(include_str!("../examples/bracket.sc"));
        ok(include_str!("../examples/plate.sc"));
    }

    #[test]
    fn units() {
        let e = fails("param a = 30 deg\nsketch S on XY {\ncircle c = (0,0) radius 5\n}\nextrude E from S distance a");
        assert!(e.contains("line 5") && e.contains("`distance` needs a length, `a` is an angle"), "{e}");
        let e = fails("param L = 10 mm\nsketch S on XY {\ncircle c = (0,0) radius 5\n}\nextrude E from S distance 10 taper L");
        assert!(e.contains("`taper` needs an angle"), "{e}");
        let e = fails("param L = 10 mm\nsketch S on XY {\ncircle c = (0,0) radius (L*L)\n}");
        assert!(e.contains("`radius` needs a length") && e.contains("is an area"), "{e}");
        let e = fails("param L = 10 mm\nparam A = 2 deg\nparam B = L + A");
        assert!(e.contains("parameter `B`: `+` adds a length to an angle"), "{e}");
        let e = fails("sketch S on XY {\n circle c = (0,0) radius 5\n dim angle c -> c = 5 mm\n}");
        assert!(e.contains("line 3") && e.contains("an angle dimension needs an angle"), "{e}");
        ok(
            "param L = 10 mm\nparam n = 2\nsketch S on XY {\ncircle c = (0,0) radius (L/n)\n}\nextrude E from S distance (L - 2) symmetric taper 3 deg",
        );
    }

    #[test]
    fn unknown_parameter() {
        let e = fails("sketch S on XY {\ncircle c = (0,0) radius 5\n}\nextrude E from S distance thick");
        assert!(e.contains("unknown parameter `thick`"), "{e}");
    }

    #[test]
    fn ranges() {
        let e =
            fails("sketch S on XY {\ncircle c = (0,0) radius 5\n}\nextrude E from S distance 5\npattern P features E dir (1,0,0) count 0 spacing 5");
        assert!(e.contains("`count` must be a whole number of at least 1"), "{e}");
        let e = fails("cylinder C radius (-3) height 5");
        assert!(e.contains("`radius` must be positive"), "{e}");
    }

    #[test]
    fn references() {
        let e =
            fails("sketch S on XY {\ncircle c = (0,0) radius 5\n}\nextrude E from S distance 5\npattern P features S dir (1,0,0) count 2 spacing 5");
        assert!(e.contains("`S` is a sketch, not a feature"), "{e}");
        let e =
            fails("sketch S on XY {\ncircle c = (0,0) radius 5\n}\nextrude E from S distance 5\npattern P features Q dir (1,0,0) count 2 spacing 5");
        assert!(e.contains("unknown feature `Q`"), "{e}");
        let e = fails("sketch S on XY {\ncircle c = (0,0) radius 5\n}\nextrude E from S distance 5\nfillet F edges (5,0,2) radius 1 body E");
        assert!(e.contains("`E` is a feature, not a body") && e.contains("-> body E"), "{e}");
        let e = fails("box A size 10 10 10 -> body A\nbox B size 10 10 10 -> body A");
        assert!(e.contains("body `A` is already created"), "{e}");
        let e = fails("box A size 10 10 10 -> body Blk\nremove R bodies Blk, Other");
        assert!(e.contains("unknown body `Other`; bodies so far: Blk"), "{e}");
        ok("box A size 10 10 10 -> body Blk\ncylinder C radius 2 height 20 cut targets Blk\nremove R bodies Body2");
        ok("box A size 10 10 10\nbox B size 5 5 5\ncombine J target Body1 tools Body2");
    }
}
