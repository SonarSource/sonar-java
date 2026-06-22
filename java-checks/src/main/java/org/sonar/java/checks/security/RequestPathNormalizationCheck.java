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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8899")
public class RequestPathNormalizationCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Normalize this path before using it in a security check.";

  private static final MethodMatchers PATH_RETRIEVAL_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("javax.servlet.http.HttpServletRequest", "jakarta.servlet.http.HttpServletRequest")
      .names("getRequestURI", "getPathInfo", "getServletPath")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofTypes("javax.ws.rs.core.UriInfo", "jakarta.ws.rs.core.UriInfo")
      .names("getPath")
      .addWithoutParametersMatcher()
      .build()
  );

  private static final MethodMatchers NORMALIZATION_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.lang.String")
      .names("replaceAll")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.net.URI")
      .names("normalize")
      .addWithoutParametersMatcher()
      .build()
  );

  private static final MethodMatchers SECURITY_CHECK_METHODS = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("startsWith", "equals", "matches", "equalsIgnoreCase", "regionMatches")
    .withAnyParameters()
    .build();

  private static final MethodMatchers OBJECTS_EQUALS = MethodMatchers.create()
    .ofTypes("java.util.Objects")
    .names("equals")
    .addParametersMatcher("java.lang.Object", "java.lang.Object")
    .build();

  private static final MethodMatchers PATTERN_MATCHES = MethodMatchers.create()
    .ofTypes("java.util.regex.Pattern")
    .names("matches")
    .withAnyParameters()
    .build();

  private final Map<Symbol, PathState> pathVariables = new HashMap<>();

  private enum PathState {
    UNNORMALIZED,
    NORMALIZED
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    pathVariables.clear();
    super.setContext(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.VARIABLE,
      Tree.Kind.ASSIGNMENT,
      Tree.Kind.METHOD_INVOCATION
    );
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      handleVariableDeclaration((VariableTree) tree);
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      handleAssignment((AssignmentExpressionTree) tree);
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      handleMethodInvocation((MethodInvocationTree) tree);
    }
  }

  private void handleVariableDeclaration(VariableTree declaration) {
    ExpressionTree initializer = declaration.initializer();
    if (initializer != null && initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) initializer;
      Symbol symbol = declaration.symbol();
      updatePathStateFromMethodInvocation(symbol, mit);
    }
  }

  private void handleAssignment(AssignmentExpressionTree assignment) {
    if (assignment.expression().is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) assignment.expression();
      Symbol symbol = getAssignedSymbol(assignment);

      if (symbol != null) {
        updatePathStateFromMethodInvocation(symbol, mit);
      }
    }
  }

  private void handleMethodInvocation(MethodInvocationTree mit) {
    if (!isInSecurityContext(mit)) {
      return;
    }

    checkSecurityCheckMethods(mit);
    checkObjectsEquals(mit);
    checkPatternMatches(mit);
  }

  private void checkSecurityCheckMethods(MethodInvocationTree mit) {
    if (!SECURITY_CHECK_METHODS.matches(mit)) {
      return;
    }

    if (checkReceiverForUnnormalizedPath(mit)) {
      return;
    }
    checkArgumentsForUnnormalizedPath(mit);
  }

  private boolean checkReceiverForUnnormalizedPath(MethodInvocationTree mit) {
    ExpressionTree receiver = getReceiver(mit);
    if (receiver != null && isUnnormalizedPath(receiver)) {
      reportIssue(receiver, MESSAGE);
      return true;
    }
    return false;
  }

  private void checkArgumentsForUnnormalizedPath(MethodInvocationTree mit) {
    for (ExpressionTree arg : mit.arguments()) {
      if (isUnnormalizedPath(arg)) {
        reportIssue(arg, MESSAGE);
      }
    }
  }

  private void checkObjectsEquals(MethodInvocationTree mit) {
    if (!OBJECTS_EQUALS.matches(mit)) {
      return;
    }

    List<ExpressionTree> arguments = mit.arguments();
    if (arguments.size() != 2) {
      return;
    }

    for (ExpressionTree arg : arguments) {
      if (isUnnormalizedPath(arg)) {
        reportIssue(arg, MESSAGE);
      }
    }
  }

  private void checkPatternMatches(MethodInvocationTree mit) {
    if (!PATTERN_MATCHES.matches(mit)) {
      return;
    }

    List<ExpressionTree> arguments = mit.arguments();
    if (arguments.size() < 2) {
      return;
    }

    ExpressionTree pathArg = arguments.get(1);
    if (isUnnormalizedPath(pathArg)) {
      reportIssue(pathArg, MESSAGE);
    }
  }

  private void updatePathStateFromMethodInvocation(Symbol symbol, MethodInvocationTree mit) {
    if (PATH_RETRIEVAL_METHODS.matches(mit)) {
      pathVariables.put(symbol, PathState.UNNORMALIZED);
    } else if (NORMALIZATION_METHODS.matches(mit) && isNormalizationCall(mit)) {
      ExpressionTree receiver = getReceiver(mit);
      if (receiver != null && isUnnormalizedPath(receiver)) {
        pathVariables.put(symbol, PathState.NORMALIZED);
      }
    }
  }

  private boolean isUnnormalizedPath(ExpressionTree expression) {
    expression = ExpressionUtils.skipParentheses(expression);

    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      return isUnnormalizedMethodInvocation((MethodInvocationTree) expression);
    }

    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      PathState state = pathVariables.get(symbol);
      return state == PathState.UNNORMALIZED;
    }

    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expression;
      Symbol symbol = mse.identifier().symbol();
      PathState state = pathVariables.get(symbol);
      return state == PathState.UNNORMALIZED;
    }

    return false;
  }

  private static boolean isUnnormalizedMethodInvocation(MethodInvocationTree mit) {
    return PATH_RETRIEVAL_METHODS.matches(mit);
  }

  private static boolean isNormalizationCall(MethodInvocationTree mit) {
    // URI.normalize() is always a normalization
    if (mit.methodSymbol().owner().type().is("java.net.URI")) {
      return true;
    }

    // For String.replaceAll, check if the pattern looks like path normalization
    List<ExpressionTree> arguments = mit.arguments();
    if (!arguments.isEmpty()) {
      ExpressionTree firstArg = ExpressionUtils.skipParentheses(arguments.get(0));
      if (firstArg.is(Tree.Kind.STRING_LITERAL)) {
        String literal = firstArg.asConstant(String.class).orElse("");
        // Match patterns like "/+", "//+", "/{2,}", etc.
        return literal.contains("/+") || literal.contains("//+") || literal.contains("/{2,}");
      }
    }
    return false;
  }

  private static boolean isInSecurityContext(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    while (parent != null) {
      if (parent.is(Tree.Kind.IF_STATEMENT, Tree.Kind.CONDITIONAL_EXPRESSION,
                     Tree.Kind.RETURN_STATEMENT, Tree.Kind.THROW_STATEMENT,
                     Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR)) {
        return true;
      }
      if (parent.is(Tree.Kind.METHOD, Tree.Kind.CLASS)) {
        break;
      }
      parent = parent.parent();
    }
    return false;
  }

  @CheckForNull
  private static ExpressionTree getReceiver(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) methodSelect).expression();
    }
    return null;
  }

  @CheckForNull
  private static Symbol getAssignedSymbol(AssignmentExpressionTree assignment) {
    ExpressionTree variable = assignment.variable();
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) variable).symbol();
    } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) variable).identifier().symbol();
    }
    return null;
  }
}
