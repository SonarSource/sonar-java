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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.model.springcontext.ProfileExpression.UNCONDITIONAL;
import static org.sonar.java.model.springcontext.ProfileExpression.UNKNOWN;
import static org.sonar.java.model.springcontext.ProfileExpression.and;
import static org.sonar.java.model.springcontext.ProfileExpression.not;
import static org.sonar.java.model.springcontext.ProfileExpression.or;
import static org.sonar.java.model.springcontext.ProfileExpression.profile;

class ProfileExpressionTest {

  // ---- Unconditional and Unknown ------------------------------------------

  @Test
  void sentinels_are_distinguishable_from_each_other_and_from_real_expressions() {
    assertThat(UNCONDITIONAL.isUnconditional()).isTrue();
    assertThat(UNCONDITIONAL.isUnknown()).isFalse();
    assertThat(UNKNOWN.isUnknown()).isTrue();
    assertThat(UNKNOWN.isUnconditional()).isFalse();
    assertThat(profile("dev").isUnconditional()).isFalse();
    assertThat(profile("dev").isUnknown()).isFalse();
  }

  // ---- Profile ------------------------------------------------------------

  @Test
  void profile_trims_the_declared_name() {
    assertThat(profile("  dev  ")).isEqualTo(new ProfileExpression.Profile("dev"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "a&b", "a|b", "!a", "(a", "a)"})
  void profile_is_unknown_when_the_name_is_blank_or_holds_an_operator(String name) {
    assertThat(profile(name).isUnknown()).isTrue();
  }

  // ---- Not ----------------------------------------------------------------

  @Test
  void not_negates_a_real_expression() {
    assertThat(not(profile("dev"))).isEqualTo(new ProfileExpression.Not(profile("dev")));
  }

  @Test
  void not_keeps_double_negations_as_declared() {
    assertThat(not(not(profile("dev")))).isEqualTo(new ProfileExpression.Not(new ProfileExpression.Not(profile("dev"))));
  }

  @Test
  void not_is_unknown_for_both_sentinels() {
    assertThat(not(UNKNOWN).isUnknown()).isTrue();
    assertThat(not(UNCONDITIONAL).isUnknown()).isTrue();
  }

  // ---- And ----------------------------------------------------------------

  @Test
  void and_is_unknown_when_any_operand_is_unknown() {
    assertThat(and(List.of(profile("dev"), UNKNOWN)).isUnknown()).isTrue();
  }

  @Test
  void and_drops_unconditional_operands() {
    assertThat(and(List.of(UNCONDITIONAL, profile("dev")))).isEqualTo(profile("dev"));
    assertThat(and(List.of(UNCONDITIONAL, profile("dev"), profile("test"))))
      .isEqualTo(new ProfileExpression.And(Set.of(profile("dev"), profile("test"))));
  }

  @Test
  void and_flattens_nested_conjunctions() {
    assertThat(and(List.of(and(List.of(profile("a"), profile("b"))), profile("c"))))
      .isEqualTo(new ProfileExpression.And(Set.of(profile("a"), profile("b"), profile("c"))));
  }

  @Test
  void and_of_nothing_is_unconditional() {
    assertThat(and(List.of()).isUnconditional()).isTrue();
    assertThat(and(List.of(UNCONDITIONAL, UNCONDITIONAL)).isUnconditional()).isTrue();
  }

  @Test
  void and_of_a_single_operand_is_that_operand() {
    assertThat(and(List.of(profile("dev")))).isEqualTo(profile("dev"));
  }

  @Test
  void and_deduplicates_and_orders_operands() {
    ProfileExpression expression = and(List.of(profile("c"), profile("a"), profile("b"), profile("a")));

    assertThat(expression).isInstanceOfSatisfying(ProfileExpression.And.class,
      conjunction -> assertThat(conjunction.operands()).containsExactly(profile("a"), profile("b"), profile("c")));
  }

  @Test
  void and_is_commutative_for_equality_and_canonical_form() {
    ProfileExpression aAndB = and(List.of(profile("a"), profile("b")));
    ProfileExpression bAndA = and(List.of(profile("b"), profile("a")));

    assertThat(aAndB).isEqualTo(bAndA);
    assertThat(aAndB.toCanonicalString()).isEqualTo(bAndA.toCanonicalString()).isEqualTo("(a & b)");
  }

  // ---- Or -----------------------------------------------------------------

  @Test
  void or_is_unknown_when_any_operand_is_unknown() {
    assertThat(or(List.of(profile("dev"), UNKNOWN)).isUnknown()).isTrue();
  }

  @Test
  void or_is_unconditional_when_any_operand_is_unconditional() {
    assertThat(or(List.of(profile("dev"), UNCONDITIONAL)).isUnconditional()).isTrue();
    assertThat(or(List.of(UNKNOWN, UNCONDITIONAL)).isUnconditional()).isTrue();
    assertThat(or(List.of(UNCONDITIONAL, UNKNOWN)).isUnconditional()).isTrue();
  }

  @Test
  void or_flattens_nested_disjunctions() {
    assertThat(or(List.of(or(List.of(profile("a"), profile("b"))), profile("c"))))
      .isEqualTo(new ProfileExpression.Or(Set.of(profile("a"), profile("b"), profile("c"))));
  }

  @Test
  void or_of_nothing_is_unconditional() {
    assertThat(or(List.of()).isUnconditional()).isTrue();
  }

  @Test
  void or_of_a_single_operand_is_that_operand() {
    assertThat(or(List.of(profile("dev")))).isEqualTo(profile("dev"));
  }

  @Test
  void or_deduplicates_and_orders_operands() {
    ProfileExpression expression = or(List.of(profile("c"), profile("a"), profile("b"), profile("a")));

    assertThat(expression).isInstanceOfSatisfying(ProfileExpression.Or.class,
      disjunction -> assertThat(disjunction.operands()).containsExactly(profile("a"), profile("b"), profile("c")));
  }

  @Test
  void compound_operands_are_immutable() {
    ProfileExpression.And expression = (ProfileExpression.And) and(List.of(profile("a"), profile("b")));

    Set<ProfileExpression> compoundOperands = expression.operands();
    ProfileExpression profileExpr = profile("c");
    assertThatThrownBy(() -> compoundOperands.add(profileExpr)).isInstanceOf(UnsupportedOperationException.class);
  }

  // ---- profileNames -------------------------------------------------------

  @Test
  void profile_names_are_collected_in_lexicographic_order_without_duplicates() {
    assertThat(profile("dev").profileNames()).containsExactly("dev");
    assertThat(not(profile("dev")).profileNames()).containsExactly("dev");
    assertThat(and(List.of(profile("dev"), not(profile("test")))).profileNames()).containsExactly("dev", "test");
    assertThat(or(List.of(profile("b"), profile("a"), profile("b"))).profileNames()).containsExactly("a", "b");
  }

  @Test
  void sentinels_refer_to_no_profile_name() {
    assertThat(UNCONDITIONAL.profileNames()).isEmpty();
    assertThat(UNKNOWN.profileNames()).isEmpty();
  }

  // ---- isActiveUnder ------------------------------------------------------

  @Test
  void a_conjunction_with_a_negation_is_active_only_for_the_matching_profiles() {
    ProfileExpression devAndNotTest = and(List.of(profile("dev"), not(profile("test"))));

    assertThat(devAndNotTest.isActiveUnder(Set.of())).isFalse();
    assertThat(devAndNotTest.isActiveUnder(Set.of("dev"))).isTrue();
    assertThat(devAndNotTest.isActiveUnder(Set.of("dev", "test"))).isFalse();
    assertThat(devAndNotTest.isActiveUnder(Set.of("test"))).isFalse();
  }

  @Test
  void a_disjunction_of_a_conjunction_is_active_when_either_side_matches() {
    ProfileExpression aAndBOrC = or(List.of(and(List.of(profile("a"), profile("b"))), profile("c")));

    assertThat(aAndBOrC.isActiveUnder(Set.of())).isFalse();
    assertThat(aAndBOrC.isActiveUnder(Set.of("a"))).isFalse();
    assertThat(aAndBOrC.isActiveUnder(Set.of("a", "b"))).isTrue();
    assertThat(aAndBOrC.isActiveUnder(Set.of("c"))).isTrue();
  }

  @Test
  void a_negated_disjunction_is_active_only_when_neither_profile_matches() {
    ProfileExpression notAOrB = not(or(List.of(profile("a"), profile("b"))));

    assertThat(notAOrB.isActiveUnder(Set.of())).isTrue();
    assertThat(notAOrB.isActiveUnder(Set.of("a"))).isFalse();
    assertThat(notAOrB.isActiveUnder(Set.of("a", "b"))).isFalse();
  }

  @Test
  void both_sentinels_are_active_under_any_profiles() {
    assertThat(UNCONDITIONAL.isActiveUnder(Set.of())).isTrue();
    assertThat(UNCONDITIONAL.isActiveUnder(Set.of("dev"))).isTrue();
    assertThat(UNKNOWN.isActiveUnder(Set.of())).isTrue();
    assertThat(UNKNOWN.isActiveUnder(Set.of("dev"))).isTrue();
  }

  // ---- toCanonicalString --------------------------------------------------

  @Test
  void compound_expressions_are_printed_fully_parenthesized() {
    assertThat(UNCONDITIONAL.toCanonicalString()).isEmpty();
    assertThat(UNKNOWN.toCanonicalString()).isEqualTo("&");
    assertThat(profile("dev").toCanonicalString()).isEqualTo("dev");
    assertThat(not(profile("dev")).toCanonicalString()).isEqualTo("!dev");
    assertThat(and(List.of(profile("a"), profile("b"), profile("c"))).toCanonicalString()).isEqualTo("(a & b & c)");
    assertThat(or(List.of(profile("a"), profile("b"))).toCanonicalString()).isEqualTo("(a | b)");
    assertThat(not(or(List.of(profile("a"), profile("b")))).toCanonicalString()).isEqualTo("!(a | b)");
    assertThat(or(List.of(and(List.of(profile("a"), profile("b"))), profile("c"))).toCanonicalString()).isEqualTo("((a & b) | c)");
  }
}
