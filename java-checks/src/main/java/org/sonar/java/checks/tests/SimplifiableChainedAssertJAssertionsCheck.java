package org.sonar.java.checks.tests;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S5838")
public class SimplifiableChainedAssertJAssertionsCheck extends AbstractMethodDetection {

  private static final String ABSTRACT_ASSERT = "org.assertj.core.api.AbstractAssert";
  private static final String ASSERTIONS = "org.assertj.core.api.Assertions";
  private static final List<String> ASSERTION_MESSAGE_NAMES = Arrays.asList("as", "describedAs", "withFailMessage", "overridingErrorMessage");

  private static final MethodMatchers ASSERTION_PREDICATES = MethodMatchers.create()
    .ofSubTypes(ABSTRACT_ASSERT).name(name -> !ASSERTION_MESSAGE_NAMES.contains(name)).withAnyParameters().build();

  // TODO: Support more assertion subject methods / from different classes
  private static final MethodMatchers ASSERTIONS_SUBJECT_METHODS = MethodMatchers.create()
    .ofTypes(ASSERTIONS).names("assertThat").withAnyParameters().build();

  private static final String HAS_SIZE = "hasSize";
  private static final String IS_EQUAL_TO = "isEqualTo";
  private static final String IS_FALSE = "isFalse";
  private static final String IS_GREATER_THAN = "isGreaterThan";
  private static final String IS_GREATER_THAN_OR_EQUAL_TO = "isGreaterThanOrEqualTo";
  private static final String IS_LESS_THAN = "isLessThan";
  private static final String IS_LESS_THAN_OR_EQUAL_TO = "isLessThanOrEqualTo";
  private static final String IS_NEGATIVE = "isNegative";
  private static final String IS_NOT_EQUAL_TO = "isNotEqualTo";
  private static final String IS_NOT_NEGATIVE = "isNotNegative";
  private static final String IS_NOT_POSITIVE = "isNotPositive";
  private static final String IS_NOT_ZERO = "isNotZero";
  private static final String IS_POSITIVE = "isPositive";
  private static final String IS_TRUE = "isTrue";
  private static final String IS_ZERO = "isZero";

  /**
   * Stores multiple lists of simplifiers which are mapped to by a key. The key is the method name of the predicate
   * that this simplifier applies to. The simplifiers in this map are not provided with the subject argument.
   *
   * For instance, if you have a key {@code hasSize} that maps to a list containing
   * {@code PredicateSimplifierWithoutContext.withSingleArg(arg -> isZero(arg), "isEmpty()")} then it can be read as:
   * "<b>{@code hasSize}</b> with an argument that is <b>zero</b> can be simplified to <b>{@code isEmpty()}</b>".
   */
  private static final Map<String, List<SimplifierWithoutContext>> CONTEXT_FREE_SIMPLIFIERS = ImmutableMap.<String, List<SimplifierWithoutContext>>builder()
    .put(HAS_SIZE, ImmutableList.of(
      PredicateSimplifierWithoutContext.withSingleArg(Helper::isZero, "isEmpty()")))
    .put(IS_EQUAL_TO, ImmutableList.of(
      PredicateSimplifierWithoutContext.withSingleArg(ExpressionUtils::isNullLiteral, "isNull()"),
      PredicateSimplifierWithoutContext.withSingleArg(Helper::isTrue, "isTrue()"),
      PredicateSimplifierWithoutContext.withSingleArg(Helper::isFalse, "isFalse()")))
    .put(IS_NOT_EQUAL_TO, ImmutableList.of(
      PredicateSimplifierWithoutContext.withSingleArg(ExpressionUtils::isNullLiteral, "isNotNull()")))
    .build();

  /**
   * Stores multiple lists of simplifiers with context, similar to {@link #CONTEXT_FREE_SIMPLIFIERS}. The
   * simplifiers in this map, though, have access to the subject as well (i.e. the {@code assertThat(...)} method
   * and its argument).
   */
  private static final Map<String, List<SimplifierWithContext>> SIMPLIFIERS_WITH_CONTEXT = ImmutableMap.<String, List<SimplifierWithContext>>builder()
    .put(IS_EQUAL_TO, ImmutableList.of(
      PredicateSimplifierWithContext.methodCallInSubject(ImplMatchers.TO_STRING, msgWithActualCustom("hasToString", "expectedString")),
      PredicateSimplifierWithContext.withSingleArgument(predicateArg -> hasMethodCallAsArg(predicateArg, ImplMatchers.HASH_CODE),
        subjectArg -> hasMethodCallAsArg(subjectArg, ImplMatchers.HASH_CODE), msgWithActualExpected("hasSameHashCodeAs")),
      compareToSimplifier(Helper::isZero, msgWithActualExpected("isEqualByComparingTo"))))
    .put(IS_FALSE, ImmutableList.of(
      PredicateSimplifierWithContext.methodCallInSubject(ImplMatchers.EQUALS_METHOD, msgWithActualExpected(IS_NOT_EQUAL_TO)),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> Helper.equalsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNotNull")),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> Helper.notEqualsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNull")),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.EQUAL_TO), msgWithActualExpected("isNotSameAs")),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.NOT_EQUAL_TO), msgWithActualExpected("isSameAs")),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.INSTANCE_OF), msgWithActualCustom("isNotInstanceOf", "ExpectedClass.class"))))
    .put(IS_GREATER_THAN, ImmutableList.of(
      compareToSimplifier(Helper::isNegOne, msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO)),
      compareToSimplifier(Helper::isZero, msgWithActualExpected(IS_GREATER_THAN))))
    .put(IS_GREATER_THAN_OR_EQUAL_TO, ImmutableList.of(
      compareToSimplifier(Helper::isZero, msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO)),
      compareToSimplifier(Helper::isOne, msgWithActualExpected(IS_GREATER_THAN))))
    .put(IS_LESS_THAN, ImmutableList.of(
      compareToSimplifier(Helper::isOne, msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO)),
      compareToSimplifier(Helper::isZero, msgWithActualExpected(IS_LESS_THAN))))
    .put(IS_LESS_THAN_OR_EQUAL_TO, ImmutableList.of(
      compareToSimplifier(Helper::isZero, msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO)),
      compareToSimplifier(Helper::isNegOne, msgWithActualExpected(IS_LESS_THAN))))
    .put(IS_NEGATIVE, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected(IS_LESS_THAN))))
    .put(IS_NOT_EQUAL_TO, ImmutableList.of(
      compareToSimplifier(Helper::isZero, msgWithActualExpected("isNotEqualByComparingTo"))))
    .put(IS_NOT_NEGATIVE, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected(IS_GREATER_THAN_OR_EQUAL_TO))))
    .put(IS_NOT_POSITIVE, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected(IS_LESS_THAN_OR_EQUAL_TO))))
    .put(IS_NOT_ZERO, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected("isNotEqualByComparingTo"))))
    .put(IS_POSITIVE, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected(IS_GREATER_THAN))))
    .put(IS_TRUE, ImmutableList.of(
      PredicateSimplifierWithContext.methodCallInSubject(ImplMatchers.EQUALS_METHOD, msgWithActualExpected(IS_EQUAL_TO)),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> Helper.equalsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNull")),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> Helper.notEqualsTo(arg, ExpressionUtils::isNullLiteral), msgWithActual("isNotNull")),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.EQUAL_TO), msgWithActualExpected("isSameAs")),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.NOT_EQUAL_TO), msgWithActualExpected("isNotSameAs")),
      PredicateSimplifierWithContext.withSubjectArgumentCondition(arg -> arg.is(Tree.Kind.INSTANCE_OF), msgWithActualCustom("isInstanceOf", "ExpectedClass.class"))))
    .put(IS_ZERO, ImmutableList.of(
      compareToSimplifier(msgWithActualExpected("isEqualByComparingTo"))))
    .build();

  private static class ImplMatchers {
    public static final MethodMatchers EQUALS_METHOD = MethodMatchers.create().ofAnyType().names("equals")
      .addParametersMatcher(parameters -> parameters.size() == 1).build();
    public static final MethodMatchers TO_STRING = MethodMatchers.create().ofAnyType().names("toString")
      .addWithoutParametersMatcher().build();
    public static final MethodMatchers HASH_CODE = MethodMatchers.create().ofAnyType().names("hashCode")
      .addWithoutParametersMatcher().build();
    public static final MethodMatchers COMPARE_TO = MethodMatchers.create().ofSubTypes("java.lang.Comparable")
      .names("compareTo").addParametersMatcher(parameters -> parameters.size() == 1).build();
  }

  private static String msgWithActual(String predicateName) {
    return String.format("assertThat(actual).%s()", predicateName);
  }

  private static String msgWithActualExpected(String predicateName) {
    return String.format("assertThat(actual).%s(expected)", predicateName);
  }

  private static String msgWithActualCustom(String predicateName, String predicateArg) {
    return String.format("assertThat(actual).%s(%s)", predicateName, predicateArg);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return ASSERTIONS_SUBJECT_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree subjectMit) {
    List<MethodInvocationTree> predicates = new ArrayList<>();
    Optional<MethodInvocationTree> nextPredicate = Optional.of(subjectMit);
    while (true) {
      nextPredicate = MethodTreeUtils.subsequentMethodInvocation(nextPredicate.get(), ASSERTION_PREDICATES);
      if (nextPredicate.isPresent()) {
        predicates.add(nextPredicate.get());
      } else {
        break;
      }
    }

    boolean wasIssueRaised = checkPredicatesForSimplification(predicates, CONTEXT_FREE_SIMPLIFIERS,
      SimplifierWithoutContext::simplify,
      (predicate, replacement) -> reportIssue(ExpressionUtils.methodName(predicate), String.format("Replace with %s", replacement)));

    // We do not continue when we have already raised an issue to avoid potentially conflicting issue reports. If we
    // have more than one predicate we also avoid continuing to avoid FP on cases such as:
    // assertThat(Integer.valueOf(1).compareTo(2)).isGreaterThan(1).isLessThan(10)
    if (wasIssueRaised || predicates.size() > 1) {
      return;
    }

    checkPredicatesForSimplification(predicates, SIMPLIFIERS_WITH_CONTEXT,
      (simplifier, predicate) -> simplifier.simplify(subjectMit, predicate),
      (predicate, replacement) -> reportIssue(ExpressionUtils.methodName(predicate), String.format("Replace with %s", replacement),
        Collections.singletonList(new JavaFileScannerContext.Location("", subjectMit)), null));
  }

  /**
   * @return {@code true} when an issue was reported, {@code false} otherwise.
   */
  private static <T> boolean checkPredicatesForSimplification(
    List<MethodInvocationTree> predicates,
    Map<String, List<T>> simplifiers,
    BiFunction<T, MethodInvocationTree, Optional<String>> simplificationMethod,
    BiConsumer<MethodInvocationTree, String> reportingMethod) {
    BooleanFlag issueRaised = new BooleanFlag();
    predicates.forEach(predicate -> {
      String predicateName = ExpressionUtils.methodName(predicate).name();
      if (simplifiers.containsKey(predicateName)) {
        simplifiers.get(predicateName).stream()
          .map(simplifier -> simplificationMethod.apply(simplifier, predicate))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst()
          .ifPresent(replacement -> {
            reportingMethod.accept(predicate, replacement);
            issueRaised.setTrue();
          });
      }
    });
    return issueRaised.value();
  }

  // Consider moving this helper (or its content) to an external public helper class
  private static class Helper {
    private static boolean isZero(ExpressionTree tree) {
      return tree.is(Tree.Kind.INT_LITERAL) && "0".equals(((LiteralTree) tree).value());
    }

    private static boolean isOne(ExpressionTree tree) {
      return tree.is(Tree.Kind.INT_LITERAL) && "1".equals(((LiteralTree) tree).value());
    }

    private static boolean isNegOne(ExpressionTree tree) {
      return tree.is(Tree.Kind.UNARY_MINUS) && isOne(((UnaryExpressionTree) tree).expression());
    }

    private static boolean isTrue(ExpressionTree tree) {
      return tree.is(Tree.Kind.BOOLEAN_LITERAL) && "true".equals(((LiteralTree) tree).value());
    }

    private static boolean isFalse(ExpressionTree tree) {
      return tree.is(Tree.Kind.BOOLEAN_LITERAL) && "false".equals(((LiteralTree) tree).value());
    }

    private static boolean equalsTo(ExpressionTree expression, Predicate<ExpressionTree> comparedWithPredicate) {
      return expression.is(Tree.Kind.EQUAL_TO) && leftOrRightIs((BinaryExpressionTree) expression, comparedWithPredicate);
    }

    private static boolean notEqualsTo(ExpressionTree expression, Predicate<ExpressionTree> comparedWithPredicate) {
      return expression.is(Tree.Kind.NOT_EQUAL_TO) && leftOrRightIs((BinaryExpressionTree) expression, comparedWithPredicate);
    }

    private static boolean leftOrRightIs(BinaryExpressionTree bet, Predicate<ExpressionTree> sidePredicate) {
      return sidePredicate.test(bet.leftOperand()) || sidePredicate.test(bet.rightOperand());
    }
  }

  private static boolean hasMethodCallAsArg(ExpressionTree arg, MethodMatchers methodCallMatcher) {
    while (arg.is(Tree.Kind.PARENTHESIZED_EXPRESSION) || arg.is(Tree.Kind.MEMBER_SELECT)) {
      arg = ((ParenthesizedTree) arg).expression();
    }

    if (arg.is(Tree.Kind.METHOD_INVOCATION)) {
      return methodCallMatcher.matches((MethodInvocationTree) arg);
    }
    return false;
  }

  private static PredicateSimplifierWithContext compareToSimplifier(Predicate<ExpressionTree> predicateArgCondition, String simplification) {
    return PredicateSimplifierWithContext.withSingleArgument(predicateArgCondition,
      arg -> hasMethodCallAsArg(arg, ImplMatchers.COMPARE_TO), simplification);
  }

  private static PredicateSimplifierWithContext compareToSimplifier(String simplification) {
    return PredicateSimplifierWithContext.methodCallInSubject(ImplMatchers.COMPARE_TO, simplification);
  }

  private interface SimplifierWithoutContext {
    Optional<String> simplify(MethodInvocationTree predicate);
  }

  private interface SimplifierWithContext {
    Optional<String> simplify(MethodInvocationTree subject, MethodInvocationTree predicate);
  }

  private static class PredicateSimplifierWithoutContext implements SimplifierWithoutContext {
    private final Predicate<MethodInvocationTree> mitPredicate;
    private final String simplification;

    public PredicateSimplifierWithoutContext(
      Predicate<MethodInvocationTree> mitPredicate,
      String simplification) {

      this.mitPredicate = mitPredicate;
      this.simplification = simplification;
    }

    public static PredicateSimplifierWithoutContext withSingleArg(Predicate<ExpressionTree> argumentPredicate, String simplified) {
      return new PredicateSimplifierWithoutContext(mit -> {
        Arguments arguments = mit.arguments();
        return arguments.size() == 1 && argumentPredicate.test(arguments.get(0));
      }, simplified);
    }

    @Override
    public Optional<String> simplify(MethodInvocationTree predicate) {
      if (mitPredicate.test(predicate)) {
        return Optional.of(simplification);
      } else {
        return Optional.empty();
      }
    }
  }

  private static class PredicateSimplifierWithContext implements SimplifierWithContext {
    private final Predicate<MethodInvocationTree> predicateCondition;
    private final Predicate<MethodInvocationTree> subjectCondition;
    private final String simplification;

    public PredicateSimplifierWithContext(
      @Nullable Predicate<MethodInvocationTree> predicateCondition,
      Predicate<MethodInvocationTree> subjectCondition,
      String simplification) {
      if (predicateCondition != null) {
        this.predicateCondition = predicateCondition;
      } else {
        this.predicateCondition = x -> true;
      }
      this.subjectCondition = subjectCondition;
      this.simplification = simplification;
    }

    public static PredicateSimplifierWithContext withSubjectArgumentCondition(
      Predicate<ExpressionTree> subjectArgumentCondition, String simplification) {
      return new PredicateSimplifierWithContext(null, subjectMit -> subjectMit.arguments().size() == 1 && subjectArgumentCondition.test(unwrap(subjectMit.arguments().get(0))),
        simplification);
    }

    public static PredicateSimplifierWithContext methodCallInSubject(
      MethodMatchers methodCallMatcher,
      String simplification) {

      return withSubjectArgumentCondition(arg -> hasMethodCallAsArg(arg, methodCallMatcher), simplification);
    }

    public static PredicateSimplifierWithContext withSingleArgument(
      Predicate<ExpressionTree> predicateArgsCondition, Predicate<ExpressionTree> subjectArgsCondition,
      String simplification) {

      return new PredicateSimplifierWithContext(predicateMit -> predicateMit.arguments().size() == 1 && predicateArgsCondition.test(unwrap(predicateMit.arguments().get(0))),
        subjectMit -> subjectMit.arguments().size() == 1 && subjectArgsCondition.test(unwrap(subjectMit.arguments().get(0))),
        simplification);
    }

    @Override
    public Optional<String> simplify(MethodInvocationTree subject, MethodInvocationTree predicate) {
      if (predicateCondition.test(predicate) && subjectCondition.test(subject)) {
        return Optional.of(simplification);
      } else {
        return Optional.empty();
      }
    }

    private static ExpressionTree unwrap(ExpressionTree expression) {
      while (expression.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        expression = ((ParenthesizedTree) expression).expression();
      }
      return expression;
    }
  }

  private static class BooleanFlag {
    private boolean flag = false;

    public void setTrue() {
      flag = true;
    }

    public boolean value() {
      return flag;
    }
  }
}
