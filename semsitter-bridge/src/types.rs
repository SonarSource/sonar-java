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

//! Type declarations by erased binary name (`java.util.Map$Entry`), from the first source that
//! knows the type: the scanned project graph, the classpath index (jars and the JDK runtime
//! image), then the JDK table SemSitter embeds.

use std::collections::{BTreeSet, HashMap, VecDeque};

use serde_json::{Map, Value, json};
use udg_family_jvm::external::classpath::classfile::{Annotation, Element};
use udg_family_jvm::external::classpath::{ClasspathIndex, ClasspathType};
use udg_family_jvm::external::jdk_table::{JdkMemberKind, JdkType, jdk_type, parameter_type_variables};

const ACC_STATIC: u16 = 0x0008;
const ACC_FINAL: u16 = 0x0010;
const ACC_PUBLIC: u16 = 0x0001;
const ACC_PRIVATE: u16 = 0x0002;
const ACC_PROTECTED: u16 = 0x0004;
const ACC_ENUM: u16 = 0x4000;
const ACC_ABSTRACT: u16 = 0x0400;

/// Strips the generic arity suffix of a graph id segment: ``java.util.List`1`` is `java.util.List`.
pub fn erase(id: &str) -> String {
  let mut out = String::with_capacity(id.len());
  let mut chars = id.chars().peekable();
  while let Some(c) = chars.next() {
    if c == '`' {
      while chars.peek().is_some_and(char::is_ascii_digit) {
        chars.next();
      }
    } else {
      out.push(c);
    }
  }
  out
}

/// Project types of the graph: erased name to graph id, and graph id to member ids.
#[derive(Default)]
pub struct ProjectTypes {
  pub keys: HashMap<String, String>,
  pub members: HashMap<String, Vec<String>>,
}

impl ProjectTypes {
  pub fn new(facts: &HashMap<String, Value>) -> Self {
    let mut types = ProjectTypes::default();
    for (id, fact) in facts {
      if fact.get("tier").and_then(Value::as_str) != Some("IN_PROJECT") {
        continue;
      }
      if let Some(parent) = fact.get("parent").and_then(Value::as_str) {
        types.members.entry(parent.to_string()).or_default().push(id.clone());
      }
      if !id.contains('#') && is_type_kind(fact.get("kind").and_then(Value::as_str).unwrap_or_default()) {
        types.keys.insert(erase(id), id.clone());
      }
    }
    for members in types.members.values_mut() {
      members.sort();
    }
    types
  }
}

fn is_type_kind(kind: &str) -> bool {
  matches!(kind, "class" | "interface" | "enum" | "record" | "annotation" | "exception")
}

/// Declarations of `names` and of all their supertypes.
pub fn lookup(
  names: &[String],
  project: &ProjectTypes,
  facts: &HashMap<String, Value>,
  classpath: &ClasspathIndex,
) -> Map<String, Value> {
  let mut found = Map::new();
  let mut queue: VecDeque<String> = names.iter().map(|n| erase(n)).collect();
  let mut seen: BTreeSet<String> = BTreeSet::new();
  while let Some(name) = queue.pop_front() {
    if !seen.insert(name.clone()) {
      continue;
    }
    let info = project_type(&name, project, facts)
      .or_else(|| classpath.get(&name).map(|ty| classpath_type(&name, ty)))
      .or_else(|| jdk_type(&name).map(jdk_type_info));
    let Some(info) = info else {
      continue;
    };
    for key in ["super", "interfaces"] {
      for supertype in strings(&info, key) {
        queue.push_back(erase(&supertype));
      }
    }
    found.insert(name, info);
  }
  found
}

fn strings(value: &Value, key: &str) -> Vec<String> {
  match value.get(key) {
    Some(Value::String(s)) => vec![s.clone()],
    Some(Value::Array(items)) => items.iter().filter_map(|i| i.as_str().map(str::to_string)).collect(),
    _ => Vec::new(),
  }
}

fn project_type(name: &str, project: &ProjectTypes, facts: &HashMap<String, Value>) -> Option<Value> {
  let id = project.keys.get(name)?;
  let fact = facts.get(id)?;
  let kind = fact.get("kind").and_then(Value::as_str).unwrap_or("class");
  let extends = strings(fact, "extends");
  let implements = strings(fact, "implements");
  let (superclass, interfaces) = if kind == "interface" || kind == "annotation" {
    (None, extends)
  } else {
    (extends.into_iter().next(), implements)
  };
  let members: Vec<Value> = project
    .members
    .get(id)
    .into_iter()
    .flatten()
    .filter_map(|member| facts.get(member).map(|f| json!({"id": member, "facts": f})))
    .collect();
  Some(json!({
    "origin": "project",
    "id": id,
    "kind": kind,
    "super": superclass,
    "interfaces": interfaces,
    "modifiers": fact.pointer("/props/modifiers").cloned().unwrap_or_else(|| json!([])),
    "members": members,
  }))
}

fn classpath_type(name: &str, ty: &ClasspathType) -> Value {
  let parameters = ty.signature.as_deref().map(type_parameters).unwrap_or_default();
  let mut members: Vec<Value> = ty
    .methods
    .iter()
    .map(|m| {
      let returned = m.signature.as_deref().and_then(|s| class_return_parameter(s, &parameters));
      json!({
        "k": "m",
        "name": m.name,
        "desc": m.descriptor,
        "static": m.is_static,
        "abstract": m.is_abstract,
        "varargs": m.is_varargs,
        "vis": "public",
        "retParam": returned,
        "retSelf": returned.is_some_and(|index| parameters[index].1 == name),
        "paramVars": m.signature.as_deref().map(|s| parameter_variables(s, &parameters)),
        "paramTypes": m.signature.as_deref().map(parameter_types),
        "typeVars": m.signature.as_deref().map(|s| type_parameters(s).into_iter().map(|(name, _)| name).collect::<Vec<_>>()),
        "annotations": annotations_json(&m.annotations),
      })
    })
    .collect();
  members.extend(ty.fields.iter().filter_map(|f| {
    let desc = f.declared_type.as_ref()?;
    Some(json!({
      "k": "f",
      "name": f.name,
      "desc": desc,
      "static": f.access_flags & ACC_STATIC != 0,
      "final": f.access_flags & ACC_FINAL != 0,
      "enum": f.access_flags & ACC_ENUM != 0,
      "vis": access_visibility(f.access_flags),
      "constant": f.constant_string,
      "annotations": annotations_json(&f.annotations),
    }))
  }));
  let mut info = json!({
    "origin": "classpath",
    "kind": classpath_kind(ty),
    "super": ty.super_class,
    "interfaces": ty.interfaces,
    "modifiers": classpath_modifiers(ty.access_flags),
    "annotations": annotations_json(&ty.annotations),
    "typeParameters": parameters.len(),
    "typeVars": parameters.iter().map(|(name, _)| name).collect::<Vec<_>>(),
    "members": members,
  });
  if let (Some(signature), Some(object)) = (ty.signature.as_deref(), info.as_object_mut()) {
    let (superclass, interfaces) = signature_supertypes(signature);
    object.insert("superType".into(), json!(superclass));
    object.insert("interfaceTypes".into(), json!(interfaces));
  }
  info
}

fn annotations_json(annotations: &[Annotation]) -> Value {
  Value::Array(annotations.iter().map(annotation_json).collect())
}

fn annotation_json(annotation: &Annotation) -> Value {
  json!({
    "name": annotation.name,
    "values": annotation.pairs.iter().map(|(name, value)| element_json(name, value)).collect::<Vec<_>>(),
  })
}

fn element_json(name: &str, value: &Element) -> Value {
  match value {
    Element::Boolean(value) => json!({"name": name, "kind": "boolean", "value": value}),
    Element::Byte(value) => json!({"name": name, "kind": "byte", "value": value}),
    Element::Char(value) => json!({"name": name, "kind": "char", "value": value}),
    Element::Short(value) => json!({"name": name, "kind": "short", "value": value}),
    Element::Int(value) => json!({"name": name, "kind": "int", "value": value}),
    Element::Long(value) => json!({"name": name, "kind": "long", "value": value}),
    Element::Float(bits) => json!({"name": name, "kind": "float", "value": f32::from_bits(*bits)}),
    Element::Double(bits) => json!({"name": name, "kind": "double", "value": f64::from_bits(*bits)}),
    Element::String(value) => json!({"name": name, "kind": "string", "value": value}),
    Element::Class(value) => json!({"name": name, "kind": "class", "value": value}),
    Element::Enum { type_name, const_name } => json!({"name": name, "kind": "enum", "type": type_name, "const": const_name}),
    Element::Annotation(annotation) => json!({"name": name, "kind": "annotation", "value": annotation_json(annotation)}),
    Element::Array(items) => json!({
      "name": name,
      "kind": "array",
      "value": items.iter().map(|item| element_json("value", item)).collect::<Vec<_>>(),
    }),
  }
}

/// Class-file access flags as the modifier names `JSema` already applies to JDK types.
fn classpath_modifiers(flags: u16) -> Vec<&'static str> {
  let mut modifiers = Vec::new();
  if flags & ACC_PUBLIC != 0 {
    modifiers.push("public");
  } else if flags & ACC_PROTECTED != 0 {
    modifiers.push("protected");
  } else if flags & ACC_PRIVATE != 0 {
    modifiers.push("private");
  }
  if flags & ACC_STATIC != 0 {
    modifiers.push("static");
  }
  if flags & ACC_FINAL != 0 {
    modifiers.push("final");
  }
  if flags & ACC_ABSTRACT != 0 {
    modifiers.push("abstract");
  }
  modifiers
}

/// The classfile reader only distinguishes classes from interfaces. A record is a class whose superclass is `java.lang.Record`.
fn classpath_kind(ty: &ClasspathType) -> Value {
  if ty.super_class.as_deref() == Some("java.lang.Record") {
    return json!("record");
  }
  serde_json::to_value(ty.kind).unwrap_or(Value::Null)
}

/// Type parameters of a generic signature, `<K:Ljava/lang/Object;V:...>...`, with the erased dotted
/// class of their first bound.
fn type_parameters(signature: &str) -> Vec<(String, String)> {
  let bytes = signature.as_bytes();
  let mut out = Vec::new();
  if bytes.first() != Some(&b'<') {
    return out;
  }
  let mut i = 1;
  while i < bytes.len() && bytes[i] != b'>' {
    let start = i;
    while i < bytes.len() && bytes[i] != b':' {
      i += 1;
    }
    let name = signature[start..i].to_string();
    let mut bound = String::new();
    while i < bytes.len() && bytes[i] == b':' {
      i += 1;
      if i < bytes.len() && bytes[i] != b':' && bytes[i] != b'>' {
        let begin = i;
        i = skip_type(bytes, i);
        if bound.is_empty() {
          bound = erased_class(&signature[begin..i]);
        }
      }
    }
    out.push((name, bound));
  }
  out
}

fn skip_type(bytes: &[u8], mut i: usize) -> usize {
  match bytes.get(i) {
    Some(b'[') => skip_type(bytes, i + 1),
    Some(b'L') => {
      let mut depth = 0;
      while i < bytes.len() {
        match bytes[i] {
          b'<' => depth += 1,
          b'>' => depth -= 1,
          b';' if depth == 0 => return i + 1,
          _ => {}
        }
        i += 1;
      }
      i
    }
    Some(b'T') => bytes[i..].iter().position(|&b| b == b';').map_or(bytes.len(), |end| i + end + 1),
    _ => i + 1,
  }
}

/// The generic superclass and interfaces of a class `Signature`, as type facts (`id`, `args`,
/// `kind` `typevar`/`array`/`wildcard`).
fn signature_supertypes(signature: &str) -> (Option<Value>, Vec<Value>) {
  let bytes = signature.as_bytes();
  let mut i = 0;
  if bytes.first() == Some(&b'<') {
    let mut depth = 0;
    while i < bytes.len() {
      match bytes[i] {
        b'<' => depth += 1,
        b'>' => depth -= 1,
        _ => {}
      }
      i += 1;
      if depth == 0 {
        break;
      }
    }
  }
  let superclass = (i < bytes.len()).then(|| signature_type(signature, &mut i));
  let mut interfaces = Vec::new();
  while i < bytes.len() {
    interfaces.push(signature_type(signature, &mut i));
  }
  (superclass, interfaces)
}

/// One JVM type signature starting at `i`, which is moved past it.
fn signature_type(signature: &str, i: &mut usize) -> Value {
  let bytes = signature.as_bytes();
  let Some(&head) = bytes.get(*i) else {
    return json!({"kind": "unknown"});
  };
  *i += 1;
  match head {
    b'[' => json!({"kind": "array", "elem": signature_type(signature, i)}),
    b'T' => {
      let end = signature[*i..].find(';').map_or(bytes.len(), |e| *i + e);
      let name = &signature[*i..end];
      *i = end + 1;
      json!({"kind": "typevar", "id": name})
    }
    b'*' => json!({"kind": "wildcard"}),
    b'+' | b'-' => {
      signature_type(signature, i);
      json!({"kind": "wildcard"})
    }
    b'L' => {
      let mut name = Vec::new();
      let mut args = Vec::new();
      while *i < bytes.len() {
        match bytes[*i] {
          b';' => {
            *i += 1;
            break;
          }
          b'<' => {
            *i += 1;
            args.clear();
            while *i < bytes.len() && bytes[*i] != b'>' {
              args.push(signature_type(signature, i));
            }
            *i += 1;
          }
          b'.' => {
            name.push(b'$');
            *i += 1;
          }
          b'/' => {
            name.push(b'.');
            *i += 1;
          }
          c => {
            name.push(c);
            *i += 1;
          }
        }
      }
      json!({"id": String::from_utf8_lossy(&name), "args": args})
    }
    primitive => json!({"id": primitive_name(primitive)}),
  }
}

fn primitive_name(code: u8) -> &'static str {
  match code {
    b'B' => "byte",
    b'C' => "char",
    b'D' => "double",
    b'F' => "float",
    b'I' => "int",
    b'J' => "long",
    b'S' => "short",
    b'Z' => "boolean",
    _ => "void",
  }
}

fn erased_class(signature: &str) -> String {
  signature
    .strip_prefix('L')
    .map(|rest| rest.split(['<', ';']).next().unwrap_or_default().replace('/', "."))
    .unwrap_or_default()
}

/// The JDK table's class and method parameter type variables, in the shape of [`parameter_variables`].
fn jdk_parameter_variables(class: Option<&str>, method: Option<&str>) -> Option<Vec<String>> {
  if class.is_none() && method.is_none() {
    return None;
  }
  let class = class.map(parameter_type_variables).unwrap_or_default();
  let method = method.map(parameter_type_variables).unwrap_or_default();
  let slot = |prefix: char, slots: &[Option<(usize, &str)>], i: usize| {
    slots.get(i).copied().flatten().map(|(index, array)| format!("{prefix}{index}{array}"))
  };
  Some(
    (0..class.len().max(method.len()))
      .map(|i| slot('c', &class, i).or_else(|| slot('m', &method, i)).unwrap_or_default())
      .collect(),
  )
}

/// Per parameter of a method signature, the type variable it is: `c1` for the owner's second type
/// parameter, `m0[]` for an array of the method's first one, or empty.
fn parameter_variables(signature: &str, parameters: &[(String, String)]) -> Vec<String> {
  let own = type_parameters(signature);
  let bytes = signature.as_bytes();
  let Some(open) = signature.find('(') else {
    return Vec::new();
  };
  let mut out = Vec::new();
  let mut i = open + 1;
  while i < bytes.len() && bytes[i] != b')' {
    let end = skip_type(bytes, i);
    let parameter = &signature[i..end];
    let element = parameter.trim_start_matches('[');
    let array = "[]".repeat(parameter.len() - element.len());
    let variable = element.strip_prefix('T').and_then(|v| v.strip_suffix(';'));
    out.push(match variable {
      Some(name) => match own.iter().position(|(p, _)| p == name) {
        Some(index) => format!("m{index}{array}"),
        None => parameters.iter().position(|(p, _)| p == name).map_or_else(String::new, |index| format!("c{index}{array}")),
      },
      None => String::new(),
    });
    i = end;
  }
  out
}

/// The declared parameter types of a method `Signature`, as type facts keeping the type variables
/// they are written in terms of, so that the caller can substitute them for the receiver's arguments.
fn parameter_types(signature: &str) -> Vec<Value> {
  let bytes = signature.as_bytes();
  let Some(open) = signature.find('(') else {
    return Vec::new();
  };
  let mut i = open + 1;
  let mut out = Vec::new();
  while i < bytes.len() && bytes[i] != b')' {
    out.push(signature_type(signature, &mut i));
  }
  out
}

/// Index of the class type parameter a method signature returns, when it returns one directly.
fn class_return_parameter(signature: &str, parameters: &[(String, String)]) -> Option<usize> {
  let returned = signature[signature.rfind(')')? + 1..].strip_prefix('T')?;
  let name = &returned[..returned.find(';')?];
  if type_parameters(signature).iter().any(|(own, _)| own == name) {
    return None;
  }
  parameters.iter().position(|(parameter, _)| parameter == name)
}

fn access_visibility(flags: u16) -> &'static str {
  if flags & ACC_PUBLIC != 0 {
    "public"
  } else if flags & ACC_PROTECTED != 0 {
    "protected"
  } else if flags & ACC_PRIVATE != 0 {
    "private"
  } else {
    "package"
  }
}

fn jdk_type_info(ty: &JdkType) -> Value {
  let is_enum = serde_json::to_value(ty.kind).ok().and_then(|k| k.as_str().map(|k| k == "enum")).unwrap_or(false);
  let own_descriptor = format!("L{};", ty.fqn.replace('.', "/"));
  let members: Vec<Value> = ty
    .members
    .iter()
    .map(|m| {
      let constant = is_enum && m.kind == JdkMemberKind::Field && m.is_static && m.is_final && m.descriptor == own_descriptor;
      json!({
        "enum": constant,
        "k": if m.kind == JdkMemberKind::Method { "m" } else { "f" },
        "name": m.name,
        "desc": m.descriptor,
        "static": m.is_static,
        "final": m.is_final,
        "abstract": m.is_abstract,
        "vis": format!("{:?}", m.visibility).to_lowercase(),
        "retParam": m.return_type_class_param_index,
        "paramVars": jdk_parameter_variables(m.parameter_type_variables, m.parameter_method_type_variables),
      })
    })
    .collect();
  let mut modifiers = Vec::new();
  if ty.is_public {
    modifiers.push("public");
  }
  if ty.is_abstract {
    modifiers.push("abstract");
  }
  if ty.is_sealed {
    modifiers.push("sealed");
  }
  if is_enum {
    modifiers.push("final");
  }
  json!({
    "origin": "jdk",
    "kind": serde_json::to_value(ty.kind).unwrap_or(Value::Null),
    "super": ty.super_type,
    "interfaces": ty.interfaces,
    "modifiers": modifiers,
    "typeParameters": ty.type_parameters,
    "members": members,
  })
}

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn supertypes_keep_type_arguments_and_type_variables() {
    let (superclass, interfaces) = signature_supertypes(
      "<E:Ljava/lang/Object;>Ljava/util/AbstractList<TE;>;Ljava/util/List<[TE;>;Ljava/util/Map$Entry<*+Ljava/lang/Number;>;",
    );
    assert_eq!(superclass, Some(json!({"id": "java.util.AbstractList", "args": [{"kind": "typevar", "id": "E"}]})));
    assert_eq!(
      interfaces,
      vec![
        json!({"id": "java.util.List", "args": [{"kind": "array", "elem": {"kind": "typevar", "id": "E"}}]}),
        json!({"id": "java.util.Map$Entry", "args": [{"kind": "wildcard"}, {"kind": "wildcard"}]}),
      ]
    );
    // A member of a generic outer class keeps only its own arguments.
    let (superclass, _) = signature_supertypes("Lp/Outer<TT;>.Inner<Ljava/lang/String;>;");
    assert_eq!(superclass, Some(json!({"id": "p.Outer$Inner", "args": [{"id": "java.lang.String", "args": []}]})));
  }
}
