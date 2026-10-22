# solidcraft-expr

CAD-independent scalar expressions, dimensions, unit conversion and parameter dependency graphs.
Extracted from `crates/doc/src/expr.rs` in
[SolidCraft](https://github.com/tangency-labs/solidcraft), source revision
`926a7353a377e342d2718971fb259fae517d1ddd`, under MIT OR Apache-2.0.
The original license texts accompany this package.

The extraction replaces document-specific errors with `Error` and adds inferred-dimension
parameter evaluation (`ParamDef.unit = "auto"`) for DSL helpers.

This package is vendored in the DSL as `crates/solidcraft-expr` and in the earlier companion
engine checkout as `crates/expr`. Upstream 0.19.0 has not adopted this extraction: it retains
its own expression module, including newer leading-decimal-comma syntax. The current assembly
compatibility patch leaves that module intact. Run the DSL engine integration suite when
updating either evaluator; do not assume the stock engine links this crate.
