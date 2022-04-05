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

  private static final Set<String> ALLOW_LIST = Collections.singleton("anonymous");
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

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
    return Stream.of(getCredentialWords().split(","))
      .map(String::trim)
      .map(word -> Pattern.compile("(" + word + ")" + suffix, Pattern.CASE_INSENSITIVE))
      .collect(Collectors.toList());
  }

  protected Optional<String> isSettingCredential(MethodInvocationTree tree) {
    List<ExpressionTree> arguments = tree.arguments();
    if (arguments.size() == 2 && isArgumentsSuperTypeOfString(arguments) && !isCredentialLikeName(arguments.get(1)) && isPotentialCredential(arguments.get(1))) {
      return isCredential(arguments.get(0));
    }
    return Optional.empty();
  }

  private Optional<String> isCredential(ExpressionTree argument) {
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

  private Optional<String> isCredentialVariableName(IdentifierTree identifierTree) {
    return isCredentialLikeName(identifierTree.name());
  }

  protected boolean isCredentialLikeName(ExpressionTree expression) {
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      return isCredentialLikeName(LiteralUtils.trimQuotes(((LiteralTree) expression).value())).isPresent();
    }
    return false;
  }

  private Optional<String> isCredentialLikeName(String name) {
    return variablePatterns()
      .map(pattern -> pattern.matcher(name))
      // contains "pwd" or similar
      .filter(Matcher::find)
      .map(matcher -> matcher.group(1))
      .findAny();
  }

  protected Optional<String> isCredentialVariable(ExpressionTree variable) {
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return isCredentialVariableName(((MemberSelectExpressionTree) variable).identifier());
    } else if (variable.is(Tree.Kind.IDENTIFIER)) {
      return isCredentialVariableName((IdentifierTree) variable);
    }
    return Optional.empty();
  }

  protected boolean isCallOnStringLiteral(ExpressionTree expr) {
    return expr.is(Tree.Kind.MEMBER_SELECT) &&
      isPotentialCredential(((MemberSelectExpressionTree) expr).expression());
  }

  protected void handleStringLiteral(LiteralTree tree) {
    String cleanedLiteral = LiteralUtils.trimQuotes(tree.value());
    if (!isPartOfConstantCredentialDeclaration(tree)) {
      literalPatterns().map(pattern -> pattern.matcher(cleanedLiteral))
        // contains "pwd=" or similar
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1))
        .filter(match -> !isExcludedLiteral(cleanedLiteral, match))
        .findAny()
        .ifPresent(credential -> report(tree, credential));
    }
  }

  private boolean isPartOfConstantCredentialDeclaration(LiteralTree tree) {
    Tree parent = tree.parent();
    return parent != null && parent.is(Tree.Kind.VARIABLE) && isCredentialVariableName(((VariableTree) parent).simpleName()).isPresent();
  }

  protected boolean isPotentialCredential(@Nullable String literal) {
    if (literal == null) {
      return false;
    }
    String trimmed = literal.trim();
    return trimmed.length() >= minCredentialLength() && !ALLOW_LIST.contains(trimmed);
  }

  private boolean isExcludedLiteral(String cleanedLiteral, String match) {
    String followingString = cleanedLiteral.substring(cleanedLiteral.indexOf(match) + match.length() + 1);
    return !isPotentialCredential(followingString)
      || followingString.startsWith("?")
      || followingString.startsWith(":")
      || followingString.startsWith("\\\"")
      || followingString.contains("%s");
  }

  protected void handleVariable(VariableTree tree) {
    IdentifierTree variable = tree.simpleName();
    isCredentialVariableName(variable)
      .filter(credentialVariableName -> {
        ExpressionTree initializer = tree.initializer();
        return initializer != null && isNotExcluded(initializer) && isNotCredentialConst(initializer);
      })
      .ifPresent(credentialVariableName -> report(variable, credentialVariableName));
  }

  protected void handleAssignment(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    isCredentialVariable(variable)
      .filter(credentialVariableName -> isNotExcluded(tree.expression()))
      .ifPresent(credentialVariableName -> report(variable, credentialVariableName));
  }

  private static boolean isArgumentsSuperTypeOfString(List<ExpressionTree> arguments) {
    return arguments.stream().allMatch(arg -> arg.symbolType().is(JAVA_LANG_STRING) ||
      arg.symbolType().is(JAVA_LANG_OBJECT));
  }

  private boolean isNotCredentialConst(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      ExpressionTree methodSelect = ((MethodInvocationTree) expression).methodSelect();
      return methodSelect.is(Tree.Kind.MEMBER_SELECT) && isNotCredentialConst(((MemberSelectExpressionTree) methodSelect).expression());
    }
    String literal = ExpressionsHelper.getConstantValueAsString(expression).value();
    return literal == null || variablePatterns().map(pattern -> pattern.matcher(literal))
      .noneMatch(Matcher::find);
  }

  private boolean isNotExcluded(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      return STRING_TO_CHAR_ARRAY.matches(mit) && isCallOnStringLiteral(mit.methodSelect());
    } else {
      return isPotentialCredential(expression);
    }
  }

  protected boolean isPotentialCredential(ExpressionTree expression) {
    return isPotentialCredential(ExpressionsHelper.getConstantValueAsString(expression).value());
  }

}
