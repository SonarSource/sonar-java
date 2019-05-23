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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2068")
public class HardCodedCredentialsCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_CREDENTIAL_WORDS = "password,passwd,pwd,passphrase,java.naming.security.credentials";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  private static final MethodMatcher PASSWORD_AUTHENTICATION_CONSTRUCTOR = MethodMatcher.create()
    .typeDefinition("java.net.PasswordAuthentication")
    .name("<init>")
    .addParameter(JAVA_LANG_STRING)
    .addParameter("char[]");

  private static final MethodMatcher STRING_TO_CHAR_ARRAY = MethodMatcher.create()
    .typeDefinition(JAVA_LANG_STRING)
    .name("toCharArray")
    .withoutParameter();

  private static final MethodMatcher EQUALS_MATCHER = MethodMatcher.create()
    .name("equals")
    .parameters(JAVA_LANG_OBJECT);

  private static final MethodMatcher GET_CONNECTION_MATCHER = MethodMatcher.create()
    .typeDefinition("java.sql.DriverManager")
    .name("getConnection").withAnyParameters();

  private static final int GET_CONNECTION_PASSWORD_ARGUMENT = 2;

  @RuleProperty(
    key = "credentialWords",
    description = "Comma separated list of words identifying potential credentials",
    defaultValue = DEFAULT_CREDENTIAL_WORDS)
  public String credentialWords = DEFAULT_CREDENTIAL_WORDS;

  private List<Pattern> variablePatterns = null;
  private List<Pattern> literalPatterns = null;

  private Stream<Pattern> variablePatterns() {
    if (variablePatterns == null) {
      variablePatterns = toPatterns("");
    }
    return variablePatterns.stream();
  }

  private Stream<Pattern> literalPatterns() {
    if (literalPatterns == null) {
      literalPatterns = toPatterns("=\\S.");
    }
    return literalPatterns.stream();
  }

  private List<Pattern> toPatterns(String suffix) {
    return Stream.of(credentialWords.split(","))
      .map(String::trim)
      .map(word -> Pattern.compile("(" + word + ")" + suffix, Pattern.CASE_INSENSITIVE))
      .collect(Collectors.toList());
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.STRING_LITERAL, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      handleStringLiteral((LiteralTree) tree);
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      handleVariable((VariableTree) tree);
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      handleAssignment((AssignmentExpressionTree) tree);
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
      handleConstructor((NewClassTree) tree);
    } else {
      handleMethodInvocation((MethodInvocationTree) tree);
    }
  }

  private Optional<String> isSettingPassword(MethodInvocationTree tree) {
    List<ExpressionTree> arguments = tree.arguments();
    if (arguments.size() == 2 && isArgumentsSuperTypeOfString(arguments) && isNotEmptyString(arguments.get(1))) {
      return isPassword(arguments.get(0));
    }
    return Optional.empty();
  }

  private Optional<String> isPassword(ExpressionTree argument) {
    String value = ExpressionsHelper.getConstantValueAsString(argument).value();
    if (StringUtils.isEmpty(value)) {
      return Optional.empty();
    }
    return variablePatterns()
      .map(pattern -> pattern.matcher(value))
      // should exactly match "pwd" or similar
      .filter(Matcher::matches)
      .map(matcher -> matcher.group(1))
      .findAny();
  }

  private Optional<String> isPasswordVariableName(IdentifierTree identifierTree) {
    String identifierName = identifierTree.name();
    return variablePatterns()
      .map(pattern -> pattern.matcher(identifierName))
      // contains "pwd" or similar
      .filter(Matcher::find)
      .map(matcher -> matcher.group(1))
      .findAny();
  }

  private Optional<String> isPasswordVariable(ExpressionTree variable) {
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return isPasswordVariableName(((MemberSelectExpressionTree) variable).identifier());
    } else if (variable.is(Tree.Kind.IDENTIFIER)) {
      return isPasswordVariableName((IdentifierTree) variable);
    }
    return Optional.empty();
  }

  private static boolean isCallOnStringLiteral(ExpressionTree expr) {
    return expr.is(Tree.Kind.MEMBER_SELECT) &&
      isNotEmptyString(((MemberSelectExpressionTree) expr).expression());
  }

  private void handleStringLiteral(LiteralTree tree) {
    String cleanedLiteral = LiteralUtils.trimQuotes(tree.value());
    literalPatterns().map(pattern -> pattern.matcher(cleanedLiteral))
      // contains "pwd=" or similar
      .filter(Matcher::find)
      .map(matcher -> matcher.group(1))
      .findAny()
      .ifPresent(credential -> report(tree, credential));
  }

  private void handleVariable(VariableTree tree) {
    IdentifierTree variable = tree.simpleName();
    isPasswordVariableName(variable)
      .filter(passwordVariableName -> isNotEmptyStringOrCharArrayFromString(tree.initializer()))
      .ifPresent(passwordVariableName -> report(variable, passwordVariableName));
  }

  private void handleAssignment(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    isPasswordVariable(variable)
      .filter(passwordVariableName -> isNotEmptyStringOrCharArrayFromString(tree.expression()))
      .ifPresent(passwordVariableName -> report(variable, passwordVariableName));
  }

  private static boolean isArgumentsSuperTypeOfString(List<ExpressionTree> arguments) {
    return arguments.stream().allMatch(arg -> arg.symbolType().is(JAVA_LANG_STRING) ||
      arg.symbolType().is(JAVA_LANG_OBJECT));
  }

  private static boolean isNotEmptyStringOrCharArrayFromString(@Nullable ExpressionTree expression) {
    if (expression != null && expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      return STRING_TO_CHAR_ARRAY.matches(mit) && isCallOnStringLiteral(mit.methodSelect());
    } else {
      return isNotEmptyString(expression);
    }
  }

  private static boolean isNotEmptyString(@Nullable ExpressionTree expression) {
    if (expression == null) {
      return false;
    }
    String literal = ExpressionsHelper.getConstantValueAsString(expression).value();
    return literal != null && !literal.trim().isEmpty();
  }

  private void handleConstructor(NewClassTree tree) {
    if (!PASSWORD_AUTHENTICATION_CONSTRUCTOR.matches(tree)) {
      return;
    }
    ExpressionTree secondArg = tree.arguments().get(1);
    if (secondArg.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) secondArg;
      if (STRING_TO_CHAR_ARRAY.matches(mit) && isCallOnStringLiteral(mit.methodSelect())) {
        reportIssue(tree, "Remove this hard-coded password.");
      }
    }
  }

  private void handleMethodInvocation(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (EQUALS_MATCHER.matches(mit) && methodSelect.is(Kind.MEMBER_SELECT)) {
      handleEqualsMethod(mit, (MemberSelectExpressionTree) methodSelect);
    } else if (GET_CONNECTION_MATCHER.matches(mit)) {
      handleGetConnectionMethod(mit);
    } else {
      isSettingPassword(mit).ifPresent(settingPassword -> report(methodSelect, settingPassword));
    }
  }

  private void handleEqualsMethod(MethodInvocationTree mit, MemberSelectExpressionTree methodSelect) {
    ExpressionTree leftExpression = methodSelect.expression();
    ExpressionTree rightExpression = mit.arguments().get(0);

    isPasswordVariable(leftExpression)
      .filter(passwordVariableName -> isNotEmptyString(rightExpression))
      .ifPresent(passwordVariableName -> report(leftExpression, passwordVariableName));

    isPasswordVariable(rightExpression)
      .filter(passwordVariableName -> isNotEmptyString(leftExpression))
      .ifPresent(passwordVariableName -> report(rightExpression, passwordVariableName));
  }

  private void handleGetConnectionMethod(MethodInvocationTree mit) {
    if (mit.arguments().size() > GET_CONNECTION_PASSWORD_ARGUMENT) {
      ExpressionTree expression = mit.arguments().get(GET_CONNECTION_PASSWORD_ARGUMENT);
      if (isNotEmptyString(expression)) {
        reportIssue(expression, "Remove this hard-coded password.");
      }
    }
  }

  private void report(Tree tree, String match) {
    reportIssue(tree, "'" + match + "' detected in this expression, review this potentially hard-coded credential.");
  }
}
