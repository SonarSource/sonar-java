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
package org.sonar.java.checks.regex;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5846")
public class EmptyLineRegexCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "Remove MULTILINE mode or change the regex.";

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_PATTERN = "java.util.regex.Pattern";

  private static final String EMPTY_LINE_REGEX = "^$";
  private static final String EMPTY_LINE_MULTILINE_REGEX = "(?m)^$";

  private static final MethodMatchers STRING_REPLACE = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("replaceAll", "replaceFirst")
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
    .build();

  private static final MethodMatchers PATTERN_COMPILE = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_PATTERN)
    .names("compile")
    .addParametersMatcher(JAVA_LANG_STRING)
    .addParametersMatcher(JAVA_LANG_STRING, "int")
    .build();

  private static final MethodMatchers PATTERN_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_UTIL_PATTERN)
    .names("matcher")
    .addParametersMatcher("java.lang.CharSequence")
    .build();

  private static final MethodMatchers PATTERN_FIND = MethodMatchers.create()
    .ofTypes("java.util.regex.Matcher")
    .names("find")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers STRING_IS_EMPTY = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("isEmpty")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (PATTERN_COMPILE.matches(mit)) {
      checkPatternCompile(mit);
    } else if (STRING_REPLACE.matches(mit)) {
      checkStringReplace(mit);
    }
  }

  private void checkPatternCompile(MethodInvocationTree mit) {
    ExpressionTree firstArgument = mit.arguments().get(0);

    if (isEmptyLineMultilineRegex(firstArgument)) {
      reportIfUsedOnEmpty(mit, firstArgument);
    } else if (mit.arguments().size() == 2) {
      ExpressionTree secondArgument = mit.arguments().get(1);
      if (isEmptyLineRegex(firstArgument) && isMultilineFlag(secondArgument)) {
        reportIfUsedOnEmpty(mit, secondArgument);
      }
    }
  }

  private void checkStringReplace(MethodInvocationTree mit) {
    ExpressionTree firstArgument = mit.arguments().get(0);
    ExpressionTree methodSelect = mit.methodSelect();
    if (isEmptyLineMultilineRegex(firstArgument)
    && methodSelect.is(Tree.Kind.MEMBER_SELECT)
    && canBeEmpty(((MemberSelectExpressionTree) methodSelect).expression())) {
      reportIssue(firstArgument, MESSAGE);
    }
  }

  private static boolean isEmptyLineMultilineRegex(ExpressionTree regexArgument) {
    return regexArgument.asConstant(String.class).filter(EMPTY_LINE_MULTILINE_REGEX::equals).isPresent();
  }

  private static boolean isEmptyLineRegex(ExpressionTree regexArgument) {
    return regexArgument.asConstant(String.class).filter(EMPTY_LINE_REGEX::equals).isPresent();
  }

  private static boolean isMultilineFlag(ExpressionTree flag) {
    if (flag.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) flag;
      return memberSelect.expression().symbolType().isSubtypeOf(JAVA_UTIL_PATTERN)
        && "MULTILINE".equals(memberSelect.identifier().name());
    }
    return false;
  }

  private void reportIfUsedOnEmpty(MethodInvocationTree mit, Tree reportLocation) {
    Tree parent = mit.parent();
    if (parent != null && parent.is(Tree.Kind.VARIABLE)) {
      // Pattern stored in a variable, check all usage for possibly empty string
      List<Tree> stringNotTestedForEmpty = ((VariableTree) parent).symbol().usages().stream()
        .map(EmptyLineRegexCheck::getStringInMatcherFind)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(EmptyLineRegexCheck::canBeEmpty)
        .collect(Collectors.toList());

      if (!stringNotTestedForEmpty.isEmpty()) {
        reportWithSecondaries(reportLocation, stringNotTestedForEmpty);
      }
    } else {
      // Pattern can be used directly
      getStringInMatcherFind(mit)
        .filter(EmptyLineRegexCheck::canBeEmpty)
        .ifPresent(str -> reportWithSecondaries(reportLocation, Collections.singletonList(str)));
    }
  }

  private static Optional<ExpressionTree> getStringInMatcherFind(ExpressionTree mit) {
    return MethodTreeUtils.subsequentMethodInvocation(mit, PATTERN_MATCHER)
      .filter(matcherMit -> MethodTreeUtils.subsequentMethodInvocation(matcherMit, PATTERN_FIND).isPresent())
      .map(matcherMit -> matcherMit.arguments().get(0));
  }

  private static boolean canBeEmpty(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      Symbol identifierSymbol = ((IdentifierTree) expressionTree).symbol();
      Symbol owner = identifierSymbol.owner();
      return owner != null && owner.isMethodSymbol() && identifierSymbol.usages().stream().noneMatch(EmptyLineRegexCheck::isIsEmpty);
    } else if (expressionTree.is(Tree.Kind.STRING_LITERAL)) {
      return LiteralUtils.trimQuotes(((LiteralTree) expressionTree).value()).isEmpty();
    } else if (expressionTree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      return canBeEmpty(((ParenthesizedTree) expressionTree).expression());
    }
    // If not sure, consider it as not empty to avoid FP.
    return false;
  }

  private static boolean isIsEmpty(IdentifierTree id) {
    return MethodTreeUtils.subsequentMethodInvocation(id, STRING_IS_EMPTY).isPresent();
  }

  private void reportWithSecondaries(Tree reportLocation, List<Tree> secondaries) {
    reportIssue(reportLocation, MESSAGE,
      secondaries.stream().map(secondary -> new JavaFileScannerContext.Location("This string can be empty.", secondary)).collect(Collectors.toList()),
      null);
  }

}
