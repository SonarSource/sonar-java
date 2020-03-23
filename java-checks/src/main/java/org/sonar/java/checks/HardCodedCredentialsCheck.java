/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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
  private static final Set<String> WHITE_LIST = Collections.singleton("anonymous");
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final Pattern URL_PREFIX = Pattern.compile("^\\w{1,8}://");
  private static final Pattern NON_EMPTY_URL_CREDENTIAL = Pattern.compile("(?<user>[^\\s:]*+):(?<password>\\S++)");

  private static final int MINIMUM_PASSWORD_LENGTH = 1;

  private static final MethodMatchers PASSWORD_AUTHENTICATION_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("java.net.PasswordAuthentication")
    .constructor()
    .addParametersMatcher(JAVA_LANG_STRING, "char[]")
    .build();

  private static final MethodMatchers STRING_TO_CHAR_ARRAY = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("toCharArray")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers EQUALS_MATCHER = MethodMatchers.create()
    .ofAnyType()
    .names("equals")
    .addParametersMatcher(JAVA_LANG_OBJECT)
    .build();

  private static final MethodMatchers GET_CONNECTION_MATCHER = MethodMatchers.create()
    .ofTypes("java.sql.DriverManager")
    .names("getConnection")
    .withAnyParameters()
    .build();

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
    if (arguments.size() == 2 && isArgumentsSuperTypeOfString(arguments) && !isPasswordLikeName(arguments.get(1)) && isNotExcludedString(arguments.get(1))) {
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
    return isPasswordLikeName(identifierTree.name());
  }

  private boolean isPasswordLikeName(ExpressionTree expression) {
    if (expression.is(Kind.STRING_LITERAL)) {
      return isPasswordLikeName(LiteralUtils.trimQuotes(((LiteralTree) expression).value())).isPresent();
    }
    return false;
  }

  private Optional<String> isPasswordLikeName(String name) {
    return variablePatterns()
      .map(pattern -> pattern.matcher(name))
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
      isNotExcludedString(((MemberSelectExpressionTree) expr).expression());
  }

  private void handleStringLiteral(LiteralTree tree) {
    String cleanedLiteral = LiteralUtils.trimQuotes(tree.value());
    if (isURLWithCredentials(cleanedLiteral)) {
      reportIssue(tree, "Review this hard-coded URL, which may contain a credential.");
    } else if (!isPartOfConstantPasswordDeclaration(tree)) {
      literalPatterns().map(pattern -> pattern.matcher(cleanedLiteral))
        // contains "pwd=" or similar
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1))
        .filter(match -> !isExcludedLiteral(cleanedLiteral, match))
        .findAny()
        .ifPresent(credential -> report(tree, credential));
    }
  }

  private static boolean isURLWithCredentials(String stringLiteral) {
    if (URL_PREFIX.matcher(stringLiteral).find()) {
      try {
        String userInfo = new URL(stringLiteral).getUserInfo();
        if (userInfo != null) {
          Matcher matcher = NON_EMPTY_URL_CREDENTIAL.matcher(userInfo);
          return matcher.matches() && !matcher.group("user").equals(matcher.group("password"));
        }
      } catch (MalformedURLException e) {
        // ignore, stringLiteral is not a valid URL
      }
    }
    return false;
  }

  private boolean isPartOfConstantPasswordDeclaration(LiteralTree tree) {
    Tree parent = tree.parent();
    return parent != null && parent.is(Kind.VARIABLE) && isPasswordVariableName(((VariableTree) parent).simpleName()).isPresent();
  }

  private static boolean isExcludedLiteral(String cleanedLiteral, String match) {
    String followingString = cleanedLiteral.substring(cleanedLiteral.indexOf(match) + match.length() + 1);
    return !isNotExcludedString(followingString)
      || followingString.startsWith("?")
      || followingString.startsWith(":")
      || followingString.startsWith("\\\"")
      || followingString.contains("%s");
  }

  private void handleVariable(VariableTree tree) {
    IdentifierTree variable = tree.simpleName();
    isPasswordVariableName(variable)
      .filter(passwordVariableName -> {
        ExpressionTree initializer = tree.initializer();
        return initializer != null && isNotExcluded(initializer) && isNotPasswordConst(initializer);
      })
      .ifPresent(passwordVariableName -> report(variable, passwordVariableName));
  }

  private void handleAssignment(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    isPasswordVariable(variable)
      .filter(passwordVariableName -> isNotExcluded(tree.expression()))
      .ifPresent(passwordVariableName -> report(variable, passwordVariableName));
  }

  private static boolean isArgumentsSuperTypeOfString(List<ExpressionTree> arguments) {
    return arguments.stream().allMatch(arg -> arg.symbolType().is(JAVA_LANG_STRING) ||
      arg.symbolType().is(JAVA_LANG_OBJECT));
  }

  private boolean isNotPasswordConst(ExpressionTree expression) {
    if (expression.is(Kind.METHOD_INVOCATION)) {
      ExpressionTree methodSelect = ((MethodInvocationTree) expression).methodSelect();
      return methodSelect.is(Kind.MEMBER_SELECT) && isNotPasswordConst(((MemberSelectExpressionTree) methodSelect).expression());
    }
    String literal = ExpressionsHelper.getConstantValueAsString(expression).value();
    return literal == null || variablePatterns().map(pattern -> pattern.matcher(literal))
      .noneMatch(Matcher::find);
  }

  private static boolean isNotExcluded(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      return STRING_TO_CHAR_ARRAY.matches(mit) && isCallOnStringLiteral(mit.methodSelect());
    } else {
      return isNotExcludedString(expression);
    }
  }

  private static boolean isNotExcludedString(ExpressionTree expression) {
    return isNotExcludedString(ExpressionsHelper.getConstantValueAsString(expression).value());
  }

  private static boolean isNotExcludedString(@Nullable String literal) {
    return literal != null &&
      !literal.trim().isEmpty() &&
      literal.length() > MINIMUM_PASSWORD_LENGTH &&
      !WHITE_LIST.contains(literal);
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
      isSettingPassword(mit).ifPresent(settingPassword -> report(ExpressionUtils.methodName(mit), settingPassword));
    }
  }

  private void handleEqualsMethod(MethodInvocationTree mit, MemberSelectExpressionTree methodSelect) {
    ExpressionTree leftExpression = methodSelect.expression();
    ExpressionTree rightExpression = mit.arguments().get(0);

    isPasswordVariable(leftExpression)
      .filter(passwordVariableName -> isNotExcludedString(rightExpression) && !isPasswordLikeName(rightExpression))
      .ifPresent(passwordVariableName -> report(leftExpression, passwordVariableName));

    isPasswordVariable(rightExpression)
      .filter(passwordVariableName -> isNotExcludedString(leftExpression) && !isPasswordLikeName(leftExpression))
      .ifPresent(passwordVariableName -> report(rightExpression, passwordVariableName));
  }

  private void handleGetConnectionMethod(MethodInvocationTree mit) {
    if (mit.arguments().size() > GET_CONNECTION_PASSWORD_ARGUMENT) {
      ExpressionTree expression = mit.arguments().get(GET_CONNECTION_PASSWORD_ARGUMENT);
      if (isNotExcludedString(expression)) {
        reportIssue(expression, "Remove this hard-coded password.");
      }
    }
  }

  private void report(Tree tree, String match) {
    reportIssue(tree, "'" + match + "' detected in this expression, review this potentially hard-coded credential.");
  }
}
