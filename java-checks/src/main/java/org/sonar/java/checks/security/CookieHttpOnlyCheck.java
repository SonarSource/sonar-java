/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3330")
public class CookieHttpOnlyCheck extends IssuableSubscriptionVisitor {
  private final List<Symbol.VariableSymbol> compliantConstructorInitializations = new ArrayList<>();
  private final List<Symbol.VariableSymbol> ignoredVariables = new ArrayList<>();
  private final List<Symbol.VariableSymbol> variablesToReport = new ArrayList<>();
  private final List<MethodInvocationTree> settersToReport = new ArrayList<>();
  private final List<NewClassTree> newClassToReport = new ArrayList<>();

  private static final List<String> IGNORED_COOKIE_NAMES = ImmutableList.of("csrf", "xsrf");

  private static final String CONSTRUCTOR = "<init>";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_DATE = "java.util.Date";
  private static final String INT = "int";
  private static final String BOOLEAN = "boolean";

  private static final String MESSAGE = "Add the \"HttpOnly\" cookie attribute.";

  private static final int COOKIE_NAME_ARGUMENT = 0;

  private static final class ClassName {
    private static final String SERVLET_COOKIE = "javax.servlet.http.Cookie";
    private static final String NET_HTTP_COOKIE = "java.net.HttpCookie";
    private static final String JAX_RS_COOKIE = "javax.ws.rs.core.Cookie";
    private static final String JAX_RS_NEW_COOKIE = "javax.ws.rs.core.NewCookie";
    private static final String SHIRO_COOKIE = "org.apache.shiro.web.servlet.SimpleCookie";
    private static final String PLAY_COOKIE = "play.mvc.Http$Cookie";
    private static final String PLAY_COOKIE_BUILDER = "play.mvc.Http$CookieBuilder";
  }

  private static final List<String> SETTER_NAMES = Arrays.asList("setHttpOnly", "withHttpOnly");

  private static final List<String> CLASSES = Arrays.asList(
    ClassName.SERVLET_COOKIE,
    ClassName.NET_HTTP_COOKIE,
    ClassName.JAX_RS_COOKIE,
    ClassName.SHIRO_COOKIE,
    ClassName.PLAY_COOKIE,
    ClassName.PLAY_COOKIE_BUILDER);

  private static final MethodMatcher PLAY_COOKIE_BUILDER = MethodMatcher.create()
    .typeDefinition(ClassName.PLAY_COOKIE).name("builder").withAnyParameters();

  private static final List<MethodMatcher> CONSTRUCTORS_WITH_HTTP_ONLY_PARAM = Arrays.asList(
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(ClassName.JAX_RS_NEW_COOKIE)).name(CONSTRUCTOR)
      .parameters(ClassName.JAX_RS_COOKIE, JAVA_LANG_STRING, INT, JAVA_UTIL_DATE, BOOLEAN, BOOLEAN),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(ClassName.JAX_RS_NEW_COOKIE)).name(CONSTRUCTOR)
      .parameters(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, JAVA_LANG_STRING, INT, JAVA_UTIL_DATE, BOOLEAN, BOOLEAN),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(ClassName.JAX_RS_NEW_COOKIE)).name(CONSTRUCTOR)
      .parameters(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, BOOLEAN, BOOLEAN),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(ClassName.PLAY_COOKIE)).name(CONSTRUCTOR)
      .parameters(JAVA_LANG_STRING, JAVA_LANG_STRING, "java.lang.Integer", JAVA_LANG_STRING, JAVA_LANG_STRING, BOOLEAN, BOOLEAN));

  private static final List<MethodMatcher> CONSTRUCTORS_WITH_GOOD_DEFAULT = Arrays.asList(
    MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SHIRO_COOKIE)).name(CONSTRUCTOR).withoutParameter(),
    MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SHIRO_COOKIE)).name(CONSTRUCTOR).parameters(JAVA_LANG_STRING));

  @Override
  public void setContext(JavaFileScannerContext context) {
    compliantConstructorInitializations.clear();
    ignoredVariables.clear();
    variablesToReport.clear();
    settersToReport.clear();
    newClassToReport.clear();
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    for (VariableSymbol var : variablesToReport) {
      VariableTree declaration = var.declaration();
      if (declaration != null) {
        reportIssue(declaration.simpleName(), MESSAGE);
      }
    }
    for (MethodInvocationTree mit : settersToReport) {
      reportIssue(mit.arguments(), MESSAGE);
    }
    for (NewClassTree newClassTree : newClassToReport) {
      reportIssue(newClassTree, MESSAGE);
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.VARIABLE,
      Tree.Kind.ASSIGNMENT,
      Tree.Kind.METHOD_INVOCATION,
      Tree.Kind.RETURN_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        checkVariableDeclaration((VariableTree) tree);
      } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
        checkAssignment((AssignmentExpressionTree) tree);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        checkSetterInvocation((MethodInvocationTree) tree);
      } else {
        categorizeBasedOnConstructor((ReturnStatementTree) tree);
      }
    }
  }

  private void checkAssignment(AssignmentExpressionTree assignment) {
    checkCookieBuilder(assignment);
    if (shouldVerify(assignment)) {
      categorizeBasedOnConstructor((NewClassTree) assignment.expression(),
        (VariableSymbol) ((IdentifierTree) assignment.variable()).symbol());
    }
  }

  private void checkVariableDeclaration(VariableTree declaration) {
    checkCookieBuilder(declaration);
    if (shouldVerify(declaration)) {
      categorizeBasedOnConstructor((NewClassTree) declaration.initializer(),
        (VariableSymbol) declaration.symbol());
    }
  }

  private void checkCookieBuilder(AssignmentExpressionTree assignment) {
    if (assignment.expression().is(Tree.Kind.METHOD_INVOCATION)
      && (assignment.variable().is(Tree.Kind.IDENTIFIER) || assignment.variable().is(Tree.Kind.MEMBER_SELECT))) {
      MethodInvocationTree mit = (MethodInvocationTree) assignment.expression();
      VariableSymbol variableSymbol = getVariableSymbol(assignment);
      if (variableSymbol != null) {
        addToIgnoredVariables(variableSymbol, mit);
      }
    }
  }

  @CheckForNull
  private static VariableSymbol getVariableSymbol(AssignmentExpressionTree assignment) {
    VariableSymbol variableSymbol = null;
    if (assignment.variable().is(Tree.Kind.IDENTIFIER)) {
      Symbol reference = ((IdentifierTree) assignment.variable()).symbol();
      if (reference.isVariableSymbol()) {
        variableSymbol = (VariableSymbol) reference;
      }
    } else {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) assignment.variable();
      if (mse.identifier().symbol().isVariableSymbol()) {
        variableSymbol = (VariableSymbol) mse.identifier().symbol();
      }
    }
    return variableSymbol;
  }

  private void addToIgnoredVariables(VariableSymbol variableSymbol, MethodInvocationTree mit) {
    if (PLAY_COOKIE_BUILDER.matches(mit) && isIgnoredCookieName(mit.arguments())) {
      ignoredVariables.add(variableSymbol);
    }
  }

  private void checkCookieBuilder(VariableTree declaration) {
    if (declaration.initializer() != null && declaration.initializer().is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) declaration.initializer();
      addToIgnoredVariables((VariableSymbol) declaration.symbol(), mit);
    }
  }

  private void categorizeBasedOnConstructor(ReturnStatementTree returnStatement) {
    ExpressionTree returnedExpression = returnStatement.expression();
    if (returnedExpression != null && returnedExpression.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClass = (NewClassTree) returnedExpression;
      if (!isIgnoredCookieName(newClass.arguments()) && !isCompliantConstructorCall(newClass) && CLASSES.stream().anyMatch(newClass.symbolType()::isSubtypeOf)) {
        newClassToReport.add(newClass);
      }
    }
  }

  private void categorizeBasedOnConstructor(NewClassTree newClassTree, VariableSymbol variableSymbol) {
    if (isIgnoredCookieName(newClassTree.arguments())) {
      ignoredVariables.add(variableSymbol);
    } else if (isCompliantConstructorCall(newClassTree)) {
      compliantConstructorInitializations.add(variableSymbol);
    } else {
      variablesToReport.add(variableSymbol);
    }
  }

  private static boolean shouldVerify(VariableTree variableDeclaration) {
    ExpressionTree initializer = variableDeclaration.initializer();
    if (initializer != null && initializer.is(Tree.Kind.NEW_CLASS)) {
      boolean isSupportedClass = CLASSES.stream().anyMatch(variableDeclaration.type().symbolType()::isSubtypeOf)
        || CLASSES.stream().anyMatch(initializer.symbolType()::isSubtypeOf);
      return variableDeclaration.symbol().owner().isMethodSymbol() && isSupportedClass;
    }
    return false;
  }

  private static boolean shouldVerify(AssignmentExpressionTree assignment) {
    if (assignment.expression().is(Tree.Kind.NEW_CLASS) && assignment.variable().is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) assignment.variable();
      boolean isMethodVariable = identifier.symbol().isVariableSymbol()
        && identifier.symbol().owner().isMethodSymbol();
      boolean isSupportedClass = CLASSES.stream().anyMatch(identifier.symbolType()::isSubtypeOf)
        || CLASSES.stream().anyMatch(assignment.expression().symbolType()::isSubtypeOf);
      return isMethodVariable && isSupportedClass;
    }
    return false;
  }

  private static boolean isCompliantConstructorCall(NewClassTree newClassTree) {
    if (CONSTRUCTORS_WITH_HTTP_ONLY_PARAM.stream().anyMatch(matcher -> matcher.matches(newClassTree))) {
      Arguments arguments = newClassTree.arguments();
      ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
      return LiteralUtils.isTrue(lastArgument);
    } else {
      return CONSTRUCTORS_WITH_GOOD_DEFAULT.stream().anyMatch(matcher -> matcher.matches(newClassTree));
    }
  }

  private static boolean isIgnoredCookieName(Arguments arguments) {
    if (arguments.isEmpty()) {
      return false;
    }
    ExpressionTree nameArgument = arguments.get(COOKIE_NAME_ARGUMENT);
    String name = ExpressionsHelper.getConstantValueAsString(nameArgument).value();
    return name != null && IGNORED_COOKIE_NAMES.stream().anyMatch(cookieName -> name.toLowerCase(Locale.ENGLISH).contains(cookieName));
  }

  private void checkSetterInvocation(MethodInvocationTree mit) {
    if (isExpectedSetter(mit)) {
      if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree expression = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
        boolean isCalledOnIdentifier = expression.is(Tree.Kind.IDENTIFIER);
        boolean isCalledOnMemberSelect = expression.is(Tree.Kind.MEMBER_SELECT);
        if (isCalledOnIdentifier || isCalledOnMemberSelect) {
          updateIssuesToReport(mit);
        } else if (!setterArgumentHasCompliantValue(mit.arguments())) {
          // builder method
          settersToReport.add(mit);
        }
      } else if (!setterArgumentHasCompliantValue(mit.arguments())) {
        // sub-class method
        settersToReport.add(mit);
      }
    }
  }

  private static boolean isExpectedSetter(MethodInvocationTree mit) {
    return mit.arguments().size() == 1
      && mit.symbol().isMethodSymbol()
      && CLASSES.stream().anyMatch(mit.symbol().owner().type()::isSubtypeOf)
      && SETTER_NAMES.contains(getIdentifier(mit).name())
      && isIgnoredBuilder(mit);
  }

  private static boolean isIgnoredBuilder(MethodInvocationTree mit) {
    if (!mit.symbol().owner().type().isSubtypeOf(ClassName.PLAY_COOKIE_BUILDER)) {
      return true;
    }
    return getMethodChain(mit)
      .filter(method -> "builder".contains(getIdentifier(method).name()))
      .noneMatch(method -> isIgnoredCookieName(method.arguments()));
  }

  private static Stream<MethodInvocationTree> getMethodChain(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expressionTree = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
        return Stream.concat(Stream.of(mit), getMethodChain((MethodInvocationTree) expressionTree));
      }
    }
    return Stream.of(mit);
  }

  private void updateIssuesToReport(MethodInvocationTree mit) {
    MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
    VariableSymbol reference;
    if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
      reference = (VariableSymbol) ((IdentifierTree) mse.expression()).symbol();
    } else {
      reference = (VariableSymbol) ((MemberSelectExpressionTree) mse.expression()).identifier().symbol();
    }
    if (ignoredVariables.contains(reference)) {
      // ignore XSRF-TOKEN cookies
      return;
    }
    if (setterArgumentHasCompliantValue(mit.arguments())) {
      variablesToReport.remove(reference);
    } else if (compliantConstructorInitializations.contains(reference)) {
      variablesToReport.add(reference);
    } else if (!variablesToReport.contains(reference)) {
      settersToReport.add(mit);
    }
  }

  private static boolean setterArgumentHasCompliantValue(Arguments arguments) {
    ExpressionTree expressionTree = arguments.get(0);
    Boolean booleanValue = ExpressionsHelper.getConstantValueAsBoolean(expressionTree).value();
    return booleanValue == null || booleanValue;
  }

  private static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }
}
