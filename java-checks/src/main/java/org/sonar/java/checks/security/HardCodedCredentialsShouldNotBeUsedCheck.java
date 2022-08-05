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
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.CredentialsMethodsLoader;
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
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6437")
public class HardCodedCredentialsShouldNotBeUsedCheck extends IssuableSubscriptionVisitor {
  private static final Path CREDENTIALS_METHODS_FILE = Path.of("src", "main", "resources", "credentials-methods.json");
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
      .build()
  );

  private static final String ISSUE_MESSAGE = "Revoke and change this password, as it is compromised.";


  private static Map<String, List<CredentialsMethodsLoader.CredentialsMethod>> methodMatchers;

  public HardCodedCredentialsShouldNotBeUsedCheck() {
    loadSignatures();
  }

  private static synchronized void loadSignatures() {
    if (methodMatchers != null) {
      return;
    }
    try {
      methodMatchers = CredentialsMethodsLoader.load(CREDENTIALS_METHODS_FILE);
    } catch (IOException e) {
      LOG.warn(e.getMessage());
      methodMatchers = Collections.emptyMap();
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    String methodName = invocation.symbol().name();
    List<CredentialsMethodsLoader.CredentialsMethod> candidates = methodMatchers.get(methodName);
    if (candidates == null) {
      return;
    }
    for (CredentialsMethodsLoader.CredentialsMethod candidate : candidates) {
      MethodMatchers matcher = candidate.methodMatcher;
      if (matcher.matches(invocation)) {
        checkArguments(invocation, candidate.targetArguments);
      }
    }
  }

  private void checkArguments(MethodInvocationTree invocation, List<CredentialsMethodsLoader.TargetArgument> argumentsToExamine) {
    for (CredentialsMethodsLoader.TargetArgument argumentToExamine : argumentsToExamine) {
      int argumentIndex = argumentToExamine.index;
      Arguments arguments = invocation.arguments();
      if (arguments.size() <= argumentIndex) {
        return;
      }
      ExpressionTree argument = arguments.get(argumentIndex);
      if (argument.is(Tree.Kind.STRING_LITERAL, Tree.Kind.NEW_ARRAY)) {
        reportIssue(argument, ISSUE_MESSAGE);
      } else if (argument.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) argument;
        Optional<Object> identifierAsConstant = identifier.asConstant();
        if (identifierAsConstant.isPresent()) {
          reportIssue(argument, ISSUE_MESSAGE);
        }
        Symbol symbol = identifier.symbol();
        if (!symbol.isVariableSymbol() || JUtils.isParameter(symbol) || isReassigned(symbol)) {
          return;
        }

        VariableTree variable = (VariableTree) symbol.declaration();

        if (isStringDerivedFromPlainText(variable) || isArrayDerivedFromPlainText(variable)) {
          reportIssue(argument, ISSUE_MESSAGE, List.of(new JavaFileScannerContext.Location("", variable)), null);
        }
      } else if (argument.is(Tree.Kind.METHOD_INVOCATION) && isArrayDerivedFromPlainText((MethodInvocationTree) argument)) {
        reportIssue(argument, ISSUE_MESSAGE);
      }
    }
  }

  private static boolean isReassigned(Symbol symbol) {
    return !ReassignmentFinder.getReassignments(symbol.owner().declaration(), symbol.usages()).isEmpty();
  }

  private static boolean isStringDerivedFromPlainText(VariableTree variable) {
    Symbol symbol = variable.symbol();
    return symbol.type().is(JAVA_LANG_STRING) && variable.initializer().asConstant().isPresent();
  }

  private static boolean isArrayDerivedFromPlainText(VariableTree variable) {
    Symbol symbol = variable.symbol();
    org.sonar.plugins.java.api.semantic.Type type = symbol.type();
    if (!type.is("byte[]") && !type.is("char[]")) {
      return false;
    }
    ExpressionTree initializer = variable.initializer();
    if (!initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      return true;
    }
    MethodInvocationTree initializationCall = (MethodInvocationTree) initializer;
    return isArrayDerivedFromPlainText(initializationCall);
  }

  private static boolean isArrayDerivedFromPlainText(MethodInvocationTree invocation) {
    if (!STRING_TO_ARRAY_METHODS.matches(invocation)) {
      return true;
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
      ExpressionTree expression = tree.expression();
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) expression;
        Symbol symbol = identifier.symbol();
        if (symbol.isVariableSymbol()) {
          VariableTree variable = (VariableTree) symbol.declaration();
          if (variable.symbol().type().is(JAVA_LANG_STRING) && variable.initializer().asConstant().isPresent()) {
            finding = variable;
          }
        }
      } else if (expression.is(Tree.Kind.STRING_LITERAL)) {
        finding = tree;
      }
    }
  }
}
