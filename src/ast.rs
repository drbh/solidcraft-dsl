//! The syntax tree of a `.sc` file.

/// A reference to a sketch entity: `outline.3.end`, `a`, `origin`, `rect.bottom_left`.
#[derive(Debug, Clone, PartialEq)]
pub struct Ref {
    pub base: String,
    pub parts: Vec<String>,
}

impl Ref {
    pub fn text(&self) -> String {
        let mut s = self.base.clone();
        for p in &self.parts {
            s.push('.');
            s.push_str(p);
        }
        s
    }
}

/// A sketch point: coordinates, or a reference to an existing point (`p1`, `l2.end`, `c1.center`).
#[derive(Debug, Clone, PartialEq)]
pub enum Pt {
    Xy([Comp; 2]),
    Ref(Ref),
}

/// A coordinate component: a number, or an expression the engine evaluates.
#[derive(Debug, Clone, PartialEq)]
pub enum Comp {
    Num(f64, String),
    Expr(String),
}

#[derive(Debug, Clone)]
pub struct Param {
    /// Pure helper, expanded symbolically into native expressions rather than a UI parameter.
    pub local: bool,
    pub name: String,
    pub expr: String,
    pub comment: Option<String>,
    pub line: usize,
}

#[derive(Debug, Clone)]
pub enum Plane {
    /// `XY`, `XZ`, `YZ` or a construction plane's name.
    Named(String),
    /// `face (x,y,z)`: the planar body face at that point.
    Face(Vec<Comp>),
}

#[derive(Debug, Clone)]
pub struct Sketch {
    pub name: String,
    pub plane: Plane,
    pub offset: Option<String>,
    pub flip: bool,
    /// `explicit`: creators never connect a new point to an existing one at the same place
    /// (`connect: false`); every coincidence is a written constraint. Decompiled files use it.
    pub explicit: bool,
    pub stmts: Vec<SketchStmt>,
    pub line: usize,
}

#[derive(Debug, Clone)]
pub enum CircleSize {
    Radius(String),
    Diameter(String),
}

#[derive(Debug, Clone)]
pub enum RectKind {
    TwoPoint,
    Center,
    Rounded(String),
}

#[derive(Debug, Clone)]
pub struct SketchStmt {
    pub line: usize,
    pub kind: SketchKind,
}

#[derive(Debug, Clone)]
pub enum SketchKind {
    Line {
        name: String,
        pts: Vec<Pt>,
        construction: bool,
        infer: bool,
    },
    Polyline {
        name: String,
        pts: Vec<Pt>,
        closed: bool,
        construction: bool,
        infer: bool,
    },
    Rect {
        name: String,
        kind: RectKind,
        p0: [Comp; 2],
        p1: [Comp; 2],
        construction: bool,
    },
    Circle {
        name: String,
        center: Pt,
        size: CircleSize,
    },
    ArcCenter {
        name: String,
        center: Pt,
        start: Pt,
        end: Pt,
    },
    ArcThree {
        name: String,
        start: Pt,
        through: Pt,
        end: Pt,
    },
    Point {
        name: String,
        p: [Comp; 2],
        /// Explicit persistent placement; ordinary coordinates are only solver seeds.
        bound: bool,
    },
    Polygon {
        name: String,
        center: [Comp; 2],
        radius: String,
        sides: String,
        circumscribed: bool,
    },
    Slot {
        name: String,
        centers: bool,
        p0: [Comp; 2],
        p1: [Comp; 2],
        width: String,
    },
    Spline {
        name: String,
        pts: Vec<Pt>,
        periodic: bool,
    },
    /// `dim [type] a [-> b] = expr`
    Dim {
        kind: Option<String>,
        refs: Vec<Ref>,
        value: String,
    },
    /// `horizontal l`, `coincident a b`, `symmetric a b about y`, `fix a b c`, `midpoint p l`
    Constraint {
        kind: String,
        refs: Vec<Ref>,
        about: Option<Ref>,
    },
    Fillet {
        point: Ref,
        radius: String,
    },
    /// `fix a b …` (points or curves)
    Fix {
        refs: Vec<Ref>,
    },
}

/// A value given to a feature option.
#[derive(Debug, Clone, PartialEq)]
pub enum Val {
    /// An expression: a bare word or number, a quoted string, or a parenthesised group.
    Expr(String),
    /// `(x, y[, z])`, possibly several: `profiles (1,2) (3,4)`.
    Tuples(Vec<Vec<Comp>>),
    /// `A, B, C`
    List(Vec<String>),
    /// A sketch entity reference, `Sketch.a` or `a`.
    Ref(Ref),
    Flag,
}

#[derive(Debug, Clone)]
pub struct Feature {
    /// `extrude`, `revolve`, `hole`, `box`, … (`pattern circular` is `pattern_circular`).
    pub kind: String,
    pub name: String,
    pub opts: Vec<(String, Val)>,
    pub line: usize,
}

#[derive(Debug, Clone)]
pub enum Item {
    Sketch(Sketch),
    Feature(Feature),
}

#[derive(Debug, Clone, Default)]
pub struct Design {
    pub name: Option<String>,
    pub units: Option<String>,
    pub params: Vec<Param>,
    pub items: Vec<Item>,
    pub components: Vec<Component>,
    pub instances: Vec<Instance>,
    pub connectors: Vec<Connector>,
    pub grounds: Vec<(String, usize)>,
    pub joints: Vec<Joint>,
}

#[derive(Debug, Clone)]
pub struct Component {
    pub name: String,
    pub design: Design,
    pub line: usize,
}

#[derive(Debug, Clone)]
pub struct Instance {
    pub name: String,
    pub component: String,
    pub at: Vec<Comp>,
    pub axis: Vec<Comp>,
    pub angle: String,
    pub line: usize,
}

/// A component-local frame, independent of transient face and edge numbering.
#[derive(Debug, Clone)]
pub struct Connector {
    pub name: String,
    pub at: Vec<Comp>,
    pub z: Vec<Comp>,
    pub x: Vec<Comp>,
    pub line: usize,
}

#[derive(Debug, Clone)]
pub struct Joint {
    pub name: String,
    pub kind: String,
    pub a: Ref,
    pub b: Ref,
    pub values: Vec<Comp>,
    pub limits: Vec<Vec<Comp>>,
    pub offset: String,
    pub angle: String,
    pub flip: bool,
    pub line: usize,
}
