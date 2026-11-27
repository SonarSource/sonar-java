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

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;

class SubtypingTest {

  private static final String LIST = "java.util.List";
  private static final String NUMBER = "java.lang.Number";

  private final JTypeFixtures fixtures = new JTypeFixtures();
  private final JSema sema = fixtures.sema;

  @Test
  void nothing_is_a_subtype_of_a_missing_type() {
    assertThat(Subtyping.isSubtype(fixtures.string(), null, new HashSet<>())).isFalse();
    assertThat(fixtures.string().isSubtypeOf(Type.UNKNOWN)).isFalse();
    assertThat(JType.unknown(sema).isSubtypeOf(fixtures.object())).isFalse();
  }

  @Test
  void capture_and_wildcard_are_subtypes_through_their_bound() {
    assertThat(fixtures.capture(fixtures.string()).isSubtypeOf("java.lang.CharSequence")).isTrue();
    assertThat(fixtures.extending(fixtures.integer()).isSubtypeOf(NUMBER)).isTrue();
    assertThat(fixtures.extending(fixtures.integer()).isSubtypeOf(fixtures.string())).isFalse();
  }

  @Test
  void type_variable_is_a_subtype_of_itself_and_of_its_bound() {
    JType variable = JType.typeVar(sema, "E", "java.util.List`1<E>");

    assertThat(variable.isSubtypeOf(JType.typeVar(sema, "E", "java.util.List`1<E>"))).isTrue();
    assertThat(variable.isSubtypeOf(JType.typeVar(sema, "T", "java.util.Set`1<T>"))).isFalse();
    assertThat(variable.isSubtypeOf(fixtures.object())).isTrue();
    assertThat(fixtures.string().isSubtypeOf(variable)).isFalse();
  }

  @Test
  void null_is_a_subtype_of_every_reference_type() {
    JType nullType = JType.nullType(sema);

    assertThat(nullType.isSubtypeOf(fixtures.string())).isTrue();
    assertThat(nullType.isSubtypeOf(fixtures.primitive("int"))).isFalse();
  }

  @Test
  void primitives_widen() {
    assertThat(fixtures.primitive("byte").isSubtypeOf(fixtures.primitive("short"))).isTrue();
    assertThat(fixtures.primitive("char").isSubtypeOf(fixtures.primitive("int"))).isTrue();
    assertThat(fixtures.primitive("int").isSubtypeOf(fixtures.primitive("long"))).isTrue();
    assertThat(fixtures.primitive("long").isSubtypeOf(fixtures.primitive("float"))).isTrue();
    assertThat(fixtures.primitive("float").isSubtypeOf(fixtures.primitive("double"))).isTrue();
    assertThat(fixtures.primitive("double").isSubtypeOf(fixtures.primitive("float"))).isFalse();
    assertThat(fixtures.primitive("boolean").isSubtypeOf(fixtures.primitive("int"))).isFalse();
    assertThat(fixtures.primitive("int").isSubtypeOf(fixtures.integer())).isFalse();
  }

  @Test
  void intersection_is_a_subtype_of_each_bound() {
    JType intersection = JType.intersection(sema, List.of(fixtures.type(NUMBER), fixtures.type("java.lang.Comparable")));

    assertThat(intersection.isSubtypeOf("java.lang.Comparable")).isTrue();
    assertThat(intersection.isSubtypeOf("java.io.Serializable")).isTrue();
    assertThat(intersection.isSubtypeOf(fixtures.string())).isFalse();
  }

  @Test
  void arrays_are_cloneable_and_serializable() {
    JType strings = JType.array(sema, fixtures.string());

    assertThat(strings.isSubtypeOf(fixtures.object())).isTrue();
    assertThat(strings.isSubtypeOf("java.lang.Cloneable")).isTrue();
    assertThat(strings.isSubtypeOf("java.io.Serializable")).isTrue();
    assertThat(strings.isSubtypeOf("java.lang.CharSequence")).isFalse();
    assertThat(strings.isSubtypeOf(JType.array(sema, fixtures.object()))).isTrue();
  }

  @Test
  void parameterizations_need_as_many_arguments() {
    JType malformed = fixtures.type(LIST, fixtures.string(), fixtures.string());

    assertThat(malformed.isSubtypeOf(fixtures.type(LIST, fixtures.string()))).isFalse();
  }

  @Test
  void lower_bounded_wildcard_contains_supertypes_of_its_bound() {
    JType consumer = fixtures.type(LIST, JType.superWildcard(sema, fixtures.integer()));

    assertThat(fixtures.type(LIST, fixtures.type(NUMBER)).isSubtypeOf(consumer)).isTrue();
    assertThat(fixtures.type(LIST, fixtures.string()).isSubtypeOf(consumer)).isFalse();
    assertThat(fixtures.type(LIST, JType.superWildcard(sema, fixtures.type(NUMBER))).isSubtypeOf(consumer)).isTrue();
    assertThat(fixtures.type(LIST, JType.superWildcard(sema, fixtures.integer())).isSubtypeOf(fixtures.type(LIST, JType.superWildcard(sema, fixtures.type(NUMBER)))))
      .isFalse();
    assertThat(fixtures.type(LIST, fixtures.extending(fixtures.integer())).isSubtypeOf(consumer)).isFalse();
  }

  @Test
  void upper_bounded_wildcard_contains_subtypes_of_its_bound() {
    JType producer = fixtures.type(LIST, fixtures.extending(fixtures.type(NUMBER)));

    assertThat(fixtures.type(LIST, fixtures.integer()).isSubtypeOf(producer)).isTrue();
    assertThat(fixtures.type(LIST, fixtures.string()).isSubtypeOf(producer)).isFalse();
    assertThat(fixtures.type(LIST, fixtures.string()).isSubtypeOf(fixtures.type(LIST, fixtures.extending(fixtures.object())))).isTrue();
    assertThat(fixtures.type(LIST, fixtures.extending(fixtures.integer())).isSubtypeOf(fixtures.type(LIST, fixtures.integer()))).isFalse();
  }
}
