//! Unit checks use SolidCraft's evaluator; there is no separate arithmetic grammar.
use solidcraft_expr::{Kind, Value, eval_with};
use std::collections::HashMap;

#[derive(Debug, Clone, PartialEq)]
pub enum Class {
    Number,
    Length,
    Angle,
    Other(String),
}
impl Class {
    pub fn describe(&self) -> String {
        match self {
            Self::Number => "a number".into(),
            Self::Length => "a length".into(),
            Self::Angle => "an angle".into(),
            Self::Other(s) => s.clone(),
        }
    }
    pub fn fits(&self, want: &Class) -> bool {
        matches!(self, Self::Number) || self == want
    }
    pub fn of(v: Value) -> Self {
        match v.kind() {
            Some(Kind::Length) => Self::Length,
            Some(Kind::Angle) => Self::Angle,
            Some(Kind::Unitless) => Self::Number,
            None if v.len == 2 && v.ang == 0 => Self::Other("an area".into()),
            None => Self::Other(v.describe()),
        }
    }
}
pub fn evaluate(expr: &str, params: &HashMap<String, Value>) -> Result<Value, String> {
    eval_with(expr, &|n| params.get(n).copied().ok_or_else(|| solidcraft_expr::Error(format!("unknown parameter `{n}`")))).map_err(|e| e.0)
}
pub fn classify(expr: &str, params: &HashMap<String, Value>) -> Result<Class, String> {
    evaluate(expr, params).map(Class::of)
}
pub fn literal(expr: &str) -> Option<f64> {
    if !solidcraft_expr::is_literal(expr) {
        return None;
    }
    evaluate(expr, &HashMap::new()).ok().map(|v| v.v)
}
#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    #[test]
    fn arithmetic_and_dimensions_share_engine_semantics() {
        let e = HashMap::from([("L".into(), Value::length(40.)), ("n".into(), Value::num(2.)), ("a".into(), Value::angle(0.5))]);
        assert_eq!(classify("sqrt(L^2 + (3 mm)^2)", &e).unwrap(), Class::Length);
        assert_eq!(classify("L*L", &e).unwrap(), Class::Other("an area".into()));
        assert_eq!(classify("sin(a)*L", &e).unwrap(), Class::Length);
        assert_eq!(classify("L/L", &e).unwrap(), Class::Number);
        for invalid in ["L+a", "sqrt(L)", "unknown(2)", "1/0", "sqrt(-1)", "foo*2"] {
            assert!(classify(invalid, &e).is_err(), "{invalid}");
        }
        assert_eq!(literal("2 cm"), Some(20.));
    }
}
