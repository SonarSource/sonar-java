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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class OverriddenMethodsTest {

  private static final String LIBRARY = """
    {
      "lib.Converter": {
        "origin": "classpath", "kind": "interface", "modifiers": ["public"], "interfaces": [],
        "typeParameters": 2, "typeVars": ["A", "B"],
        "members": [
          {"k": "m", "name": "convert", "desc": "(Ljava/lang/Object;)Ljava/lang/Object;", "vis": "public", "abstract": true, "static": false,
           "paramTypes": [{"id": "A", "kind": "typevar"}], "paramVars": ["a"], "retParam": 1, "retType": {"id": "B", "kind": "typevar"}, "throws": []},
          {"k": "m", "name": "reset", "desc": "()V", "vis": "public", "abstract": true, "static": false, "throws": []},
          {"k": "m", "name": "hashCode", "desc": "()I", "vis": "public", "abstract": true, "static": false, "throws": []}
        ]
      },
      "lib.Impl": {
        "origin": "classpath", "kind": "class", "modifiers": ["public"], "super": "java.lang.Object",
        "interfaces": ["lib.Converter"],
        "interfaceTypes": [{"id": "lib.Converter", "args": [{"id": "java.lang.String", "args": []}, {"id": "java.lang.Boolean", "args": []}]}],
        "typeParameters": 0, "typeVars": [],
        "typeAnnotations": [
          {"on": "interface", "index": 0, "path": ["<0>"], "name": "lib.NonNull", "values": []},
          {"on": "interface", "index": 0, "path": ["<1>"], "name": "javax.annotation.Nullable", "values": []}
        ],
        "members": [
          {"k": "m", "name": "convert", "desc": "(Ljava/lang/String;)Ljava/lang/Boolean;", "vis": "public", "static": false, "throws": []},
          {"k": "m", "name": "reset", "desc": "()V", "vis": "public", "static": false, "throws": []},
          {"k": "m", "name": "helper", "desc": "()V", "vis": "private", "static": false, "throws": []},
          {"k": "m", "name": "create", "desc": "()Llib/Impl;", "vis": "public", "static": true, "throws": []}
        ]
      },
      "lib.Sub": {
        "origin": "classpath", "kind": "class", "modifiers": ["public"], "super": "lib.Impl", "interfaces": [],
        "typeParameters": 0, "typeVars": [],
        "members": [
          {"k": "m", "name": "convert", "desc": "(Ljava/lang/String;)Ljava/lang/Boolean;", "vis": "public", "static": false, "throws": []},
          {"k": "m", "name": "convert", "desc": "(Ljava/lang/Integer;)Ljava/lang/Boolean;", "vis": "public", "static": false, "throws": []},
          {"k": "m", "name": "helper", "desc": "()V", "vis": "private", "static": false, "throws": []},
          {"k": "m", "name": "create", "desc": "()Llib/Impl;", "vis": "public", "static": true, "throws": []}
        ]
      },
      "java.lang.Object": {
        "origin": "classpath", "kind": "class", "modifiers": ["public"], "interfaces": [], "typeParameters": 0, "typeVars": [],
        "members": [
          {"k": "m", "name": "hashCode", "desc": "()I", "vis": "public", "static": false, "throws": []}
        ]
      },
      "java.lang.String": {"origin": "classpath", "kind": "class", "modifiers": ["public", "final"], "super": "java.lang.Object", "interfaces": [], "typeParameters": 0, "typeVars": [], "members": []},
      "java.lang.Boolean": {"origin": "classpath", "kind": "class", "modifiers": ["public", "final"], "super": "java.lang.Object", "interfaces": [], "typeParameters": 0, "typeVars": [], "members": []},
      "java.lang.Integer": {"origin": "classpath", "kind": "class", "modifiers": ["public", "final"], "super": "java.lang.Object", "interfaces": [], "typeParameters": 0, "typeVars": [], "members": []},
      "javax.annotation.Nullable": {"origin": "classpath", "kind": "annotation", "modifiers": ["public"], "interfaces": [], "typeParameters": 0, "typeVars": [], "members": []},
      "lib.Marker": {
        "origin": "classpath", "kind": "interface", "modifiers": ["public"], "interfaces": [], "typeParameters": 0, "typeVars": [],
        "members": [
          {"k": "m", "name": "hashCode", "desc": "()I", "vis": "public", "abstract": true, "static": false, "throws": []}
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
  void a_method_overrides_the_method_of_its_parameterized_interface() {
    List<Symbol.MethodSymbol> overridden = method("lib.Impl", "convert").overriddenSymbols();

    assertThat(overridden).hasSize(1);
    assertThat(overridden.get(0).owner().name()).isEqualTo("Converter");
  }

  @Test
  void the_annotated_type_argument_of_the_supertype_is_the_result_annotation_of_the_overridden_method() {
    Symbol.MethodSymbol library = method("lib.Converter", "convert");
    Symbol.MethodSymbol overridden = method("lib.Impl", "convert").overriddenSymbols().get(0);

    SymbolMetadata metadata = overridden.metadata();
    assertThat(metadata.isAnnotatedWith("javax.annotation.Nullable")).isTrue();
    assertThat(metadata.isAnnotatedWith("lib.NonNull")).isFalse();
    assertThat(metadata.valuesForAnnotation("javax.annotation.Nullable")).isEmpty();
    assertThat(metadata.valuesForAnnotation("lib.NonNull")).isNull();
    assertThat(metadata.symbolAnnotations()).hasSize(1);
    assertThat(metadata.findAnnotationTree(metadata.annotations().get(0))).isNull();
    assertThat(metadata.parametersMetadata()).isEmpty();
    assertThat(metadata.nullabilityData().type()).isEqualTo(SymbolMetadata.NullabilityType.WEAK_NULLABLE);
    assertThat(metadata.nullabilityData(SymbolMetadata.NullabilityTarget.FIELD).type()).isEqualTo(SymbolMetadata.NullabilityType.WEAK_NULLABLE);
    assertThat(overridden.name()).isEqualTo("convert");
    assertThat(overridden).isEqualTo(library).isEqualTo(overridden);
  }

  @Test
  void the_overriding_method_inherits_the_nullability_of_the_written_type_argument() {
    SymbolMetadata.NullabilityData nullability = method("lib.Impl", "convert").metadata().nullabilityData();

    assertThat(nullability.type()).isEqualTo(SymbolMetadata.NullabilityType.WEAK_NULLABLE);
    assertThat(nullability.level()).isEqualTo(SymbolMetadata.NullabilityLevel.METHOD);
  }

  @Test
  void a_type_argument_annotation_is_found_through_a_supertype() {
    List<Symbol.MethodSymbol> overridden = method("lib.Sub", "convert", "java.lang.String").overriddenSymbols();

    assertThat(overridden).extracting(method -> method.owner().name()).containsExactly("Impl", "Converter");
    assertThat(overridden.get(0).metadata().annotations()).isEmpty();
    assertThat(overridden.get(1).metadata().isAnnotatedWith("javax.annotation.Nullable")).isTrue();
  }

  @Test
  void a_method_without_a_type_variable_result_keeps_its_own_metadata() {
    List<Symbol.MethodSymbol> overridden = method("lib.Impl", "reset").overriddenSymbols();

    assertThat(overridden).containsExactly(method("lib.Converter", "reset"));
  }

  @Test
  void an_overload_with_other_parameters_overrides_nothing() {
    assertThat(method("lib.Sub", "convert", "java.lang.Integer").overriddenSymbols()).isEmpty();
  }

  @Test
  void private_and_static_methods_override_nothing() {
    assertThat(method("lib.Sub", "helper").overriddenSymbols()).isEmpty();
    assertThat(method("lib.Sub", "create").overriddenSymbols()).isEmpty();
  }

  @Test
  void an_interface_method_overrides_the_public_methods_of_object() {
    List<Symbol.MethodSymbol> overridden = method("lib.Marker", "hashCode").overriddenSymbols();

    assertThat(overridden).extracting(method -> method.owner().type().fullyQualifiedName()).containsExactly("java.lang.Object");
  }

  @Test
  void an_unknown_method_overrides_nothing() {
    JMethodSymbol unknown = new JMethodSymbol(sema, null, "convert");
    unknown.unknown(true);

    assertThat(unknown.overriddenSymbols()).isEmpty();
  }

  private Symbol.MethodSymbol method(String type, String name, String... parameters) {
    return sema.getClassType(type).symbol().lookupSymbols(name).stream()
      .filter(Symbol.MethodSymbol.class::isInstance)
      .map(Symbol.MethodSymbol.class::cast)
      .filter(method -> parameters.length == 0 || method.parameterTypes().get(0).is(parameters[0]))
      .findFirst()
      .orElseThrow();
  }
}
