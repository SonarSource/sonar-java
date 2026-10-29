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
use semsitter_bridge::server::{print_csts, serve_jsonl, serve_shm};

const USAGE: &str = "\
usage:
  semsitter-bridge serve [--shm | --jsonl] [--dump-json DIR]
  semsitter-bridge cst --root DIR [--classpath CP] [--dump-json DIR] FILE...
  semsitter-bridge parse [--dump-json DIR] FILE
CP uses the platform path separator.
--shm serves protobuf over shared memory, with one ping byte on stdin/stdout.
--jsonl is the line-delimited JSON server.
--dump-json writes udg.json, semantic-dump JSON and annotated CST JSON under DIR.";

fn main() -> Result<()> {
  let args: Vec<String> = std::env::args().skip(1).collect();
  match args.first().map(String::as_str) {
    None | Some("serve") => serve_command(&args[1..]),
    Some("cst") => cst(&args[1..]),
    Some("parse") => parse(&args[1..]),
    Some("-h" | "--help") => {
      println!("{USAGE}");
      Ok(())
    }
    Some(other) => bail!("unknown command: {other}\n{USAGE}"),
  }
}

fn serve_command(args: &[String]) -> Result<()> {
  let mut shm = false;
  let mut dump = None;
  let mut args = args.iter();
  while let Some(arg) = args.next() {
    match arg.as_str() {
      "--shm" => shm = true,
      "--jsonl" => shm = false,
      "--dump-json" => dump = Some(PathBuf::from(args.next().ok_or_else(|| anyhow!(USAGE))?)),
      "-h" | "--help" => {
        println!("{USAGE}");
        return Ok(());
      }
      other => bail!("unknown argument: {other}\n{USAGE}"),
    }
  }
  let stdin = std::io::stdin().lock();
  let stdout = std::io::stdout().lock();
  if shm {
    serve_shm(stdin, stdout, dump)
  } else {
    serve_jsonl(stdin, stdout, dump)
  }
}

fn parse(args: &[String]) -> Result<()> {
  let mut dump = None;
  let mut file = None;
  let mut args = args.iter();
  while let Some(arg) = args.next() {
    match arg.as_str() {
      "--dump-json" => dump = Some(PathBuf::from(args.next().ok_or_else(|| anyhow!(USAGE))?)),
      other if file.is_none() => file = Some(other),
      other => bail!("unknown argument: {other}\n{USAGE}"),
    }
  }
  let file = file.ok_or_else(|| anyhow!(USAGE))?;
  let cst = semsitter_bridge::parse_only(&std::fs::read(file)?)?;
  if let Some(dir) = &dump {
    std::fs::create_dir_all(dir)?;
    std::fs::write(dir.join("parse.json"), serde_json::to_vec_pretty(&cst)?)?;
  }
  let mut stdout = std::io::stdout().lock();
  serde_json::to_writer(&mut stdout, &cst)?;
  writeln!(stdout)?;
  Ok(())
}

fn cst(args: &[String]) -> Result<()> {
  let mut root = None;
  let mut classpath = Vec::new();
  let mut dump = None;
  let mut files = Vec::new();
  let mut args = args.iter();
  while let Some(arg) = args.next() {
    match arg.as_str() {
      "--root" => root = args.next().map(PathBuf::from),
      "--classpath" => {
        let value = args.next().ok_or_else(|| anyhow!(USAGE))?;
        classpath.extend(std::env::split_paths(value).map(|p| p.to_string_lossy().into_owned()));
      }
      "--dump-json" => dump = Some(PathBuf::from(args.next().ok_or_else(|| anyhow!(USAGE))?)),
      file => files.push(PathBuf::from(file)),
    }
  }
  let root = root.ok_or_else(|| anyhow!(USAGE))?;
  print_csts(&root, &files, &classpath, dump.as_deref(), std::io::stdout().lock())
}
