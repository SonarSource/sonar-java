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

use std::io::{Read, Write};
use std::thread;

use semsitter_bridge::server::serve_shm;
use semsitter_bridge::shm::{Link, ATTACHED, READY};
use semsitter_bridge::wire::{self, Request};

#[test]
fn protobuf_round_trips_a_tree() {
  let document = serde_json::json!({
    "ok": true,
    "cst": {
      "hasError": false,
      "semantic": true,
      "path": "A.java",
      "symbols": [{"id": "A", "kind": "class"}],
      "facts": {"A": {"kind": "class", "tier": "IN_PROJECT", "extends": ["java.lang.Object"]}},
      "tree": {"k": "program", "n": true, "p": [1, 0, 1, 10], "b": [0, 10], "t": "class A {}", "decls": ["A"],
        "refs": [{"access": "r", "outcome": "bound", "target": "A"}]}
    }
  });
  let decoded = wire::decode_response(&wire::encode_response(7, &document)).unwrap();
  assert_eq!(decoded["id"], 7);
  assert_eq!(decoded["cst"]["tree"]["k"], "program");
  assert_eq!(decoded["cst"]["tree"]["decls"], serde_json::json!(["A"]));
  assert_eq!(decoded["cst"]["tree"]["refs"][0]["target"], "A");
  assert_eq!(decoded["cst"]["symbols"][0]["id"], "A");
  assert_eq!(decoded["cst"]["facts"]["A"]["extends"], serde_json::json!(["java.lang.Object"]));
}

#[cfg(unix)]
#[test]
fn shared_memory_serves_a_parse() {
  use std::os::unix::net::UnixStream;

  let (server_side, mut client_side) = UnixStream::pair().unwrap();
  let server = thread::spawn(move || {
    let input = server_side.try_clone().unwrap();
    serve_shm(input, server_side, None).unwrap();
  });

  let mut ready = [0u8; 1];
  client_side.read_exact(&mut ready).unwrap();
  assert_eq!(ready[0], READY);
  let mut link = Link::open(&std::process::id().to_string()).unwrap();
  client_side.write_all(&[ATTACHED]).unwrap();
  client_side.flush().unwrap();

  let mut to_server = client_side.try_clone().unwrap();
  let mut from_server = client_side;
  let request = wire::encode_request(3, &Request::Parse { source: "class B {}".into() });
  link.write_request(&request, &mut to_server, &mut from_server).unwrap();
  let response = link.read_response(&mut from_server, &mut to_server).unwrap();
  let decoded = wire::decode_response(&response).unwrap();
  assert_eq!(decoded["ok"], true);
  assert_eq!(decoded["cst"]["tree"]["k"], "program");

  let quit = wire::encode_request(4, &Request::Quit);
  link.write_request(&quit, &mut to_server, &mut from_server).unwrap();
  let _ = link.read_response(&mut from_server, &mut to_server).unwrap();
  server.join().unwrap();
}
