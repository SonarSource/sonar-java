package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class ExpressionEvaluator {

  private Map<MethodMatchers, Integer> supportedConstructors = new HashMap<>();

  public ExpressionEvaluator(Map<MethodMatchers, Integer> constructors) {
    if (constructors != null) {
      supportedConstructors = constructors;
    }
  }

  public static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatchers STRING_TO_ARRAY_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("getBytes", "toLowerCase", "toUpperCase")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("toCharArray", "trim", "strip", "stripIndent", "stripLeading", "stripTrailing", "intern", "translateEscapes")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("subSequence", "substring")
      .addParametersMatcher("int")
      .addParametersMatcher("int", "int")
      .build(),
    MethodMatchers.create()
      .ofAnyType()
      .names("toString")
      .addWithoutParametersMatcher()
      .build());

  private static final MethodMatchers STRING_VALUE_OF = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("valueOf")
    .withAnyParameters()
    .build();

  private static final MethodMatchers STRING_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .constructor()
    .addParametersMatcher(parameters -> !parameters.isEmpty())
    .build();

  public boolean isExpressionDerivedFromPlainText(ExpressionTree expression, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    ExpressionTree arg = ExpressionUtils.skipParentheses(expression);
    switch (arg.kind()) {
      case IDENTIFIER:
        IdentifierTree identifier = (IdentifierTree) arg;
        return isDerivedFromPlainText(identifier, secondaryLocations, visited);
      case NEW_ARRAY:
        NewArrayTree newArrayTree = (NewArrayTree) arg;
        return isDerivedFromPlainText(newArrayTree, secondaryLocations, visited);
      case NEW_CLASS:
        NewClassTree newClassTree = (NewClassTree) arg;
        return isDerivedFromPlainText(newClassTree, secondaryLocations, visited);
      case METHOD_INVOCATION:
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) arg;
        return isDerivedFromPlainText(methodInvocationTree, secondaryLocations, visited);
      case CONDITIONAL_EXPRESSION: // needed?
        ConditionalExpressionTree conditionalTree = (ConditionalExpressionTree) arg;
        return isDerivedFromPlainText(conditionalTree, secondaryLocations, visited);
      case MEMBER_SELECT: // needed?
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) arg;
        return isDerivedFromPlainText(memberSelect.identifier(), secondaryLocations, visited);
      case STRING_LITERAL:
        return !LiteralUtils.isEmptyString(arg);
      case TYPE_CAST:
        TypeCastTree typeCast = (TypeCastTree) arg;
        return isExpressionDerivedFromPlainText(typeCast.expression(), secondaryLocations, visited);
      case BOOLEAN_LITERAL:
      case CHAR_LITERAL:
      case DOUBLE_LITERAL:
      case FLOAT_LITERAL:
      case INT_LITERAL:
      case LONG_LITERAL:
        return true;
      default:
        if (arg instanceof BinaryExpressionTree) {
          BinaryExpressionTree binaryExpression = (BinaryExpressionTree) arg;
          return isDerivedFromPlainText(binaryExpression, secondaryLocations, visited);
        }
        return false;
    }
  }

  private boolean isDerivedFromPlainText(BinaryExpressionTree binaryExpression, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    return isExpressionDerivedFromPlainText(binaryExpression.rightOperand(), secondaryLocations, visited) &&
      isExpressionDerivedFromPlainText(binaryExpression.leftOperand(), secondaryLocations, visited);
  }

  private boolean isDerivedFromPlainText(IdentifierTree identifier, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    Symbol symbol = identifier.symbol();
    boolean firstVisit = visited.add(symbol);
    if (!firstVisit || !symbol.isVariableSymbol() || JUtils.isParameter(symbol) || isNonFinalField(symbol)) {
      return false;
    }
    VariableTree variable = (VariableTree) symbol.declaration();
    if (variable == null) {
      return JUtils.constantValue((Symbol.VariableSymbol) symbol).isPresent();
    }

    ExpressionTree initializer = variable.initializer();
    List<ExpressionTree> assignments = new ArrayList<>();
    Optional.ofNullable(initializer).ifPresent(assignments::add);
    ReassignmentFinder.getReassignments(variable, symbol.usages()).stream()
      .map(AssignmentExpressionTree::expression)
      .forEach(assignments::add);

    boolean identifierIsDerivedFromPlainText = !assignments.isEmpty() &&
      assignments.stream()
        .allMatch(expression -> isExpressionDerivedFromPlainText(expression, secondaryLocations, visited));

    if (identifierIsDerivedFromPlainText) {
      secondaryLocations.add(new JavaFileScannerContext.Location("", variable));
      return true;
    }
    return false;
  }

  private boolean isNonFinalField(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isTypeSymbol() && !symbol.isFinal();
  }

  private boolean isDerivedFromPlainText(NewArrayTree invocation, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    ListTree<ExpressionTree> initializers = invocation.initializers();
    return !initializers.isEmpty() && initializers.stream()
      .allMatch(expression -> isExpressionDerivedFromPlainText(expression, secondaryLocations, visited));
  }

  private boolean isDerivedFromPlainText(NewClassTree invocation, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    for (Map.Entry<MethodMatchers, Integer> entry : supportedConstructors.entrySet()) {
      if (entry.getKey().matches(invocation)) {
        return isExpressionDerivedFromPlainText(
          invocation.arguments().get(entry.getValue()), secondaryLocations, visited);
      }
    }
    return false;
  }

  private boolean isDerivedFromPlainText(MethodInvocationTree invocation, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {

    if (STRING_VALUE_OF.matches(invocation)) {
      return isExpressionDerivedFromPlainText(invocation.arguments().get(0), secondaryLocations, visited);
    }

    if (!STRING_TO_ARRAY_METHODS.matches(invocation)) {
      return false;
    }
    ExpressionTree methodSelect = ExpressionUtils.skipParentheses(invocation.methodSelect());
    return methodSelect.is(Tree.Kind.MEMBER_SELECT) &&
      isExpressionDerivedFromPlainText(((MemberSelectExpressionTree) methodSelect).expression(), secondaryLocations, visited);
  }

  private boolean isDerivedFromPlainText(ConditionalExpressionTree conditionalTree, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    return isExpressionDerivedFromPlainText(conditionalTree.trueExpression(), secondaryLocations, visited) &&
      isExpressionDerivedFromPlainText(conditionalTree.falseExpression(), secondaryLocations, visited);
  }

}
