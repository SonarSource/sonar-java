/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
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

  private static final String DEFAULT_CREDENTIAL_WORDS = "password,passwd,pwd";

  private static final MethodMatcher PASSWORD_AUTHENTICATION_CONSTRUCTOR = MethodMatcher.create()
    .typeDefinition("java.net.PasswordAuthentication")
    .name("<init>")
    .addParameter("java.lang.String")
    .addParameter("char[]");

  private static final MethodMatcher STRING_TO_CHAR_ARRAY = MethodMatcher.create()
    .typeDefinition("java.lang.String")
    .name("toCharArray")
    .withoutParameter();

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
    return ImmutableList.of(Tree.Kind.STRING_LITERAL, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      handleStringLiteral((LiteralTree) tree);
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      handleVariable((VariableTree) tree);
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      handleAssignement((AssignmentExpressionTree) tree);
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
      handleConstructor((NewClassTree) tree);
    } else {
      handleMethodInvocation((MethodInvocationTree) tree);
    }
  }

  private Optional<String> isSettingPassword(MethodInvocationTree tree) {
    List<ExpressionTree> arguments = tree.arguments();
    if (arguments.size() == 2 && argumentsAreLiterals(arguments)) {
      return isPassword((LiteralTree) arguments.get(0));
    }
    return Optional.empty();
  }

  private Optional<String> isPassword(LiteralTree argument) {
    if (!argument.is(Tree.Kind.STRING_LITERAL)) {
      return Optional.empty();
    }
    String cleanedLiteral = LiteralUtils.trimQuotes(argument.value());
    return variablePatterns()
      .map(pattern -> pattern.matcher(cleanedLiteral))
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
    return expr.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) expr).expression().is(Tree.Kind.STRING_LITERAL);
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
    if (isStringLiteral(tree.initializer())) {
      isPasswordVariableName(variable).ifPresent(passwordVariableName -> report(variable, passwordVariableName));
    }
  }

  private void handleAssignement(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    if (isStringLiteral(tree.expression())) {
      isPasswordVariable(variable).ifPresent(passwordVariableName -> report(variable, passwordVariableName));
    }
  }

  private static boolean argumentsAreLiterals(List<ExpressionTree> arguments) {
    return arguments.stream().allMatch(arg -> arg.is(
        Kind.INT_LITERAL,
        Kind.LONG_LITERAL,
        Kind.FLOAT_LITERAL,
        Kind.DOUBLE_LITERAL,
        Kind.BOOLEAN_LITERAL,
        Kind.CHAR_LITERAL,
        Kind.STRING_LITERAL,
        Kind.NULL_LITERAL));
  }

  private static boolean isStringLiteral(@Nullable ExpressionTree initializer) {
    return initializer != null && initializer.is(Tree.Kind.STRING_LITERAL);
  }

  private void handleConstructor(NewClassTree tree) {
    if (!PASSWORD_AUTHENTICATION_CONSTRUCTOR.matches(tree)) {
      return;
    }
    ExpressionTree secondArg = tree.arguments().get(1);
    if (secondArg.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) secondArg;
      if (isCallOnStringLiteral(mit.methodSelect()) && STRING_TO_CHAR_ARRAY.matches(mit)) {
        reportIssue(tree, "Remove this hard-coded password.");
      }
    }
  }

  private void handleMethodInvocation(MethodInvocationTree tree) {
    isSettingPassword(tree).ifPresent(settingPassword -> report(tree.methodSelect(), settingPassword));
  }

  private void report(Tree tree, String match) {
    reportIssue(tree, "'" + match + "' detected in this expression, review this potentially hardcoded credential.");
  }
}
