//! Reusable parts. `use Clip from "clip.sc" at (x,y,z) join targets Box` copies another design's
//! geometry into this one when it is built: the part's names are prefixed with `Clip_` so copies
//! don't collide, `with (p = expr)` overrides its parameters, and generated `move` and `combine`
//! features place it and join it. The expansion keeps a map back to the original lines, so
//! errors and the source map point at the `use` statement (and into the part file).
use std::collections::HashMap;
use std::path::{Path, PathBuf};

use crate::ast::Use;
use crate::lex::{Tok, lex};

type R<T> = Result<T, String>;

/// Where a line of the expanded text came from.
#[derive(Debug, Clone, PartialEq)]
pub enum Origin {
    /// A line of the design itself.
    Here(usize),
    /// A line of a used part: the `use` statement's line, and the part's file and line.
    Used { line: usize, file: PathBuf, at: usize },
}

impl Origin {
    /// The line in the design file this line belongs to.
    pub fn line(&self) -> usize {
        match self {
            Origin::Here(n) | Origin::Used { line: n, .. } => *n,
        }
    }
}

pub struct Expanded {
    pub text: String,
    /// `origin[n - 1]` for line `n` of `text`.
    pub origin: Vec<Origin>,
}

impl Expanded {
    /// Rewrite a `line N: …` message against the original files.
    pub fn error(&self, e: String) -> String {
        let Some((n, rest)) = e.strip_prefix("line ").and_then(|r| r.split_once(':')).and_then(|(n, rest)| Some((n.parse::<usize>().ok()?, rest)))
        else {
            return e;
        };
        match self.origin.get(n.wrapping_sub(1)) {
            Some(Origin::Here(line)) => format!("line {line}:{rest}"),
            Some(Origin::Used { line, file, at }) => format!("line {line}: in {}:{at}:{rest}", file.display()),
            None => e,
        }
    }
}

/// `in file:N: …` for an error at `line N: …` of a part file.
fn located(file: &str, e: String) -> String {
    match e.strip_prefix("line ").and_then(|r| r.split_once(':')) {
        Some((n, rest)) if n.parse::<usize>().is_ok() => format!("in {file}:{n}:{rest}"),
        _ => format!("in {file}: {e}"),
    }
}

/// Expand every `use` in `src` (the design at `path`), recursively.
pub fn expand(src: &str, path: &Path) -> R<Expanded> {
    expand_inner(src, path, &mut vec![])
}

fn expand_inner(src: &str, path: &Path, stack: &mut Vec<PathBuf>) -> R<Expanded> {
    let mut out = Expanded { text: String::new(), origin: vec![] };
    let push = |out: &mut Expanded, text: &str, origin: Origin| {
        out.text.push_str(text);
        out.text.push('\n');
        out.origin.push(origin);
    };
    let numbered: Vec<(String, usize)> = src.lines().enumerate().map(|(i, t)| (t.to_string(), i + 1)).collect();
    for (text, no) in unroll(&numbered)? {
        let first = lex(&text)?.into_iter().next();
        let Some(mut line) = first.filter(|l| matches!(l.toks.first(), Some(Tok::Ident(w)) if w == "use")) else {
            push(&mut out, &text, Origin::Here(no));
            continue;
        };
        line.no = no;
        let u = crate::parse::parse_use(&line)?;
        let file = path.parent().unwrap_or(Path::new(".")).join(&u.file);
        let canonical = std::fs::canonicalize(&file).map_err(|e| format!("line {no}: {}: {e}", file.display()))?;
        if stack.contains(&canonical) || canonical == std::fs::canonicalize(path).unwrap_or_default() {
            return Err(format!("line {no}: `{}` uses itself (through {})", u.file, file.display()));
        }
        if stack.len() > 16 {
            return Err(format!("line {no}: parts are used more than 16 deep"));
        }
        let part_src = std::fs::read_to_string(&file).map_err(|e| format!("line {no}: {}: {e}", file.display()))?;
        stack.push(canonical);
        let part = expand_inner(&part_src, &file, stack).map_err(|e| format!("line {no}: {}", located(&file.display().to_string(), e)))?;
        stack.pop();
        let (body, origins) = instantiate(&part, &u, no).map_err(|e| format!("line {no}: {e}"))?;
        for (text, origin) in body.iter().zip(origins) {
            let origin = match origin {
                Some(Origin::Here(at)) => Origin::Used { line: no, file: file.clone(), at },
                Some(Origin::Used { at, file: inner, .. }) => Origin::Used { line: no, file: inner, at },
                None => Origin::Here(no),
            };
            push(&mut out, text, origin);
        }
    }
    Ok(out)
}

/// Unroll `for v from a to b [step s] { … }` blocks: the body once for each value, with `{v}`
/// (in names and strings) and the word `v` (in expressions) replaced by it. Lines keep their
/// numbers in the original text.
fn unroll(lines: &[(String, usize)]) -> R<Vec<(String, usize)>> {
    let mut out = Vec::new();
    let mut i = 0;
    while i < lines.len() {
        let (text, no) = (&lines[i].0, lines[i].1);
        let Some((var, values)) = for_head(text, no)? else {
            out.push(lines[i].clone());
            i += 1;
            continue;
        };
        // The body runs to the `}` that closes the `for`, past any blocks inside it.
        let mut depth = 1;
        let mut j = i + 1;
        while j < lines.len() {
            let t = crate::lex::strip_comment(&lines[j].0).trim().to_string();
            if t.ends_with('{') {
                depth += 1;
            } else if t == "}" {
                depth -= 1;
                if depth == 0 {
                    break;
                }
            }
            j += 1;
        }
        if j == lines.len() {
            return Err(format!("line {no}: `for` block is never closed with `}}`"));
        }
        let body = unroll(&lines[i + 1..j])?;
        for v in values {
            for (t, n) in &body {
                out.push((substitute(t, &var, v), *n));
            }
        }
        i = j + 1;
    }
    Ok(out)
}

/// `for v from a to b [step s] {`: the variable and its values.
fn for_head(text: &str, no: usize) -> R<Option<(String, Vec<i64>)>> {
    let toks = match lex(text)?.into_iter().next() {
        Some(l) => l.toks,
        None => return Ok(None),
    };
    if !matches!(toks.first(), Some(Tok::Ident(w)) if w == "for") {
        return Ok(None);
    }
    let bad = || format!("line {no}: expected `for v from <whole number> to <whole number> [step <n>] {{`");
    let mut k = 1;
    let word = |toks: &[Tok], k: &mut usize| -> Option<String> {
        let w = match toks.get(*k) {
            Some(Tok::Ident(w)) => Some(w.clone()),
            _ => None,
        };
        *k += 1;
        w
    };
    let int = |toks: &[Tok], k: &mut usize| -> Option<i64> {
        let neg = toks.get(*k) == Some(&Tok::Punct('-'));
        if neg {
            *k += 1;
        }
        let v = match toks.get(*k) {
            Some(Tok::Num(v, _)) if v.fract() == 0.0 => Some(*v as i64),
            _ => None,
        };
        *k += 1;
        v.map(|v| if neg { -v } else { v })
    };
    let var = word(&toks, &mut k).ok_or_else(bad)?;
    if word(&toks, &mut k).as_deref() != Some("from") {
        return Err(bad());
    }
    let from = int(&toks, &mut k).ok_or_else(bad)?;
    if word(&toks, &mut k).as_deref() != Some("to") {
        return Err(bad());
    }
    let to = int(&toks, &mut k).ok_or_else(bad)?;
    let mut step = 1;
    if matches!(toks.get(k), Some(Tok::Ident(w)) if w == "step") {
        k += 1;
        step = int(&toks, &mut k).filter(|s| *s > 0).ok_or_else(bad)?;
    }
    if toks.get(k) != Some(&Tok::Punct('{')) || k + 1 != toks.len() {
        return Err(bad());
    }
    let values: Vec<i64> = if from <= to { (from..=to).step_by(step as usize).collect() } else { (to..=from).rev().step_by(step as usize).collect() };
    if values.len() > 1000 {
        return Err(format!("line {no}: a `for` runs at most 1000 times"));
    }
    Ok(Some((var, values)))
}

/// `{var}` anywhere outside comments, and the word `var` outside strings, become `v`.
fn substitute(line: &str, var: &str, v: i64) -> String {
    let (code, comment) = match line.find("//") {
        Some(i) => line.split_at(i),
        None => (line, ""),
    };
    let code = code.replace(&format!("{{{var}}}"), &v.to_string());
    let value = if v < 0 { format!("({v})") } else { v.to_string() };
    let names = HashMap::from([(var.to_string(), value)]);
    rename(&code, &names) + comment
}

/// The part's lines with its names prefixed, its parameters overridden, and the placing features
/// after them (with no origin of their own).
fn instantiate(part: &Expanded, u: &Use, line: usize) -> R<(Vec<String>, Vec<Option<Origin>>)> {
    let design = crate::parse(&part.text).map_err(|e| located(&u.file, part.error(e)))?;
    let prefix = format!("{}_", u.alias);
    let mut names: HashMap<String, String> = HashMap::new();
    for p in &design.params {
        names.insert(p.name.clone(), format!("{prefix}{}", p.name));
    }
    let mut created = Vec::new();
    for item in &design.items {
        match item {
            crate::ast::Item::Sketch(s) => {
                names.insert(s.name.clone(), format!("{prefix}{}", s.name));
            }
            crate::ast::Item::Feature(f) => {
                if !f.name.contains('@') {
                    names.insert(f.name.clone(), format!("{prefix}{}", f.name));
                }
            }
        }
    }
    // Bodies: those a `-> body` names are created here; any other body named is too.
    for l in lex(&part.text)? {
        for w in l.toks.windows(3) {
            if let [Tok::Arrow, Tok::Ident(b), Tok::Ident(name)] = w
                && b == "body"
                && !created.contains(name)
            {
                created.push(name.clone());
            }
        }
    }
    if created.is_empty() {
        return Err(format!("`{}` creates no body (`-> body Name`) to use", u.file));
    }
    for b in &created {
        names.insert(b.clone(), format!("{prefix}{b}"));
    }
    if let Some(name) = &u.body {
        if created.len() != 1 {
            return Err(format!("`-> body {name}` names one body, but `{}` creates {}", u.file, created.join(", ")));
        }
        names.insert(created[0].clone(), name.clone());
    }
    let bodies: Vec<String> = created.iter().map(|b| names[b].clone()).collect();
    let mut text = Vec::new();
    let mut origin = Vec::new();
    let mut overridden = vec![false; u.overrides.len()];
    for (i, raw) in part.text.lines().enumerate() {
        let first = raw.split_whitespace().next().unwrap_or("");
        // The part's own header and print setup belong to it, not to the design using it; its
        // checks too, once its bodies are joined into another.
        if matches!(first, "design" | "print") || (first == "check" && u.join.is_some()) {
            continue;
        }
        let mut renamed = rename(raw, &names);
        if first == "param"
            && let Some(k) = u.overrides.iter().position(|(p, _)| renamed.split_whitespace().nth(1) == Some(&format!("{prefix}{p}")))
        {
            renamed = format!("param {prefix}{} = {}", u.overrides[k].0, u.overrides[k].1);
            overridden[k] = true;
        }
        text.push(renamed);
        origin.push(part.origin.get(i).cloned());
    }
    if let Some(k) = overridden.iter().position(|o| !o) {
        return Err(format!("`{}` has no `param {}` to override", u.file, u.overrides[k].0));
    }
    let list = bodies.join(", ");
    let comps = |c: &[crate::ast::Comp]| c.iter().map(|c| crate::expressions::text(c).to_string()).collect::<Vec<_>>().join(", ");
    if let Some((axis, angle)) = &u.rotate {
        text.push(format!("move {prefix}Turn bodies {list} axis ({}) angle ({angle})", comps(axis)));
        origin.push(None);
    }
    if let Some(at) = &u.at {
        text.push(format!("move {prefix}Place bodies {list} to ({})", comps(at)));
        origin.push(None);
    }
    if let Some(targets) = &u.join {
        for (k, target) in targets.iter().enumerate() {
            let tools = if k == 0 { list.clone() } else { String::new() };
            if tools.is_empty() {
                return Err("join one target: the part's bodies become part of it".into());
            }
            text.push(format!("combine {prefix}Join target {target} tools {tools} join"));
            origin.push(None);
        }
    }
    let _ = line;
    Ok((text, origin))
}

/// Replace the names in `names` wherever they appear as words outside comments and strings.
fn rename(line: &str, names: &HashMap<String, String>) -> String {
    let mut out = String::with_capacity(line.len());
    let chars: Vec<char> = line.chars().collect();
    let mut i = 0;
    let mut quoted = false;
    while i < chars.len() {
        let c = chars[i];
        if quoted {
            out.push(c);
            quoted = c != '"';
            i += 1;
        } else if c == '"' {
            out.push(c);
            quoted = true;
            i += 1;
        } else if c == '/' && chars.get(i + 1) == Some(&'/') {
            out.extend(&chars[i..]);
            break;
        } else if c.is_alphabetic() || c == '_' {
            let start = i;
            while i < chars.len() && (chars[i].is_alphanumeric() || chars[i] == '_') {
                i += 1;
            }
            let word: String = chars[start..i].iter().collect();
            // `Sketch.entity`: only the sketch is a design-level name.
            let after_dot = start > 0 && chars[start - 1] == '.';
            match names.get(&word) {
                Some(new) if !after_dot => out.push_str(new),
                _ => out.push_str(&word),
            }
        } else {
            out.push(c);
            i += 1;
        }
    }
    out
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    use serde_json::{Value, json};

    fn files(name: &str, parts: &[(&str, &str)]) -> PathBuf {
        let dir = std::env::temp_dir().join(format!("sc-reuse-{name}-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        for (file, text) in parts {
            std::fs::write(dir.join(file), text).unwrap();
        }
        dir
    }

    #[test]
    fn used_parts_are_renamed_placed_and_mapped_back() {
        let peg = "design Peg units mm\nparam h = 6 mm\ncylinder Post radius 2 height h -> body Peg\ncheck Peg size.z == h\n";
        let dir = files("ok", &[("peg.sc", peg)]);
        let host = "design Host units mm\nparam z = 2 mm\nbox B size 20 20 2 -> body Plate\n\
                    use A from \"peg.sc\" with (h = (z*4)) rotate X 90 deg at (5, 5, z) -> body Pin\n\
                    use C from \"peg.sc\" at (15, 15, z) join targets Plate\n";
        let c = crate::build_file(host, &dir.join("host.sc")).unwrap();
        let cmds = c.script["commands"].as_array().unwrap();
        let by = |id: &str| cmds.iter().filter(|c| c["command"] == id).map(|c| c["params"].clone()).collect::<Vec<_>>();
        let params: Vec<String> =
            by("parameters.add").iter().map(|p| format!("{}={}", p["name"].as_str().unwrap(), p["expression"].as_str().unwrap())).collect();
        assert_eq!(params, ["z=2 mm", "A_h=(z * 4)", "C_h=6 mm"]);
        assert_eq!(by("solid.cylinder")[0]["body_name"], json!("Pin"));
        let moves = by("solid.move");
        assert_eq!((moves[0]["angle"].clone(), moves[1]["translate"].clone()), (json!("90 deg"), json!([5.0, 5.0, 2.0])));
        assert_eq!(by("solid.combine")[0], json!({"target": "Plate", "tools": ["C_Peg"], "operation": "join", "name": "C_Join"}));
        // Every command maps to a line of the host file: the part's to its `use` line.
        assert!(c.lines.iter().all(|&n| (1..=5).contains(&n)), "{:?}", c.lines);
        // The named copy keeps its check (on Pin); the joined one's is dropped.
        assert_eq!(c.checks.iter().map(|k| (k.body.as_str(), k.line)).collect::<Vec<_>>(), [("Pin", 4)]);
        let _ = std::fs::remove_dir_all(&dir);
    }

    #[test]
    fn use_errors_name_the_files() {
        let dir = files(
            "err",
            &[
                ("bad.sc", "design Bad\nbox B size 1 1 1 -> body Bad\nbox C size -1 1 1\n"),
                ("loop.sc", "use L from \"loop.sc\"\n"),
                ("peg.sc", "box B size 1 1 1 -> body Peg\n"),
            ],
        );
        let host = |line: &str| format!("design H\n{line}\n");
        let e = crate::build_file(&host("use X from \"bad.sc\""), &dir.join("h.sc")).unwrap_err();
        assert!(e.starts_with("line 2: in ") && e.contains("bad.sc:3:"), "{e}");
        let e = crate::build_file(&host("use X from \"loop.sc\""), &dir.join("h.sc")).unwrap_err();
        assert!(e.contains("uses itself"), "{e}");
        let e = crate::build_file(&host("use X from \"peg.sc\" with (nope = 1)"), &dir.join("h.sc")).unwrap_err();
        assert!(e.contains("has no `param nope`"), "{e}");
        let e = crate::build_file(&host("use X from \"missing.sc\""), &dir.join("h.sc")).unwrap_err();
        assert!(e.starts_with("line 2: ") && e.contains("missing.sc"), "{e}");
        assert!(crate::build(&host("use X from \"peg.sc\"")).unwrap_err().contains("build the design from its file"));
        let _ = std::fs::remove_dir_all(&dir);
    }

    #[test]
    fn for_blocks_unroll_with_their_variable() {
        let src = "design L units mm\nbox B size 80 20 4 -> body P\nfor k from 1 to 3 {\n  cylinder H{k} radius (k*1 mm) height 6 base (k*20 mm, 10, -1 mm) cut targets P // k {k}\n  sketch S{k} on XY offset 4 mm {\n    text \"#{k}\" at (k*20 mm, 2) height 3 mm\n  }\n}\nfor i from 2 to 0 step 2 {\n  for j from 1 to 2 {\n    box C{j}x{i} size 1 1 1 corner (i - 3, j, 10) -> body C{j}x{i}\n  }\n}\n";
        let c = crate::build(src).unwrap();
        let cmds = c.script["commands"].as_array().unwrap();
        let names: Vec<&str> = cmds.iter().filter(|c| c["command"] != "sketch.text").filter_map(|c| c["params"]["name"].as_str()).collect();
        assert_eq!(names, ["B", "H1", "S1", "H2", "S2", "H3", "S3", "C1x2", "C2x2", "C1x0", "C2x0"]);
        let texts: Vec<&Value> = cmds.iter().filter(|c| c["command"] == "sketch.text").map(|c| &c["params"]["text"]).collect();
        assert_eq!(texts, [&json!("#1"), &json!("#2"), &json!("#3")]);
        let by = |n: &str| cmds.iter().find(|c| c["params"]["name"] == n).unwrap()["params"].clone();
        assert_eq!(by("H2")["radius"], json!("2 * 1 mm"));
        assert_eq!(by("C2x0")["corner"], json!(["0 - 3", 2.0, 10.0]));
        // Every line maps back to its line in the loop body.
        assert!(c.lines.iter().all(|&n| n <= 13), "{:?}", c.lines);
        let e = crate::build("design L\nfor k from 1 to 3 {\n  box B{k} size -1 1 1\n}\n").unwrap_err();
        assert!(e.starts_with("line 3:"), "{e}");
        assert!(crate::build("design L\nfor k from 1 to {\n}\n").unwrap_err().contains("expected `for v from"));
        assert!(crate::build("design L\nfor k from 1 to 3 {\n box B size 1 1 1\n").unwrap_err().contains("never closed"));
    }

    #[test]
    fn rename_skips_comments_strings_and_entity_names() {
        let names = HashMap::from([("w".to_string(), "C_w".to_string()), ("Sk".to_string(), "C_Sk".to_string())]);
        assert_eq!(rename("extrude E from Sk distance (w*2) // w", &names), "extrude E from C_Sk distance (C_w*2) // w");
        assert_eq!(rename("line l = (0,0) Sk.w \"w\"", &names), "line l = (0,0) C_Sk.w \"w\"");
    }
}
