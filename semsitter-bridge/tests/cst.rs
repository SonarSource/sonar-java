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

use std::path::{Path, PathBuf};

use serde_json::Value;

fn write(dir: &Path, rel: &str, text: &str) -> PathBuf {
  let path = dir.join(rel);
  std::fs::create_dir_all(path.parent().unwrap()).unwrap();
  std::fs::write(&path, text).unwrap();
  path
}

fn find<'a>(node: &'a Value, text: &str, out: &mut Vec<&'a Value>) {
  if node.get("t").and_then(Value::as_str) == Some(text) {
    out.push(node);
  }
  for child in node.get("c").and_then(Value::as_array).into_iter().flatten() {
    find(child, text, out);
  }
}

fn nodes<'a>(cst: &'a Value, text: &str) -> Vec<&'a Value> {
  let mut out = Vec::new();
  find(&cst["tree"], text, &mut out);
  out
}

#[test]
fn cst_carries_declarations_references_and_facts() {
  let dir = tempfile::tempdir().unwrap();
  let a = write(
    dir.path(),
    "src/p/A.java",
    "package p;\nimport java.util.List;\npublic class A {\n  int f;\n  String name(List<String> l) { return l.get(f); }\n}\n",
  );
  let b = write(
    dir.path(),
    "src/p/B.java",
    "package p;\nclass B extends A {\n  void run() { A a = new A(); String s = a.name(null); }\n}\n",
  );

  let analysis = semsitter_bridge::analyze(dir.path(), &[a.clone(), b], &[]).unwrap();
  assert_eq!(analysis.files(), ["src/p/A.java", "src/p/B.java"]);

  let cst_a = analysis.cst(a.to_str().unwrap()).unwrap();
  assert_eq!(cst_a["path"], "src/p/A.java");
  assert_eq!(cst_a["semantic"], true);
  assert_eq!(cst_a["tree"]["k"], "program");
  assert_eq!(cst_a["hasError"], false);

  let class_name = nodes(&cst_a, "A")[0];
  assert_eq!(class_name["decls"][0], "p.A");
  assert_eq!(class_name["p"], serde_json::json!([3, 13, 3, 14]));

  let get = nodes(&cst_a, "get")[0];
  let target = get["refs"][0]["target"].as_str().unwrap();
  assert!(target.starts_with("java.util.List"), "{target}");

  let field_use = nodes(&cst_a, "f")[1];
  assert_eq!(field_use["refs"][0]["target"], "p.A#f");

  let cst_b = analysis.cst("src/p/B.java").unwrap();
  assert_eq!(cst_b["facts"]["p.B"]["extends"][0], "p.A");
  let name_call = nodes(&cst_b, "name")[0];
  assert_eq!(name_call["refs"][0]["target"], "p.A#name(java.util.List`1)");
  assert_eq!(cst_b["facts"]["p.A#name(java.util.List`1)"]["returns"][0], "java.lang.String");
}

#[test]
fn types_come_from_the_project_graph_and_the_jdk_table_with_their_supertypes() {
  let dir = tempfile::tempdir().unwrap();
  let a = write(dir.path(), "p/A.java", "package p;\npublic class A extends java.util.ArrayList<String> { int f; void m() {} }\n");
  let analysis = semsitter_bridge::analyze(dir.path(), &[a], &[]).unwrap();
  let types = analysis.types(&["p.A".to_string()]);
  assert_eq!(types["p.A"]["origin"], "project");
  assert_eq!(types["p.A"]["super"], "java.util.ArrayList`1");
  let member_ids: Vec<&str> = types["p.A"]["members"].as_array().unwrap().iter().map(|m| m["id"].as_str().unwrap()).collect();
  assert!(member_ids.contains(&"p.A#m()"), "{member_ids:?}");
  assert_eq!(types["java.util.ArrayList"]["origin"], "jdk");
  assert_eq!(types["java.util.AbstractList"]["super"], "java.util.AbstractCollection");
  let list_get = types["java.util.List"]["members"].as_array().unwrap().iter().find(|m| m["name"] == "get").unwrap();
  assert_eq!(list_get["desc"], "(I)Ljava/lang/Object;");
}

#[test]
fn dump_json_writes_the_udg_the_semantic_dump_and_the_cst() {
  let dir = tempfile::tempdir().unwrap();
  let dump = tempfile::tempdir().unwrap();
  let file = write(dir.path(), "A.java", "class A { int f; }");
  semsitter_bridge::analyze_dumping(dir.path(), &[file], &[], Some(dump.path())).unwrap();
  assert!(dump.path().join("udg.json").is_file());
  let semdump = std::fs::read_to_string(dump.path().join("semdump/A.java.semdump.json")).unwrap();
  assert!(semdump.contains("semantic-dump/1"), "{semdump}");
  let cst = std::fs::read_to_string(dump.path().join("cst/A.java.json")).unwrap();
  assert!(cst.contains("\"k\": \"program\""), "{cst}");
}

#[test]
fn a_bom_is_neither_a_syntax_error_nor_a_column() {
  let cst = semsitter_bridge::parse_only(b"\xEF\xBB\xBFclass A {}").unwrap();
  assert_eq!(cst["hasError"], false);
  assert_eq!(nodes(&cst, "class")[0]["p"], serde_json::json!([1, 0, 1, 5]));
}

#[test]
fn parse_only_keeps_every_token_and_utf16_columns() {
  let cst = semsitter_bridge::parse_only("class É { String s = \"é\"; }".as_bytes()).unwrap();
  let literal = &nodes(&cst, "\"é\"")[0];
  assert_eq!(literal["k"], "string_literal");
  assert_eq!(literal["p"], serde_json::json!([1, 21, 1, 24]));
  assert_eq!(nodes(&cst, "{").len(), 1);
}
