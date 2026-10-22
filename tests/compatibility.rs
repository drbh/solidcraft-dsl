//! Opt-in gate for a full compatibility run: unsupported capabilities must not skip.
#![allow(clippy::unwrap_used)]
use serde_json::Value;
use std::process::Command;

#[test]
fn full_engine_capabilities_are_present() {
    if std::env::var_os("SOLIDCRAFT_REQUIRE_FULL_SUPPORT").is_none() {
        return;
    }
    let cli = std::env::var("SOLIDCRAFT_CLI").unwrap_or_else(|_| "solidcraft-cli".into());
    let out = Command::new(&cli).arg("commands").output().unwrap();
    assert!(out.status.success(), "engine discovery failed: {}", String::from_utf8_lossy(&out.stderr));
    let commands: Value = serde_json::from_slice(&out.stdout).unwrap();
    let mut missing = vec![];
    for (id, field) in [
        ("sketch.point", "expression"),
        ("component.create", "occurrence_name"),
        ("occurrence.copy", "occurrence_name"),
        ("occurrence.properties", "world_name"),
    ] {
        if !commands.as_array().unwrap().iter().any(|c| c["id"] == id && c["params"].as_str().is_some_and(|p| p.contains(field))) {
            missing.push(format!("{id}: {field}"));
        }
    }
    assert!(missing.is_empty(), "{cli} cannot run the complete DSL suite; missing capabilities: {}", missing.join(", "));
}
