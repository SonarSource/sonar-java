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

use std::io::{BufRead, BufReader, Write};
use std::process::{Command, Stdio};

#[test]
fn bridge_speaks_jsonl_on_stdin_stdout() {
    let mut child = Command::new(env!("CARGO_BIN_EXE_semsitter-bridge"))
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .unwrap();
    let mut stdin = child.stdin.take().unwrap();
    let mut stdout = BufReader::new(child.stdout.take().unwrap());

    writeln!(stdin, r#"{{"id":1,"op":"parse","source":"hello"}}"#).unwrap();
    stdin.flush().unwrap();
    let mut line = String::new();
    stdout.read_line(&mut line).unwrap();
    let parsed: serde_json::Value = serde_json::from_str(&line).unwrap();
    assert_eq!(parsed["id"], 1);
    assert_eq!(parsed["ok"], true);
    assert_eq!(parsed["hasError"], false);
    assert_eq!(parsed["tree"]["kind"], "source_file");
    assert_eq!(parsed["tree"]["children"][0]["kind"], "hello");
    assert_eq!(parsed["tree"]["startRow"], 0);

    writeln!(stdin, r#"{{"id":2,"op":"quit"}}"#).unwrap();
    stdin.flush().unwrap();
    line.clear();
    stdout.read_line(&mut line).unwrap();
    let quit: serde_json::Value = serde_json::from_str(&line).unwrap();
    assert_eq!(quit["id"], 2);
    assert_eq!(quit["ok"], true);
    assert!(child.wait().unwrap().success());
}
