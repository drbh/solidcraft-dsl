//! `solidcraft-dsl`: a declarative text format for SolidCraft designs, compiled to the command
//! script that `solidcraft-cli run` and the MCP `batch` tool execute. See README.md.
#![deny(clippy::unwrap_used, clippy::expect_used, clippy::panic, clippy::unimplemented, clippy::todo, clippy::unreachable)]
#![forbid(unsafe_code)]

/// The file extension of a design file, without the dot. Changing it: edit this constant, the
/// editor files (`editors/vscode/package.json`, `editors/vim/ftdetect/solidcraft.vim`), rename
/// `examples/*` and the `include_str!` paths in tests; `extension_is_consistent` lists what it
/// still finds. (`.sc` is shared with Scala and SuperCollider; expect it to change.)
pub const FILE_EXTENSION: &str = "sc";

pub mod artifacts;
pub mod assembly;
pub mod ast;
pub mod check;
pub mod checks;
pub mod compile;
pub mod control;
pub mod decompile;
pub mod evaluate;
pub mod expressions;
mod glb;
pub mod layout;
pub mod lex;
pub mod link;
pub mod native;
pub mod parse;
pub mod reuse;
pub mod units;
pub mod watch;

pub use check::check;
pub use compile::{Compiled, compile};
pub use decompile::decompile as decompile_readable;
/// Lossless source for a resolved native JSON document. For packaged assets use `native::Native::read`.
pub fn decompile(design: &serde_json::Value) -> Result<String, String> {
    native::Native::from_document(design.clone())?.to_source()
}
pub use parse::parse;

/// Build the design in the file at `path` (whose text is `src`): like `build`, after expanding
/// the parts it `use`s, with lines and errors mapped back to the original files.
pub fn build_file(src: &str, path: &std::path::Path) -> Result<Compiled, String> {
    if native::is_source(src) || !src.lines().any(|l| l.trim_start().starts_with("use ")) {
        return build(src);
    }
    let x = reuse::expand(src, path)?;
    let mut c = build(&x.text).map_err(|e| x.error(e))?;
    let line = |n: usize| x.origin.get(n.wrapping_sub(1)).map(reuse::Origin::line).unwrap_or(n);
    for n in c.lines.iter_mut() {
        *n = line(*n);
    }
    for n in c.dims.iter_mut().flatten() {
        *n = line(*n);
    }
    for k in c.checks.iter_mut() {
        k.line = line(k.line);
    }
    for site in c.expressions["sites"].as_array_mut().into_iter().flatten() {
        if let Some(n) = site["line"].as_u64() {
            site["line"] = serde_json::json!(line(n as usize));
        }
    }
    Ok(c)
}

/// Parse, check and compile a `.sc` source.
pub fn build(src: &str) -> Result<Compiled, String> {
    if native::is_source(src) {
        return native::Native::from_source(src).map(native::Native::compiled);
    }
    let d = parse(src)?;
    check(&d)?;
    let mut c = compile(&d)?;
    c.checks = checks::lower(&d)?;
    c.print_layout = checks::print_layout(&d)?;
    Ok(c)
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::FILE_EXTENSION;

    /// Every place that names the extension agrees with `FILE_EXTENSION`.
    #[test]
    fn extension_is_consistent() {
        let vscode = include_str!("../editors/vscode/package.json");
        assert!(vscode.contains(&format!("\".{FILE_EXTENSION}\"")), "editors/vscode/package.json does not list .{FILE_EXTENSION}");
        let vim = include_str!("../editors/vim/ftdetect/solidcraft.vim");
        assert!(vim.contains(&format!("*.{FILE_EXTENSION} ")), "editors/vim/ftdetect/solidcraft.vim does not detect *.{FILE_EXTENSION}");
        let readme = include_str!("../README.md");
        assert!(readme.contains(&format!("`.{FILE_EXTENSION}`")), "README.md does not name .{FILE_EXTENSION}");
        for name in std::fs::read_dir(concat!(env!("CARGO_MANIFEST_DIR"), "/examples")).unwrap().flatten() {
            let n = name.file_name();
            let n = n.to_string_lossy();
            // Directories hold an example's companion code (firmware, simulation).
            if name.path().is_dir() || n.ends_with(".print.json") || n.ends_with(".checks.json") || n.ends_with(".scad") {
                continue;
            }
            assert!(n.ends_with(&format!(".{FILE_EXTENSION}")), "examples/{n} does not end in .{FILE_EXTENSION}");
        }
    }
}
