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
package org.sonar.java.semsitter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactValue;
import org.sonarsource.semsitter.wire.Prop;
import org.sonarsource.semsitter.wire.TypeRef;

/** Stateless readers of the facts and member ids served by SemSitter. */
final class SemaFacts {

  private SemaFacts() {
  }

  private static final int MEMBER_IDS_LIMIT = 200_000;
  private static final Map<String, MemberId> MEMBER_IDS = new ConcurrentHashMap<>();
  private static final MemberId NOT_A_MEMBER = new MemberId("", "", null, false, -1, List.of());

  /** The name a member symbol created from {@code id} gets, or null when the id names no member. */
  @Nullable
  static String memberName(String id) {
    MemberId parsed = parseMember(id);
    return parsed == null ? null : parsed.name();
  }

  /** The declared type a document gives a symbol, or null when it gives none. */
  @Nullable
  static TypeRef typeFact(org.sonarsource.semsitter.wire.Symbol symbol) {
    FactValue value = symbol.getFactsMap().get("type");
    return value != null && value.hasType() ? value.getType() : null;
  }

  static void addTypeName(Set<String> names, @Nullable String id) {
    if (id == null || id.isEmpty()) {
      return;
    }
    int hash = id.indexOf('#');
    String owner = hash < 0 ? id : id.substring(0, hash);
    if (owner.indexOf('%') >= 0 || owner.indexOf('(') >= 0 || (owner.indexOf('<') >= 0 && hash < 0)) {
      return;
    }
    String erased = erase(owner);
    if (!erased.isEmpty() && !JSema.PRIMITIVES.contains(erased) && erased.indexOf(' ') < 0) {
      names.add(erased);
    }
  }

  static void collectFactNames(Set<String> names, TypeRef type) {
    addTypeName(names, type.getId());
    for (TypeRef argument : type.getArgumentsList()) {
      collectFactNames(names, argument);
    }
    if (type.hasElement()) {
      collectFactNames(names, type.getElement());
    }
  }

  static boolean isWellKnown(String fqn) {
    return "java.lang.Object".equals(fqn) || "java.lang.Cloneable".equals(fqn) || "java.io.Serializable".equals(fqn);
  }

  /** The graph fact a {@code types} answer gives a project member, in the shape a document gives it. */
  static Fact fact(JsonObject json) {
    Objects.requireNonNull(json);
    Fact.Builder fact = Fact.newBuilder()
      .setKind(Objects.requireNonNullElse(text(json, "kind"), ""))
      .setParent(Objects.requireNonNullElse(text(json, "parent"), ""))
      .addAllExtends(strings(json, JSema.EXTENDS))
      .addAllImplements(strings(json, "implements"))
      .addAllReturns(strings(json, "returns"))
      .addAllType(strings(json, "type"))
      .addAllParams(strings(json, "params"))
      .addAllAnnotations(strings(json, JSema.ANNOTATIONS));
    if (json.get("props") instanceof JsonObject props) {
      for (String name : props.keySet()) {
        fact.addProps(Prop.newBuilder().setName(name).addAllValues(strings(props, name)));
      }
    }
    return fact.build();
  }

  static String parameterKey(String methodId, int index) {
    return methodId + "@param" + index;
  }

  /** A constant spelled as text, as the boxed value of the field's primitive type. */
  static Object constantOf(@Nullable Type type, String text) {
    String name = type == null ? "" : type.fullyQualifiedName();
    try {
      return switch (name) {
        case "int" -> Integer.valueOf(text);
        case "long" -> Long.valueOf(text);
        case "short" -> Short.valueOf(text);
        case "byte" -> Byte.valueOf(text);
        case "float" -> Float.valueOf(text);
        case "double" -> Double.valueOf(text);
        case "boolean" -> Boolean.valueOf(text);
        case "char" -> text.length() == 1 ? Integer.valueOf(text.charAt(0)) : text;
        default -> text;
      };
    } catch (NumberFormatException e) {
      return text;
    }
  }

  static boolean isTypeFact(Fact fact) {
    String kind = fact.getKind();
    return "class".equals(kind) || "interface".equals(kind) || "enum".equals(kind) || JSema.RECORD.equals(kind)
      || "annotation".equals(kind) || "exception".equals(kind);
  }

  static void applyMemberFlags(JSymbol symbol, JsonObject member) {
    List<String> modifiers = new ArrayList<>();
    if (truth(member, JSema.STATIC)) {
      modifiers.add(JSema.STATIC);
    }
    if (truth(member, "final")) {
      modifiers.add("final");
    }
    if (truth(member, "synchronized")) {
      modifiers.add("synchronized");
    }
    if (truth(member, JSema.ABSTRACT)) {
      modifiers.add(JSema.ABSTRACT);
    }
    String visibility = text(member, "vis");
    if (visibility != null && !"package".equals(visibility)) {
      modifiers.add(visibility);
    }
    symbol.addModifiers(modifiers);
  }

  static List<String> parents(JsonObject info) {
    List<String> parents = new ArrayList<>();
    String superName = text(info, "super");
    if (superName != null) {
      parents.add(superName);
    }
    parents.addAll(strings(info, "interfaces"));
    parents.addAll(strings(info, "super"));
    return parents;
  }

  /** Member ids repeat across lookups and files, so their parse is kept; the cache is dropped when it grows large. */
  @Nullable
  static MemberId parseMember(String id) {
    MemberId cached = MEMBER_IDS.get(id);
    if (cached == null) {
      cached = parseMemberUncached(id);
      if (MEMBER_IDS.size() >= MEMBER_IDS_LIMIT) {
        MEMBER_IDS.clear();
      }
      MEMBER_IDS.put(id, cached == null ? NOT_A_MEMBER : cached);
    }
    return cached == NOT_A_MEMBER ? null : cached;
  }

  @Nullable
  private static MemberId parseMemberUncached(String id) {
    int hash = id.indexOf('#');
    if (hash < 0) {
      return null;
    }
    String owner = id.substring(0, hash);
    String rest = id.substring(hash + 1);
    int paren = rest.endsWith(")") ? rest.lastIndexOf('(') : -1;
    if (paren < 0) {
      return new MemberId(owner, rest, null, false, -1, List.of());
    }
    String name = rest.substring(0, paren);
    String params = rest.substring(paren + 1, rest.length() - 1);
    boolean arityOnly = !params.isEmpty() && params.chars().allMatch(Character::isDigit);
    List<String> paramList = splitParams(params);
    int arity = arityOnly ? Integer.parseInt(params) : paramList.size();
    return new MemberId(owner, name, params, arityOnly, arity, paramList);
  }

  private static List<String> splitParams(String params) {
    if (params.isEmpty()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    int depth = 0;
    int start = 0;
    for (int i = 0; i < params.length(); i++) {
      char current = params.charAt(i);
      if (current == '<') {
        depth++;
      } else if (current == '>') {
        depth--;
      } else if (current == ',' && depth == 0) {
        result.add(params.substring(start, i));
        start = i + 1;
      }
    }
    result.add(params.substring(start));
    return result;
  }

  static String erase(String id) {
    if (id.indexOf('`') < 0) {
      return id;
    }
    StringBuilder out = new StringBuilder(id.length());
    int i = 0;
    while (i < id.length()) {
      char current = id.charAt(i);
      i++;
      if (current == '`') {
        while (i < id.length() && Character.isDigit(id.charAt(i))) {
          i++;
        }
      } else {
        out.append(current);
      }
    }
    return out.toString();
  }

  static String simpleName(String fqn) {
    int boundary = Math.max(fqn.lastIndexOf('.'), fqn.lastIndexOf('$'));
    String name = boundary < 0 ? fqn : fqn.substring(boundary + 1);
    return isAnonymousName(name) ? "" : name;
  }

  /** An anonymous class is named by its ordinal, which a source name never is. */
  static boolean isAnonymousName(String name) {
    return !name.isEmpty() && name.chars().allMatch(Character::isDigit);
  }

  static String packageName(String fqn) {
    String top = fqn;
    int nested = top.indexOf('$');
    if (nested >= 0) {
      top = top.substring(0, nested);
    }
    int dot = top.lastIndexOf('.');
    return dot < 0 ? "" : top.substring(0, dot);
  }

  @Nullable
  static String text(@Nullable JsonObject object, String key) {
    JsonElement element = object == null ? null : object.get(key);
    return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
  }

  @Nullable
  static String text(String value) {
    return value.isEmpty() ? null : value;
  }

  /** The values of the last property named {@code name}, as a later key of a JSON object replaces an earlier one. */
  static List<String> prop(Fact fact, String name) {
    List<String> values = List.of();
    for (Prop prop : fact.getPropsList()) {
      if (name.equals(prop.getName())) {
        values = prop.getValuesList();
      }
    }
    return values;
  }

  static boolean truth(JsonObject object, String key) {
    return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsBoolean();
  }

  static List<String> strings(JsonObject object, String key) {
    if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
      return List.of();
    }
    JsonElement element = object.get(key);
    if (element.isJsonPrimitive()) {
      return List.of(element.getAsString());
    }
    if (!element.isJsonArray()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    for (JsonElement item : element.getAsJsonArray()) {
      if (item.isJsonPrimitive()) {
        values.add(item.getAsString());
      }
    }
    return values;
  }

  static List<String> flat(JsonObject object, String key) {
    return flat(strings(object, key));
  }

  static List<String> flat(List<String> strings) {
    List<String> values = new ArrayList<>();
    for (String value : strings) {
      for (String part : value.split(",")) {
        if (!part.isBlank()) {
          values.add(part.trim());
        }
      }
    }
    return values;
  }
}
