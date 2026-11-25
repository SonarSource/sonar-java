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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryAnnotationsTest {

  private static final String LIBRARY = """
    {
      "lib.Annotated": {
        "origin": "classpath", "kind": "class", "modifiers": ["public"], "super": "java.lang.Object", "interfaces": [],
        "typeParameters": 0, "typeVars": [],
        "annotations": ["lib.Marker"],
        "members": [
          {
            "k": "m", "name": "configure", "desc": "(Ljava/lang/String;I)V", "vis": "public", "abstract": false, "final": false, "static": false,
            "synchronized": false, "varargs": false, "retSelf": false, "throws": [],
            "annotations": [
              {"name": "java.lang.Deprecated", "values": [{"kind": "string", "name": "since", "value": "2"}, {"kind": "boolean", "name": "forRemoval", "value": true}]},
              {"name": "lib.Limits", "values": [
                {"kind": "byte", "name": "b", "value": 1},
                {"kind": "short", "name": "s", "value": 2},
                {"kind": "int", "value": 3},
                {"kind": "long", "name": "l", "value": 4},
                {"kind": "float", "name": "f", "value": 1.5},
                {"kind": "double", "name": "d", "value": 2.5},
                {"kind": "char", "name": "c", "value": 65},
                {"kind": "class", "name": "type", "value": "java.lang.String"},
                {"kind": "annotation", "name": "nested", "value": {"name": "lib.Marker"}},
                {"kind": "array", "name": "all", "value": [{"kind": "int", "value": 7}, "bare", {"kind": "string", "value": "x"}]},
                {"kind": "array", "name": "none", "value": 3},
                {"kind": "long", "name": "missing"},
                {"kind": "mystery", "name": "unknown", "value": 1},
                {"name": "untyped", "value": 1},
                "ignored"
              ]},
              {"values": []}
            ],
            "parameterAnnotations": [[{"name": "lib.NonNull", "values": []}], []],
            "typeAnnotations": [
              {"on": "type", "path": [], "name": "lib.Nullable", "values": []},
              {"on": "type", "path": [], "name": "lib.Limits", "values": []},
              {"on": "type", "path": [{"kind": "array"}], "name": "lib.Deep", "values": []},
              {"on": "param", "path": [], "name": "lib.Other", "values": []}
            ]
          }
        ]
      }
    }
    """;

  private JSema sema;

  @BeforeEach
  void setUp() {
    sema = new JSema();
    sema.loadPackages(name -> null);
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(JsonParser.parseString(LIBRARY).getAsJsonObject());
  }

  @Test
  void library_member_annotations_keep_values_of_every_kind() {
    Symbol.MethodSymbol method = configure();

    Map<String, Object> limits = method.metadata().valuesForAnnotation("lib.Limits").stream()
      .collect(Collectors.toMap(SymbolMetadata.AnnotationValue::name, value -> describe(value.value())));

    assertThat(limits)
      .containsEntry("b", (byte) 1)
      .containsEntry("s", (short) 2)
      .containsEntry("value", 3)
      .containsEntry("l", 4L)
      .containsEntry("f", 1.5f)
      .containsEntry("d", 2.5)
      .containsEntry("c", 'A')
      .containsEntry("type", "java.lang.String")
      .containsEntry("nested", "lib.Marker")
      .containsEntry("all", "[7, null, x]")
      .containsEntry("none", "[]")
      .containsEntry("missing", "null")
      .containsEntry("unknown", "null")
      .containsEntry("untyped", "null")
      .hasSize(14);
  }

  @Test
  void library_member_annotations_include_outermost_type_annotations_once() {
    List<String> names = configure().metadata().annotations().stream().map(annotation -> annotation.symbol().type().fullyQualifiedName()).toList();

    assertThat(names).containsExactly("java.lang.Deprecated", "lib.Limits", "lib.Nullable");
  }

  @Test
  void deprecated_library_member_is_deprecated() {
    assertThat(configure().isDeprecated()).isTrue();
  }

  @Test
  void library_parameter_annotations_belong_to_their_parameter() {
    List<Symbol> parameters = configure().declarationParameters();

    assertThat(parameters).hasSize(2);
    assertThat(parameters.get(0).metadata().isAnnotatedWith("lib.NonNull")).isTrue();
    assertThat(parameters.get(1).metadata().annotations()).isEmpty();
  }

  @Test
  void annotation_names_given_as_strings_are_read() {
    Symbol.TypeSymbol type = sema.getClassType("lib.Annotated").symbol();

    assertThat(type.metadata().isAnnotatedWith("lib.Marker")).isTrue();
  }

  private Symbol.MethodSymbol configure() {
    return (Symbol.MethodSymbol) sema.getClassType("lib.Annotated").symbol().lookupSymbols("configure").iterator().next();
  }

  private static Object describe(Object value) {
    if (value instanceof Object[] array) {
      return Arrays.stream(array).map(LibraryAnnotationsTest::describe).map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
    }
    if (value instanceof Symbol symbol) {
      return symbol.type().fullyQualifiedName();
    }
    return value == null ? "null" : value;
  }
}
