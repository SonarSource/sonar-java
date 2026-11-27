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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

/**
 * A semantic model holding the JDK types the replay recorded, plus types written as JSON by a test, with shortcuts to
 * build types and find library methods.
 */
final class JTypeFixtures {

  private static final JsonObject JDK = loadJdk();

  final JSema sema;

  JTypeFixtures(String... types) {
    sema = new JSema();
    sema.loadPackages(name -> null);
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(JDK.deepCopy());
    for (String json : types) {
      sema.ingestTypes(json(json));
    }
  }

  static JsonObject json(String json) {
    return JsonParser.parseString(json).getAsJsonObject();
  }

  /**
   * The type information of a classpath class {@code name}, as {@code {"name": {...}}}: a public class extending
   * {@code Object} with no type parameters, unless {@code fields} (the inside of a JSON object) says otherwise.
   */
  static String library(String name, String fields, String... members) {
    JsonObject info = json("""
      {"origin": "classpath", "kind": "class", "modifiers": ["public"], "super": "java.lang.Object", "superType": {"id": "java.lang.Object", "args": []},
       "interfaces": [], "interfaceTypes": [], "typeParameters": 0, "typeVars": [], "typeVarBounds": []}
      """);
    json("{" + fields + "}").entrySet().forEach(entry -> info.add(entry.getKey(), entry.getValue()));
    JsonArray array = new JsonArray();
    Arrays.stream(members).map(JTypeFixtures::json).forEach(array::add);
    info.add("members", array);
    JsonObject type = new JsonObject();
    type.add(name, info);
    return type.toString();
  }

  /** A public instance method of a library class, with no parameters nor type variables unless {@code fields} says otherwise. */
  static String member(String name, String descriptor, String fields) {
    JsonObject member = json("""
      {"k": "m", "vis": "public", "abstract": false, "final": false, "static": false, "synchronized": false, "varargs": false, "retSelf": false,
       "throws": [], "paramTypes": [], "paramVars": [], "typeVars": []}
      """);
    member.addProperty("name", name);
    member.addProperty("desc", descriptor);
    json("{" + fields + "}").entrySet().forEach(entry -> member.add(entry.getKey(), entry.getValue()));
    return member.toString();
  }

  /** The raw type {@code fqn}, or its parameterization with {@code arguments}. */
  JType type(String fqn, JType... arguments) {
    return arguments.length == 0 ? (JType) sema.getClassType(fqn) : JType.named(sema, fqn, List.of(arguments));
  }

  /** The type a symbol fact such as {@code {"id": "java.util.List", "args": [...]}} describes. */
  JType fact(String json) {
    return sema.factTypes().typeFromFact(json(json));
  }

  JType string() {
    return type("java.lang.String");
  }

  JType object() {
    return type("java.lang.Object");
  }

  JType integer() {
    return type("java.lang.Integer");
  }

  JType primitive(String name) {
    return sema.primitiveType(name);
  }

  JType extending(JType bound) {
    return JType.wildcard(sema, bound);
  }

  JType capture(JType bound) {
    return JType.capture(sema, bound);
  }

  /** The method {@code name} of {@code owner} whose parameters erase to {@code parameters}. */
  JMethodSymbol method(String owner, String name, String... parameters) {
    for (Symbol member : sema.getClassType(owner).symbol().lookupSymbols(name)) {
      List<String> erased = ((Symbol.MethodSymbol) member).parameterTypes().stream().map(Type::erasure).map(Type::fullyQualifiedName).toList();
      if (member instanceof JMethodSymbol method && erased.equals(Arrays.asList(parameters))) {
        return method;
      }
    }
    throw new IllegalArgumentException(owner + "#" + name + Arrays.toString(parameters));
  }

  private static JsonObject loadJdk() {
    try {
      return Replays.readJson(Replays.TYPES);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
