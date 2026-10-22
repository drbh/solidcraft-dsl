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
pub mod compile;
pub mod control;
pub mod decompile;
pub mod evaluate;
pub mod expressions;
mod glb;
pub mod layout;
pub mod lex;
pub mod native;
pub mod parse;
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

/// Parse, check and compile a `.sc` source.
pub fn build(src: &str) -> Result<Compiled, String> {
    if native::is_source(src) {
        return native::Native::from_source(src).map(native::Native::compiled);
    }
    let d = parse(src)?;
    check(&d)?;
    compile(&d)
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
            if n.ends_with(".print.json") || n.ends_with(".checks.json") {
                continue;
            }
            assert!(n.ends_with(&format!(".{FILE_EXTENSION}")), "examples/{n} does not end in .{FILE_EXTENSION}");
        }
    }
}
