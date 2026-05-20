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
package org.sonar.java.model;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static org.assertj.core.api.Assertions.assertThat;

class TypeUtilsTest {

  @Test
  void private_constructor() throws Exception {
    assertThat(isFinal(TypeUtils.class.getModifiers())).isTrue();
    Constructor<TypeUtils> constructor = TypeUtils.class.getDeclaredConstructor();
    assertThat(isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void isValueBasedType_returns_true_for_value_based_types() {
    // java.lang wrapper types
    assertThat(isValueBasedType("java.lang.Boolean b")).isTrue();
    assertThat(isValueBasedType("java.lang.Byte b")).isTrue();
    assertThat(isValueBasedType("java.lang.Character c")).isTrue();
    assertThat(isValueBasedType("java.lang.Double d")).isTrue();
    assertThat(isValueBasedType("java.lang.Float f")).isTrue();
    assertThat(isValueBasedType("java.lang.Integer i")).isTrue();
    assertThat(isValueBasedType("java.lang.Long l")).isTrue();
    assertThat(isValueBasedType("java.lang.Short s")).isTrue();
    // java.util optional types
    assertThat(isValueBasedType("java.util.Optional<String> o")).isTrue();
    assertThat(isValueBasedType("java.util.OptionalDouble od")).isTrue();
    assertThat(isValueBasedType("java.util.OptionalInt oi")).isTrue();
    assertThat(isValueBasedType("java.util.OptionalLong ol")).isTrue();
    // java.time.chrono types
    assertThat(isValueBasedType("java.time.chrono.HijrahDate hd")).isTrue();
    assertThat(isValueBasedType("java.time.chrono.JapaneseDate jd")).isTrue();
    assertThat(isValueBasedType("java.time.chrono.MinguoDate md")).isTrue();
    assertThat(isValueBasedType("java.time.chrono.ThaiBuddhistDate td")).isTrue();
    // java.time types
    assertThat(isValueBasedType("java.time.LocalDate ld")).isTrue();
    assertThat(isValueBasedType("java.time.LocalDateTime ldt")).isTrue();
    assertThat(isValueBasedType("java.time.Duration d")).isTrue();
    assertThat(isValueBasedType("java.time.Instant i")).isTrue();
  }

  @Test
  void isValueBasedType_returns_false_for_java_time_clock() {
    assertThat(isValueBasedType("java.time.Clock c")).isFalse();
  }

  @Test
  void isValueBasedType_returns_false_for_non_value_based_types() {
    assertThat(isValueBasedType("String s")).isFalse();
    assertThat(isValueBasedType("Object o")).isFalse();
    assertThat(isValueBasedType("java.util.List<String> l")).isFalse();
    assertThat(isValueBasedType("java.util.Map<String, String> m")).isFalse();
    // java.time.format is two levels deep and not in the explicit list
    assertThat(isValueBasedType("java.time.format.DateTimeFormatter dtf")).isFalse();
  }

  @Test
  void isValueBasedType_returns_false_for_unknown_type() {
    assertThat(isValueBasedType("UnknownClass u")).isFalse();
  }

  private static boolean isValueBasedType(String fieldDecl) {
    CompilationUnitTree cu = JParserTestUtils.parse("class A { " + fieldDecl + "; }");
    VariableTree field = (VariableTree) ((ClassTree) cu.types().get(0)).members().get(0);
    return TypeUtils.isValueBasedType(field.type().symbolType());
  }

}
