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

//! Protobuf codec for [`super::proto`](../../proto/bridge.proto). The messages are the only
//! bytes that cross the process boundary; JSON stays a debug dump.

use anyhow::{Result, anyhow};
use serde_json::{Map, Value, json};

pub enum Request {
  Analyze { root: String, files: Vec<String>, classpath: Vec<String> },
  Cst { path: String },
  Types { names: Vec<String> },
  Parse { source: String },
  Quit,
}

pub struct Incoming {
  pub id: u32,
  pub request: Request,
}

pub fn decode_request(bytes: &[u8]) -> Result<Incoming> {
  let mut id = 0u32;
  let mut analyze = None;
  let mut cst = None;
  let mut types = None;
  let mut parse = None;
  let mut quit = false;
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Varint(v)) => id = v as u32,
      (2, Wire::Bytes(b)) => analyze = Some(decode_analyze(&b)?),
      (3, Wire::Bytes(b)) => cst = Some(decode_path(&b)?),
      (4, Wire::Bytes(b)) => types = Some(decode_names(&b)?),
      (5, Wire::Bytes(b)) => parse = Some(decode_source(&b)?),
      (6, Wire::Varint(v)) => quit = v != 0,
      _ => {}
    }
  }
  let request = if let Some((root, files, classpath)) = analyze {
    Request::Analyze { root, files, classpath }
  } else if let Some(path) = cst {
    Request::Cst { path }
  } else if let Some(names) = types {
    Request::Types { names }
  } else if let Some(source) = parse {
    Request::Parse { source }
  } else if quit {
    Request::Quit
  } else {
    return Err(anyhow!("request has no op"));
  };
  Ok(Incoming { id, request })
}

pub fn encode_response(id: u32, body: &Value) -> Vec<u8> {
  let mut out = Vec::new();
  varint_field(&mut out, 1, id as u64);
  let ok = body.get("ok").and_then(Value::as_bool).unwrap_or(false);
  if ok {
    varint_field(&mut out, 2, 1);
  }
  if let Some(error) = body.get("error").and_then(Value::as_str) {
    string_field(&mut out, 3, error);
  }
  if let Some(root) = body.get("root").and_then(Value::as_str) {
    let mut msg = Vec::new();
    string_field(&mut msg, 1, root);
    for file in strings(body, "files") {
      string_field(&mut msg, 2, &file);
    }
    message_field(&mut out, 4, &msg);
  }
  if let Some(document) = body.get("cst") {
    message_field(&mut out, 5, &encode_document(document));
  }
  if let Some(types) = body.get("types") {
    if let Ok(json) = serde_json::to_string(types) {
      string_field(&mut out, 6, &json);
    }
  }
  out
}

pub fn decode_response(bytes: &[u8]) -> Result<Value> {
  let mut id = Value::Null;
  let mut ok = false;
  let mut error = None;
  let mut analyze = None;
  let mut document = None;
  let mut types = None;
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Varint(v)) => id = json!(v),
      (2, Wire::Varint(v)) => ok = v != 0,
      (3, Wire::Bytes(b)) => error = Some(String::from_utf8_lossy(&b).into_owned()),
      (4, Wire::Bytes(b)) => analyze = Some(decode_analyze_result(&b)?),
      (5, Wire::Bytes(b)) => document = Some(decode_document(&b)?),
      (6, Wire::Bytes(b)) => types = Some(serde_json::from_slice(&b).unwrap_or(Value::Null)),
      _ => {}
    }
  }
  let mut object = Map::new();
  object.insert("id".into(), id);
  object.insert("ok".into(), Value::Bool(ok));
  if let Some(error) = error {
    object.insert("error".into(), Value::String(error));
  }
  if let Some((root, files)) = analyze {
    object.insert("root".into(), Value::String(root));
    object.insert("files".into(), json!(files));
  }
  if let Some(document) = document {
    object.insert("cst".into(), document);
  }
  if let Some(types) = types {
    object.insert("types".into(), types);
  }
  Ok(Value::Object(object))
}

fn encode_document(document: &Value) -> Vec<u8> {
  let mut out = Vec::new();
  if document.get("hasError").and_then(Value::as_bool) == Some(true) {
    varint_field(&mut out, 1, 1);
  }
  if let Some(tree) = document.get("tree") {
    message_field(&mut out, 2, &encode_node(tree));
  }
  if let Some(path) = document.get("path").and_then(Value::as_str) {
    string_field(&mut out, 3, path);
  }
  if document.get("semantic").and_then(Value::as_bool) == Some(true) {
    varint_field(&mut out, 4, 1);
  }
  if let Some(symbols) = document.get("symbols") {
    if let Ok(json) = serde_json::to_string(symbols) {
      string_field(&mut out, 5, &json);
    }
  }
  if let Some(facts) = document.get("facts").and_then(Value::as_object) {
    for (key, fact) in facts {
      let mut entry = Vec::new();
      string_field(&mut entry, 1, key);
      message_field(&mut entry, 2, &encode_fact(fact));
      message_field(&mut out, 6, &entry);
    }
  }
  out
}

fn encode_fact(fact: &Value) -> Vec<u8> {
  let mut out = Vec::new();
  if let Some(kind) = fact.get("kind").and_then(Value::as_str) {
    string_field(&mut out, 1, kind);
  }
  if let Some(tier) = fact.get("tier").and_then(Value::as_str) {
    string_field(&mut out, 2, tier);
  }
  if let Some(parent) = fact.get("parent").and_then(Value::as_str) {
    string_field(&mut out, 3, parent);
  }
  if let Some(props) = fact.get("props").and_then(Value::as_object) {
    for (name, values) in props {
      let mut prop = Vec::new();
      string_field(&mut prop, 1, name);
      for value in values.as_array().into_iter().flatten().filter_map(Value::as_str) {
        string_field(&mut prop, 2, value);
      }
      message_field(&mut out, 4, &prop);
    }
  }
  for (field, key) in [(5, "extends"), (6, "implements"), (7, "permits"), (8, "returns"), (9, "type"), (10, "params"), (11, "overrides"), (12, "annotations")] {
    for value in strings(fact, key) {
      string_field(&mut out, field, &value);
    }
  }
  out
}

fn encode_node(node: &Value) -> Vec<u8> {
  let mut out = Vec::new();
  if let Some(kind) = node.get("k").and_then(Value::as_str) {
    string_field(&mut out, 1, kind);
  }
  if node.get("n").and_then(Value::as_bool) == Some(true) {
    varint_field(&mut out, 2, 1);
  }
  if let Some(field) = node.get("f").and_then(Value::as_str) {
    string_field(&mut out, 3, field);
  }
  if node.get("m").and_then(Value::as_bool) == Some(true) {
    varint_field(&mut out, 4, 1);
  }
  if let Some(position) = node.get("p").and_then(Value::as_array) {
    for (field, value) in position.iter().enumerate().take(4) {
      varint_field(&mut out, 5 + field as u32, value.as_u64().unwrap_or(0));
    }
  }
  if let Some(span) = node.get("b").and_then(Value::as_array) {
    for (field, value) in span.iter().enumerate().take(2) {
      varint_field(&mut out, 9 + field as u32, value.as_u64().unwrap_or(0));
    }
  }
  if let Some(text) = node.get("t").and_then(Value::as_str) {
    string_field(&mut out, 11, text);
  }
  for child in node.get("c").and_then(Value::as_array).into_iter().flatten() {
    message_field(&mut out, 12, &encode_node(child));
  }
  for decl in strings(node, "decls") {
    string_field(&mut out, 13, &decl);
  }
  for reference in node.get("refs").and_then(Value::as_array).into_iter().flatten() {
    message_field(&mut out, 14, &encode_ref(reference));
  }
  out
}

fn encode_ref(reference: &Value) -> Vec<u8> {
  let mut out = Vec::new();
  for (field, key) in [(1, "access"), (2, "outcome"), (3, "target"), (4, "reason")] {
    if let Some(value) = reference.get(key).and_then(Value::as_str) {
      string_field(&mut out, field, value);
    }
  }
  if reference.get("bounded").and_then(Value::as_bool) == Some(true) {
    varint_field(&mut out, 5, 1);
  }
  if let Some(facts) = reference.get("facts") {
    if !facts.is_null() {
      if let Ok(json) = serde_json::to_string(facts) {
        string_field(&mut out, 6, &json);
      }
    }
  }
  out
}

fn decode_document(bytes: &[u8]) -> Result<Value> {
  let mut object = Map::new();
  object.insert("hasError".into(), Value::Bool(false));
  object.insert("semantic".into(), Value::Bool(false));
  object.insert("symbols".into(), json!([]));
  object.insert("facts".into(), json!({}));
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Varint(v)) => object.insert("hasError".into(), Value::Bool(v != 0)),
      (2, Wire::Bytes(b)) => object.insert("tree".into(), decode_node(&b)?),
      (3, Wire::Bytes(b)) => object.insert("path".into(), Value::String(text(&b))),
      (4, Wire::Varint(v)) => object.insert("semantic".into(), Value::Bool(v != 0)),
      (5, Wire::Bytes(b)) => object.insert("symbols".into(), serde_json::from_slice(&b).unwrap_or_else(|_| json!([]))),
      (6, Wire::Bytes(b)) => {
        let (key, fact) = decode_fact_entry(&b)?;
        object
          .entry("facts")
          .or_insert_with(|| json!({}))
          .as_object_mut()
          .expect("facts is an object")
          .insert(key, fact);
        None
      }
      _ => None,
    };
  }
  Ok(Value::Object(object))
}

fn decode_fact_entry(bytes: &[u8]) -> Result<(String, Value)> {
  let mut key = String::new();
  let mut fact = json!({});
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Bytes(b)) => key = text(&b),
      (2, Wire::Bytes(b)) => fact = decode_fact(&b)?,
      _ => {}
    }
  }
  Ok((key, fact))
}

fn decode_fact(bytes: &[u8]) -> Result<Value> {
  let mut object = Map::new();
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Bytes(b)) => object.insert("kind".into(), Value::String(text(&b))),
      (2, Wire::Bytes(b)) => object.insert("tier".into(), Value::String(text(&b))),
      (3, Wire::Bytes(b)) => object.insert("parent".into(), Value::String(text(&b))),
      (4, Wire::Bytes(b)) => {
        let (name, values) = decode_prop(&b)?;
        let props = object.entry("props").or_insert_with(|| json!({}));
        props.as_object_mut().expect("props").insert(name, json!(values));
        None
      }
      (5, Wire::Bytes(b)) => push_string(&mut object, "extends", text(&b)),
      (6, Wire::Bytes(b)) => push_string(&mut object, "implements", text(&b)),
      (7, Wire::Bytes(b)) => push_string(&mut object, "permits", text(&b)),
      (8, Wire::Bytes(b)) => push_string(&mut object, "returns", text(&b)),
      (9, Wire::Bytes(b)) => push_string(&mut object, "type", text(&b)),
      (10, Wire::Bytes(b)) => push_string(&mut object, "params", text(&b)),
      (11, Wire::Bytes(b)) => push_string(&mut object, "overrides", text(&b)),
      (12, Wire::Bytes(b)) => push_string(&mut object, "annotations", text(&b)),
      _ => None,
    };
  }
  Ok(Value::Object(object))
}

fn decode_prop(bytes: &[u8]) -> Result<(String, Vec<String>)> {
  let mut name = String::new();
  let mut values = Vec::new();
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Bytes(b)) => name = text(&b),
      (2, Wire::Bytes(b)) => values.push(text(&b)),
      _ => {}
    }
  }
  Ok((name, values))
}

fn decode_node(bytes: &[u8]) -> Result<Value> {
  let mut object = Map::new();
  let mut position = [0u64; 4];
  let mut span = [0u64; 2];
  let mut saw_position = false;
  let mut saw_span = false;
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Bytes(b)) => object.insert("k".into(), Value::String(text(&b))),
      (2, Wire::Varint(v)) if v != 0 => object.insert("n".into(), Value::Bool(true)),
      (3, Wire::Bytes(b)) => object.insert("f".into(), Value::String(text(&b))),
      (4, Wire::Varint(v)) if v != 0 => object.insert("m".into(), Value::Bool(true)),
      (5..=8, Wire::Varint(v)) => {
        position[(field - 5) as usize] = v;
        saw_position = true;
        None
      }
      (9 | 10, Wire::Varint(v)) => {
        span[(field - 9) as usize] = v;
        saw_span = true;
        None
      }
      (11, Wire::Bytes(b)) => object.insert("t".into(), Value::String(text(&b))),
      (12, Wire::Bytes(b)) => {
        let children = object.entry("c").or_insert_with(|| json!([]));
        children.as_array_mut().expect("children").push(decode_node(&b)?);
        None
      }
      (13, Wire::Bytes(b)) => push_string(&mut object, "decls", text(&b)),
      (14, Wire::Bytes(b)) => {
        let refs = object.entry("refs").or_insert_with(|| json!([]));
        refs.as_array_mut().expect("refs").push(decode_ref(&b)?);
        None
      }
      _ => None,
    };
  }
  if saw_position {
    object.insert("p".into(), json!(position));
  }
  if saw_span {
    object.insert("b".into(), json!(span));
  }
  Ok(Value::Object(object))
}

fn decode_ref(bytes: &[u8]) -> Result<Value> {
  let mut object = Map::new();
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Bytes(b)) => object.insert("access".into(), Value::String(text(&b))),
      (2, Wire::Bytes(b)) => object.insert("outcome".into(), Value::String(text(&b))),
      (3, Wire::Bytes(b)) => object.insert("target".into(), Value::String(text(&b))),
      (4, Wire::Bytes(b)) => object.insert("reason".into(), Value::String(text(&b))),
      (5, Wire::Varint(v)) if v != 0 => object.insert("bounded".into(), Value::Bool(true)),
      (6, Wire::Bytes(b)) => object.insert("facts".into(), serde_json::from_slice(&b).unwrap_or(Value::Null)),
      _ => None,
    };
  }
  Ok(Value::Object(object))
}

fn decode_analyze(bytes: &[u8]) -> Result<(String, Vec<String>, Vec<String>)> {
  let mut root = String::new();
  let mut files = Vec::new();
  let mut classpath = Vec::new();
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Bytes(b)) => root = text(&b),
      (2, Wire::Bytes(b)) => files.push(text(&b)),
      (3, Wire::Bytes(b)) => classpath.push(text(&b)),
      _ => {}
    }
  }
  Ok((root, files, classpath))
}

fn decode_analyze_result(bytes: &[u8]) -> Result<(String, Vec<String>)> {
  let mut root = String::new();
  let mut files = Vec::new();
  for (field, value) in fields(bytes)? {
    match (field, value) {
      (1, Wire::Bytes(b)) => root = text(&b),
      (2, Wire::Bytes(b)) => files.push(text(&b)),
      _ => {}
    }
  }
  Ok((root, files))
}

fn decode_path(bytes: &[u8]) -> Result<String> {
  for (field, value) in fields(bytes)? {
    if field == 1 {
      if let Wire::Bytes(b) = value {
        return Ok(text(&b));
      }
    }
  }
  Ok(String::new())
}

fn decode_names(bytes: &[u8]) -> Result<Vec<String>> {
  let mut names = Vec::new();
  for (field, value) in fields(bytes)? {
    if field == 1 {
      if let Wire::Bytes(b) = value {
        names.push(text(&b));
      }
    }
  }
  Ok(names)
}

fn decode_source(bytes: &[u8]) -> Result<String> {
  decode_path(bytes)
}

fn push_string(object: &mut Map<String, Value>, key: &str, value: String) -> Option<Value> {
  let array = object.entry(key).or_insert_with(|| json!([]));
  array.as_array_mut().expect(key).push(Value::String(value));
  None
}

fn strings(value: &Value, key: &str) -> Vec<String> {
  value
    .get(key)
    .and_then(Value::as_array)
    .into_iter()
    .flatten()
    .filter_map(|v| v.as_str().map(str::to_string))
    .collect()
}

fn text(bytes: &[u8]) -> String {
  String::from_utf8_lossy(bytes).into_owned()
}

enum Wire {
  Varint(u64),
  Bytes(Vec<u8>),
}

fn fields(bytes: &[u8]) -> Result<Vec<(u32, Wire)>> {
  let mut out = Vec::new();
  let mut i = 0;
  while i < bytes.len() {
    let (key, next) = varint_at(bytes, i)?;
    i = next;
    let field = (key >> 3) as u32;
    match key & 7 {
      0 => {
        let (value, next) = varint_at(bytes, i)?;
        i = next;
        out.push((field, Wire::Varint(value)));
      }
      2 => {
        let (len, next) = varint_at(bytes, i)?;
        i = next;
        let len = len as usize;
        if i + len > bytes.len() {
          return Err(anyhow!("truncated protobuf field {field}"));
        }
        out.push((field, Wire::Bytes(bytes[i..i + len].to_vec())));
        i += len;
      }
      1 => i += 8,
      5 => i += 4,
      other => return Err(anyhow!("unsupported protobuf wire type {other}")),
    }
  }
  Ok(out)
}

fn varint_at(bytes: &[u8], mut i: usize) -> Result<(u64, usize)> {
  let mut value = 0u64;
  let mut shift = 0;
  while i < bytes.len() && shift < 64 {
    let byte = bytes[i];
    i += 1;
    value |= ((byte & 0x7f) as u64) << shift;
    if byte & 0x80 == 0 {
      return Ok((value, i));
    }
    shift += 7;
  }
  Err(anyhow!("truncated varint"))
}

fn varint_field(out: &mut Vec<u8>, field: u32, value: u64) {
  varint(out, (field as u64) << 3);
  varint(out, value);
}

fn string_field(out: &mut Vec<u8>, field: u32, value: &str) {
  bytes_field(out, field, value.as_bytes());
}

fn message_field(out: &mut Vec<u8>, field: u32, value: &[u8]) {
  bytes_field(out, field, value);
}

fn bytes_field(out: &mut Vec<u8>, field: u32, value: &[u8]) {
  varint(out, ((field as u64) << 3) | 2);
  varint(out, value.len() as u64);
  out.extend_from_slice(value);
}

fn varint(out: &mut Vec<u8>, mut value: u64) {
  loop {
    let mut byte = (value & 0x7f) as u8;
    value >>= 7;
    if value != 0 {
      byte |= 0x80;
    }
    out.push(byte);
    if value == 0 {
      break;
    }
  }
}

pub fn encode_request(id: u32, request: &Request) -> Vec<u8> {
  let mut out = Vec::new();
  varint_field(&mut out, 1, id as u64);
  match request {
    Request::Analyze { root, files, classpath } => {
      let mut msg = Vec::new();
      string_field(&mut msg, 1, root);
      for file in files {
        string_field(&mut msg, 2, file);
      }
      for entry in classpath {
        string_field(&mut msg, 3, entry);
      }
      message_field(&mut out, 2, &msg);
    }
    Request::Cst { path } => {
      let mut msg = Vec::new();
      string_field(&mut msg, 1, path);
      message_field(&mut out, 3, &msg);
    }
    Request::Types { names } => {
      let mut msg = Vec::new();
      for name in names {
        string_field(&mut msg, 1, name);
      }
      message_field(&mut out, 4, &msg);
    }
    Request::Parse { source } => {
      let mut msg = Vec::new();
      string_field(&mut msg, 1, source);
      message_field(&mut out, 5, &msg);
    }
    Request::Quit => varint_field(&mut out, 6, 1),
  }
  out
}
