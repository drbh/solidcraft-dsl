# solidcraft-expr

CAD-independent scalar expressions, dimensions, unit conversion and parameter dependency graphs.
Extracted from `crates/doc/src/expr.rs` in
[SolidCraft](https://github.com/bherbruck/solidcraft), source revision
`926a7353a377e342d2718971fb259fae517d1ddd`, under MIT OR Apache-2.0.
The original license texts accompany this package.

The extraction replaces document-specific errors with `Error` and adds inferred-dimension
parameter evaluation (`ParamDef.unit = "auto"`) for DSL helpers. The engine retains its
public document error adapters. Arithmetic and parser behavior are otherwise shared.

This package is vendored in the DSL as `crates/solidcraft-expr` and in the engine as
`crates/expr` until it can be published as a versioned dependency. Keep their source identical;
run `cargo test -p solidcraft-expr` in both repositories after edits.
