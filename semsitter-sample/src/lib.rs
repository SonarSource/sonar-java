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

use tree_sitter_language::LanguageFn;

unsafe extern "C" {
    fn tree_sitter_sample() -> *const ();
}

pub const LANGUAGE: LanguageFn = unsafe { LanguageFn::from_raw(tree_sitter_sample) };

pub fn language() -> tree_sitter::Language {
    LANGUAGE.into()
}

/// Handles one JSONL request.
///
/// Stdout of `semsitter-bridge` is response lines only. A request is
/// `{"id":1,"op":"parse","source":"..."}` or `{"id":2,"op":"quit"}`.
/// A parse response is `{"id":1,"ok":true,"hasError":false,"tree":{...}}`.
/// Tree positions are tree-sitter's 0-based rows and columns.
/// The boolean is true when the process should exit after writing the response.
pub fn handle_line(line: &str) -> (String, bool) {
    let request: serde_json::Value = match serde_json::from_str::<serde_json::Value>(line) {
        Ok(value) if value.is_object() => value,
        Ok(_) => return (error_response(serde_json::Value::Null, "request must be an object"), false),
        Err(error) => return (error_response(serde_json::Value::Null, &error.to_string()), false),
    };
    let id = request.get("id").cloned().unwrap_or(serde_json::Value::Null);
    match request.get("op").and_then(serde_json::Value::as_str) {
        Some("quit") => (ok_response(id, None, None), true),
        Some("parse") => match request.get("source").and_then(serde_json::Value::as_str) {
            Some(source) => match parse_source(source) {
                Ok((tree, has_error)) => (ok_response(id, Some(tree), Some(has_error)), false),
                Err(error) => (error_response(id, &error), false),
            },
            None => (error_response(id, "missing source"), false),
        },
        Some(other) => (error_response(id, &format!("unknown op {other}")), false),
        None => (error_response(id, "missing op"), false),
    }
}

fn parse_source(source: &str) -> Result<(serde_json::Value, bool), String> {
    let mut parser = tree_sitter::Parser::new();
    parser
        .set_language(&language())
        .map_err(|error| error.to_string())?;
    let tree = parser.parse(source, None).ok_or("parser returned no tree")?;
    let root = tree.root_node();
    Ok((node_json(root, 0), root.has_error()))
}

fn node_json(node: tree_sitter::Node, depth: usize) -> serde_json::Value {
    if depth > 64 {
        return serde_json::json!({"truncated": true});
    }
    let start = node.start_position();
    let end = node.end_position();
    let child_count = node.child_count();
    let mut children = Vec::with_capacity(usize::try_from(child_count).unwrap_or(0));
    for index in 0..child_count {
        if let Some(child) = node.child(index) {
            children.push(node_json(child, depth + 1));
        }
    }
    serde_json::json!({
        "kind": node.kind(),
        "named": node.is_named(),
        "error": node.is_error(),
        "missing": node.is_missing(),
        "startByte": node.start_byte(),
        "endByte": node.end_byte(),
        "startRow": start.row,
        "startColumn": start.column,
        "endRow": end.row,
        "endColumn": end.column,
        "children": children,
    })
}

fn ok_response(id: serde_json::Value, tree: Option<serde_json::Value>, has_error: Option<bool>) -> String {
    let mut response = serde_json::Map::new();
    response.insert("id".to_string(), id);
    response.insert("ok".to_string(), serde_json::Value::Bool(true));
    if let Some(has_error) = has_error {
        response.insert("hasError".to_string(), serde_json::Value::Bool(has_error));
    }
    if let Some(tree) = tree {
        response.insert("tree".to_string(), tree);
    }
    serde_json::Value::Object(response).to_string()
}

fn error_response(id: serde_json::Value, error: &str) -> String {
    serde_json::json!({
        "id": id,
        "ok": false,
        "error": error,
    })
    .to_string()
}

#[cfg(test)]
mod tests {
    use udg_model::location_index::LineRange;
    use udg_model::syntax_unit::{SyntaxSpan, SyntaxUnit};

    use super::language;

    #[test]
    fn sample_rule_parses_hello() {
        let mut parser = tree_sitter::Parser::new();
        parser.set_language(&language()).unwrap();

        let tree = parser.parse("hello", None).unwrap();
        let root = tree.root_node();
        assert_eq!(root.kind(), "source_file");
        assert!(!root.has_error());
        assert_eq!(root.child(0).unwrap().kind(), "hello");

        let unit = SyntaxUnit {
            span: SyntaxSpan {
                syntax_kind: root.kind().to_string(),
                lines: LineRange::point(1),
            },
            nested: Vec::new(),
        };
        assert_eq!(unit.span.syntax_kind, "source_file");
        assert_eq!(unit.span.lines, LineRange::point(1));
    }

    #[test]
    fn jsonl_parse_hello() {
        let (response, quit) = super::handle_line(r#"{"id":7,"op":"parse","source":"hello"}"#);
        assert!(!quit);
        let json: serde_json::Value = serde_json::from_str(&response).unwrap();
        assert_eq!(json["id"], 7);
        assert_eq!(json["ok"], true);
        assert_eq!(json["hasError"], false);
        assert_eq!(json["tree"]["kind"], "source_file");
        assert_eq!(json["tree"]["children"][0]["kind"], "hello");
    }

    #[test]
    fn jsonl_keeps_an_error_tree() {
        let (response, quit) = super::handle_line(r#"{"id":1,"op":"parse","source":"nope"}"#);
        assert!(!quit);
        let json: serde_json::Value = serde_json::from_str(&response).unwrap();
        assert_eq!(json["ok"], true);
        assert_eq!(json["hasError"], true);
        assert_eq!(json["tree"]["kind"], "ERROR");
    }

    #[test]
    fn jsonl_quit_and_bad_request() {
        let (response, quit) = super::handle_line(r#"{"id":2,"op":"quit"}"#);
        assert!(quit);
        let json: serde_json::Value = serde_json::from_str(&response).unwrap();
        assert_eq!(json["ok"], true);

        let (response, quit) = super::handle_line("not-json");
        assert!(!quit);
        let json: serde_json::Value = serde_json::from_str(&response).unwrap();
        assert_eq!(json["ok"], false);
        assert!(json["error"].as_str().unwrap().contains("expected"));
    }
}
