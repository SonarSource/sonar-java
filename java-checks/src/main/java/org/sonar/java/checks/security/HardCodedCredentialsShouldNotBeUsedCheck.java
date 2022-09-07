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
package org.sonar.java.checks.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.helpers.CredentialMethod;
import org.sonar.java.checks.helpers.CredentialMethodsLoader;
import org.sonar.java.checks.helpers.ReassignmentFinder;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6437")
public class HardCodedCredentialsShouldNotBeUsedCheck extends IssuableSubscriptionVisitor {
  public static final String CREDENTIALS_METHODS_FILE = "/org/sonar/java/checks/security/S6437-methods.json";

  private static final Logger LOG = Loggers.get(HardCodedCredentialsShouldNotBeUsedCheck.class);

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatchers STRING_TO_ARRAY_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("getBytes")
      .addWithoutParametersMatcher()
      .addParametersMatcher("java.nio.charset.Charset")
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("toCharArray")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("subSequence")
      .addParametersMatcher("int", "int")
      .build()
  );

  private static final MethodMatchers SUPPORTED_CONSTRUCTORS = MethodMatchers.create()
    .ofTypes(
      JAVA_LANG_STRING,
      "java.lang.StringBuffer",
      "java.lang.StringBuilder",
      "java.nio.CharBuffer"
    ).constructor()
    .addParametersMatcher(parameters -> !parameters.isEmpty())
    .build();
  private static final String ISSUE_MESSAGE = "Revoke and change this password, as it is compromised.";


  private Map<String, List<CredentialMethod>> methods;

  public HardCodedCredentialsShouldNotBeUsedCheck() {
    this(CREDENTIALS_METHODS_FILE);
  }

  @VisibleForTesting
  HardCodedCredentialsShouldNotBeUsedCheck(String resourcePath) {
    try {
      methods = CredentialMethodsLoader.load(resourcePath);
    } catch (IOException e) {
      LOG.error(e.getMessage());
      methods = Collections.emptyMap();
    }
  }

  public Map<String, List<CredentialMethod>> getMethods() {
    return this.methods;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    String methodName;
    boolean isConstructor = tree.is(Tree.Kind.NEW_CLASS);
    if (isConstructor) {
      NewClassTree newClass = (NewClassTree) tree;
      methodName = newClass.symbolType().name();
    } else {
      MethodInvocationTree invocation = (MethodInvocationTree) tree;
      methodName = invocation.symbol().name();
    }
    List<CredentialMethod> candidates = methods.get(methodName);
    if (candidates == null) {
      return;
    }
    for (CredentialMethod candidate : candidates) {
      MethodMatchers matcher = candidate.methodMatcher();
      if (isConstructor) {
        NewClassTree constructor = (NewClassTree) tree;
        if (matcher.matches(constructor)) {
          checkArguments(constructor.arguments(), candidate);
        }
      } else {
        MethodInvocationTree invocation = (MethodInvocationTree) tree;
        if (matcher.matches(invocation)) {
          checkArguments(invocation.arguments(), candidate);
        }
      }
    }
  }

  private void checkArguments(Arguments arguments, CredentialMethod method) {
    for (int targetArgumentIndex : method.indices) {
      ExpressionTree argument = ExpressionUtils.skipParentheses(arguments.get(targetArgumentIndex));
      var secondaryLocations = new ArrayList<JavaFileScannerContext.Location>();
      if (isExpressionDerivedFromPlainText(argument, secondaryLocations)) {
        if (secondaryLocations.isEmpty()) {
          reportIssue(argument, ISSUE_MESSAGE);
        } else {
          reportIssue(argument, ISSUE_MESSAGE, secondaryLocations, null);
        }
      }
    }
  }

  private static boolean isExpressionDerivedFromPlainText(ExpressionTree expression, List<JavaFileScannerContext.Location> secondaryLocations) {
    ExpressionTree arg = ExpressionUtils.skipParentheses(expression);
    switch (arg.kind()) {
      case IDENTIFIER:
        IdentifierTree identifier = (IdentifierTree) arg;
        return isDerivedFromPlainText(identifier, secondaryLocations);
      case NEW_ARRAY:
        NewArrayTree newArrayTree = (NewArrayTree) arg;
        return isDerivedFromPlainText(newArrayTree, secondaryLocations);
      case NEW_CLASS:
        NewClassTree newClassTree = (NewClassTree) arg;
        return isDerivedFromPlainText(newClassTree, secondaryLocations);
      case METHOD_INVOCATION:
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) arg;
        return isDerivedFromPlainText(methodInvocationTree, secondaryLocations);
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree conditionalTree = (ConditionalExpressionTree) arg;
        return isDerivedFromPlainText(conditionalTree, secondaryLocations);
      case MEMBER_SELECT:
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) arg;
        return isDerivedFromPlainText(memberSelect.identifier(), secondaryLocations);
      case STRING_LITERAL:
        return !LiteralUtils.isEmptyString(arg);
      case TYPE_CAST:
        TypeCastTree typeCast = (TypeCastTree) arg;
        return isExpressionDerivedFromPlainText(typeCast.expression(), secondaryLocations);
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
          return isDerivedFromPlainText(binaryExpression, secondaryLocations);
        }
        return false;
    }
  }

  private static boolean isDerivedFromPlainText(BinaryExpressionTree binaryExpression, List<JavaFileScannerContext.Location> secondaryLocations) {
    return isExpressionDerivedFromPlainText(binaryExpression.rightOperand(), secondaryLocations) &&
      isExpressionDerivedFromPlainText(binaryExpression.leftOperand(), secondaryLocations);
  }

  private static boolean isDerivedFromPlainText(IdentifierTree identifier, List<JavaFileScannerContext.Location> secondaryLocations) {
    Symbol symbol = identifier.symbol();
    if (!symbol.isVariableSymbol() || JUtils.isParameter(symbol) || isNonFinalField(symbol)) {
      return false;
    }
    VariableTree variable = (VariableTree) symbol.declaration();
    if (variable == null) {
      return JUtils.constantValue((Symbol.VariableSymbol) symbol).isPresent();
    }

    if (isStringDerivedFromPlainText(variable)) {
      secondaryLocations.add(new JavaFileScannerContext.Location("", variable));
      return true;
    }

    ExpressionTree initializer = variable.initializer();

    List<ExpressionTree> assignments = new ArrayList<>();
    Optional.ofNullable(initializer).ifPresent(assignments::add);
    ReassignmentFinder.getReassignments(variable, symbol.usages()).stream()
      .map(AssignmentExpressionTree::expression)
      .forEach(assignments::add);

    boolean identifierIsDerivedFromPlainText = !assignments.isEmpty() &&
      assignments.stream()
        .allMatch(expression -> isExpressionDerivedFromPlainText(expression, secondaryLocations));

    if (identifierIsDerivedFromPlainText) {
      secondaryLocations.add(new JavaFileScannerContext.Location("", variable));
      return true;
    }
    return false;
  }

  private static boolean isNonFinalField(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isTypeSymbol() && !symbol.isFinal();
  }

  private static boolean isStringDerivedFromPlainText(VariableTree variable) {
    Symbol symbol = variable.symbol();
    return symbol.type().is(JAVA_LANG_STRING) &&
      variable.initializer().asConstant(String.class)
        .map(value -> !value.isEmpty()).orElse(false);
  }

  private static boolean isDerivedFromPlainText(NewArrayTree invocation, List<JavaFileScannerContext.Location> secondaryLocations) {
    return !invocation.initializers().isEmpty() && invocation.initializers().stream()
      .map(ExpressionUtils::skipParentheses)
      .allMatch(expression -> isExpressionDerivedFromPlainText(expression, secondaryLocations));
  }

  private static boolean isDerivedFromPlainText(NewClassTree invocation, List<JavaFileScannerContext.Location> secondaryLocations) {
    if (!SUPPORTED_CONSTRUCTORS.matches(invocation)) {
      return false;
    }
    return invocation.arguments().stream()
      .map(ExpressionUtils::skipParentheses)
      .allMatch(expression -> isExpressionDerivedFromPlainText(expression, secondaryLocations));
  }

  private static boolean isDerivedFromPlainText(MethodInvocationTree invocation, List<JavaFileScannerContext.Location> secondaryLocations) {
    if (!STRING_TO_ARRAY_METHODS.matches(invocation)) {
      return false;
    }
    ExpressionTree methodSelect = ExpressionUtils.skipParentheses(invocation.methodSelect());
    return methodSelect.is(Tree.Kind.MEMBER_SELECT) &&
      isExpressionDerivedFromPlainText(((MemberSelectExpressionTree) methodSelect).expression(), secondaryLocations);
  }

  private static boolean isDerivedFromPlainText(ConditionalExpressionTree conditionalTree, List<JavaFileScannerContext.Location> secondaryLocations) {
    ExpressionTree trueExpression = ExpressionUtils.skipParentheses(conditionalTree.trueExpression());
    ExpressionTree falseExpression = ExpressionUtils.skipParentheses(conditionalTree.falseExpression());
    return isExpressionDerivedFromPlainText(trueExpression, secondaryLocations) &&
      isExpressionDerivedFromPlainText(falseExpression, secondaryLocations);
  }
}
