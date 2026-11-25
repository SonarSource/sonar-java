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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonarsource.semsitter.wire.Fact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SemaAnnotationsTest {

  private JSema sema;
  private SemaAnnotations annotations;

  @BeforeEach
  void setUp() {
    sema = new JSema();
    sema.loadPackages(name -> null);
    sema.loadTypes(names -> new JsonObject());
    annotations = sema.annotationFacts();
  }

  @Test
  void element_values_without_a_value_of_their_kind_are_null() {
    assertThat(element("{\"kind\": \"boolean\"}")).isEqualTo(false);
    assertThat(element("{\"kind\": \"boolean\", \"value\": true}")).isEqualTo(true);
    assertThat(element("{\"kind\": \"int\", \"value\": {}}")).isNull();
    assertThat(element("{\"kind\": \"string\"}")).isNull();
    assertThat(element("{\"kind\": \"string\", \"value\": \"s\"}")).isEqualTo("s");
    assertThat(element("{\"kind\": \"annotation\", \"value\": \"lib.Marker\"}")).isNull();
    assertThat(element("{\"kind\": \"annotation\"}")).isNull();
    assertThat(element("{\"kind\": \"mystery\", \"value\": 1}")).isNull();
    assertThat(element("{\"value\": 1}")).isNull();
  }

  @Test
  void class_values_are_type_symbols() {
    assertThat(element("{\"kind\": \"class\", \"value\": \"java.util.List`1\"}"))
      .isInstanceOfSatisfying(Symbol.TypeSymbol.class, type -> assertThat(type.type().fullyQualifiedName()).isEqualTo("java.util.List"));
  }

  @Test
  void parameter_annotations_come_from_the_type_annotation_facts_of_a_member() {
    annotations.registerParameterAnnotations("p.A#m(int)", json("""
      {"facts": {"props": {"type_annotation_values": [
        "not json [",
        "{\\"on\\": \\"param\\"}",
        "[1, {\\"on\\": \\"type\\", \\"path\\": [], \\"index\\": 0, \\"id\\": \\"lib.Wrong\\"},
          {\\"on\\": \\"param\\", \\"path\\": [\\"<0>\\"], \\"index\\": 0, \\"id\\": \\"lib.Deep\\"},
          {\\"on\\": \\"param\\", \\"path\\": 1, \\"index\\": 0, \\"id\\": \\"lib.Odd\\"},
          {\\"on\\": \\"param\\", \\"path\\": [], \\"id\\": \\"lib.NoIndex\\"},
          {\\"on\\": \\"param\\", \\"path\\": [], \\"index\\": 0},
          {\\"on\\": \\"param\\", \\"path\\": [], \\"index\\": 1, \\"id\\": \\"lib.Second\\"}]"
      ]}}}
      """));

    assertThat(names(sema.declaredMembers.libraryAnnotations.get(SemaFacts.parameterKey("p.A#m(int)", 1)))).containsExactly("lib.Second");
    assertThat(sema.declaredMembers.libraryAnnotations).hasSize(1);
  }

  @Test
  void members_without_type_annotation_facts_register_nothing() {
    annotations.registerParameterAnnotations("p.A#m()", json("{}"));
    annotations.registerParameterAnnotations("p.A#m()", json("{\"facts\": {}}"));

    assertThat(sema.declaredMembers.libraryAnnotations).isEmpty();
  }

  @Test
  void declared_annotations_of_a_graph_fact_keep_no_values() {
    sema.facts.put("p.A", Fact.newBuilder().addAnnotations("lib.Marker`1").build());

    List<DeclaredAnnotation> declared = annotations.declaredAnnotations("p.A");

    assertThat(names(declared)).containsExactly("lib.Marker");
    assertThat(declared.get(0).valuesKnown()).isFalse();
    FactAnnotation instance = new FactAnnotation(sema, "lib.Marker", declared.get(0).values(), false);
    assertThat(instance.symbol().name()).isEqualTo("Marker");
    assertThat(instance.values()).extracting(AnnotationValue::name, AnnotationValue::value).containsExactly(tuple("value", null));
    assertThat(annotations.declaredAnnotations("p.Other")).isEmpty();
    assertThat(annotations.declaredAnnotations(null)).isEmpty();
  }

  @Test
  void package_annotations_are_read_once_from_the_package_info_type() {
    AtomicInteger requests = new AtomicInteger();
    sema.loadTypes(names -> {
      if (names.contains("p.package-info")) {
        requests.incrementAndGet();
        return json("{\"p.package-info\": {\"kind\": \"interface\", \"annotations\": [\"lib.Marker\", {\"values\": []}, 3]}}");
      }
      return new JsonObject();
    });

    assertThat(names(annotations.packageAnnotations("p"))).containsExactly("lib.Marker", "3");
    assertThat(names(annotations.packageAnnotations("p"))).containsExactly("lib.Marker", "3");
    assertThat(annotations.packageAnnotations("q")).isEmpty();
    assertThat(requests.get()).isEqualTo(1);
  }

  @Test
  void package_annotations_are_unknown_before_types_are_loaded() {
    assertThat(new JSema().annotationFacts().packageAnnotations("p")).isEmpty();
  }

  @Test
  void outermost_type_annotations_add_to_the_declared_ones_once() {
    List<DeclaredAnnotation> read = annotations.readAnnotations(json("""
      {"annotations": [{"name": "lib.A", "values": 1}, {"name": "lib.B", "values": [1, {"kind": "int", "value": 2}]}],
       "typeAnnotations": [
         1,
         {"on": "type", "path": [], "name": "lib.A", "values": []},
         {"on": "type", "path": [], "name": "lib.C", "values": []},
         {"on": "type", "path": [{"kind": "array"}], "name": "lib.Deep", "values": []},
         {"on": "type", "name": "lib.NoPath", "values": []},
         {"on": "param", "path": [], "name": "lib.Param", "values": []}
       ]}
      """));

    assertThat(names(read)).containsExactly("lib.A", "lib.B", "lib.C");
    assertThat(read.get(0).values()).isEmpty();
    assertThat(read.get(1).values()).extracting(AnnotationValue::name, AnnotationValue::value).containsExactly(tuple("value", 2));
    assertThat(annotations.readAnnotations(json("{\"annotations\": \"lib.A\"}"))).isEmpty();
  }

  @Test
  void supertype_argument_annotations_follow_the_supertype_clauses() {
    sema.ingestTypes(json("""
      {
        "lib.Base": {"origin": "classpath", "kind": "class", "super": "lib.Box`1", "interfaces": ["lib.Pair", "lib.Single"],
          "typeAnnotations": [
            {"on": "super", "path": ["<0>"], "name": "lib.OnSuper", "values": []},
            {"on": "interface", "index": 1, "path": ["<0>"], "name": "lib.OnSingle", "values": []},
            {"on": "interface", "index": 5, "path": ["<0>"], "name": "lib.OutOfRange", "values": []},
            {"on": "interface", "index": -1, "path": ["<0>"], "name": "lib.Negative", "values": []},
            {"on": "interface", "path": ["<0>"], "name": "lib.NoIndex", "values": []},
            {"on": "other", "path": ["<0>"], "name": "lib.Other", "values": []}
          ]},
        "lib.Derived": {"origin": "classpath", "kind": "class", "super": "lib.Base", "interfaces": []},
        "lib.View": {"origin": "classpath", "kind": "interface", "super": ["lib.Single"], "interfaces": [],
          "typeAnnotations": [{"on": "interface", "index": 0, "path": ["<0>"], "name": "lib.OnView", "values": []}]},
        "lib.Loop": {"origin": "classpath", "kind": "class", "super": "lib.Loop", "interfaces": []}
      }
      """));

    assertThat(names(supertypeArgument("lib.Base", "lib.Box", 0))).containsExactly("lib.OnSuper");
    assertThat(names(supertypeArgument("lib.Base", "lib.Single", 0))).containsExactly("lib.OnSingle");
    assertThat(names(supertypeArgument("lib.Derived", "lib.Single", 0))).containsExactly("lib.OnSingle");
    assertThat(names(supertypeArgument("lib.View", "lib.Single", 0))).containsExactly("lib.OnView");
    assertThat(supertypeArgument("lib.Base", "lib.Box", 1)).isEmpty();
    assertThat(supertypeArgument("lib.Base", "lib.Pair", 0)).isEmpty();
    assertThat(supertypeArgument("lib.Loop", "lib.Box", 0)).isEmpty();
    assertThat(supertypeArgument("lib.Missing", "lib.Box", 0)).isEmpty();
    assertThat(annotations.supertypeArgumentAnnotations(sema.getClassType("lib.Derived"), sema.getClassType("lib.Box"), 0)).isNotEmpty();
  }

  private List<DeclaredAnnotation> supertypeArgument(String name, String declaring, int position) {
    Set<String> visited = new HashSet<>();
    return annotations.supertypeArgumentAnnotations(name, declaring, position, visited);
  }

  private Object element(String value) {
    return annotations.readAnnotationElement(json(value));
  }

  private static JsonObject json(String text) {
    return JsonParser.parseString(text).getAsJsonObject();
  }

  private static List<String> names(List<DeclaredAnnotation> declared) {
    return declared.stream().map(DeclaredAnnotation::name).toList();
  }
}
