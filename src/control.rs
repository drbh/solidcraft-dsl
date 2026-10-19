//! A client for the app's control channel: JSON lines on a TCP socket, the first request
//! carrying the launch token (docs/control-protocol.md).

use std::io::{BufRead, BufReader, Write};
use std::net::TcpStream;
use std::path::PathBuf;
use std::time::Duration;

use serde_json::{Value, json};

pub struct Control {
    stream: TcpStream,
    reader: BufReader<TcpStream>,
    token: Option<String>,
    next_id: u64,
}

/// Where the app wrote the token for `port` on this platform.
pub fn token_path(port: u16) -> Option<PathBuf> {
    let home = std::env::var_os("HOME").map(PathBuf::from);
    let dir = if cfg!(target_os = "macos") {
        home?.join("Library/Application Support/SolidCraft/control")
    } else if cfg!(target_os = "windows") {
        PathBuf::from(std::env::var_os("LOCALAPPDATA")?).join("SolidCraft/control")
    } else {
        match std::env::var_os("XDG_DATA_HOME") {
            Some(d) => PathBuf::from(d).join("solidcraft/control"),
            None => home?.join(".local/share/solidcraft/control"),
        }
    };
    Some(dir.join(format!("{port}.token")))
}

/// The token for `addr`: `SOLIDCRAFT_CONTROL_TOKEN`, else the app's token file.
pub fn find_token(addr: &str) -> Result<String, String> {
    if let Ok(t) = std::env::var("SOLIDCRAFT_CONTROL_TOKEN") {
        return Ok(t.trim().to_string());
    }
    let port: u16 = addr.rsplit(':').next().and_then(|p| p.parse().ok()).ok_or_else(|| format!("`{addr}` has no port"))?;
    let path = token_path(port).ok_or("cannot work out the token file's location")?;
    std::fs::read_to_string(&path)
        .map(|t| t.trim().to_string())
        .map_err(|e| format!("{}: {e} (is the app running with --control {port}? or set SOLIDCRAFT_CONTROL_TOKEN)", path.display()))
}

impl Control {
    pub fn connect(addr: &str, token: Option<String>) -> Result<Control, String> {
        let stream = TcpStream::connect(addr).map_err(|e| format!("cannot connect to the app at {addr}: {e}"))?;
        stream.set_read_timeout(Some(Duration::from_secs(120))).map_err(|e| e.to_string())?;
        let reader = BufReader::new(stream.try_clone().map_err(|e| e.to_string())?);
        Ok(Control { stream, reader, token, next_id: 1 })
    }

    /// One request, its result; an error reply becomes `Err`.
    pub fn call(&mut self, method: &str, params: Value) -> Result<Value, String> {
        let id = self.next_id;
        self.next_id += 1;
        let mut msg = json!({"id": id, "method": method, "params": params});
        if let Some(t) = self.token.take() {
            msg["token"] = json!(t);
        }
        let mut line = serde_json::to_string(&msg).map_err(|e| e.to_string())?;
        line.push('\n');
        self.stream.write_all(line.as_bytes()).map_err(|e| format!("the app went away: {e}"))?;
        let mut reply = String::new();
        loop {
            reply.clear();
            let n = self.reader.read_line(&mut reply).map_err(|e| format!("the app went away: {e}"))?;
            if n == 0 {
                return Err("the app closed the connection".into());
            }
            let v: Value = serde_json::from_str(&reply).map_err(|e| format!("bad reply from the app: {e}"))?;
            if v.get("id") != Some(&json!(id)) {
                continue; // a notification or a stray reply
            }
            if let Some(e) = v.get("error") {
                let msg = e.get("message").and_then(Value::as_str).unwrap_or("error");
                return Err(format!("{method}: {msg}"));
            }
            return Ok(v.get("result").cloned().unwrap_or(Value::Null));
        }
    }
}
