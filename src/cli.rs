//! CLI syntax is parsed by lexopt; engine run arguments remain an untouched OsString tail.
use lexopt::prelude::*;
use std::ffi::OsString;

#[derive(Debug)]
pub struct Args {
    pub command: String,
    pub readable: bool,
    pub engine_check: bool,
    pub probe: Option<String>,
    pub path: Option<String>,
    pub output: Option<String>,
    pub requests: Option<String>,
    pub expect: Option<String>,
    pub previews: bool,
    pub json: bool,
    pub connect: String,
    pub token: Option<String>,
    pub interval: u64,
    pub once: bool,
    pub engine_args: Vec<OsString>,
}

pub fn parse(mut parser: lexopt::Parser) -> Result<Args, lexopt::Error> {
    let mut a = Args {
        command: String::new(),
        readable: false,
        engine_check: false,
        probe: None,
        path: None,
        output: None,
        requests: None,
        expect: None,
        previews: true,
        json: false,
        connect: "127.0.0.1:7878".into(),
        token: None,
        interval: 250,
        once: false,
        engine_args: vec![],
    };
    while let Some(arg) = parser.next()? {
        match arg {
            Long("help") | Short('h') => {
                a.command = "help".into();
                return Ok(a);
            }
            Value(v) if a.command.is_empty() => {
                a.command = v.string()?;
                if !["compile", "check", "explain", "run", "decompile", "watch", "evaluate", "observe", "capabilities", "pack", "roundtrip"]
                    .contains(&a.command.as_str())
                {
                    return Err(format!("unknown command: {}", a.command).into());
                }
            }
            Value(v) if a.path.is_none() && a.command != "capabilities" => {
                a.path = Some(v.string()?);
                if a.command == "run" {
                    a.engine_args = parser.raw_args()?.collect();
                    break;
                }
            }
            Short('o') | Long("output") if matches!(a.command.as_str(), "compile" | "decompile" | "pack") => {
                a.output = Some(parser.value()?.string()?)
            }
            Long("output-dir") if matches!(a.command.as_str(), "evaluate" | "observe" | "roundtrip") => a.output = Some(parser.value()?.string()?),
            Long("requests") if matches!(a.command.as_str(), "evaluate" | "observe") => a.requests = Some(parser.value()?.string()?),
            Long("expect") if a.command == "evaluate" => a.expect = Some(parser.value()?.string()?),
            Long("no-previews") if matches!(a.command.as_str(), "evaluate" | "observe") => a.previews = false,
            Long("json") if matches!(a.command.as_str(), "evaluate" | "observe" | "capabilities" | "explain" | "roundtrip") => a.json = true,
            Long("connect") if a.command == "watch" => a.connect = parser.value()?.string()?,
            Long("token") if a.command == "watch" => a.token = Some(parser.value()?.string()?),
            Long("interval") if a.command == "watch" => {
                a.interval = parser.value()?.parse()?;
                if a.interval == 0 {
                    return Err("--interval must be positive".into());
                }
            }
            Long("readable") if a.command == "decompile" => a.readable = true,
            Long("engine-check") if a.command == "roundtrip" => a.engine_check = true,
            Long("probe") if a.command == "roundtrip" => {
                a.probe = Some(parser.value()?.string()?);
                a.engine_check = true;
            }
            Long("once") if a.command == "watch" => a.once = true,
            _ => return Err(arg.unexpected()),
        }
    }
    if a.command.is_empty() {
        a.command = "help".into();
    }
    if !matches!(a.command.as_str(), "help" | "capabilities") && a.path.is_none() {
        return Err("missing input file".into());
    }
    if matches!(a.command.as_str(), "evaluate" | "observe" | "roundtrip") && a.output.is_none() {
        return Err("--output-dir is required".into());
    }
    Ok(a)
}

#[cfg(test)]
#[allow(clippy::unwrap_used)]
mod tests {
    use super::*;
    fn args(s: &[&str]) -> Result<Args, lexopt::Error> {
        parse(lexopt::Parser::from_args(s))
    }
    #[test]
    fn lexopt_parses_equals_short_options_and_end_of_options() {
        let a = args(&["evaluate", "--output-dir=build", "--requests=views.json", "--json", "--", "-part.sc"]).unwrap();
        assert_eq!(a.path.as_deref(), Some("-part.sc"));
        assert_eq!(a.output.as_deref(), Some("build"));
        assert!(a.json);
        assert_eq!(args(&["compile", "-oout.json", "part.sc"]).unwrap().output.as_deref(), Some("out.json"));
        assert_eq!(args(&["observe", "--help"]).unwrap().command, "help");
    }
    #[test]
    fn malformed_arguments_fail_and_engine_tail_is_preserved() {
        for a in [
            &["evaluate", "part.sc", "--output-dir"][..],
            &["check", "part.sc", "--typo"],
            &["watch", "part.sc", "--interval=bad"],
            &["watch", "part.sc", "--interval=0"],
            &["check", "a.sc", "b.sc"],
            &["unknown", "a.sc"],
        ] {
            assert!(args(a).is_err(), "{a:?}");
        }
        let a = args(&["run", "a.sc", "--out", "a.step", "--future-option=x"]).unwrap();
        assert_eq!(a.engine_args, vec!["--out", "a.step", "--future-option=x"]);
        assert_eq!(args(&["capabilities", "--json"]).unwrap().command, "capabilities");
    }
}
