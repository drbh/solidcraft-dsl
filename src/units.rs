//! Unit classes of expressions: is `L - 2*inset` a length, `30 deg` an angle, `4` a number?
//!
//! This is a classifier, not an evaluator: it follows the engine's expression grammar (numbers
//! with an optional unit, parameters, `+ - * /`, parentheses, unary minus, function calls) far
//! enough to tell a length from an angle from a plain number, so a file is refused before the
//! engine sees `distance 30 deg`. Anything it does not understand is `Unknown` and passes.

use std::collections::HashMap;

/// What an expression measures.
#[derive(Debug, Clone, PartialEq)]
pub enum Class {
    /// A plain number: a count, a factor, or a bare value in the design's units.
    Number,
    Length,
    Angle,
    /// Something else, named for the error (`an area`).
    Other(String),
    /// Not classified (an unknown function, a parameter the engine defines).
    Unknown,
}

impl Class {
    pub fn describe(&self) -> String {
        match self {
            Class::Number => "a number".into(),
            Class::Length => "a length".into(),
            Class::Angle => "an angle".into(),
            Class::Other(s) => s.clone(),
            Class::Unknown => "unknown".into(),
        }
    }

    /// Can a value of this class be given where `want` is expected? A plain number always can
    /// (it is read in the design's units); so can anything unknown.
    pub fn fits(&self, want: &Class) -> bool {
        matches!(self, Class::Number | Class::Unknown) || matches!(want, Class::Unknown) || self == want
    }
}

#[derive(Debug, Clone, PartialEq)]
enum Tok {
    Num(f64),
    Ident(String),
    Op(char),
}

fn lex(s: &str) -> Result<Vec<Tok>, String> {
    let chars: Vec<char> = s.chars().collect();
    let mut toks = Vec::new();
    let mut i = 0;
    while i < chars.len() {
        let c = chars[i];
        if c.is_whitespace() {
            i += 1;
        } else if c.is_ascii_digit() || (c == '.' && chars.get(i + 1).is_some_and(char::is_ascii_digit)) {
            let start = i;
            while i < chars.len() && (chars[i].is_ascii_digit() || chars[i] == '.' || chars[i] == 'e' || chars[i] == 'E') {
                // `1e-3`: the sign after an exponent belongs to the number.
                if (chars[i] == 'e' || chars[i] == 'E') && matches!(chars.get(i + 1), Some('-' | '+')) {
                    i += 1;
                }
                i += 1;
            }
            let text: String = chars[start..i].iter().collect();
            let n: f64 = text.parse().map_err(|_| format!("`{text}` is not a number"))?;
            toks.push(Tok::Num(n));
        } else if c.is_alphabetic() || c == '_' {
            let start = i;
            while i < chars.len() && (chars[i].is_alphanumeric() || chars[i] == '_') {
                i += 1;
            }
            toks.push(Tok::Ident(chars[start..i].iter().collect()));
        } else if "+-*/(),^%".contains(c) {
            toks.push(Tok::Op(c));
            i += 1;
        } else if c == '°' {
            toks.push(Tok::Ident("deg".into()));
            i += 1;
        } else if c == '"' {
            // The engine takes quoted expressions too; the quotes carry no meaning.
            i += 1;
        } else {
            return Err(format!("unexpected `{c}` in the expression"));
        }
    }
    Ok(toks)
}

fn unit_class(u: &str) -> Option<Class> {
    match u {
        "mm" | "cm" | "m" | "in" | "ft" | "mil" | "um" => Some(Class::Length),
        "deg" | "rad" | "degrees" | "radians" => Some(Class::Angle),
        _ => None,
    }
}

struct P<'a> {
    toks: &'a [Tok],
    i: usize,
    params: &'a HashMap<String, Class>,
}

impl P<'_> {
    fn peek(&self) -> Option<&Tok> {
        self.toks.get(self.i)
    }
    fn eat(&mut self, op: char) -> bool {
        if self.peek() == Some(&Tok::Op(op)) {
            self.i += 1;
            true
        } else {
            false
        }
    }

    fn sum(&mut self) -> Result<Class, String> {
        let mut a = self.product()?;
        loop {
            let op = if self.eat('+') {
                '+'
            } else if self.eat('-') {
                '-'
            } else {
                return Ok(a);
            };
            let b = self.product()?;
            a = match (&a, &b) {
                (Class::Unknown, _) | (_, Class::Unknown) => Class::Unknown,
                (Class::Number, x) | (x, Class::Number) => x.clone(),
                (x, y) if x == y => x.clone(),
                (x, y) => return Err(format!("`{op}` adds {} to {}", x.describe(), y.describe())),
            };
        }
    }

    fn product(&mut self) -> Result<Class, String> {
        let mut a = self.unary()?;
        loop {
            let op = if self.eat('*') {
                '*'
            } else if self.eat('/') {
                '/'
            } else if self.eat('%') {
                '%'
            } else {
                return Ok(a);
            };
            let b = self.unary()?;
            a = match (op, &a, &b) {
                (_, Class::Unknown, _) | (_, _, Class::Unknown) => Class::Unknown,
                ('%', x, _) => x.clone(),
                (_, x, Class::Number) => x.clone(),
                ('*', Class::Number, x) => x.clone(),
                ('/', Class::Number, x) => Class::Other(format!("the inverse of {}", x.describe())),
                ('/', x, y) if x == y => Class::Number,
                ('*', Class::Length, Class::Length) => Class::Other("an area".into()),
                (_, x, y) => Class::Other(format!("{} {op} {}", x.describe(), y.describe())),
            };
        }
    }

    fn unary(&mut self) -> Result<Class, String> {
        if self.eat('-') || self.eat('+') {
            return self.unary();
        }
        let base = self.atom()?;
        if self.eat('^') {
            let e = self.unary()?;
            return Ok(match (&base, &e) {
                (Class::Number, _) => Class::Number,
                (Class::Unknown, _) | (_, Class::Unknown) => Class::Unknown,
                (x, _) => Class::Other(format!("a power of {}", x.describe())),
            });
        }
        Ok(base)
    }

    fn atom(&mut self) -> Result<Class, String> {
        match self.toks.get(self.i).cloned() {
            Some(Tok::Num(_)) => {
                self.i += 1;
                if let Some(Tok::Ident(u)) = self.peek()
                    && let Some(c) = unit_class(u)
                {
                    self.i += 1;
                    return Ok(c);
                }
                Ok(Class::Number)
            }
            Some(Tok::Op('(')) => {
                self.i += 1;
                let c = self.sum()?;
                if !self.eat(')') {
                    return Err("missing `)`".into());
                }
                Ok(c)
            }
            Some(Tok::Ident(name)) => {
                self.i += 1;
                if self.eat('(') {
                    let mut args = Vec::new();
                    if !self.eat(')') {
                        loop {
                            args.push(self.sum()?);
                            if self.eat(')') {
                                break;
                            }
                            if !self.eat(',') {
                                return Err(format!("missing `)` after `{name}(`"));
                            }
                        }
                    }
                    return Ok(match name.as_str() {
                        "sin" | "cos" | "tan" | "exp" | "ln" | "log" | "log10" => Class::Number,
                        "asin" | "acos" | "atan" | "atan2" => Class::Angle,
                        "abs" | "floor" | "ceil" | "round" | "min" | "max" => args.first().cloned().unwrap_or(Class::Unknown),
                        _ => Class::Unknown,
                    });
                }
                match name.as_str() {
                    "pi" | "e" | "PI" | "E" | "tau" => Ok(Class::Number),
                    _ => match self.params.get(&name) {
                        Some(c) => Ok(c.clone()),
                        None => match unit_class(&name) {
                            // `mm` on its own, as in `L mm`: the engine's unit suffix.
                            Some(c) => Ok(c),
                            None => Err(format!("unknown parameter `{name}`")),
                        },
                    },
                }
            }
            Some(Tok::Op(c)) => Err(format!("unexpected `{c}` in the expression")),
            None => Err("the expression ends early".into()),
        }
    }
}

/// The class of `expr`, given the classes of the parameters defined so far. `Err` names what
/// is wrong with the expression itself (an unknown parameter, mismatched units in a sum).
pub fn classify(expr: &str, params: &HashMap<String, Class>) -> Result<Class, String> {
    let toks = lex(expr)?;
    if toks.is_empty() {
        return Err("empty expression".into());
    }
    let mut p = P { toks: &toks, i: 0, params };
    let c = p.sum()?;
    if p.i != toks.len() {
        return Err(format!(
            "unexpected `{}`",
            match &toks[p.i] {
                Tok::Num(n) => n.to_string(),
                Tok::Ident(s) => s.clone(),
                Tok::Op(c) => c.to_string(),
            }
        ));
    }
    Ok(c)
}

/// A literal `6`, `6 mm` or `-2.5 deg` as a number (in its own unit), for range checks.
pub fn literal(expr: &str) -> Option<f64> {
    let toks = lex(expr).ok()?;
    match toks.as_slice() {
        [Tok::Num(n)] => Some(*n),
        [Tok::Num(n), Tok::Ident(u)] if unit_class(u).is_some() => Some(*n),
        [Tok::Op('-'), Tok::Num(n)] => Some(-n),
        [Tok::Op('-'), Tok::Num(n), Tok::Ident(u)] if unit_class(u).is_some() => Some(-n),
        _ => None,
    }
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;

    fn env() -> HashMap<String, Class> {
        HashMap::from([("L".to_string(), Class::Length), ("n".to_string(), Class::Number), ("a".to_string(), Class::Angle)])
    }

    #[test]
    fn classes() {
        let e = env();
        assert_eq!(classify("60 mm", &e).unwrap(), Class::Length);
        assert_eq!(classify("30 deg", &e).unwrap(), Class::Angle);
        assert_eq!(classify("4", &e).unwrap(), Class::Number);
        assert_eq!(classify("L - 2*8 mm", &e).unwrap(), Class::Length);
        assert_eq!(classify("L / 2", &e).unwrap(), Class::Length);
        assert_eq!(classify("L / L", &e).unwrap(), Class::Number);
        assert_eq!(classify("(L - 2*n) * n", &e).unwrap(), Class::Length);
        assert_eq!(classify("L * L", &e).unwrap(), Class::Other("an area".into()));
        assert_eq!(classify("sin(a) * L", &e).unwrap(), Class::Length);
        assert_eq!(classify("sqrt(L)", &e).unwrap(), Class::Unknown);
        assert_eq!(classify("-L", &e).unwrap(), Class::Length);
        assert_eq!(classify("30°", &e).unwrap(), Class::Angle);
    }

    #[test]
    fn errors() {
        let e = env();
        assert!(classify("L + a", &e).unwrap_err().contains("adds a length to an angle"));
        assert!(classify("foo * 2", &e).unwrap_err().contains("unknown parameter `foo`"));
        assert!(classify("(L", &e).unwrap_err().contains("missing `)`"));
        assert!(classify("L L", &e).unwrap_err().contains("unexpected `L`"));
    }

    #[test]
    fn literals() {
        assert_eq!(literal("6"), Some(6.0));
        assert_eq!(literal("6 mm"), Some(6.0));
        assert_eq!(literal("-2.5 deg"), Some(-2.5));
        assert_eq!(literal("L"), None);
        assert_eq!(literal("2*3"), None);
    }
}
