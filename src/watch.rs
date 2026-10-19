//! `watch`: keep a `.sc` file and the running app in step, the file being the source of truth.
//!
//! Saving the file rebuilds the design in the app. Changing a value in the app (a parameter in
//! the parameters panel, a dimension dragged or typed in a sketch) writes that value back to the
//! one line of the file that holds it, leaving everything else as it was. Any other edit in the
//! app is reported as not reflected in the file.
//!
//! Until the app emits change events, this polls `document.inspect`: the user parameters and
//! the model dimensions `d1`, `d2`, … (numbered in creation order, which is the file's order of
//! dimension statements) are compared with the values recorded right after the last apply.

use std::collections::BTreeMap;
use std::path::Path;
use std::time::{Duration, SystemTime};

use serde_json::{Value, json};

use crate::control::Control;

pub struct Options {
    pub path: String,
    pub addr: String,
    pub token: Option<String>,
    pub interval: Duration,
    /// Apply once and exit (no polling).
    pub once: bool,
}

/// What the app held right after the file was applied.
struct Baseline {
    /// User parameter name → expression, and the file line of each.
    params: BTreeMap<String, (String, usize)>,
    /// Model dimension `dN` → expression, with the file line of the statement that made it.
    dims: Vec<(String, String, Option<usize>)>,
}

fn mtime(path: &str) -> Option<SystemTime> {
    std::fs::metadata(path).ok()?.modified().ok()
}

/// Replace the value after the first `=` on `line`, keeping its indentation, the left side
/// and a trailing `//` comment.
pub fn patch_line(line: &str, value: &str) -> Option<String> {
    let eq = line.find('=')?;
    let rest = &line[eq + 1..];
    let comment = comment_start(rest).map(|i| rest[i..].trim_end());
    let mut out = format!("{}= {value}", &line[..eq]);
    if let Some(c) = comment {
        out.push(' ');
        out.push_str(c);
    }
    Some(out)
}

/// Where a `//` comment starts in `s`, outside quotes.
fn comment_start(s: &str) -> Option<usize> {
    let b = s.as_bytes();
    let mut quoted = false;
    for i in 0..b.len() {
        match b[i] {
            b'"' => quoted = !quoted,
            b'/' if !quoted && b.get(i + 1) == Some(&b'/') => return Some(i),
            _ => {}
        }
    }
    None
}

struct Session {
    opts: Options,
    ctl: Control,
    baseline: Option<Baseline>,
    seen: Option<SystemTime>,
}

impl Session {
    fn say(&self, msg: &str) {
        let name = Path::new(&self.opts.path).file_name().and_then(|n| n.to_str()).unwrap_or(&self.opts.path);
        println!("{name}: {msg}");
    }

    /// Compile the file and rebuild the design in the app.
    fn apply(&mut self) -> Result<(), String> {
        let src = std::fs::read_to_string(&self.opts.path).map_err(|e| format!("{}: {e}", self.opts.path))?;
        let design = crate::parse(&src)?;
        crate::check(&design)?;
        let c = crate::compile(&design)?;
        let commands = c.script["commands"].clone();
        let n = commands.as_array().map_or(0, Vec::len);
        self.ctl.call("engine.execute", json!({"command": "file.new", "params": {}}))?;
        let r = self.ctl.call("engine.batch", json!({"commands": commands, "rollback": false}))?;
        if let Some(f) = r.get("failed") {
            let i = f.get("index").and_then(Value::as_u64).unwrap_or(0) as usize;
            let line = c.lines.get(i).copied().unwrap_or(0);
            let msg = f.get("error").and_then(Value::as_str).unwrap_or("failed");
            let id = commands.get(i).and_then(|c| c["command"].as_str()).unwrap_or("?");
            self.baseline = None;
            return Err(format!("line {line}: {id}: {msg} (the design in the app stops there)"));
        }
        self.say(&format!("applied {n} commands ({} sketches, {} features)", c.sketches, c.features));
        // Record what the app holds now, so later changes in the app stand out.
        let inspect = self.ctl.call("document.inspect", json!({}))?;
        let mut params = BTreeMap::new();
        let mut dims = Vec::new();
        for p in inspect["params"].as_array().into_iter().flatten() {
            let name = p["name"].as_str().unwrap_or("").to_string();
            let expr = p["expression"].as_str().unwrap_or("").to_string();
            if p["model"].as_bool() == Some(true) {
                dims.push((name, expr, None));
            } else if let Some(line) = design.params.iter().find(|q| q.name == name).map(|q| q.line) {
                params.insert(name, (expr, line));
            }
        }
        if dims.len() == c.dims.len() {
            for (d, line) in dims.iter_mut().zip(&c.dims) {
                d.2 = *line;
            }
        } else {
            self.say(&format!(
                "the app has {} model dimensions, the file accounts for {}: dimension changes in the app will not be written back",
                dims.len(),
                c.dims.len()
            ));
            for d in &mut dims {
                d.2 = None;
            }
        }
        self.baseline = Some(Baseline { params, dims });
        Ok(())
    }

    /// Compare the app with the baseline and write changed values back to the file.
    fn poll(&mut self) -> Result<(), String> {
        let Some(base) = self.baseline.as_mut() else { return Ok(()) };
        let inspect = self.ctl.call("document.inspect", json!({}))?;
        let Some(live) = inspect["params"].as_array() else { return Ok(()) };
        let mut edits: Vec<(usize, String, String)> = Vec::new(); // (line, value, what)
        let mut notes: Vec<String> = Vec::new();
        let mut live_names = 0;
        let mut dim_i = 0;
        for p in live {
            let name = p["name"].as_str().unwrap_or("");
            let expr = p["expression"].as_str().unwrap_or("");
            if p["model"].as_bool() == Some(true) {
                if let Some(d) = base.dims.get_mut(dim_i) {
                    if d.0 == name && d.1 != expr {
                        match d.2 {
                            Some(line) => edits.push((line, expr.to_string(), format!("dimension {name}"))),
                            None => notes.push(format!("{name} changed to `{expr}` in the app; that radius lives in an option, not written back")),
                        }
                        d.1 = expr.to_string();
                    }
                }
                dim_i += 1;
            } else if let Some(b) = base.params.get_mut(name) {
                live_names += 1;
                if b.0 != expr {
                    edits.push((b.1, expr.to_string(), format!("parameter {name}")));
                    b.0 = expr.to_string();
                }
            } else {
                // A parameter added in the app: the file does not know it.
                notes.push(format!("parameter `{name}` added in the app is not in the file; add `param {name} = {expr}` to keep it"));
                base.params.insert(name.to_string(), (expr.to_string(), 0));
            }
        }
        let base_empty = base.params.is_empty();
        for n in &notes {
            self.say(n);
        }
        if live_names == 0 && !base_empty {
            self.say("the design in the app no longer matches the file (opened another design?); save the file to apply it again");
            self.baseline = None;
            return Ok(());
        }
        if edits.is_empty() {
            return Ok(());
        }
        let text = std::fs::read_to_string(&self.opts.path).map_err(|e| format!("{}: {e}", self.opts.path))?;
        let mut lines: Vec<String> = text.lines().map(str::to_string).collect();
        for (line, value, what) in &edits {
            let Some(slot) = line.checked_sub(1).and_then(|i| lines.get_mut(i)) else { continue };
            match patch_line(slot, value) {
                Some(new) => {
                    self.say(&format!("{what} ← `{value}` (line {line})"));
                    *slot = new;
                }
                None => self.say(&format!("{what} changed to `{value}`, but line {line} has no `=` to patch")),
            }
        }
        let mut out = lines.join("\n");
        if text.ends_with('\n') {
            out.push('\n');
        }
        std::fs::write(&self.opts.path, out).map_err(|e| format!("{}: {e}", self.opts.path))?;
        // Our own write is not a change to apply.
        self.seen = mtime(&self.opts.path);
        Ok(())
    }
}

pub fn watch(opts: Options) -> Result<(), String> {
    let token = match &opts.token {
        Some(t) => t.clone(),
        None => crate::control::find_token(&opts.addr)?,
    };
    let ctl = Control::connect(&opts.addr, Some(token))?;
    let mut s = Session { opts, ctl, baseline: None, seen: None };
    s.seen = mtime(&s.opts.path);
    if let Err(e) = s.apply() {
        if s.opts.once {
            return Err(e);
        }
        s.say(&format!("error: {e}"));
    }
    if s.opts.once {
        return Ok(());
    }
    s.say("watching (save the file to apply it; values changed in the app are written back)");
    loop {
        std::thread::sleep(s.opts.interval);
        let now = mtime(&s.opts.path);
        if now != s.seen {
            s.seen = now;
            if let Err(e) = s.apply() {
                s.say(&format!("error: {e}"));
            }
            continue;
        }
        if let Err(e) = s.poll() {
            // The app went away or is busy: say so once and keep trying.
            s.say(&format!("error: {e}"));
            if e.contains("went away") || e.contains("closed the connection") {
                return Err(e);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn patch_keeps_left_side_and_comment() {
        assert_eq!(patch_line("param L = 60 mm", "55 mm").as_deref(), Some("param L = 55 mm"));
        assert_eq!(patch_line("  dim outline.1 = L  // length", "70 mm").as_deref(), Some("  dim outline.1 = 70 mm // length"));
        assert_eq!(patch_line("dim horizontal origin -> a = L - inset", "40").as_deref(), Some("dim horizontal origin -> a = 40"));
        assert_eq!(patch_line("extrude E from S distance 5", "x"), None);
    }
}
