//! Linked parts. `component Wheel from "wheel.sc"` makes `Wheel` a component linked to the part's
//! own design, as SolidCraft's Insert Design does: the compiler emits `component.insert_design`
//! naming the part's source, and `prepare` builds each part (and the parts it links in turn) into
//! a `.solidcraft` design and points the command at it. The part keeps its own timeline and
//! parameters; the assembly holds its bodies and can refresh them with `component.update_links`.
use std::collections::HashMap;
use std::path::{Path, PathBuf};

use serde_json::{Value, json};
use sha2::{Digest, Sha256};

use crate::compile::Link;

/// Read the drawings a design imports (`svg "logo.svg"`) relative to its file, and put their
/// text in the commands, so the script needs no files beside it.
pub fn inline_files(script: &mut Value, source: &Path) -> Result<(), String> {
    let dir = source.parent().unwrap_or(Path::new("."));
    for c in script["commands"].as_array_mut().into_iter().flatten() {
        if !matches!(c["command"].as_str(), Some("sketch.insert_svg" | "sketch.insert_dxf")) {
            continue;
        }
        let Some(path) = c["params"]["path"].as_str().map(str::to_string) else { continue };
        let file = dir.join(&path);
        let text = std::fs::read_to_string(&file).map_err(|e| format!("{}: {e}", file.display()))?;
        let params = c["params"].as_object_mut().ok_or("command params must be an object")?;
        params.remove("path");
        params.insert("text".into(), Value::String(text));
    }
    Ok(())
}

/// Where built part designs go.
#[derive(Clone, Copy, PartialEq)]
pub enum Naming {
    /// `<stem>-<hash>.solidcraft`, reused while the part's source (and its parts) are unchanged.
    Cache,
    /// `<stem>.solidcraft` beside a packed assembly, rebuilt every time, so the saved assembly's
    /// links point at stable, human-named files.
    Beside,
}

/// A part built for an assembly.
#[derive(Debug, Clone)]
pub struct Part {
    pub source: PathBuf,
    pub design: PathBuf,
    /// Hash of the part's source and, recursively, of the parts it links.
    pub sha256: String,
}

pub struct Parts<'a> {
    cli: &'a str,
    dir: PathBuf,
    naming: Naming,
    stack: Vec<PathBuf>,
    built: HashMap<PathBuf, Part>,
    names: HashMap<String, PathBuf>,
}

impl<'a> Parts<'a> {
    pub fn new(cli: &'a str, dir: &Path, naming: Naming) -> Self {
        Parts { cli, dir: dir.into(), naming, stack: vec![], built: HashMap::new(), names: HashMap::new() }
    }

    /// Every part built so far, in the order first needed.
    pub fn parts(&self) -> Vec<Part> {
        let mut v: Vec<Part> = self.built.values().cloned().collect();
        v.sort_by(|a, b| a.source.cmp(&b.source));
        v
    }

    /// Build the parts `links` name (relative to `source`, the file that names them) and point
    /// their commands in `script` at the built designs. Errors carry the linking line.
    pub fn prepare(&mut self, script: &mut Value, links: &[Link], source: &Path) -> Result<Vec<Part>, String> {
        if self.stack.is_empty() {
            // The assembly itself takes part in cycle detection.
            let own = std::fs::canonicalize(source).map_err(|e| format!("{}: {e}", source.display()))?;
            self.stack.push(own);
            let result = self.prepare(script, links, source);
            self.stack.clear();
            return result;
        }
        let base = source.parent().unwrap_or(Path::new("."));
        let mut out = vec![];
        for link in links {
            let path = base.join(&link.source);
            let part = self.build(&path).map_err(|e| format!("line {}: {}: {e}", link.line, link.source))?;
            let command = script["commands"].get_mut(link.command).ok_or("linked part command out of range")?;
            if command["command"] != "component.insert_design" {
                return Err("linked part command mismatch".into());
            }
            command["params"]["path"] = json!(part.design);
            out.push(part);
        }
        Ok(out)
    }

    fn build(&mut self, path: &Path) -> Result<Part, String> {
        let path = std::fs::canonicalize(path).map_err(|e| format!("cannot read part file: {e}"))?;
        if let Some(part) = self.built.get(&path) {
            return Ok(part.clone());
        }
        if self.stack.contains(&path) {
            let name = |p: &PathBuf| p.file_name().map_or_else(|| p.display().to_string(), |n| n.to_string_lossy().into_owned());
            let chain: Vec<String> = self.stack.iter().chain([&path]).map(name).collect();
            return Err(format!("parts link each other in a cycle: {}", chain.join(" -> ")));
        }
        if self.stack.len() >= 32 {
            return Err("parts are linked more than 32 levels deep".into());
        }
        let src = std::fs::read_to_string(&path).map_err(|e| format!("{}: {e}", path.display()))?;
        let mut c = crate::build_file(&src, &path)?;
        self.stack.push(path.clone());
        let nested = self.prepare(&mut c.script, &c.links.clone(), &path);
        self.stack.pop();
        let nested = nested?;
        let mut hash = Sha256::new();
        hash.update(src.as_bytes());
        for part in &nested {
            hash.update(part.sha256.as_bytes());
        }
        hash.update(self.cli.as_bytes());
        let sha256 = format!("{:x}", hash.finalize());
        let stem = path.file_stem().and_then(|s| s.to_str()).unwrap_or("part").to_string();
        let file = match self.naming {
            Naming::Beside if self.names.get(&stem).is_none_or(|p| *p == path) => format!("{stem}.solidcraft"),
            // Two parts with one file name (in different folders) keep apart by hash.
            _ => format!("{stem}-{}.solidcraft", &sha256[..12]),
        };
        self.names.insert(stem.clone(), path.clone());
        std::fs::create_dir_all(&self.dir).map_err(|e| format!("{}: {e}", self.dir.display()))?;
        let design = std::path::absolute(self.dir.join(&file)).map_err(|e| e.to_string())?;
        if self.naming == Naming::Beside || !design.exists() {
            self.write(&c, &design, &stem, &sha256)?;
        }
        let part = Part { source: path.clone(), design, sha256 };
        self.built.insert(path, part.clone());
        Ok(part)
    }

    fn write(&self, c: &crate::Compiled, design: &Path, stem: &str, sha256: &str) -> Result<(), String> {
        // Write beside the target and rename, so a failed or concurrent build never leaves a
        // partial design that a later run would take for a cached one.
        let tmp = design.with_extension(format!("{}.tmp", std::process::id()));
        if let Some(native) = &c.native {
            native.write(&tmp)?;
        } else {
            crate::assembly::check_engine(self.cli, c)?;
            let script = self.dir.join(format!("{stem}-{}.{}.json", &sha256[..12], std::process::id()));
            std::fs::write(&script, c.script.to_string()).map_err(|e| format!("{}: {e}", script.display()))?;
            let out = std::process::Command::new(self.cli)
                .arg("run")
                .arg(&script)
                .args(["--quiet", "--save"])
                .arg(&tmp)
                .output()
                .map_err(|e| format!("cannot run `{}`: {e} (set SOLIDCRAFT_CLI to the solidcraft-cli binary)", self.cli));
            let _ = std::fs::remove_file(&script);
            let out = out?;
            if !out.status.success() {
                let _ = std::fs::remove_file(&tmp);
                let message = String::from_utf8_lossy(&out.stderr).trim().to_string();
                // The script runner numbers its steps from one.
                let step = message.split("step ").nth(1).and_then(|s| s.split_whitespace().next()).and_then(|s| s.parse::<usize>().ok());
                let line = step.and_then(|n| n.checked_sub(1)).and_then(|i| c.lines.get(i));
                return Err(match line {
                    Some(line) => format!("line {line}: the part does not build: {message}"),
                    None => format!("the part does not build: {message}"),
                });
            }
        }
        std::fs::rename(&tmp, design).map_err(|e| format!("{}: {e}", design.display()))
    }
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn imported_drawings_are_inlined_relative_to_the_design() {
        let dir = std::env::temp_dir().join(format!("sc-inline-{}", std::process::id()));
        std::fs::create_dir_all(dir.join("art")).unwrap();
        std::fs::write(dir.join("art/a.svg"), "<svg/>").unwrap();
        let mut script = json!({"commands": [{"command": "sketch.insert_svg", "params": {"path": "art/a.svg", "at": [1, 2]}}]});
        inline_files(&mut script, &dir.join("part.sc")).unwrap();
        assert_eq!(script["commands"][0]["params"], json!({"at": [1, 2], "text": "<svg/>"}));
        let mut missing = json!({"commands": [{"command": "sketch.insert_dxf", "params": {"path": "nope.dxf"}}]});
        assert!(inline_files(&mut missing, &dir.join("part.sc")).unwrap_err().contains("nope.dxf"));
        let _ = std::fs::remove_dir_all(&dir);
    }
}
