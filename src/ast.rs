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
    /// `ellipse e = (cx,cy) major (x,y) minor r`: `major` is the end of the major axis.
    Ellipse {
        name: String,
        center: [Comp; 2],
        major: [Comp; 2],
        minor: String,
    },
    /// `text "LABEL" at (x,y) [height h] [angle a] [align left|center|right] [font "F"] [bold] [italic]`.
    /// The engine numbers glyph curves from the same counters as everything else, so text
    /// comes last in its sketch.
    Text {
        text: String,
        at: [Comp; 2],
        height: Option<String>,
        angle: Option<String>,
        align: Option<String>,
        font: Option<String>,
        bold: bool,
        italic: bool,
    },
    /// `offset a b distance d [side (x,y)] [symmetric]`: an offset copy of whole entities or of
    /// curves, kept at the distance (a live dimension) as they change.
    Offset {
        refs: Vec<Ref>,
        distance: String,
        side: Option<[Comp; 2]>,
        symmetric: bool,
    },
    /// `mirror a b about l`: copies held symmetric to the originals about line `l`.
    Mirror {
        refs: Vec<Ref>,
        about: Ref,
    },
    /// `pattern circular a count n [center (x,y) | center p] [angle a]`, or
    /// `pattern rectangular a count n spacing s [dir (x,y)] [count2 m spacing2 t [dir2 (x,y)]]`.
    /// Counts include the original.
    Pattern {
        refs: Vec<Ref>,
        circular: bool,
        count: String,
        /// Coordinates make a free centre point; name a fixed point to hold the pattern still.
        center: Option<Pt>,
        angle: Option<String>,
        spacing: Option<String>,
        dir: Option<[Comp; 2]>,
        count2: Option<String>,
        spacing2: Option<String>,
        dir2: Option<[Comp; 2]>,
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
    pub checks: Vec<Check>,
}

/// `check Body quantity [Other] op value [unit] [up axis]`: a property of a built body that
/// `evaluate` verifies, e.g. `check Cap clearance Bottle >= 0.2 mm`.
#[derive(Debug, Clone)]
pub struct Check {
    pub body: String,
    /// shells, volume, area, mass, size.x|y|z, clearance or overhang
    pub quantity: String,
    /// The second body of a clearance check.
    pub other: Option<String>,
    pub op: String,
    pub value: String,
    /// A unit the expression language has no word for: mm2, mm3, cm3, g or kg.
    pub unit: Option<String>,
    /// The print direction of an overhang check.
    pub up: Vec<Comp>,
    /// The longest bridge an overhang check accepts.
    pub bridge: Option<String>,
    pub line: usize,
}

#[derive(Debug, Clone)]
pub struct Component {
    pub name: String,
    pub design: Design,
    pub line: usize,
    /// `component Name from "part.sc"`: the part file whose design this component links to,
    /// as written (relative to the file that names it). Its block holds connectors only.
    pub source: Option<String>,
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
