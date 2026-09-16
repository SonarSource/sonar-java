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
package org.sonar.java.model.springcontext;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.springcontext.ProfileExpression.and;
import static org.sonar.java.model.springcontext.ProfileExpression.not;
import static org.sonar.java.model.springcontext.ProfileExpression.or;
import static org.sonar.java.model.springcontext.ProfileExpression.profile;

class ProfileExpressionParserTest {

  // ---- Valid expressions --------------------------------------------------

  @ParameterizedTest
  @MethodSource
  void parses_valid_expressions(String expression, ProfileExpression expected) {
    assertThat(ProfileExpressionParser.parse(expression)).isEqualTo(expected);
  }

  static Stream<Arguments> parses_valid_expressions() {
    return Stream.of(
      Arguments.of("dev", profile("dev")),
      Arguments.of("  dev  ", profile("dev")),
      Arguments.of("my profile", profile("my profile")),
      Arguments.of("dev-1_x.y", profile("dev-1_x.y")),
      Arguments.of("!dev", not(profile("dev"))),
      Arguments.of("! dev", not(profile("dev"))),
      Arguments.of("!!dev", not(not(profile("dev")))),
      Arguments.of("dev & !test", and(List.of(profile("dev"), not(profile("test"))))),
      Arguments.of("a & b & c", and(List.of(profile("a"), profile("b"), profile("c")))),
      Arguments.of("a | b | c", or(List.of(profile("a"), profile("b"), profile("c")))),
      Arguments.of("(a & b) | c", or(List.of(and(List.of(profile("a"), profile("b"))), profile("c")))),
      Arguments.of("a & (b | c)", and(List.of(profile("a"), or(List.of(profile("b"), profile("c")))))),
      Arguments.of("!(dev | test)", not(or(List.of(profile("dev"), profile("test"))))),
      Arguments.of("((a))", profile("a")),
      Arguments.of("(  a  &  b  )", and(List.of(profile("a"), profile("b")))),
      Arguments.of("!(a & !(b | c))", not(and(List.of(profile("a"), not(or(List.of(profile("b"), profile("c")))))))));
  }

  // ---- Malformed expressions ----------------------------------------------

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "a & b | c", "a | b & c", "(", ")", "(a", "a)", "()", "!", "a &", "& a", "a ||b", "(a)(b)", "dev!"})
  void rejects_malformed_expressions(String expression) {
    assertThat(ProfileExpressionParser.parse(expression).isUnknown()).isTrue();
  }

  @Test
  void rejects_expressions_nested_beyond_the_depth_limit() {
    String deeplyNested = "(".repeat(200) + "a" + ")".repeat(200);

    assertThat(ProfileExpressionParser.parse(deeplyNested).isUnknown()).isTrue();
  }

  // ---- Round-trip ---------------------------------------------------------

  @ParameterizedTest
  @MethodSource
  void canonical_form_round_trips(ProfileExpression expected) {
    assertThat(ProfileExpressionParser.parse(expected.toCanonicalString())).isEqualTo(expected);
  }

  static Stream<Arguments> canonical_form_round_trips() {
    return Stream.of(
      Arguments.of(profile("dev")),
      Arguments.of(not(profile("dev"))),
      Arguments.of(not(not(profile("dev")))),
      Arguments.of(and(List.of(profile("dev"), not(profile("test"))))),
      Arguments.of(and(List.of(profile("a"), profile("b"), profile("c")))),
      Arguments.of(or(List.of(profile("a"), profile("b"), profile("c")))),
      Arguments.of(or(List.of(and(List.of(profile("a"), profile("b"))), profile("c")))),
      Arguments.of(and(List.of(profile("a"), or(List.of(profile("b"), profile("c")))))),
      Arguments.of(not(or(List.of(profile("dev"), profile("test"))))),
      Arguments.of(not(and(List.of(profile("a"), not(or(List.of(profile("b"), profile("c")))))))));
  }

  @Test
  void unknown_round_trips_through_its_canonical_form() {
    assertThat(ProfileExpression.UNKNOWN.toCanonicalString()).isEqualTo("&");
    assertThat(ProfileExpressionParser.parse("&").isUnknown()).isTrue();
  }

  @Test
  void unconditional_is_not_expressible_as_a_parseable_string() {
    assertThat(ProfileExpression.UNCONDITIONAL.toCanonicalString()).isEmpty();
    assertThat(ProfileExpressionParser.parse("").isUnknown()).isTrue();
  }
}
