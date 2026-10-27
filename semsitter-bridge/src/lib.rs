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

//! Java CST annotated with SemSitter semantics.
//!
//! [`analyze`] runs one SemSitter scan over a batch of Java files (with the project classpath),
//! writes the `semantic-dump/1` of every file, and keeps the graph facts SonarJava needs on top
//! of it (return types, supertypes, overrides, item kinds and modifiers).
//! [`Analysis::cst`] then parses a file with tree-sitter-java and attaches, by exact byte span,
//! the symbols declared at a name (`decls`) and the resolved occurrences (`refs`).

pub mod types;

use std::collections::{BTreeMap, BTreeSet, HashMap};
use std::io;
use std::path::{Path, PathBuf};
use std::sync::Arc;

use anyhow::{Context, Result, anyhow};
use serde_json::{Map, Value, json};
use tree_sitter::{Node, Parser, Tree};
use udg_gen::overlay_passes::overlay_registry;
use udg_gen::scanner::{ScanOptions, scan_timed_with_registry};
use udg_gen::semantic_dump::{Scanned, languages, run_with};
use udg_gen::udg_model::graph::{DependencyTarget, NodeTier, UdgDependencyKind, UdgGraph};
use udg_gen::udg_model::source_provider::SourceProvider;
use udg_family_jvm::external::classpath::LazyClasspath;
use udg_lang_sdk::settings::ResolvedSettings;

const DUMP_SUFFIX: &str = ".semdump.json";
const BOM: &[u8] = b"\xEF\xBB\xBF";
const STACK_SIZE: usize = 512 * 1024 * 1024;
const GRAPH_EDGES: &[(&str, &str)] = &[
  ("extends", "extends"),
  ("implements", "implements"),
  ("permits", "permits"),
  ("returns", "returns"),
  ("is type", "type"),
  ("has param", "params"),
  ("overrides", "overrides"),
  ("annotated by", "annotations"),
];

/// Result of one SemSitter scan over a batch of files.
pub struct Analysis {
  root: PathBuf,
  files: Vec<String>,
  dumps: HashMap<String, Value>,
  facts: HashMap<String, Value>,
  calls: HashMap<String, Vec<CallSite>>,
  project_types: types::ProjectTypes,
  classpath: LazyClasspath,
}

#[derive(Debug)]
struct ListedSources {
  files: Vec<PathBuf>,
}

impl SourceProvider for ListedSources {
  fn read(&self, path: &Path) -> io::Result<String> {
    Ok(String::from_utf8_lossy(&std::fs::read(path)?).into_owned())
  }

  fn read_bytes(&self, path: &Path) -> io::Result<Vec<u8>> {
    std::fs::read(path)
  }

  fn list_files(&self, root: &Path, _project_root: &Path, extensions: &[&str]) -> Vec<PathBuf> {
    let mut found: Vec<PathBuf> = self
      .files
      .iter()
      .filter(|f| f.starts_with(root))
      .filter(|f| {
        f.extension()
          .and_then(|e| e.to_str())
          .is_some_and(|e| extensions.iter().any(|x| x.trim_start_matches('.').eq_ignore_ascii_case(e)))
      })
      .cloned()
      .collect();
    found.sort();
    found
  }

  fn file_exists(&self, path: &Path) -> bool {
    path.is_file()
  }
}

/// Scans `files` (Java sources) as one project rooted at `root`, resolving library types
/// against `classpath` (jars, zips and class directories, declarations only).
pub fn analyze(root: &Path, files: &[PathBuf], classpath: &[String]) -> Result<Analysis> {
  let files: Vec<PathBuf> = files.iter().filter_map(|f| std::fs::canonicalize(f).ok()).collect();
  let root = project_root(root, &files);
  let out = tempfile::tempdir()?;
  let mut opts = ScanOptions {
    src_dirs: vec![root.clone()],
    project_root: Some(root.clone()),
    source_provider: Some(Arc::new(ListedSources { files: files.clone() })),
    ..ScanOptions::default()
  };
  opts.extras.modifiers = true;
  opts.extras.structural = true;
  opts.extras.type_reference_edges = true;
  opts.extras.override_edges = true;
  opts.extras.annotation_edges = true;
  opts.extras.field_reference_edges = true;
  opts.extras.ucfg_classpath = classpath.to_vec();

  let mut facts = HashMap::new();
  let mut calls = HashMap::new();
  if !files.is_empty() {
    run_with(opts, out.path(), &languages(), |opts| {
      let registry = overlay_registry();
      let (graph, timings, _failures, _artefacts) = scan_timed_with_registry(opts, &registry)?;
      facts = graph_facts(&graph);
      calls = call_sites(&graph);
      Ok(Scanned {
        graph,
        files: timings.into_iter().map(|t| t.path).collect(),
        settings: registry.settings_report().unwrap_or_else(|| Arc::clone(ResolvedSettings::empty())),
      })
    })?;
  }

  let rel: Vec<String> = files.iter().filter_map(|f| f.strip_prefix(&root).ok()).map(to_slash).collect();
  let dumps = rel
    .iter()
    .filter_map(|r| {
      let text = std::fs::read_to_string(out.path().join(format!("{r}{DUMP_SUFFIX}"))).ok()?;
      Some((r.clone(), serde_json::from_str(&text).ok()?))
    })
    .collect();
  let project_types = types::ProjectTypes::new(&facts);
  Ok(Analysis {
    root,
    files: rel,
    dumps,
    facts,
    calls,
    project_types,
    classpath: LazyClasspath::new(classpath.to_vec()),
  })
}

impl Analysis {
  pub fn root(&self) -> &Path {
    &self.root
  }

  /// Project-relative paths (forward slashes) of the analyzed files.
  pub fn files(&self) -> &[String] {
    &self.files
  }

  /// Declarations of the types named `names` (erased binary names) and of their supertypes.
  pub fn types(&self, names: &[String]) -> Value {
    Value::Object(types::lookup(names, &self.project_types, &self.facts, self.classpath.index()))
  }

  /// Resolves an absolute or project-relative path to the key used by [`Self::cst`].
  pub fn relative(&self, path: &str) -> Option<String> {
    if self.dumps.contains_key(path) || self.files.iter().any(|f| f == path) {
      return Some(path.to_string());
    }
    let canonical = std::fs::canonicalize(path).ok()?;
    canonical.strip_prefix(&self.root).ok().map(to_slash)
  }

  /// CST of one analyzed file with its semantics: tree, declared symbols and the graph facts
  /// of every symbol id the file mentions.
  pub fn cst(&self, path: &str) -> Result<Value> {
    let rel = self.relative(path).ok_or_else(|| anyhow!("not analyzed: {path}"))?;
    let source = std::fs::read(self.root.join(&rel)).with_context(|| format!("reading {rel}"))?;
    let dump = self.dumps.get(&rel);
    let calls = self.calls.get(&rel).map(Vec::as_slice).unwrap_or_default();
    let index = SemanticIndex::new(dump, calls, &source);
    let mut document = document(&source, &index)?;
    let symbols = dump.and_then(|d| d.get("symbols")).cloned().unwrap_or_else(|| json!([]));
    let facts: Map<String, Value> = index
      .ids
      .iter()
      .filter_map(|id| self.facts.get(id).map(|f| (id.clone(), f.clone())))
      .collect();
    let object = document.as_object_mut().expect("document is an object");
    object.insert("path".into(), Value::String(rel.clone()));
    object.insert("semantic".into(), Value::Bool(dump.is_some()));
    object.insert("symbols".into(), symbols);
    object.insert("facts".into(), Value::Object(facts));
    Ok(document)
  }
}

/// CST of `source` without semantics.
pub fn parse_only(source: &[u8]) -> Result<Value> {
  document(source, &SemanticIndex::default())
}

fn document(source: &[u8], index: &SemanticIndex) -> Result<Value> {
  let source = source.to_vec();
  let index = index.clone();
  std::thread::Builder::new()
    .stack_size(STACK_SIZE)
    .spawn(move || {
      let tree = parse(&source)?;
      let lines = Lines::new(&source);
      let root = tree.root_node();
      Ok(json!({
        "hasError": root.has_error(),
        "tree": node_json(root, None, None, &source, &lines, &index),
      }))
    })?
    .join()
    .map_err(|_| anyhow!("CST serialization panicked"))?
}

fn parse(source: &[u8]) -> Result<Tree> {
  let mut parser = Parser::new();
  parser.set_language(&tree_sitter_java::LANGUAGE.into())?;
  let tree = if source.starts_with(BOM) {
    let mut blanked = source.to_vec();
    blanked[..BOM.len()].fill(b' ');
    parser.parse(&blanked, None)
  } else {
    parser.parse(source, None)
  };
  tree.ok_or_else(|| anyhow!("tree-sitter returned no tree"))
}

/// One CST node. `called` is the graph call target of the enclosing `method_invocation`, used
/// for its `name` when the semantic dump has no occurrence there.
fn node_json(
  node: Node,
  field: Option<&str>,
  called: Option<&str>,
  source: &[u8],
  lines: &Lines,
  index: &SemanticIndex,
) -> Value {
  let mut object = Map::new();
  let (start, end) = (node.start_byte(), node.end_byte());
  object.insert("k".into(), Value::String(node.kind().to_string()));
  if node.is_named() {
    object.insert("n".into(), Value::Bool(true));
  }
  if let Some(field) = field {
    object.insert("f".into(), Value::String(field.to_string()));
  }
  if node.is_missing() {
    object.insert("m".into(), Value::Bool(true));
  }
  let (start_line, start_col) = lines.position(source, start);
  let (end_line, end_col) = lines.position(source, end);
  object.insert("p".into(), json!([start_line, start_col, end_line, end_col]));
  object.insert("b".into(), json!([start, end]));
  let call = if node.kind() == "method_invocation" {
    index.calls.get(&(start, end)).map(String::as_str)
  } else {
    None
  };
  let mut cursor = node.walk();
  let children: Vec<Value> = if cursor.goto_first_child() {
    let mut children = Vec::new();
    loop {
      let child_field = cursor.field_name();
      let child_call = call.filter(|_| child_field == Some("name"));
      children.push(node_json(cursor.node(), child_field, child_call, source, lines, index));
      if !cursor.goto_next_sibling() {
        break;
      }
    }
    children
  } else {
    Vec::new()
  };
  if children.is_empty() || is_literal(node.kind()) {
    object.insert(
      "t".into(),
      Value::String(String::from_utf8_lossy(&source[start..end]).into_owned()),
    );
  }
  if !children.is_empty() {
    object.insert("c".into(), Value::Array(children));
  }
  if children_free_or_name(node) {
    if let Some(decls) = index.decls.get(&(start, end)) {
      object.insert("decls".into(), json!(decls));
    }
    if let Some(refs) = index.refs.get(&(start, end)) {
      object.insert("refs".into(), Value::Array(refs.clone()));
    } else if let Some(target) = called {
      object.insert(
        "refs".into(),
        json!([{"access": "r", "outcome": "bound", "target": target, "reason": "graph-call"}]),
      );
    }
  }
  Value::Object(object)
}

fn children_free_or_name(node: Node) -> bool {
  node.child_count() == 0 || matches!(node.kind(), "this" | "super")
}

fn is_literal(kind: &str) -> bool {
  matches!(
    kind,
    "string_literal" | "character_literal" | "text_block" | "decimal_integer_literal"
      | "hex_integer_literal" | "octal_integer_literal" | "binary_integer_literal"
      | "decimal_floating_point_literal" | "hex_floating_point_literal"
  )
}

/// Byte offset to (1-based line, 0-based UTF-16 column), the convention of SonarJava tokens.
struct Lines {
  starts: Vec<usize>,
}

impl Lines {
  fn new(source: &[u8]) -> Self {
    let mut starts = vec![0];
    let mut i = 0;
    while i < source.len() {
      match source[i] {
        b'\n' => starts.push(i + 1),
        b'\r' if source.get(i + 1) != Some(&b'\n') => starts.push(i + 1),
        _ => {}
      }
      i += 1;
    }
    Lines { starts }
  }

  fn position(&self, source: &[u8], offset: usize) -> (usize, usize) {
    let line = self.starts.partition_point(|&s| s <= offset) - 1;
    let prefix = String::from_utf8_lossy(&source[self.starts[line]..offset]);
    (line + 1, prefix.trim_start_matches('\u{feff}').encode_utf16().count())
  }

  fn offset(&self, (line, column): (usize, usize)) -> Option<usize> {
    self.starts.get(line.checked_sub(1)?).map(|start| start + column)
  }
}

/// Semantic dump indexed by byte span.
#[derive(Default, Clone)]
struct SemanticIndex {
  decls: HashMap<(usize, usize), Vec<String>>,
  refs: HashMap<(usize, usize), Vec<Value>>,
  calls: HashMap<(usize, usize), String>,
  ids: BTreeSet<String>,
}

impl SemanticIndex {
  fn new(dump: Option<&Value>, calls: &[CallSite], source: &[u8]) -> Self {
    let mut index = SemanticIndex::default();
    let lines = Lines::new(source);
    for call in calls {
      if let (Some(start), Some(end)) = (lines.offset(call.start), lines.offset(call.end)) {
        index.ids.insert(call.target.clone());
        index.calls.insert((start, end), call.target.clone());
      }
    }
    let Some(dump) = dump else {
      return index;
    };
    for symbol in array(dump, "symbols") {
      let Some(id) = symbol.get("id").and_then(Value::as_str) else {
        continue;
      };
      index.ids.insert(id.to_string());
      collect_type_ids(symbol.pointer("/facts/type"), &mut index.ids);
      if let Some(span) = span(symbol) {
        index.decls.entry(span).or_default().push(id.to_string());
      }
    }
    for occurrence in array(dump, "occurrences") {
      let Some(span) = span(occurrence) else {
        continue;
      };
      let mut reference = occurrence.clone();
      if let Some(object) = reference.as_object_mut() {
        for key in ["start", "end", "name", "line", "col"] {
          object.remove(key);
        }
      }
      if let Some(target) = occurrence.get("target").and_then(Value::as_str) {
        index.ids.insert(target.to_string());
      }
      for candidate in occurrence
        .pointer("/facts/candidates")
        .and_then(Value::as_array)
        .into_iter()
        .flatten()
      {
        if let Some(id) = candidate.as_str().or_else(|| candidate.get("id").and_then(Value::as_str)) {
          index.ids.insert(id.to_string());
        }
      }
      index.refs.entry(span).or_default().push(reference);
    }
    index
  }
}

fn collect_type_ids(ty: Option<&Value>, ids: &mut BTreeSet<String>) {
  let Some(ty) = ty else {
    return;
  };
  if let Some(id) = ty.get("id").and_then(Value::as_str) {
    ids.insert(id.to_string());
  }
  collect_type_ids(ty.get("elem"), ids);
  for arg in ty.get("args").and_then(Value::as_array).into_iter().flatten() {
    collect_type_ids(Some(arg), ids);
  }
}

fn array<'a>(value: &'a Value, key: &str) -> impl Iterator<Item = &'a Value> {
  value.get(key).and_then(Value::as_array).into_iter().flatten()
}

fn span(value: &Value) -> Option<(usize, usize)> {
  Some((
    value.get("start")?.as_u64()? as usize,
    value.get("end")?.as_u64()? as usize,
  ))
}

/// A resolved call edge of the graph, located at its call site (1-based lines, 0-based columns).
#[derive(Clone)]
struct CallSite {
  start: (usize, usize),
  end: (usize, usize),
  target: String,
}

fn call_sites(graph: &UdgGraph) -> HashMap<String, Vec<CallSite>> {
  let mut sites: HashMap<String, Vec<CallSite>> = HashMap::new();
  for fqn in graph.items.keys() {
    for dependency in graph.get_downstream_dependencies(fqn.as_str()) {
      if dependency.kind != UdgDependencyKind::Calls {
        continue;
      }
      let DependencyTarget::Item(target) = &dependency.to_node else {
        continue;
      };
      if target.tier == NodeTier::ExternalUnknown {
        continue;
      }
      let Some(location) = &dependency.location else {
        continue;
      };
      let (Some(end_line), Some(end_offset)) = (location.end_line, location.end_line_offset) else {
        continue;
      };
      sites.entry(location.file.path.to_string()).or_default().push(CallSite {
        start: (location.start_line as usize, location.start_line_offset as usize),
        end: (end_line as usize, end_offset as usize),
        target: target.fqn.as_str().to_string(),
      });
    }
  }
  sites
}

fn graph_facts(graph: &UdgGraph) -> HashMap<String, Value> {
  graph
    .items
    .iter()
    .map(|(fqn, item)| {
      let mut edges: BTreeMap<String, Vec<String>> = BTreeMap::new();
      for dependency in graph.get_downstream_dependencies(fqn.as_str()) {
        let kind = serde_json::to_value(dependency.kind)
          .ok()
          .and_then(|k| k.as_str().map(str::to_string))
          .unwrap_or_default();
        if let Some((_, key)) = GRAPH_EDGES.iter().find(|(name, _)| *name == kind) {
          let targets = edges.entry((*key).to_string()).or_default();
          let target = dependency.to_node.key().to_string();
          if !targets.contains(&target) {
            targets.push(target);
          }
        }
      }
      let properties: Map<String, Value> = item
        .properties
        .iter()
        .map(|p| {
          let values: Vec<Value> = p.values.iter().map(|v| Value::String(v.value.to_string())).collect();
          (p.name.to_string(), Value::Array(values))
        })
        .collect();
      let mut object = Map::new();
      object.insert("kind".into(), serde_json::to_value(item.kind).unwrap_or(Value::Null));
      object.insert("tier".into(), serde_json::to_value(item.tier).unwrap_or(Value::Null));
      if let Some(parent) = &item.parent_item_fqn {
        object.insert("parent".into(), Value::String(parent.as_str().to_string()));
      }
      if !properties.is_empty() {
        object.insert("props".into(), Value::Object(properties));
      }
      for (kind, targets) in edges {
        object.insert(kind, json!(targets));
      }
      (fqn.as_str().to_string(), Value::Object(object))
    })
    .collect()
}

fn project_root(root: &Path, files: &[PathBuf]) -> PathBuf {
  let root = std::fs::canonicalize(root).unwrap_or_else(|_| root.to_path_buf());
  if files.iter().all(|f| f.starts_with(&root)) {
    return root;
  }
  let mut common = files.first().and_then(|f| f.parent()).map(Path::to_path_buf).unwrap_or(root);
  for file in files {
    while !file.starts_with(&common) {
      if !common.pop() {
        break;
      }
    }
  }
  common
}

fn to_slash(path: &Path) -> String {
  path.to_string_lossy().replace('\\', "/")
}
