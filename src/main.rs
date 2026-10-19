//! `solidcraft-dsl compile|check|run <file.sc>`
#![deny(clippy::unwrap_used, clippy::expect_used, clippy::panic, clippy::unimplemented, clippy::todo, clippy::unreachable)]
#![forbid(unsafe_code)]

use std::process::ExitCode;
use std::time::Duration;

const USAGE: &str = "usage:
  solidcraft-dsl compile <design.sc> [-o script.json]   write the SolidCraft command script (stdout by default)
  solidcraft-dsl check <design.sc>                      parse and compile, report what it holds
  solidcraft-dsl run <design.sc> [solidcraft-cli run options…]
      compile, then `solidcraft-cli run` it (SOLIDCRAFT_CLI names the binary; default: solidcraft-cli on PATH),
      e.g. `solidcraft-dsl run part.sc --save part.solidcraft --out part.step`
  solidcraft-dsl decompile <design.solidcraft|design.json> [-o design.sc]
      the design as .sc text (a .solidcraft container is converted to JSON through solidcraft-cli first)
  solidcraft-dsl watch <design.sc> [--connect 127.0.0.1:7878] [--token T] [--interval MS] [--once]
      build the file in the running app (solidcraft --control PORT), rebuild it whenever the file is saved,
      and write parameter and dimension values changed in the app back to the file's lines
";

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    match run(&args) {
        Ok(()) => ExitCode::SUCCESS,
        Err(e) => {
            eprintln!("error: {e}");
            ExitCode::FAILURE
        }
    }
}

fn read(path: &str) -> Result<String, String> {
    std::fs::read_to_string(path).map_err(|e| format!("{path}: {e}"))
}

fn solidcraft_cli() -> String {
    std::env::var("SOLIDCRAFT_CLI").unwrap_or_else(|_| "solidcraft-cli".into())
}

fn temp_dir() -> Result<std::path::PathBuf, String> {
    let dir = std::env::temp_dir().join(format!("solidcraft-dsl-{}", std::process::id()));
    std::fs::create_dir_all(&dir).map_err(|e| format!("{}: {e}", dir.display()))?;
    Ok(dir)
}

/// `decompile`: read the design as JSON (converting a container through solidcraft-cli) and write `.sc`.
fn decompile(path: &str, args: &[String]) -> Result<(), String> {
    let bytes = std::fs::read(path).map_err(|e| format!("{path}: {e}"))?;
    let design: serde_json::Value = match serde_json::from_slice(&bytes) {
        Ok(v) => v,
        Err(_) => {
            let cli = solidcraft_cli();
            let dir = temp_dir()?;
            let json = dir.join("design.json");
            let status = std::process::Command::new(&cli)
                .args(["run", path, "--save"])
                .arg(&json)
                .arg("--quiet")
                .status()
                .map_err(|e| format!("cannot run `{cli}`: {e} (set SOLIDCRAFT_CLI, or save the design as JSON)"))?;
            if !status.success() {
                return Err(format!("`{cli} run {path} --save design.json` failed"));
            }
            let text = std::fs::read(&json).map_err(|e| format!("{}: {e}", json.display()))?;
            let _ = std::fs::remove_dir_all(&dir);
            serde_json::from_slice(&text).map_err(|e| format!("{path}: not a JSON design: {e}"))?
        }
    };
    let sc = solidcraft_dsl::decompile(&design).map_err(|e| format!("{path}: {e}"))?;
    match args.iter().position(|a| a == "-o").and_then(|i| args.get(i + 1)) {
        Some(out) => std::fs::write(out, &sc).map_err(|e| format!("{out}: {e}")),
        None => {
            print!("{sc}");
            Ok(())
        }
    }
}

fn run(args: &[String]) -> Result<(), String> {
    let (Some(sub), Some(path)) = (args.first(), args.get(1)) else { return Err(USAGE.into()) };
    if sub == "decompile" {
        return decompile(path, &args[2..]);
    }
    if sub == "watch" {
        let rest = &args[2..];
        let after = |flag: &str| rest.iter().position(|a| a == flag).and_then(|i| rest.get(i + 1)).cloned();
        let interval = match after("--interval") {
            Some(ms) => Duration::from_millis(ms.parse().map_err(|_| format!("--interval {ms}: not a number of milliseconds"))?),
            None => Duration::from_millis(250),
        };
        return solidcraft_dsl::watch::watch(solidcraft_dsl::watch::Options {
            path: path.clone(),
            addr: after("--connect").unwrap_or_else(|| "127.0.0.1:7878".into()),
            token: after("--token"),
            interval,
            once: rest.iter().any(|a| a == "--once"),
        });
    }
    let src = read(path)?;
    let c = solidcraft_dsl::build(&src).map_err(|e| format!("{path}:{}", e.strip_prefix("line ").unwrap_or(&e)))?;
    let text = serde_json::to_string_pretty(&c.script).map_err(|e| e.to_string())?;
    match sub.as_str() {
        "compile" => {
            match args.iter().position(|a| a == "-o").and_then(|i| args.get(i + 1)) {
                Some(out) => std::fs::write(out, text + "\n").map_err(|e| format!("{out}: {e}"))?,
                None => println!("{text}"),
            }
            Ok(())
        }
        "check" => {
            let n = c.script["commands"].as_array().map_or(0, Vec::len);
            println!(
                "{path}: {} sketch{}, {} feature{}, {n} commands",
                c.sketches,
                if c.sketches == 1 { "" } else { "es" },
                c.features,
                if c.features == 1 { "" } else { "s" }
            );
            Ok(())
        }
        "run" => {
            let cli = solidcraft_cli();
            let dir = temp_dir()?;
            let script = dir.join("script.json");
            std::fs::write(&script, text).map_err(|e| format!("{}: {e}", script.display()))?;
            let status = std::process::Command::new(&cli)
                .arg("run")
                .arg(&script)
                .args(&args[2..])
                .status()
                .map_err(|e| format!("cannot run `{cli}`: {e} (set SOLIDCRAFT_CLI to the solidcraft-cli binary)"))?;
            let _ = std::fs::remove_dir_all(&dir);
            if status.success() { Ok(()) } else { Err(format!("`{cli} run` failed")) }
        }
        _ => Err(USAGE.into()),
    }
}
