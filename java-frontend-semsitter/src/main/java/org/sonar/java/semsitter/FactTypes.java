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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.semsitter.wire.TypeRef;

import static org.sonar.java.semsitter.SemaFacts.erase;
import static org.sonar.java.semsitter.SemaFacts.parameterKey;
import static org.sonar.java.semsitter.SemaFacts.text;

/** Builds types from the type references written in facts and descriptors. */
final class FactTypes {

  private static final String OBJECT = "java.lang.Object";

  private final JSema sema;
  private final Map<String, Symbol> placeholders = new HashMap<>();

  FactTypes(JSema sema) {
    this.sema = sema;
  }

  JType typeFromFact(JsonObject fact) {
    return typeFromFact(fact, Map.of());
  }

  JType typeFromFact(JsonObject fact, Map<String, Type> substitution) {
    return typeFromFact(fact, substitution, false);
  }

  JType typeFromFact(JsonObject fact, Map<String, Type> substitution, boolean captureWildcard) {
    String kind = text(fact, "kind");
    if (JSema.ARRAY.equals(kind) && fact.getAsJsonObject("elem") != null) {
      return JType.array(sema, typeFromFact(fact.getAsJsonObject("elem"), substitution, captureWildcard));
    }
    if (JSema.WILDCARD.equals(kind)) {
      return wildcardFromFact(fact, substitution, captureWildcard);
    }
    if ("typevar".equals(kind)) {
      return typeVariable(text(fact, "id"), substitution);
    }
    if ("unknown".equals(kind)) {
      return JType.unknown(sema);
    }
    String id = text(fact, "id");
    if (id == null) {
      return JType.unknown(sema);
    }
    List<JType> args = new ArrayList<>();
    if (fact.has("args")) {
      for (JsonElement arg : fact.getAsJsonArray("args")) {
        if (arg.isJsonObject()) {
          args.add(typeFromFact(arg.getAsJsonObject(), substitution, captureWildcard));
        }
      }
    }
    return namedType(id, args, captureWildcard);
  }

  /** The type a document declares for a symbol; its wildcards are unbounded. */
  JType typeFromRef(TypeRef type) {
    String kind = type.getKind();
    if (JSema.ARRAY.equals(kind) && type.hasElement()) {
      return JType.array(sema, typeFromRef(type.getElement()));
    }
    if (JSema.WILDCARD.equals(kind)) {
      return JType.wildcard(sema, sema.asJType(sema.getClassType(OBJECT)));
    }
    if ("typevar".equals(kind)) {
      return typeVariable(text(type.getId()), Map.of());
    }
    if ("unknown".equals(kind) || type.getId().isEmpty()) {
      return JType.unknown(sema);
    }
    List<JType> args = new ArrayList<>();
    for (TypeRef argument : type.getArgumentsList()) {
      args.add(typeFromRef(argument));
    }
    return namedType(type.getId(), args, false);
  }

  private JType typeVariable(@Nullable String id, Map<String, Type> substitution) {
    Type substituted = substitution.get(id);
    if (substituted != null) {
      return sema.asJType(substituted);
    }
    int open = id == null ? -1 : id.lastIndexOf('<');
    String name = open >= 0 && id.endsWith(">") ? id.substring(open + 1, id.length() - 1) : id;
    return JType.typeVar(sema, name == null ? "" : name, id);
  }

  private JType namedType(String id, List<JType> args, boolean captureWildcard) {
    String erased = erase(id);
    if (JSema.PRIMITIVES.contains(erased)) {
      return sema.primitiveType(erased);
    }
    if (captureWildcard) {
      boundCaptures(erased, args);
    }
    return JType.named(sema, erased, args);
  }

  /** An unbounded wildcard argument is captured with the bound its type parameter declares. */
  private void boundCaptures(String owner, List<JType> arguments) {
    for (int i = 0; i < arguments.size(); i++) {
      JType argument = arguments.get(i);
      if (argument.isCapture() && argument.element != null && argument.element.is(OBJECT)) {
        JType bound = declaredBound(owner, i);
        if (bound != null) {
          arguments.set(i, JType.capture(sema, bound));
        }
      }
    }
  }

  /** The class bound the {@code index}th type parameter of {@code owner} declares, when it is more than Object. */
  @Nullable
  private JType declaredBound(String owner, int index) {
    JsonObject info = sema.typeInfos.get(sema.sourceName(owner));
    if (info == null || !(info.get("typeVarBounds") instanceof JsonArray bounds) || bounds.size() <= index
      || !(bounds.get(index) instanceof JsonArray list) || list.isEmpty() || !(list.get(0) instanceof JsonObject fact)) {
      return null;
    }
    JType bound = typeFromFact(fact);
    return bound.isUnknown() || bound.is(OBJECT) ? null : bound;
  }

  private JType wildcardFromFact(JsonObject fact, Map<String, Type> substitution, boolean capture) {
    JType object = sema.asJType(sema.getClassType(OBJECT));
    JType upper = fact.get(JSema.EXTENDS) instanceof JsonObject bound ? typeFromFact(bound, substitution) : object;
    if (capture) {
      return JType.capture(sema, upper.isUnknown() ? object : upper);
    }
    if (fact.get("super") instanceof JsonObject bound) {
      return JType.superWildcard(sema, typeFromFact(bound, substitution));
    }
    return JType.wildcard(sema, upper);
  }

  /** A supertype fact: the loaded raw type, parameterized when the fact has type arguments. */
  Type supertypeFromFact(JsonObject fact) {
    Type raw = sema.getClassType(erase(Objects.requireNonNull(text(fact, "id"))));
    JsonArray args = fact.getAsJsonArray("args");
    if (args == null || args.isEmpty() || !(raw instanceof JType rawType)) {
      return raw;
    }
    List<JType> resolved = new ArrayList<>();
    for (JsonElement arg : args) {
      if (arg.isJsonObject()) {
        resolved.add(typeFromFact(arg.getAsJsonObject()));
      }
    }
    return JType.named(sema, rawType.fullyQualifiedName(), resolved);
  }

  static List<JsonObject> objects(JsonObject object, String key) {
    if (!object.has(key) || !object.get(key).isJsonArray()) {
      return List.of();
    }
    List<JsonObject> values = new ArrayList<>();
    for (JsonElement item : object.getAsJsonArray(key)) {
      if (item.isJsonObject()) {
        values.add(item.getAsJsonObject());
      }
    }
    return values;
  }

  /**
   * The type variable a result names as {@code <owner>::``<position>} for a callable, the position-th variable the dump
   * declares as {@code <owner><name>}, or as {@code <owner>::`<position>} for a class, where the variables of the
   * enclosing classes come first.
   */
  @Nullable
  private JTypeSymbol typeParameterItem(String name) {
    int marker = name.lastIndexOf("::`");
    boolean callable = marker >= 0 && name.startsWith("::``", marker);
    int digits = -1;
    if (marker >= 0) {
      digits = marker + (callable ? 4 : 3);
    }
    if (marker <= 0 || name.length() == digits || !name.substring(digits).chars().allMatch(Character::isDigit)) {
      return null;
    }
    String owner = name.substring(0, marker);
    int position = Integer.parseInt(name.substring(digits));
    List<String> variables = new ArrayList<>();
    if (!callable) {
      for (int nested = owner.indexOf('$'); nested > 0; nested = owner.indexOf('$', nested + 1)) {
        variables.addAll(declaredVariables(owner.substring(0, nested)));
      }
    }
    variables.addAll(declaredVariables(owner));
    if (position >= variables.size() || !(sema.resolve(variables.get(position)) instanceof JTypeSymbol variable) || !variable.isTypeParameter()) {
      return null;
    }
    if (variable.type().isUnknown()) {
      variable.type(JType.typeVar(sema, variable.name(), variables.get(position)));
    }
    return variable;
  }

  private List<String> declaredVariables(String owner) {
    List<String> variables = new ArrayList<>();
    for (Map.Entry<String, org.sonarsource.semsitter.wire.Symbol> entry : sema.dumpSymbols.entrySet()) {
      String key = entry.getKey();
      String prefix = variablePrefix(key, owner);
      if (prefix != null && key.length() > prefix.length() + 2 && key.endsWith(">") && key.indexOf('<', prefix.length() + 1) < 0
        && "type".equals(entry.getValue().getKind())) {
        variables.add(key);
      }
    }
    return variables;
  }

  @Nullable
  private static String variablePrefix(String key, String owner) {
    if (key.startsWith(owner + "<")) {
      return owner;
    }
    String erased = erase(owner);
    return key.startsWith(erased + "<") ? erased : null;
  }

  Type typeFromName(String name) {
    JTypeSymbol variable = typeParameterItem(name);
    if (variable != null) {
      return variable.type();
    }
    String erased = erase(name);
    if (JSema.PRIMITIVES.contains(erased)) {
      return sema.primitiveType(erased);
    }
    if (erased.endsWith("[]")) {
      return JType.array(sema, sema.asJType(typeFromName(erased.substring(0, erased.length() - 2))));
    }
    return sema.getClassType(erased);
  }

  /** The parameter of a method no source declares: one symbol per method and position, whichever call it is seen from. */
  Symbol parameterPlaceholder(JMethodSymbol method, int index, Type type) {
    String key = method.id + "#" + index + "#" + type.fullyQualifiedName();
    if (method.id != null && sema.declaredMembers.libraryAnnotations.containsKey(parameterKey(method.id, index))) {
      return placeholders.computeIfAbsent(key, k -> {
        JVariableSymbol annotated = new JVariableSymbol(sema, parameterKey(method.id, index), "arg" + index, "param");
        annotated.owner(method);
        annotated.type(type);
        return annotated;
      });
    }
    return method.id == null ? new JVariableSymbol.ParameterPlaceholderSymbol(index, method, type)
      : placeholders.computeIfAbsent(key, k -> new JVariableSymbol.ParameterPlaceholderSymbol(index, method, type));
  }

  List<Type> parameterTypesOf(String descriptor) {
    List<Type> types = new ArrayList<>();
    int[] index = {1};
    while (index[0] < descriptor.length() && descriptor.charAt(index[0]) != ')') {
      types.add(readDescriptor(descriptor, index));
    }
    return types;
  }

  Type readDescriptor(String descriptor, int[] index) {
    int array = 0;
    while (descriptor.charAt(index[0]) == '[') {
      array++;
      index[0]++;
    }
    Type type;
    char kind = descriptor.charAt(index[0]);
    if (kind == 'L') {
      int semi = descriptor.indexOf(';', index[0]);
      String binary = descriptor.substring(index[0] + 1, semi).replace('/', '.');
      index[0] = semi + 1;
      type = sema.getClassType(binary);
    } else {
      index[0]++;
      type = sema.primitiveType(switch (kind) {
        case 'B' -> "byte";
        case 'C' -> "char";
        case 'D' -> "double";
        case 'F' -> "float";
        case 'I' -> "int";
        case 'J' -> "long";
        case 'S' -> "short";
        case 'Z' -> "boolean";
        default -> "void";
      });
    }
    for (int i = 0; i < array; i++) {
      type = JType.array(sema, sema.asJType(type));
    }
    return type;
  }
}
