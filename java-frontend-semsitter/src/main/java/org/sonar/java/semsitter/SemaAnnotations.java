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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.model.AnnotationValueImpl;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.semsitter.wire.Fact;

import static org.sonar.java.semsitter.SemaFacts.erase;
import static org.sonar.java.semsitter.SemaFacts.parameterKey;
import static org.sonar.java.semsitter.SemaFacts.parents;
import static org.sonar.java.semsitter.SemaFacts.strings;
import static org.sonar.java.semsitter.SemaFacts.text;

/** Annotations declared on project and library symbols, and their values. */
final class SemaAnnotations {

  private static final String SUPER = "super";

  private final JSema sema;

  SemaAnnotations(JSema sema) {
    this.sema = sema;
  }

  List<DeclaredAnnotation> declaredAnnotations(@Nullable String id) {
    List<DeclaredAnnotation> typed = sema.declaredMembers.libraryAnnotations.get(id);
    if (typed != null) {
      return typed;
    }
    Fact fact = sema.facts.get(id);
    return fact == null ? List.of() : fact.getAnnotationsList().stream().map(name -> new DeclaredAnnotation(erase(name), List.of(), false)).toList();
  }

  /** Annotations written directly on the parameters of a method, as the member's type-annotation facts tell them. */
  void registerParameterAnnotations(String methodId, JsonObject member) {
    if (!(member.get("facts") instanceof JsonObject facts && facts.get("props") instanceof JsonObject props)) {
      return;
    }
    for (String encoded : strings(props, "type_annotation_values")) {
      JsonArray entries = parseArray(encoded);
      if (entries == null) {
        continue;
      }
      for (JsonElement entry : entries) {
        if (entry instanceof JsonObject annotation && "param".equals(text(annotation, "on")) && annotation.get("path") instanceof JsonArray path && path.isEmpty()
          && annotation.get("index") instanceof JsonPrimitive index && text(annotation, "id") != null) {
          sema.declaredMembers.libraryAnnotations.computeIfAbsent(parameterKey(methodId, index.getAsInt()), key -> new ArrayList<>())
            .add(new DeclaredAnnotation(text(annotation, "id"), List.of()));
        }
      }
    }
  }

  @Nullable
  private static JsonArray parseArray(String encoded) {
    try {
      return JsonParser.parseString(encoded) instanceof JsonArray entries ? entries : null;
    } catch (JsonParseException e) {
      return null;
    }
  }

  /** Annotations and deprecation a {@code types} answer gives a library type or member. */
  void libraryAnnotations(JSymbol symbol, String id, JsonObject info) {
    if (info.get("parameterAnnotations") instanceof JsonArray parameters) {
      for (int i = 0; i < parameters.size(); i++) {
        List<DeclaredAnnotation> onParameter = readAnnotationList(parameters.get(i));
        if (!onParameter.isEmpty()) {
          sema.declaredMembers.libraryAnnotations.putIfAbsent(parameterKey(id, i), onParameter);
        }
      }
    }
    List<DeclaredAnnotation> annotations = readAnnotations(info);
    if (annotations.isEmpty()) {
      return;
    }
    sema.declaredMembers.libraryAnnotations.put(id, annotations);
    if (annotations.stream().anyMatch(annotation -> "java.lang.Deprecated".equals(annotation.name()))) {
      symbol.deprecated(true);
    }
  }

  /** The annotations of the package {@code name}, from its compiled {@code package-info}. */
  List<DeclaredAnnotation> packageAnnotations(String name) {
    if (!sema.typesLoaded) {
      return List.of();
    }
    List<DeclaredAnnotation> cached = sema.declaredMembers.packageAnnotations.get(name);
    if (cached == null) {
      String key = name + ".package-info";
      JsonObject loaded = sema.typeLoader.apply(Set.of(key));
      cached = loaded.get(key) instanceof JsonObject info ? readAnnotations(info) : List.of();
      sema.declaredMembers.packageAnnotations.put(name, cached);
    }
    return cached;
  }

  /** Annotations written on the outermost type of a field or return type are annotations of the member. */
  List<DeclaredAnnotation> readAnnotations(JsonObject info) {
    List<DeclaredAnnotation> annotations = new ArrayList<>(readAnnotationList(info.get(JSema.ANNOTATIONS)));
    if (info.get("typeAnnotations") instanceof JsonArray typed) {
      addOutermostTypeAnnotations(annotations, typed);
    }
    return annotations;
  }

  private void addOutermostTypeAnnotations(List<DeclaredAnnotation> annotations, JsonArray typed) {
    for (JsonElement element : typed) {
      if (element instanceof JsonObject entry && "type".equals(text(entry, "on")) && entry.get("path") instanceof JsonArray path && path.isEmpty()) {
        JsonArray one = new JsonArray();
        one.add(entry);
        for (DeclaredAnnotation annotation : readAnnotationList(one)) {
          if (annotations.stream().noneMatch(known -> known.name().equals(annotation.name()))) {
            annotations.add(annotation);
          }
        }
      }
    }
  }

  private List<DeclaredAnnotation> readAnnotationList(@Nullable JsonElement list) {
    if (!(list instanceof JsonArray array)) {
      return List.of();
    }
    List<DeclaredAnnotation> annotations = new ArrayList<>();
    for (JsonElement element : array) {
      if (element.isJsonPrimitive()) {
        annotations.add(new DeclaredAnnotation(element.getAsString(), List.of()));
      } else if (element.isJsonObject()) {
        JsonObject annotation = element.getAsJsonObject();
        String name = text(annotation, "name");
        if (name != null) {
          annotations.add(new DeclaredAnnotation(name, readAnnotationValues(annotation.get("values"))));
        }
      }
    }
    return annotations;
  }

  private List<SymbolMetadata.AnnotationValue> readAnnotationValues(@Nullable JsonElement element) {
    if (!(element instanceof JsonArray array)) {
      return List.of();
    }
    List<SymbolMetadata.AnnotationValue> values = new ArrayList<>();
    for (JsonElement item : array) {
      if (item.isJsonObject()) {
        JsonObject value = item.getAsJsonObject();
        String name = text(value, "name");
        values.add(new AnnotationValueImpl(name == null ? "value" : name, readAnnotationElement(value)));
      }
    }
    return values;
  }

  @Nullable
  Object readAnnotationElement(JsonObject value) {
    Objects.requireNonNull(value);
    String kind = text(value, "kind");
    JsonElement raw = value.get("value");
    if (kind == null) {
      return null;
    }
    return switch (kind) {
      case "boolean" -> Boolean.valueOf(raw instanceof JsonPrimitive primitive && primitive.getAsBoolean());
      case "enum" -> sema.lookup().enumConstant(text(value, "type"), text(value, "const"));
      case "array" -> readAnnotationArray(raw);
      case "annotation" -> raw instanceof JsonObject annotation ? text(annotation, "name") : null;
      default -> raw instanceof JsonPrimitive primitive ? readPrimitive(kind, primitive) : null;
    };
  }

  @Nullable
  private Object readPrimitive(String kind, JsonPrimitive raw) {
    return switch (kind) {
      case "byte" -> Byte.valueOf(raw.getAsByte());
      case "short" -> Short.valueOf(raw.getAsShort());
      case "int" -> Integer.valueOf(raw.getAsInt());
      case "long" -> Long.valueOf(raw.getAsLong());
      case "float" -> Float.valueOf(raw.getAsFloat());
      case "double" -> Double.valueOf(raw.getAsDouble());
      case "char" -> Character.valueOf((char) raw.getAsInt());
      case "string" -> raw.getAsString();
      case "class" -> sema.typeSymbol(erase(raw.getAsString()));
      default -> null;
    };
  }

  private Object[] readAnnotationArray(@Nullable JsonElement element) {
    if (!(element instanceof JsonArray array)) {
      return new Object[0];
    }
    List<Object> values = new ArrayList<>();
    for (JsonElement item : array) {
      values.add(item.isJsonObject() ? readAnnotationElement(item.getAsJsonObject()) : null);
    }
    return values.toArray();
  }

  /**
   * The annotations written on type argument {@code position} of the supertype {@code declaring}, in the supertype clause
   * of {@code owner} or of one of its supertypes ({@code implements StatementCallback<@Nullable Object>}).
   */
  List<DeclaredAnnotation> supertypeArgumentAnnotations(Type owner, Type declaring, int position) {
    return supertypeArgumentAnnotations(
      sema.sourceName(owner.erasure().fullyQualifiedName()),
      sema.sourceName(declaring.erasure().fullyQualifiedName()),
      position,
      new HashSet<>());
  }

  List<DeclaredAnnotation> supertypeArgumentAnnotations(String name, String declaring, int position, Set<String> visited) {
    JsonObject info = sema.typeInfos.get(name);
    if (info == null || !visited.add(name)) {
      return List.of();
    }
    List<DeclaredAnnotation> written = writtenArgumentAnnotations(info, declaring, position);
    if (written != null) {
      return written;
    }
    for (String parent : parents(info)) {
      List<DeclaredAnnotation> found = supertypeArgumentAnnotations(sema.sourceName(erase(parent)), declaring, position, visited);
      if (!found.isEmpty()) {
        return found;
      }
    }
    return List.of();
  }

  /** The annotations the supertype clause of {@code info} writes on type argument {@code position} of {@code declaring}, or null when it writes none. */
  @Nullable
  private List<DeclaredAnnotation> writtenArgumentAnnotations(JsonObject info, String declaring, int position) {
    List<String> interfaces = strings(info, "interfaces");
    if (interfaces.isEmpty() && "interface".equals(text(info, "kind"))) {
      interfaces = strings(info, SUPER);
    }
    if (!(info.get("typeAnnotations") instanceof JsonArray typed)) {
      return null;
    }
    for (JsonElement element : typed) {
      if (element instanceof JsonObject entry && Substitution.isArgumentOf(entry, position)) {
        String parent = annotatedParent(info, entry, interfaces);
        if (parent != null && sema.sourceName(erase(parent)).equals(declaring)) {
          JsonArray one = new JsonArray();
          one.add(entry);
          return readAnnotationList(one);
        }
      }
    }
    return null;
  }

  @Nullable
  private static String annotatedParent(JsonObject info, JsonObject entry, List<String> interfaces) {
    JsonElement position = entry.get("index");
    String on = text(entry, "on");
    if (SUPER.equals(on)) {
      return text(info, SUPER);
    }
    if ("interface".equals(on) && position instanceof JsonPrimitive index && index.getAsInt() >= 0 && index.getAsInt() < interfaces.size()) {
      return interfaces.get(index.getAsInt());
    }
    return null;
  }

  /** The default of an annotation element read from a class file. */
  @Nullable
  Object annotationDefault(JMethodSymbol method) {
    return sema.declaredMembers.annotationDefaults.get(method.id);
  }
}
