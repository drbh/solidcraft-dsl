//! `solidcraft-dsl compile|check|run <file.sc>`
#![deny(clippy::unwrap_used, clippy::expect_used, clippy::panic, clippy::unimplemented, clippy::todo, clippy::unreachable)]
#![forbid(unsafe_code)]

mod cli;

use std::process::ExitCode;
use std::time::Duration;

const USAGE: &str = "usage:
  solidcraft-dsl compile <design.sc> [-o script.json]   concise: command script; native: document/package (-o required)
  solidcraft-dsl explain <design.sc> [--json]           resolved expressions, dependencies and persistence
  solidcraft-dsl check <design.sc>                      parse and compile, report what it holds
  solidcraft-dsl evaluate <design.sc> --output-dir DIR [--json] [--no-previews] [--expect checks.json] [--requests requests.json]
      headless build, measurements, diagnostics, review images and revision-linked artifacts
  solidcraft-dsl observe <report.json|design.solidcraft> --output-dir DIR [--requests requests.json] [--json] [--no-previews]
      gather more views, exports and engine query results from an existing build
  solidcraft-dsl capabilities [--json]
      request JSON Schema, defaults and live engine command descriptions
  solidcraft-dsl run <design.sc> [solidcraft-cli run options…]
      compile, then `solidcraft-cli run` it (SOLIDCRAFT_CLI names the binary; default: solidcraft-cli on PATH),
      e.g. `solidcraft-dsl run part.sc --save part.solidcraft --out part.step`
  solidcraft-dsl decompile <design.solidcraft|design.json> [-o design.sc] [--readable]
      lossless editable native .sc with embedded assets; --readable opts into limited concise reconstruction
  solidcraft-dsl pack <design.sc> -o design.solidcraft
      reconstruct a native document directly, or build concise DSL through the engine
  solidcraft-dsl roundtrip <design.solidcraft|design.json> --output-dir DIR [--json] [--engine-check] [--probe script.json]
      verify document and asset preservation, optionally paired engine evaluation and edit probes
  solidcraft-dsl watch <design.sc> [--connect 127.0.0.1:7878] [--token T] [--interval MS] [--once]
      build the file in the running app (solidcraft --control PORT), rebuild it whenever the file is saved,
      and write parameter and dimension values changed in the app back to the file's lines
";

fn main() -> ExitCode {
    match cli::parse(lexopt::Parser::from_env()).map_err(|e| e.to_string()).and_then(run) {
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

/// Lossless native source is the default; concise reconstruction is explicit.
fn decompile(path: &str, output: Option<&str>, readable: bool) -> Result<(), String> {
    let mut native = solidcraft_dsl::native::Native::read(std::path::Path::new(path))?;
    let sc = if readable {
        if native.container.is_some() {
            let dir = temp_dir()?;
            let resolved = dir.join("resolved.json");
            let script = dir.join("export.json");
            let commands = serde_json::json!({"commands":[{"command":"file.export","params":{"path":resolved,"format":"json"}}]});
            std::fs::write(&script, commands.to_string()).map_err(|e| e.to_string())?;
            let result = std::process::Command::new(solidcraft_cli()).arg("run").arg(&script).args(["--in", path, "--quiet"]).output();
            let parsed = match result {
                Ok(out) if out.status.success() => solidcraft_dsl::native::Native::read(&resolved),
                Ok(out) => Err(format!("cannot resolve package for --readable: {}", String::from_utf8_lossy(&out.stderr))),
                Err(e) => Err(e.to_string()),
            };
            let _ = std::fs::remove_dir_all(dir);
            native = parsed?;
        }
        eprintln!("note: --readable reconstructs supported modeling operations only; use default decompile for complete document preservation");
        solidcraft_dsl::decompile_readable(&native.document)?
    } else {
        native.to_source()?
    };
    match output {
        Some(out) => std::fs::write(out, sc).map_err(|e| format!("{out}: {e}")),
        None => {
            print!("{sc}");
            Ok(())
        }
    }
}

fn run(args: cli::Args) -> Result<(), String> {
    let sub = args.command.as_str();
    if sub == "help" {
        print!("{USAGE}");
        return Ok(());
    }
    if sub == "capabilities" {
        let value = solidcraft_dsl::artifacts::capabilities(&solidcraft_cli())?;
        println!("{}", serde_json::to_string_pretty(&value).map_err(|e| e.to_string())?);
        return Ok(());
    }
    let path = args.path.as_deref().ok_or("missing input file")?;
    if sub == "decompile" {
        return decompile(path, args.output.as_deref(), args.readable);
    }
    if sub == "roundtrip" {
        let report = solidcraft_dsl::native::roundtrip(
            std::path::Path::new(path),
            std::path::Path::new(args.output.as_deref().ok_or("--output-dir required")?),
            args.engine_check.then(solidcraft_cli),
            args.probe.as_deref().map(std::path::Path::new),
        )?;
        println!("{}", serde_json::to_string_pretty(&report).map_err(|e| e.to_string())?);
        return if report["ok"] == true { Ok(()) } else { Err("roundtrip verification failed; see report".into()) };
    }
    if matches!(sub, "evaluate" | "observe") {
        let output = args.output.as_deref().ok_or("--output-dir is required")?;
        let report = if sub == "observe" {
            solidcraft_dsl::evaluate::observe(
                std::path::Path::new(path),
                std::path::Path::new(output),
                &solidcraft_cli(),
                args.requests.as_deref().map(std::path::Path::new),
                args.previews,
            )?
        } else {
            solidcraft_dsl::evaluate::evaluate(&solidcraft_dsl::evaluate::Options {
                source: path.into(),
                output: output.into(),
                cli: solidcraft_cli(),
                previews: args.previews,
                expectations: args.expect.map(Into::into),
                requests: args.requests.map(Into::into),
            })?
        };
        if args.json {
            println!("{}", serde_json::to_string_pretty(&report).map_err(|e| e.to_string())?);
        } else {
            println!("{}: {}\nreport: {}", path, if report["ok"] == true { "passed" } else { "failed" }, report["report"].as_str().unwrap_or(""));
            for d in report["diagnostics"].as_array().into_iter().flatten() {
                println!("{}: {}", d["severity"].as_str().unwrap_or("error"), d["message"].as_str().unwrap_or(""));
            }
        }
        return if report["ok"] == true { Ok(()) } else { Err(format!("{sub} failed; see report diagnostics")) };
    }
    if sub == "watch" {
        return solidcraft_dsl::watch::watch(solidcraft_dsl::watch::Options {
            path: path.into(),
            addr: args.connect,
            token: args.token,
            interval: Duration::from_millis(args.interval),
            once: args.once,
        });
    }
    let src = read(path)?;
    let c = solidcraft_dsl::build(&src).map_err(|e| format!("{path}:{}", e.strip_prefix("line ").unwrap_or(&e)))?;
    if let Some(native) = &c.native {
        match sub {
            "compile" | "pack" => {
                let out = args.output.as_deref().ok_or("native compilation requires -o FILE.solidcraft; use `explain` for the document inventory")?;
                return native.write(std::path::Path::new(out));
            }
            "check" => {
                println!("{path}: lossless native source, {} sketches, {} features; engine validation requires evaluate", c.sketches, c.features);
                return Ok(());
            }
            "run" => {
                let dir = temp_dir()?;
                let input = dir.join("input.solidcraft");
                native.write(&input)?;
                let result =
                    std::process::Command::new(solidcraft_cli()).arg("run").arg(&input).args(&args.engine_args).status().map_err(|e| e.to_string());
                let _ = std::fs::remove_dir_all(dir);
                return if result?.success() { Ok(()) } else { Err("native engine run failed".into()) };
            }
            _ => {}
        }
    }
    let text = serde_json::to_string_pretty(&c.script).map_err(|e| e.to_string())?;
    match sub {
        "explain" => {
            println!("{}", serde_json::to_string_pretty(&c.expressions).map_err(|e| e.to_string())?);
            Ok(())
        }
        "compile" => {
            match args.output.as_deref() {
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
        "run" | "pack" => {
            if sub == "pack" && args.output.is_none() {
                return Err("pack requires -o FILE.solidcraft".into());
            }
            let cli = solidcraft_cli();
            solidcraft_dsl::assembly::check_engine(&cli, &c)?;
            let dir = temp_dir()?;
            let script = dir.join("script.json");
            std::fs::write(&script, text).map_err(|e| format!("{}: {e}", script.display()))?;
            let mut command = std::process::Command::new(&cli);
            command.arg("run").arg(&script).args(&args.engine_args);
            if sub == "pack" {
                command.arg("--save").arg(args.output.as_deref().ok_or("pack requires -o")?);
            }
            let status = command.status().map_err(|e| format!("cannot run `{cli}`: {e} (set SOLIDCRAFT_CLI to the solidcraft-cli binary)"))?;
            let _ = std::fs::remove_dir_all(&dir);
            if status.success() { Ok(()) } else { Err(format!("`{cli} run` failed")) }
        }
        _ => Err(USAGE.into()),
    }
}
