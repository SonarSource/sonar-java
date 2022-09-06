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
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
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
  private static List<JavaFileScannerContext.Location> secondaryLocation;

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
    secondaryLocation = new ArrayList<>();
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
      if (isDerivedFromPlainText(argument)) {
        if (secondaryLocation.isEmpty()) {
          reportIssue(argument, ISSUE_MESSAGE);
        } else {
          reportIssue(argument, ISSUE_MESSAGE, secondaryLocation, null);
        }
      }
    }
  }

  private static boolean isDerivedFromPlainText(ExpressionTree expression) {
    ExpressionTree arg = ExpressionUtils.skipParentheses(expression);
    switch (arg.kind()) {
      case IDENTIFIER:
        IdentifierTree identifier = (IdentifierTree) arg;
        return isDerivedFromPlainText(identifier);
      case NEW_ARRAY:
        NewArrayTree newArrayTree = (NewArrayTree) arg;
        return isDerivedFromPlainText(newArrayTree);
      case NEW_CLASS:
        NewClassTree newClassTree = (NewClassTree) arg;
        return isDerivedFromPlainText(newClassTree);
      case METHOD_INVOCATION:
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) arg;
        return isDerivedFromPlainText(methodInvocationTree);
      case CONDITIONAL_EXPRESSION:
        ConditionalExpressionTree conditionalTree = (ConditionalExpressionTree) arg;
        return isDerivedFromPlainText(conditionalTree);
      case MEMBER_SELECT:
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) arg;
        return isDerivedFromPlainText(memberSelect.identifier());
      case STRING_LITERAL:
        return !LiteralUtils.isEmptyString(arg);
      case BOOLEAN_LITERAL:
      case CHAR_LITERAL:
      case INT_LITERAL:
        return true;
      default:
        return false;
    }
  }

  private static boolean isDerivedFromPlainText(IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (!symbol.isVariableSymbol() || JUtils.isParameter(symbol) || isNonFinalField(symbol)) {
      return false;
    }
    VariableTree variable = (VariableTree) symbol.declaration();
    if (variable == null) {
      return JUtils.constantValue((Symbol.VariableSymbol) symbol).isPresent();
    }

    if (isStringDerivedFromPlainText(variable)) {
      secondaryLocation.add(new JavaFileScannerContext.Location("", variable));
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
        .allMatch(HardCodedCredentialsShouldNotBeUsedCheck::isDerivedFromPlainText);

    if (identifierIsDerivedFromPlainText) {
      secondaryLocation.add(new JavaFileScannerContext.Location("", variable));
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

  private static boolean isDerivedFromPlainText(NewArrayTree invocation) {
    ExpressionTree dimension = invocation.dimensions().get(0).expression();
    if (dimension != null && dimension.is(Tree.Kind.INT_LITERAL)) {
      Optional<Integer> sizeOfArray = dimension.asConstant(Integer.class);
      if (sizeOfArray.isPresent() && sizeOfArray.get() == 0) {
        return false;
      }
    }
    return invocation.initializers().stream()
      .map(ExpressionUtils::skipParentheses)
      .allMatch(HardCodedCredentialsShouldNotBeUsedCheck::isDerivedFromPlainText);
  }

  private static boolean isDerivedFromPlainText(NewClassTree invocation) {
    if (!SUPPORTED_CONSTRUCTORS.matches(invocation)) {
      return false;
    }
    return invocation.arguments().stream()
      .allMatch(HardCodedCredentialsShouldNotBeUsedCheck::isDerivedFromPlainText);
  }

  private static boolean isDerivedFromPlainText(MethodInvocationTree invocation) {
    if (!STRING_TO_ARRAY_METHODS.matches(invocation)) {
      return false;
    }
    StringConstantFinder visitor = new StringConstantFinder();
    invocation.accept(visitor);
    return visitor.finding != null;
  }

  private static class StringConstantFinder extends BaseTreeVisitor {
    Tree finding;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      ExpressionTree expressionTree = tree.methodSelect();
      if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        expressionTree.accept(this);
      }
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      ExpressionTree expression = ExpressionUtils.skipParentheses(tree.expression());
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) expression;
        Symbol symbol = identifier.symbol();
        if (symbol.isVariableSymbol()) {
          VariableTree variable = (VariableTree) symbol.declaration();
          if (variable.symbol().type().is(JAVA_LANG_STRING)) {
            ExpressionTree initializer = variable.initializer();
            if (initializer != null && initializer.asConstant().isPresent()) {
              finding = variable;
            }
          }
        }
      } else if (expression.is(Tree.Kind.STRING_LITERAL)) {
        finding = tree;
      }
    }
  }

  public static boolean isDerivedFromPlainText(ConditionalExpressionTree conditionalTree) {
    ExpressionTree trueExpression = ExpressionUtils.skipParentheses(conditionalTree.trueExpression());
    ExpressionTree falseExpression = ExpressionUtils.skipParentheses(conditionalTree.falseExpression());
    return isDerivedFromPlainText(trueExpression) && isDerivedFromPlainText(falseExpression);
  }
}
