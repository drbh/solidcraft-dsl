# How `.sc` compares to other code-CAD tools

A `.sc` file is a declarative feature list. It compiles to a SolidCraft command script, so the
result is a native, parametric B-rep document you can keep editing in the app. Most code-CAD
tools either produce a mesh, or produce geometry that loses its history once the script ends.
This page compares `.sc` with five common alternatives. It uses
[examples/03_bolt_plate.sc](../examples/03_bolt_plate.sc) as the running example.

## At a glance

| | `.sc` | OpenSCAD | CadQuery | build123d | Onshape FeatureScript | Zoo KCL |
| --- | --- | --- | --- | --- | --- | --- |
| Language | Own declarative DSL | Own functional language | Python library | Python library | Own language, inside Onshape | Own language |
| Kernel | SolidCraft (B-rep) | CGAL / Manifold (mesh) | OpenCascade (B-rep) | OpenCascade (B-rep) | Parasolid (B-rep) | Zoo's cloud kernel (B-rep) |
| Constraint-solved sketches | Yes: dims, constraints, `bind point` | No | No (a sketch solver exists but is experimental) | No, sketches are placed by construction | Yes, through Onshape sketches | Partial |
| Parameters stay live after build | Yes, as native engine formulas | No, re-run the script | No, re-run the script | No, re-run the script | Yes, as feature parameters | Yes, in the Zoo app |
| Units checked | Yes (`mm`, `in`, `deg`…) | No | No | No | Yes | Yes |
| Loops, conditionals, functions | No; use `pattern`, `mirror` and expressions | Yes | Full Python | Full Python | Yes | Yes |
| Fillet, chamfer, shell, threads | Native features | Workarounds (`minkowski`, `hull`, libraries) | Yes | Yes | Yes | Yes |
| Face/edge selection | By a point on the geometry | Not applicable | String selectors (`">Z"`) | Selectors, sort and filter | Queries | Tags |
| Assemblies and joints | `component`, `connector`, `joint`, `ground` | Placed copies only | `Assembly` with constraints | `Joint` objects | Onshape assemblies | Limited |
| GUI escape hatch | Open and edit the native design | None | CQ-editor viewer | Viewers (ocp_vscode) | Onshape is the GUI | Zoo Design Studio |
| Runs offline, files on disk | Yes | Yes | Yes | Yes | No, cloud only | Partly, the kernel is a cloud service |
| Typical output | SolidCraft design, STEP, STL | STL, 3MF | STEP, STL | STEP, STL | Onshape document, STEP | STEP, STL |

## The bolt plate in each tool

### `.sc`

```sc
design BoltPlate units mm
let hole_x = L - inset
let hole_y = W - inset
param L = 80 mm
param W = 50 mm
param T = 6 mm
param inset = 10 mm
param hole_d = 5 mm
box Plate size L W T -> body PlateBody
sketch Locations on XY offset T {
  point h = (hole_x, hole_y)
  dim horizontal origin -> h = hole_x
  dim vertical origin -> h = hole_y
}
hole FirstHole at Locations diameter hole_d depth T
pattern Holes features FirstHole dir (-1,0,0) count (1+1) spacing (L-2*inset) dir2 (0,-1,0) count2 2 spacing2 (W-2*inset)
```

The output has one real `hole` feature and a rectangular pattern. If you change `L` in the app,
the plate, the sketch dimension and the pattern spacing all update.

### OpenSCAD

The full port is in [examples/03_bolt_plate.scad](../examples/03_bolt_plate.scad).

```openscad
difference() {
  cube([L, W, T]);
  for (i = [0 : 1], j = [0 : 1])
    translate([-i * (L - 2 * inset), -j * (W - 2 * inset), 0]) first_hole();
}
```

This is CSG on a mesh. The holes are 64-sided cylinders (`$fn`), and the cut has to overshoot
the plate faces by an epsilon. The output has no notion of a hole or a pattern.

### CadQuery

```python
import cadquery as cq

L, W, T, inset, hole_d = 80, 50, 6, 10, 5
plate = (
    cq.Workplane("XY")
    .box(L, W, T, centered=False)
    .faces(">Z").workplane()
    .pushPoints([(x, y) for x in (inset, L - inset) for y in (inset, W - inset)])
    .hole(hole_d, T)
)
```

The geometry is exact B-rep and exports to STEP. The faces are chosen with string selectors
(`">Z"`). Parameters are Python variables, so the STEP file is a frozen result.

### build123d

```python
from build123d import *

L, W, T, inset, hole_d = 80, 50, 6, 10, 5
with BuildPart() as plate:
    Box(L, W, T, align=Align.MIN)
    with Locations((L / 2, W / 2, T)):
        with GridLocations(L - 2 * inset, W - 2 * inset, 2, 2):
            Hole(radius=hole_d / 2, depth=T)
```

This uses the same kernel as CadQuery, through a more Pythonic context-manager API.
`GridLocations` plays the role of `pattern`. As with CadQuery, the parameters disappear once you
export.

### Onshape FeatureScript and Zoo KCL

FeatureScript is how Onshape's own features are written. It is the closest match to `.sc` in
spirit: it builds a feature history over a parametric B-rep with constraint-solved sketches. But
it is verbose, tied to Onshape's cloud, and meant for writing new features more than whole
parts. KCL (Zoo) is a text language with a B-rep kernel and its own design app, but geometry is
computed by Zoo's service. Their syntax changes often enough that snippets are not reproduced
here; see their docs.

## When to pick which

- **`.sc`**: mechanical parts and mechanisms you want to stay parametric after the build,
  with solved sketches, real fillets and holes, STEP output, assemblies with joints, plain files
  for git and LLM agents, and the option to fix something by hand in the app.
- **OpenSCAD**: quick printable parts, generative or recursive shapes, a large community library
  (BOSL2), and nothing to install beyond one binary. Expect mesh-only output.
- **CadQuery / build123d**: when you need a full programming language (loops, data files,
  optimisation, generating many variants) and exact STEP output, and don't need the result to
  stay editable outside Python.
- **FeatureScript**: you already work in Onshape and want custom features or configurable parts
  there.
- **KCL**: you want a text-first language with its own GUI and are fine with a cloud kernel.

## What `.sc` gives up

- **Control flow.** There is no `for`, `if` or user-defined function. Repetition goes through
  `pattern`, `pattern_circular`, `mirror` and components. Some inputs, such as pattern counts,
  selection points and direction vectors, are evaluated at compile time and are not live (see
  [expressions.md](expressions.md)).
- **One engine.** Output depends on SolidCraft. The pinned engine version matters
  (see [expressions.md](expressions.md)).
- **Ecosystem.** There are no third-party part libraries yet. OpenSCAD and CadQuery/build123d
  have years of them.
- **Stability.** The language is experimental and may change; see the README.
