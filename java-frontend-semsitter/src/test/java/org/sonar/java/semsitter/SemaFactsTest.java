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
import org.junit.jupiter.api.Test;
import org.sonarsource.semsitter.wire.Fact;
import org.sonarsource.semsitter.wire.FactValue;
import org.sonarsource.semsitter.wire.Symbol;

import static org.assertj.core.api.Assertions.assertThat;

class SemaFactsTest {

  @Test
  void type_fact_is_only_read_from_a_type_value() {
    Symbol flagged = Symbol.newBuilder().putFacts("type", FactValue.newBuilder().setFlag(true).build()).build();

    assertThat(SemaFacts.typeFact(flagged)).isNull();
    assertThat(SemaFacts.typeFact(Symbol.getDefaultInstance())).isNull();
  }

  @Test
  void type_names_skip_locals_generic_parameters_primitives_and_malformed_owners() {
    Set<String> names = new HashSet<>();
    SemaFacts.addTypeName(names, null);
    SemaFacts.addTypeName(names, "");
    SemaFacts.addTypeName(names, "p.A%1");
    SemaFacts.addTypeName(names, "p.A#f()$Local");
    SemaFacts.addTypeName(names, "p.A(x)#m");
    SemaFacts.addTypeName(names, "p.A<T>");
    SemaFacts.addTypeName(names, "int");
    SemaFacts.addTypeName(names, "p.A b");
    SemaFacts.addTypeName(names, "p.G`1<T>#m()");
    SemaFacts.addTypeName(names, "p.B#field");

    assertThat(names).containsExactlyInAnyOrder("p.A", "p.G<T>", "p.B");
  }

  @Test
  void well_known_types_are_the_roots_of_every_hierarchy() {
    assertThat(SemaFacts.isWellKnown("java.lang.Object")).isTrue();
    assertThat(SemaFacts.isWellKnown("java.lang.Cloneable")).isTrue();
    assertThat(SemaFacts.isWellKnown("java.io.Serializable")).isTrue();
    assertThat(SemaFacts.isWellKnown("java.lang.String")).isFalse();
  }

  @Test
  void constants_are_boxed_as_the_primitive_type_of_the_field() {
    JSema sema = new JSema();

    assertThat(SemaFacts.constantOf(sema.primitiveType("int"), "1")).isEqualTo(1);
    assertThat(SemaFacts.constantOf(sema.primitiveType("long"), "2")).isEqualTo(2L);
    assertThat(SemaFacts.constantOf(sema.primitiveType("short"), "3")).isEqualTo((short) 3);
    assertThat(SemaFacts.constantOf(sema.primitiveType("byte"), "4")).isEqualTo((byte) 4);
    assertThat(SemaFacts.constantOf(sema.primitiveType("float"), "1.5")).isEqualTo(1.5f);
    assertThat(SemaFacts.constantOf(sema.primitiveType("double"), "2.5")).isEqualTo(2.5);
    assertThat(SemaFacts.constantOf(sema.primitiveType("boolean"), "true")).isEqualTo(true);
    assertThat(SemaFacts.constantOf(sema.primitiveType("char"), "A")).isEqualTo(65);
    assertThat(SemaFacts.constantOf(sema.primitiveType("char"), "AB")).isEqualTo("AB");
    assertThat(SemaFacts.constantOf(sema.primitiveType("int"), "x")).isEqualTo("x");
    assertThat(SemaFacts.constantOf(null, "text")).isEqualTo("text");
  }

  @Test
  void type_facts_are_the_kinds_of_type_declarations() {
    assertThat(List.of("class", "interface", "enum", "record", "annotation", "exception"))
      .allMatch(kind -> SemaFacts.isTypeFact(Fact.newBuilder().setKind(kind).build()));
    assertThat(SemaFacts.isTypeFact(Fact.newBuilder().setKind("method").build())).isFalse();
  }

  @Test
  void member_flags_become_modifiers_and_package_visibility_adds_none() {
    JSema sema = new JSema();
    JMethodSymbol method = new JMethodSymbol(sema, "p.A#m()", "m");
    SemaFacts.applyMemberFlags(method, json("{\"static\":true,\"final\":true,\"synchronized\":true,\"abstract\":true,\"vis\":\"package\"}"));
    JMethodSymbol hidden = new JMethodSymbol(sema, "p.A#n()", "n");
    SemaFacts.applyMemberFlags(hidden, json("{\"vis\":\"private\"}"));

    assertThat(method.isStatic()).isTrue();
    assertThat(method.isFinal()).isTrue();
    assertThat(method.isSynchronizedMethod()).isTrue();
    assertThat(method.isAbstract()).isTrue();
    assertThat(method.isPackageVisibility()).isTrue();
    assertThat(hidden.isPrivate()).isTrue();
  }

  @Test
  void member_ids_split_parameters_outside_type_arguments() {
    MemberId parsed = SemaFacts.parseMember("p.A#m(java.util.Map<K,V>,int)");

    assertThat(parsed.paramList()).containsExactly("java.util.Map<K,V>", "int");
    assertThat(parsed.arity()).isEqualTo(2);
    assertThat(SemaFacts.parseMember("p.A")).isNull();
    assertThat(SemaFacts.memberName("p.A")).isNull();
    assertThat(SemaFacts.parseMember("p.A#m(2)").arityOnly()).isTrue();
  }

  @Test
  void json_readers_ignore_values_of_another_shape() {
    JsonObject object = json("{\"o\":{},\"a\":[1,{},\"x\"],\"n\":null,\"p\":\"v\"}");

    assertThat(SemaFacts.text(null, "p")).isNull();
    assertThat(SemaFacts.text(object, "o")).isNull();
    assertThat(SemaFacts.text(object, "p")).isEqualTo("v");
    assertThat(SemaFacts.truth(object, "o")).isFalse();
    assertThat(SemaFacts.strings(null, "a")).isEmpty();
    assertThat(SemaFacts.strings(object, "n")).isEmpty();
    assertThat(SemaFacts.strings(object, "o")).isEmpty();
    assertThat(SemaFacts.strings(object, "p")).containsExactly("v");
    assertThat(SemaFacts.strings(object, "a")).containsExactly("1", "x");
    assertThat(SemaFacts.flat(List.of("a, b", " ,c"))).containsExactly("a", "b", "c");
  }

  private static JsonObject json(String text) {
    return JsonParser.parseString(text).getAsJsonObject();
  }
}
