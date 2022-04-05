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
package org.sonar.java.checks;

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
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class AbstractHardCodedCredentialChecker extends IssuableSubscriptionVisitor {

  protected static final Set<String> ALLOW_LIST = Collections.singleton("anonymous");
  protected static final String JAVA_LANG_STRING = "java.lang.String";
  protected static final String JAVA_LANG_OBJECT = "java.lang.Object";

  protected static final MethodMatchers STRING_TO_CHAR_ARRAY = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("toCharArray")
    .addWithoutParametersMatcher()
    .build();

  private List<Pattern> variablePatterns = null;
  private List<Pattern> literalPatterns = null;

  protected abstract int minCredentialLength();

  protected abstract String getCredentialWords();

  protected abstract void report(Tree tree, String match);

  protected boolean isPartOfConstantCredentialDeclaration(LiteralTree tree) {
    Tree parent = tree.parent();
    return parent != null && parent.is(Tree.Kind.VARIABLE) && isCredentialVariableName(((VariableTree) parent).simpleName()).isPresent();
  }

  protected Optional<String> isCredentialVariable(ExpressionTree variable) {
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return isCredentialVariableName(((MemberSelectExpressionTree) variable).identifier());
    } else if (variable.is(Tree.Kind.IDENTIFIER)) {
      return isCredentialVariableName((IdentifierTree) variable);
    }
    return Optional.empty();
  }

  protected Optional<String> isCredentialVariableName(IdentifierTree identifierTree) {
    return isCredentialLikeName(identifierTree.name());
  }

  protected boolean isPotentialCredential(@Nullable String literal) {
    if (literal == null) {
      return false;
    }
    String trimmed = literal.trim();
    return trimmed.length() >= minCredentialLength() && !ALLOW_LIST.contains(trimmed);
  }

  protected boolean isPotentialCredential(ExpressionTree expression) {
    return isPotentialCredential(ExpressionsHelper.getConstantValueAsString(expression).value());
  }

  protected boolean isExcludedLiteral(String cleanedLiteral, String match) {
    String followingString = cleanedLiteral.substring(cleanedLiteral.indexOf(match) + match.length() + 1);
    return !isPotentialCredential(followingString)
      || followingString.startsWith("?")
      || followingString.startsWith(":")
      || followingString.startsWith("\\\"")
      || followingString.contains("%s");
  }

  protected boolean isNotExcluded(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      return STRING_TO_CHAR_ARRAY.matches(mit) && isCallOnStringLiteral(mit.methodSelect());
    } else {
      return isPotentialCredential(expression);
    }
  }

  protected boolean isCallOnStringLiteral(ExpressionTree expr) {
    return expr.is(Tree.Kind.MEMBER_SELECT) &&
      isPotentialCredential(((MemberSelectExpressionTree) expr).expression());
  }

  protected List<Pattern> toPatterns(String suffix) {
    return Stream.of(getCredentialWords().split(","))
      .map(String::trim)
      .map(word -> Pattern.compile("(" + word + ")" + suffix, Pattern.CASE_INSENSITIVE))
      .collect(Collectors.toList());
  }

  protected Stream<Pattern> variablePatterns() {
    if (variablePatterns == null) {
      variablePatterns = toPatterns("");
    }
    return variablePatterns.stream();
  }

  protected Stream<Pattern> literalPatterns() {
    if (literalPatterns == null) {
      literalPatterns = toPatterns("=\\S.");
    }
    return literalPatterns.stream();
  }


  protected boolean isCredentialLikeName(ExpressionTree expression) {
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      return isCredentialLikeName(LiteralUtils.trimQuotes(((LiteralTree) expression).value())).isPresent();
    }
    return false;
  }

  protected Optional<String> isCredentialLikeName(String name) {
    return variablePatterns()
      .map(pattern -> pattern.matcher(name))
      // contains "pwd" or similar
      .filter(Matcher::find)
      .map(matcher -> matcher.group(1))
      .findAny();
  }

  protected Optional<String> isCredential(ExpressionTree argument) {
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

  protected static boolean isArgumentsSuperTypeOfString(List<ExpressionTree> arguments) {
    return arguments.stream().allMatch(arg -> arg.symbolType().is(JAVA_LANG_STRING) ||
      arg.symbolType().is(JAVA_LANG_OBJECT));
  }

  protected Optional<String> isSettingCredential(MethodInvocationTree tree) {
    List<ExpressionTree> arguments = tree.arguments();
    if (arguments.size() == 2 && isArgumentsSuperTypeOfString(arguments) && !isCredentialLikeName(arguments.get(1)) && isPotentialCredential(arguments.get(1))) {
      return isCredential(arguments.get(0));
    }
    return Optional.empty();
  }

  protected void handleAssignment(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    isCredentialVariable(variable)
      .filter(passwordVariableName -> isNotExcluded(tree.expression()))
      .ifPresent(passwordVariableName -> report(variable, passwordVariableName));
  }

}
