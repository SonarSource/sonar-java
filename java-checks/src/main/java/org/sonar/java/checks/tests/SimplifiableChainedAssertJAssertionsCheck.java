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
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public class SimplifiableChainedAssertJAssertionsCheck extends AbstractMethodDetection {

  private static final String ABSTRACT_ASSERT = "org.assertj.core.api.AbstractAssert";
  private static final String ASSERTIONS = "org.assertj.core.api.Assertions";
  private static final List<String> ASSERTION_MESSAGE_NAMES = Arrays.asList("as", "describedAs", "withFailMessage", "overridingErrorMessage");

  private static final MethodMatchers ASSERTION_PREDICATES = MethodMatchers.create()
    .ofSubTypes(ABSTRACT_ASSERT).name(name -> !ASSERTION_MESSAGE_NAMES.contains(name)).withAnyParameters().build();

  // TODO: Support more assertion subject methods / from different classes
  private static final MethodMatchers ASSERTIONS_SUBJECT_METHODS = MethodMatchers.create()
    .ofTypes(ASSERTIONS).names("assertThat").withAnyParameters().build();

  /**
   * Stores multiple lists of simplifiers which are mapped to by a key. The key is the method name of the predicate
   * that this simplifier applies to. The simplifiers in this map are not provided with the subject argument.
   *
   * For instance, if you have a key {@code hasSize} that maps to a list containing
   * {@code PredicateSimplifierWithoutContext.withSingleArg(arg -> isZero(arg), "isEmpty()")} then it can be read as:
   * "<b>{@code hasSize}</b> with an argument that is <b>zero</b> can be simplified to <b>{@code isEmpty()}</b>".
   */
  private static final Map<String, List<SimplifierWithoutContext>> CONTEXT_FREE_SIMPLIFIERS = ImmutableMap.<String, List<SimplifierWithoutContext>>builder()
    .put("hasSize", ImmutableList.of(
      PredicateSimplifierWithoutContext.withSingleArg(SimplifiableChainedAssertJAssertionsCheck::isZero, "isEmpty()")))
    .build();

  /**
   * Stores multiple lists of simplifiers with context, similar to {@link #CONTEXT_FREE_SIMPLIFIERS}. The
   * simplifiers in this map, though, have access to the subject as well (i.e. the {@code assertThat(...)} method
   * and its argument).
   */
  private static final Map<String, List<SimplifierWithContext>> SIMPLIFIERS_WITH_CONTEXT = ImmutableMap.<String, List<SimplifierWithContext>>builder()
    .build();

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
      (predicate, replacement) -> reportIssue(predicate, String.format("Replace with %s", replacement)));

    // We do not continue when we have already raised an issue to avoid potentially conflicting issue reports. If we
    // have more than one predicate we also avoid continuing to avoid FP on cases such as:
    // assertThat(Integer.valueOf(1).compareTo(2)).isGreaterThan(1).isLessThan(10)
    if (wasIssueRaised || predicates.size() > 1) {
      return;
    }

    checkPredicatesForSimplification(predicates, SIMPLIFIERS_WITH_CONTEXT,
      (simplifier, predicate) -> simplifier.simplify(subjectMit, predicate),
      (predicate, replacement) -> reportIssue(predicate, String.format("Replace with %s", replacement),
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
      String predicateName = predicate.symbol().name();
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

  // TODO: move into a helper
  private static boolean isZero(ExpressionTree tree) {
    return tree.is(Tree.Kind.INT_LITERAL) &&
      "0".equals(((LiteralTree) tree).value());
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
