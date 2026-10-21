//! Line-oriented tokenizer. A `.sc` file is a sequence of statements, one per line; `{` ends a
//! line that opens a block and `}` alone closes it. `//` starts a comment.

#[derive(Debug, Clone, PartialEq)]
pub enum Tok {
    Ident(String),
    /// A number with its source text (kept so `6` and `6.0` reach the engine as written).
    Num(f64, String),
    Str(String),
    /// `(`, `)`, `,`, `=`, `{`, `}`, `.`, `*`, `:`, `-`, `+`, `/`
    Punct(char),
    /// `->`
    Arrow,
}

impl Tok {
    pub fn describe(&self) -> String {
        match self {
            Tok::Ident(s) => format!("`{s}`"),
            Tok::Num(_, s) => format!("number `{s}`"),
            Tok::Str(s) => format!("string \"{s}\""),
            Tok::Punct(c) => format!("`{c}`"),
            Tok::Arrow => "`->`".into(),
        }
    }
}

/// One source line: its number (1-based), its tokens, and the raw text after comment removal.
#[derive(Debug, Clone)]
pub struct Line {
    pub no: usize,
    pub toks: Vec<Tok>,
    pub raw: String,
}

/// Strip a `//` comment that is not inside a string.
fn strip_comment(s: &str) -> &str {
    let mut in_str = false;
    let b = s.as_bytes();
    let mut i = 0;
    while i < b.len() {
        match b[i] {
            b'"' => in_str = !in_str,
            b'/' if !in_str && i + 1 < b.len() && b[i + 1] == b'/' => return &s[..i],
            _ => {}
        }
        i += 1;
    }
    s
}

pub fn tokenize_line(no: usize, text: &str) -> Result<Vec<Tok>, String> {
    let mut toks = Vec::new();
    let chars: Vec<char> = text.chars().collect();
    let mut i = 0;
    while i < chars.len() {
        let c = chars[i];
        if c.is_whitespace() {
            i += 1;
        } else if c == '-' && chars.get(i + 1) == Some(&'>') {
            toks.push(Tok::Arrow);
            i += 2;
        } else if c.is_ascii_digit()
            || (c == '-'
                && chars.get(i + 1).is_some_and(|d| d.is_ascii_digit() || *d == '.')
                && !matches!(toks.last(), Some(Tok::Num(..) | Tok::Ident(_) | Tok::Punct(')'))))
            || (c == '.'
                && chars.get(i + 1).is_some_and(|d| d.is_ascii_digit())
                && !matches!(toks.last(), Some(Tok::Ident(_) | Tok::Num(..) | Tok::Punct(')'))))
        {
            let start = i;
            i += 1;
            let mut seen_dot = chars[start] == '.';
            let mut seen_exp = false;
            while i < chars.len() {
                let d = chars[i];
                let ok = d.is_ascii_digit()
                    || (d == '.' && !seen_dot && !seen_exp && chars.get(i + 1).is_some_and(|n| n.is_ascii_digit()))
                    || ((d == 'e' || d == 'E') && !seen_exp && chars.get(i + 1).is_some_and(|n| n.is_ascii_digit() || *n == '-' || *n == '+'))
                    || ((d == '-' || d == '+') && matches!(chars[i - 1], 'e' | 'E'));
                if !ok {
                    break;
                }
                seen_dot |= d == '.';
                seen_exp |= d == 'e' || d == 'E';
                i += 1;
            }
            let s: String = chars[start..i].iter().collect();
            let v: f64 = s.parse().map_err(|_| format!("line {no}: bad number `{s}`"))?;
            if !v.is_finite() {
                return Err(format!("line {no}: bad number `{s}`"));
            }
            toks.push(Tok::Num(v, s));
        } else if c.is_alphabetic() || c == '_' {
            let start = i;
            while i < chars.len() && (chars[i].is_alphanumeric() || chars[i] == '_') {
                i += 1;
            }
            toks.push(Tok::Ident(chars[start..i].iter().collect()));
        } else if c == '"' {
            i += 1;
            let start = i;
            while i < chars.len() && chars[i] != '"' {
                i += 1;
            }
            if i >= chars.len() {
                return Err(format!("line {no}: unterminated string"));
            }
            toks.push(Tok::Str(chars[start..i].iter().collect()));
            i += 1;
        } else if "(),={}.*:-+/^%;°".contains(c) {
            toks.push(Tok::Punct(c));
            i += 1;
        } else {
            return Err(format!("line {no}: unexpected character `{c}`"));
        }
    }
    Ok(toks)
}

/// Tokenize a whole file. Blank and comment-only lines are dropped.
pub fn lex(src: &str) -> Result<Vec<Line>, String> {
    let mut out = Vec::new();
    for (i, text) in src.lines().enumerate() {
        let no = i + 1;
        let raw = strip_comment(text).trim();
        if raw.is_empty() {
            continue;
        }
        out.push(Line { no, toks: tokenize_line(no, raw)?, raw: raw.to_string() });
    }
    Ok(out)
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;

    #[test]
    fn tokens() {
        let t = tokenize_line(1, "dim horizontal origin -> a = L - hole_inset").unwrap();
        assert_eq!(t[3], Tok::Arrow);
        assert_eq!(t.len(), 9);
        let t = tokenize_line(1, "polyline closed outline = (0,0) (60,-6.5)").unwrap();
        assert!(matches!(t[12], Tok::Num(v, _) if v == -6.5), "{t:?}");
        let t = tokenize_line(1, "dim a -> b = 2 -3").unwrap();
        assert_eq!(t[6], Tok::Punct('-'), "a minus after a number is an operator: {t:?}");
        assert!(tokenize_line(1, "\"open").is_err());
        assert_eq!(lex("// only\n\nparam L = 1\n").unwrap().len(), 1);
    }
}
