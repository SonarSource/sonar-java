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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.helpers.CredentialMethod;
import org.sonar.java.checks.helpers.CredentialMethodsLoader;
import org.sonar.java.checks.helpers.ReassignmentFinder;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6437")
public class HardCodedCredentialsShouldNotBeUsedCheck extends IssuableSubscriptionVisitor {
  public static final String CREDENTIALS_METHODS_FILE = "/org/sonar/java/checks/security/credentials-methods.json";

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
      LOG.warn(e.getMessage());
      methods = Collections.emptyMap();
    }
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
    for (int targetArgumentIndex : method.indices()) {
      ExpressionTree argument = arguments.get(targetArgumentIndex);
      if (argument.is(Tree.Kind.STRING_LITERAL, Tree.Kind.NEW_ARRAY)) {
        reportIssue(argument, ISSUE_MESSAGE);
      } else if (argument.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) argument;
        Symbol symbol = identifier.symbol();
        if (!symbol.isVariableSymbol() || JUtils.isParameter(symbol) || isNonFinalField(symbol) || isReassigned(symbol)) {
          return;
        }

        VariableTree variable = (VariableTree) symbol.declaration();

        if (variable != null && (isStringDerivedFromPlainText(variable) || isDerivedFromPlainText(variable))) {
          reportIssue(argument, ISSUE_MESSAGE, List.of(new JavaFileScannerContext.Location("", variable)), null);
        }
      } else if (argument.is(Tree.Kind.METHOD_INVOCATION) && isDerivedFromPlainText((MethodInvocationTree) argument)) {
        reportIssue(argument, ISSUE_MESSAGE);
      }
    }
  }

  private static boolean isNonFinalField(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isTypeSymbol() && !symbol.isFinal();
  }

  private static boolean isReassigned(Symbol symbol) {
    return !ReassignmentFinder.getReassignments(symbol.owner().declaration(), symbol.usages()).isEmpty();
  }

  private static boolean isStringDerivedFromPlainText(VariableTree variable) {
    Symbol symbol = variable.symbol();
    return symbol.type().is(JAVA_LANG_STRING) && variable.initializer().asConstant().isPresent();
  }

  private static boolean isDerivedFromPlainText(VariableTree variable) {
    ExpressionTree initializer = variable.initializer();
    if (!initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      return true;
    }
    MethodInvocationTree initializationCall = (MethodInvocationTree) initializer;
    return isDerivedFromPlainText(initializationCall);
  }

  private static boolean isDerivedFromPlainText(MethodInvocationTree invocation) {
    if (!STRING_TO_ARRAY_METHODS.matches(invocation)) {
      return false;
    }
    StringConstantFinder visitor = new StringConstantFinder();
    invocation.accept(visitor);
    return visitor.finding != null;
  }

  public Map<String, List<CredentialMethod>> getMethods() {
    return this.methods;
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
      ExpressionTree expression = tree.expression();
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
}
