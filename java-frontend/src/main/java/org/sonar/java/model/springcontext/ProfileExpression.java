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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A condition under which a Spring bean is active, as declared by a {@code @Profile} annotation.
 *
 * <p>Instances are built by {@link ProfileExpressionParser#parse(String)} or by the factory methods of this
 * interface, never by calling the record constructors directly: the factories are what establish the
 * invariant that a {@link Profile} name is non-blank, trimmed and free of the operator characters
 * {@code ( ) & | !}.
 */
public sealed interface ProfileExpression {

  /**
   * Characters that carry meaning in a profile expression and can therefore not appear in a profile name.
   */
  String OPERATOR_CHARACTERS = "()&|!";

  /**
   * Canonical form of {@link #UNKNOWN}.
   *
   * <p>It cannot collide with the canonical form of any other expression: {@link #toCanonicalString()} only
   * ever emits {@code &} between operands inside parentheses, and {@link #profile(String)} rejects names
   * containing {@code &}, so no other expression prints as {@code "&"} alone. Reading it back is equally
   * unambiguous, since a lone {@code &} is a parse error by grammar and therefore yields {@link #UNKNOWN}.
   */
  String UNKNOWN_CANONICAL_FORM = "&";

  /**
   * The bean carries no {@code @Profile} and is always active.
   */
  ProfileExpression UNCONDITIONAL = new Unconditional();

  /**
   * The declared {@code @Profile} could not be interpreted; the bean must be assumed possibly active.
   */
  ProfileExpression UNKNOWN = new Unknown();

  record Unconditional() implements ProfileExpression {
  }

  record Unknown() implements ProfileExpression {
  }

  record Profile(String name) implements ProfileExpression {
  }

  record Not(ProfileExpression operand) implements ProfileExpression {
  }

  record And(Set<ProfileExpression> operands) implements ProfileExpression {
    public And {
      operands = lexicographicallyOrdered(operands);
    }
  }

  record Or(Set<ProfileExpression> operands) implements ProfileExpression {
    public Or {
      operands = lexicographicallyOrdered(operands);
    }
  }

  /**
   * Create a new named Spring profile expression.
   *
   * @param name The name of a single profile, which may be surrounded by whitespace.
   * @return A {@link Profile} for the trimmed name, or {@link #UNKNOWN} if it is blank or contains one of
   * {@link #OPERATOR_CHARACTERS}.
   */
  static ProfileExpression profile(String name) {
    String trimmed = name.trim();
    if (trimmed.isEmpty() || trimmed.chars().anyMatch(c -> OPERATOR_CHARACTERS.indexOf(c) >= 0)) {
      return UNKNOWN;
    }
    return new Profile(trimmed);
  }

  /**
   * Create a new negated Spring profile expression.
   *
   * @param operand The expression to negate.
   * @return The negated expression, or {@link #UNKNOWN} if the operand is {@link #UNKNOWN} or
   * {@link #UNCONDITIONAL}. Double negations are kept as declared rather than collapsed.
   */
  static ProfileExpression not(ProfileExpression operand) {
    if (operand.isUnknown() || operand.isUnconditional()) {
      return UNKNOWN;
    }
    return new Not(operand);
  }

  /**
   * Conjunction, as declared by {@code &} or by composing a class-level with a method-level {@code @Profile}.
   *
   * @param operands The expressions that must all match.
   * @return {@link #UNKNOWN} if any operand is unknown; otherwise the conjunction of the operands, with
   * {@link #UNCONDITIONAL} operands dropped, nested conjunctions flattened, duplicates removed, and the
   * remaining operands ordered lexicographically by their {@link #toCanonicalString() canonical form}.
   */
  static ProfileExpression and(Collection<? extends ProfileExpression> operands) {
    List<ProfileExpression> flattened = new ArrayList<>();
    for (ProfileExpression operand : operands) {
      if (operand.isUnknown()) {
        return UNKNOWN;
      }
      if (operand instanceof And(Set<ProfileExpression> otherOperands)) {
        flattened.addAll(otherOperands);
      } else if (!operand.isUnconditional()) {
        flattened.add(operand);
      }
    }
    return simplify(flattened, And::new);
  }

  /**
   * Disjunction, as declared by {@code |} or by the elements of a {@code @Profile} array.
   *
   * @param operands The expressions of which at least one must match.
   * @return {@link #UNCONDITIONAL} if any operand is unconditional, otherwise {@link #UNKNOWN} if any
   * operand is unknown; otherwise the disjunction of the operands, with nested disjunctions flattened,
   * duplicates removed, and the remaining operands ordered lexicographically by their
   * {@link #toCanonicalString() canonical form}.
   */
  static ProfileExpression or(Collection<? extends ProfileExpression> operands) {
    if (operands.stream().anyMatch(ProfileExpression::isUnconditional)) {
      return UNCONDITIONAL;
    }
    if (operands.stream().anyMatch(ProfileExpression::isUnknown)) {
      return UNKNOWN;
    }
    List<ProfileExpression> flattened = new ArrayList<>();
    for (ProfileExpression operand : operands) {
      if (operand instanceof Or(Set<ProfileExpression> otherOperands)) {
        flattened.addAll(otherOperands);
      } else {
        flattened.add(operand);
      }
    }
    return simplify(flattened, Or::new);
  }

  /**
   * @return Whether the bean carries no {@code @Profile} and is therefore always active.
   */
  default boolean isUnconditional() {
    return this instanceof Unconditional;
  }

  /**
   * @return Whether the declared {@code @Profile} could not be interpreted.
   */
  default boolean isUnknown() {
    return this instanceof Unknown;
  }

  /**
   * @return Every profile name this expression refers to, in lexicographic order; empty for both
   * {@link UNCONDITIONAL} and {@link UNKNOWN}.
   */
  default Set<String> profileNames() {
    return switch (this) {
      case Unconditional() -> Set.of();
      case Unknown() -> Set.of();
      case Profile(String name) -> Set.of(name);
      case Not(ProfileExpression operand) -> operand.profileNames();
      case And(Set<ProfileExpression> operands) -> namesOf(operands);
      case Or(Set<ProfileExpression> operands) -> namesOf(operands);
    };
  }

  /**
   * Evaluates this expression against a hypothetical set of active profiles.
   *
   * <p>This answers <i>satisfiability</i>, not <i>reachability</i>: a {@code true} result means the expression
   * is not contradicted by that set, not that some deployment actually activates exactly it. Both {@link UNCONDITIONAL}
   * and {@link UNKNOWN} evaluate to {@code true} for every set, so an expression we do not understand is never taken to
   * make a bean inactive.
   *
   * <p>Spring's {@code default} profile is treated as an ordinary name here, although Spring activates it only
   * when no other profile is active. Callers reasoning about whether two beans can coexist therefore
   * over-approximate for {@code @Profile("default")}, which is the safe direction.
   *
   * @param activeProfiles The names assumed to be active; every other name is assumed inactive.
   * @return Whether the bean this expression guards would be active under those profiles
   */
  default boolean isActiveUnder(Set<String> activeProfiles) {
    return switch (this) {
      case Unconditional() -> true;
      case Unknown() -> true;
      case Profile(String name) -> activeProfiles.contains(name);
      case Not(ProfileExpression operand) -> !operand.isActiveUnder(activeProfiles);
      case And(Set<ProfileExpression> operands) -> operands.stream().allMatch(operand -> operand.isActiveUnder(activeProfiles));
      case Or(Set<ProfileExpression> operands) -> operands.stream().anyMatch(operand -> operand.isActiveUnder(activeProfiles));
    };
  }

  /**
   * Renders this expression as a string {@link ProfileExpressionParser#parse(String)} reads back into an equal
   * expression, which is how a profile expression is persisted in the analysis cache.
   *
   * <p>Compound expressions are fully parenthesized, so the result never depends on operator precedence and
   * can never mix {@code &} with {@code |} at one level — which Spring rejects. {@link #UNCONDITIONAL} renders
   * as the empty string, which {@code parse} does <b>not</b> accept: callers must encode the unconditional
   * case out of band rather than round-tripping it through this method.
   *
   * @return The canonical form of this expression, or {@link #UNKNOWN_CANONICAL_FORM} if it is unknown.
   */
  default String toCanonicalString() {
    return switch (this) {
      case Unconditional() -> "";
      case Unknown() -> UNKNOWN_CANONICAL_FORM;
      case Profile(String name) -> name;
      case Not(ProfileExpression operand) -> "!" + operand.toCanonicalString();
      case And(Set<ProfileExpression> operands) -> join(operands, " & ");
      case Or(Set<ProfileExpression> operands) -> join(operands, " | ");
    };
  }

  private static ProfileExpression simplify(Collection<? extends ProfileExpression> operands, Function<Set<ProfileExpression>, ProfileExpression> factory) {
    Set<ProfileExpression> normalized = lexicographicallyOrdered(operands);
    return switch (normalized.size()) {
      case 0 -> UNCONDITIONAL;
      case 1 -> normalized.iterator().next();
      default -> factory.apply(normalized);
    };
  }

  private static Set<ProfileExpression> lexicographicallyOrdered(Collection<? extends ProfileExpression> operands) {
    Set<ProfileExpression> ordered = operands.stream()
      .sorted(Comparator.comparing(ProfileExpression::toCanonicalString))
      .collect(Collectors.toCollection(LinkedHashSet::new));
    return Collections.unmodifiableSet(ordered);
  }

  private static Set<String> namesOf(Set<ProfileExpression> operands) {
    Set<String> names = new LinkedHashSet<>();
    operands.forEach(operand -> names.addAll(operand.profileNames()));
    Set<String> orderedNames = names.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));
    return Collections.unmodifiableSet(orderedNames);
  }

  private static String join(Set<ProfileExpression> operands, String separator) {
    return operands.stream().map(ProfileExpression::toCanonicalString).collect(Collectors.joining(separator, "(", ")"));
  }
}
