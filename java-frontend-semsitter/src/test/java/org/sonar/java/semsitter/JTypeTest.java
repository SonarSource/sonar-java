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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;

class JTypeTest {

  private static final String LIST = "java.util.List";
  private static final String LIST_VARIABLE = "java.util.List`1<E>";

  private final JTypeFixtures fixtures = new JTypeFixtures();
  private final JSema sema = fixtures.sema;
  private final JType variable = JType.typeVar(sema, "E", LIST_VARIABLE);

  @Test
  void substitutes_inside_captures_wildcards_and_arrays() {
    Map<String, Type> substitution = Map.of(LIST_VARIABLE, fixtures.string());

    assertThat(fixtures.capture(variable).substitute(substitution).isCapture()).isTrue();
    assertThat(fixtures.capture(variable).substitute(substitution).erasure()).isEqualTo(fixtures.string());
    assertThat(JType.superWildcard(sema, variable).substitute(substitution).name()).isEqualTo("? super String");
    assertThat(fixtures.extending(variable).substitute(substitution).name()).isEqualTo("? extends String");
    assertThat(JType.array(sema, variable).substitute(substitution).name()).isEqualTo("String[]");
  }

  @Test
  void substitution_naming_no_variable_keeps_the_same_instance() {
    Map<String, Type> substitution = Map.of("other", fixtures.string());
    JType capture = fixtures.capture(variable);
    JType lower = JType.superWildcard(sema, variable);
    JType upper = fixtures.extending(variable);
    JType array = JType.array(sema, variable);
    JType list = fixtures.type(LIST, variable);

    assertThat(capture.substitute(substitution)).isSameAs(capture);
    assertThat(lower.substitute(substitution)).isSameAs(lower);
    assertThat(upper.substitute(substitution)).isSameAs(upper);
    assertThat(array.substitute(substitution)).isSameAs(array);
    assertThat(list.substitute(substitution)).isSameAs(list);
  }

  @Test
  void type_variable_without_id_is_substituted_by_name() {
    JType anonymous = JType.typeVar(sema, "T", null);

    assertThat(anonymous.substitute(Map.of("T", fixtures.integer()))).isSameAs(fixtures.integer());
    assertThat(anonymous.erasure()).isSameAs(anonymous);
    assertThat(fixtures.type(LIST, anonymous).isParameterized()).isTrue();
  }

  @Test
  void wildcard_of_unknown_bound_is_unknown() {
    JType unknown = JType.unknown(sema);

    assertThat(JType.wildcard(sema, unknown)).isSameAs(unknown);
  }

  @Test
  void ground_type_of_a_wildcard_is_its_bound() {
    assertThat(fixtures.string().groundType()).isSameAs(fixtures.string());
    assertThat(fixtures.extending(fixtures.string()).groundType()).isSameAs(fixtures.string());
    assertThat(JType.superWildcard(sema, fixtures.integer()).groundType()).isSameAs(fixtures.integer());
  }

  @Test
  void wrapper_of_a_class_is_null() {
    assertThat(fixtures.string().primitiveWrapperType()).isNull();
    assertThat(fixtures.primitive("int").primitiveWrapperType()).isEqualTo(fixtures.integer());
    assertThat(fixtures.integer().primitiveType()).isEqualTo(fixtures.primitive("int"));
  }

  @Test
  void declaring_type_of_a_raw_generic_type_names_its_parameters() {
    Type declaring = fixtures.type(LIST).declaringType();

    assertThat(declaring.typeArguments()).hasSize(1);
    assertThat(declaring.typeArguments().get(0).isTypeVar()).isTrue();
    assertThat(fixtures.type(LIST, fixtures.string()).declaringType()).isEqualTo(fixtures.type(LIST));
    assertThat(fixtures.string().declaringType()).isEqualTo(fixtures.string());
    assertThat(JType.array(sema, fixtures.string()).declaringType().isArray()).isTrue();
    assertThat(JType.unknown(sema).declaringType().isUnknown()).isTrue();
  }

  @Test
  void names_follow_the_source_spelling() {
    assertThat(fixtures.extending(fixtures.object()).name()).isEqualTo("?");
    assertThat(fixtures.type("java.util.Map$Entry")).hasToString("Entry");
    assertThat(JType.named(sema, "Enums$1", List.of()).name()).isEmpty();
    assertThat(JType.nullType(sema).name()).isEqualTo("<nulltype>");
  }

  @Test
  void intersection_lists_its_bounds() {
    JType intersection = JType.intersection(sema, List.of(fixtures.type("java.lang.Number"), fixtures.type("java.lang.Comparable")));

    assertThat(intersection.isIntersectionType()).isTrue();
    assertThat(intersection.getIntersectionTypes()).containsExactly(fixtures.type("java.lang.Number"), fixtures.type("java.lang.Comparable"));
    assertThat(fixtures.string().isIntersectionType()).isFalse();
    assertThat(fixtures.string().getIntersectionTypes()).containsExactly(fixtures.string());
  }

  @Test
  void captures_are_equal_only_to_themselves() {
    JType capture = fixtures.capture(fixtures.string());

    assertThat(capture).isEqualTo(capture).isNotEqualTo(fixtures.capture(fixtures.string()));
    assertThat(fixtures.string()).isNotEqualTo("java.lang.String");
  }

  @Test
  void wildcards_are_equal_when_their_bounds_are() {
    JType upper = fixtures.extending(fixtures.string());

    assertThat(upper).isEqualTo(fixtures.extending(fixtures.string()))
      .isNotEqualTo(fixtures.extending(fixtures.integer()))
      .isNotEqualTo(JType.superWildcard(sema, fixtures.string()))
      .isNotEqualTo(fixtures.string());
    assertThat(fixtures.extending(fixtures.object())).isNotEqualTo(JType.superWildcard(sema, fixtures.string()));
  }

  @Test
  void arrays_and_parameterizations_are_equal_when_their_parts_are() {
    assertThat(JType.array(sema, fixtures.string())).isEqualTo(JType.array(sema, fixtures.string()));
    assertThat(JType.array(sema, fixtures.type(LIST, fixtures.string()))).isNotEqualTo(JType.array(sema, fixtures.type(LIST, fixtures.integer())));
    assertThat(fixtures.type(LIST, fixtures.string())).isEqualTo(fixtures.type(LIST, fixtures.string()))
      .isNotEqualTo(fixtures.type(LIST, fixtures.integer()))
      .isNotEqualTo(fixtures.type(LIST));
    assertThat(fixtures.primitive("int")).isNotEqualTo(fixtures.integer());
  }

  @Test
  void type_variable_named_simply_is_the_one_its_declaration_spells_in_full() {
    JType simple = JType.typeVar(sema, "E", "E");
    JType other = JType.typeVar(sema, "E", "java.util.Set`1<E>");

    assertThat(simple).isEqualTo(variable);
    assertThat(variable).isEqualTo(simple).isNotEqualTo(other).isNotEqualTo(JType.typeVar(sema, "E", null));
    assertThat(JType.typeVar(sema, "E", null)).isNotEqualTo(variable);
  }
}
