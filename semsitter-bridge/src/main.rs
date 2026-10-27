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

use std::io::Write;
use std::path::PathBuf;

use anyhow::{Result, anyhow, bail};
use semsitter_bridge::server::{print_csts, serve};

const USAGE: &str = "\
usage:
  semsitter-bridge [serve]                                   JSONL server on stdin/stdout
  semsitter-bridge cst --root DIR [--classpath CP] FILE...   CST with semantics, one JSON per file
  semsitter-bridge parse FILE                                CST without semantics
CP uses the platform path separator.";

fn main() -> Result<()> {
  let args: Vec<String> = std::env::args().skip(1).collect();
  let stdout = std::io::stdout().lock();
  match args.first().map(String::as_str) {
    None | Some("serve") => serve(std::io::stdin().lock(), stdout),
    Some("cst") => cst(&args[1..], stdout),
    Some("parse") => {
      let file = args.get(1).ok_or_else(|| anyhow!(USAGE))?;
      let cst = semsitter_bridge::parse_only(&std::fs::read(file)?)?;
      let mut stdout = stdout;
      serde_json::to_writer(&mut stdout, &cst)?;
      writeln!(stdout)?;
      Ok(())
    }
    Some("-h" | "--help") => {
      println!("{USAGE}");
      Ok(())
    }
    Some(other) => bail!("unknown command: {other}\n{USAGE}"),
  }
}

fn cst(args: &[String], output: impl Write) -> Result<()> {
  let mut root = None;
  let mut classpath = Vec::new();
  let mut files = Vec::new();
  let mut args = args.iter();
  while let Some(arg) = args.next() {
    match arg.as_str() {
      "--root" => root = args.next().map(PathBuf::from),
      "--classpath" => {
        let value = args.next().ok_or_else(|| anyhow!(USAGE))?;
        classpath.extend(std::env::split_paths(value).map(|p| p.to_string_lossy().into_owned()));
      }
      file => files.push(PathBuf::from(file)),
    }
  }
  let root = root.ok_or_else(|| anyhow!(USAGE))?;
  print_csts(&root, &files, &classpath, output)
}
