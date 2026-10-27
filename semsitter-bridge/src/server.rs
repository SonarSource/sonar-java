/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */

//! JSONL protocol: one request object per stdin line, one response object per stdout line.
//!
//! - `{"id":1,"op":"analyze","root":"/p","files":["/p/A.java"],"classpath":["/lib.jar"]}`
//!   scans the batch and answers `{"id":1,"ok":true,"root":"/p","files":["A.java"]}`.
//! - `{"id":2,"op":"cst","path":"/p/A.java"}` answers `{"id":2,"ok":true,"cst":{...}}` with the
//!   CST of a file of the last `analyze`.
//! - `{"id":3,"op":"types","names":["java.util.List"]}` answers `{"id":3,"ok":true,"types":{...}}`
//!   with the declarations (kind, supertypes, members) of the named types and their supertypes.
//! - `{"id":4,"op":"parse","source":"class A {}"}` answers the CST without semantics.
//! - `{"id":5,"op":"quit"}` answers `{"id":5,"ok":true}` and stops the server.
//!
//! Failures answer `{"id":n,"ok":false,"error":"..."}` and keep the server running.

use std::io::{BufRead, Write};
use std::path::{Path, PathBuf};

use anyhow::{Result, anyhow};
use serde_json::{Map, Value, json};

use crate::{Analysis, analyze, parse_only};

#[derive(Default)]
pub struct Server {
  analysis: Option<Analysis>,
}

impl Server {
  /// Answers one request line, and whether the server must stop.
  pub fn handle(&mut self, line: &str) -> (Value, bool) {
    let request: Value = match serde_json::from_str(line) {
      Ok(request) => request,
      Err(e) => return (json!({"ok": false, "error": format!("invalid request: {e}")}), false),
    };
    let id = request.get("id").cloned().unwrap_or(Value::Null);
    let op = request.get("op").and_then(Value::as_str).unwrap_or_default();
    let quit = op == "quit";
    let mut response = match self.dispatch(op, &request) {
      Ok(body) => body,
      Err(e) => json!({"ok": false, "error": format!("{e:#}")}),
    };
    response.as_object_mut().expect("response is an object").insert("id".into(), id);
    (response, quit)
  }

  fn dispatch(&mut self, op: &str, request: &Value) -> Result<Value> {
    match op {
      "analyze" => {
        let root = PathBuf::from(string(request, "root")?);
        let files: Vec<PathBuf> = strings(request, "files").into_iter().map(PathBuf::from).collect();
        let classpath = strings(request, "classpath");
        let analysis = analyze(&root, &files, &classpath)?;
        let body = json!({
          "ok": true,
          "root": analysis.root().to_string_lossy(),
          "files": analysis.files(),
        });
        self.analysis = Some(analysis);
        Ok(body)
      }
      "cst" => {
        let analysis = self.analysis.as_ref().ok_or_else(|| anyhow!("no analyze request yet"))?;
        Ok(ok_with("cst", analysis.cst(string(request, "path")?)?))
      }
      "types" => {
        let analysis = self.analysis.as_ref().ok_or_else(|| anyhow!("no analyze request yet"))?;
        Ok(ok_with("types", analysis.types(&strings(request, "names"))))
      }
      "parse" => Ok(ok_with("cst", parse_only(string(request, "source")?.as_bytes())?)),
      "quit" => Ok(json!({"ok": true})),
      other => Err(anyhow!("unknown op: {other}")),
    }
  }
}

/// Serves requests from `input` until `quit` or end of input.
pub fn serve(input: impl BufRead, mut output: impl Write) -> Result<()> {
  let mut server = Server::default();
  for line in input.lines() {
    let line = line?;
    if line.trim().is_empty() {
      continue;
    }
    let (response, quit) = server.handle(&line);
    serde_json::to_writer(&mut output, &response)?;
    output.write_all(b"\n")?;
    output.flush()?;
    if quit {
      break;
    }
  }
  Ok(())
}

/// CSTs of `files` (with semantics), one JSON object per line.
pub fn print_csts(root: &Path, files: &[PathBuf], classpath: &[String], mut output: impl Write) -> Result<()> {
  let analysis = analyze(root, files, classpath)?;
  for file in analysis.files() {
    serde_json::to_writer(&mut output, &analysis.cst(file)?)?;
    output.write_all(b"\n")?;
  }
  output.flush()?;
  Ok(())
}

fn ok_with(key: &str, value: Value) -> Value {
  let mut body = Map::new();
  body.insert("ok".into(), Value::Bool(true));
  body.insert(key.into(), value);
  Value::Object(body)
}

fn string<'a>(request: &'a Value, key: &str) -> Result<&'a str> {
  request.get(key).and_then(Value::as_str).ok_or_else(|| anyhow!("missing string field: {key}"))
}

fn strings(request: &Value, key: &str) -> Vec<String> {
  request
    .get(key)
    .and_then(Value::as_array)
    .into_iter()
    .flatten()
    .filter_map(|v| v.as_str().map(str::to_string))
    .collect()
}
