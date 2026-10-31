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

use serde_json::{Value, json};
use semsitter_bridge::server::serve;

fn exchange(requests: &[Value]) -> Vec<Value> {
  let input: String = requests.iter().map(|r| format!("{r}\n")).collect();
  let mut output = Vec::new();
  serve(input.as_bytes(), &mut output).unwrap();
  String::from_utf8(output).unwrap().lines().map(|l| serde_json::from_str(l).unwrap()).collect()
}

#[test]
fn analyze_then_cst_then_quit() {
  let dir = tempfile::tempdir().unwrap();
  let file = dir.path().join("A.java");
  std::fs::write(&file, "class A { int f; int g() { return f; } }").unwrap();
  let responses = exchange(&[
    json!({"id": 1, "op": "analyze", "root": dir.path(), "files": [file], "classpath": []}),
    json!({"id": 2, "op": "cst", "path": file}),
    json!({"id": 3, "op": "cst", "path": "Missing.java"}),
    json!({"id": 4, "op": "quit"}),
    json!({"id": 5, "op": "parse", "source": "class B {}"}),
  ]);
  assert_eq!(responses.len(), 4);
  assert_eq!(responses[0]["files"], json!(["A.java"]));
  assert_eq!(responses[1]["id"], 2);
  assert_eq!(responses[1]["cst"]["symbols"].as_array().unwrap().iter().filter(|s| s["name"] == "f").count(), 1);
  assert_eq!(responses[2]["ok"], false);
  assert_eq!(responses[3], json!({"id": 4, "ok": true}));
}

#[test]
fn parse_needs_no_analysis_and_errors_keep_the_server_alive() {
  let responses = exchange(&[
    json!({"id": 1, "op": "cst", "path": "A.java"}),
    json!({"id": 2, "op": "nope"}),
    json!({"id": 3, "op": "parse", "source": "class B {}"}),
  ]);
  assert_eq!(responses[0]["ok"], false);
  assert_eq!(responses[1]["error"], "unknown op: nope");
  assert_eq!(responses[2]["cst"]["tree"]["k"], "program");
}
