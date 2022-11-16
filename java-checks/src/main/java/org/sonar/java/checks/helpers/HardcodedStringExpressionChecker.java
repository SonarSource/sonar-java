/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.helpers;

import java.util.List;
import java.util.Set;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
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

/**
 * This class is used to determine if an expression evaluates to a static string.
 * It recursively checks for the origin of the expression that it is currently evaluating.
 * When creating an instance of the class, we can specify constructors that we want to visit during the recursive steps.
 */
public class HardcodedStringExpressionChecker {

  private HardcodedStringExpressionChecker() {
  }

  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final MethodMatchers SUPPORTED_CONSTRUCTORS = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .constructor()
    .addParametersMatcher(parameters -> !parameters.isEmpty())
    .build();

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

  public static boolean isExpressionDerivedFromPlainText(ExpressionTree expression, List<JavaFileScannerContext.Location> secondaryLocations,
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

  private static boolean isDerivedFromPlainText(BinaryExpressionTree binaryExpression, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    return isExpressionDerivedFromPlainText(binaryExpression.rightOperand(), secondaryLocations, visited) &&
      isExpressionDerivedFromPlainText(binaryExpression.leftOperand(), secondaryLocations, visited);
  }

  private static boolean isDerivedFromPlainText(IdentifierTree identifier, List<JavaFileScannerContext.Location> secondaryLocations,
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

    List<ExpressionTree> assignments = ExpressionsHelper.getIdentifierAssignments(identifier);

    boolean identifierIsDerivedFromPlainText = !assignments.isEmpty() &&
      assignments.stream()
        .allMatch(expression -> isExpressionDerivedFromPlainText(expression, secondaryLocations, visited));

    if (identifierIsDerivedFromPlainText) {
      secondaryLocations.add(new JavaFileScannerContext.Location("", variable));
      return true;
    }
    return false;
  }

  private static boolean isNonFinalField(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isTypeSymbol() && !symbol.isFinal();
  }

  private static boolean isDerivedFromPlainText(NewArrayTree invocation, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    ListTree<ExpressionTree> initializers = invocation.initializers();
    return !initializers.isEmpty() && initializers.stream()
      .allMatch(expression -> isExpressionDerivedFromPlainText(expression, secondaryLocations, visited));
  }

  /**
   * When a constructor is found during the evaluation, we check if it matches with one of the specified constructors
   * we passed to the Evaluator. If it does match, we will recursively evaluate the parameter targeted by the associated
   * index in the supportedConstructors map.
   */
  private static boolean isDerivedFromPlainText(NewClassTree invocation, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    return SUPPORTED_CONSTRUCTORS.matches(invocation) &&
      isExpressionDerivedFromPlainText(invocation.arguments().get(0), secondaryLocations, visited);
  }

  private static boolean isDerivedFromPlainText(MethodInvocationTree invocation, List<JavaFileScannerContext.Location> secondaryLocations,
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

  private static boolean isDerivedFromPlainText(ConditionalExpressionTree conditionalTree, List<JavaFileScannerContext.Location> secondaryLocations,
    Set<Symbol> visited) {
    return isExpressionDerivedFromPlainText(conditionalTree.trueExpression(), secondaryLocations, visited) &&
      isExpressionDerivedFromPlainText(conditionalTree.falseExpression(), secondaryLocations, visited);
  }

}
