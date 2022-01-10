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
package org.sonar.java.checks.regex;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree;
import org.sonarsource.analyzer.commons.regex.ast.NonCapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5846")
public class EmptyLineRegexCheck extends AbstractRegexCheck {
  private static final String MESSAGE = "Remove MULTILINE mode or change the regex.";

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_PATTERN = "java.util.regex.Pattern";

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
  protected MethodMatchers getMethodInvocationMatchers() {
    // Only a few methods can contain problematic regex, we don't need to check all of them.
    return MethodMatchers.or(STRING_REPLACE, PATTERN_COMPILE);
  }

  @Override
  protected boolean filterAnnotation(AnnotationTree annotation) {
    return false;
  }

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    MethodInvocationTree mit = (MethodInvocationTree) methodInvocationOrAnnotation;
    EmptyLineMultilineVisitor visitor = new EmptyLineMultilineVisitor();
    visitor.visit(regexForLiterals);
    if (visitor.containEmptyLine) {
      if (PATTERN_COMPILE.matches(mit)) {
        List<Tree> stringNotTestedForEmpty = getStringNotTestedForEmpty(mit);
        if (!stringNotTestedForEmpty.isEmpty()) {
          reportWithSecondaries(mit.arguments().get(0), stringNotTestedForEmpty);
        }
      } else {
        // STRING_REPLACE case
        ExpressionTree methodSelect = mit.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)
          && canBeEmpty(((MemberSelectExpressionTree) methodSelect).expression())) {
          reportIssue(mit.arguments().get(0), MESSAGE);
        }
      }
    }
  }

  private static List<Tree> getStringNotTestedForEmpty(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    if (parent != null && parent.is(Tree.Kind.VARIABLE)) {
      // Pattern stored in a variable, check all usage for possibly empty string
      return ((VariableTree) parent).symbol().usages().stream()
        .map(EmptyLineRegexCheck::getStringInMatcherFind)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(EmptyLineRegexCheck::canBeEmpty)
        .collect(Collectors.toList());
    } else {
      // Pattern can be used directly
      return getStringInMatcherFind(mit)
        .filter(EmptyLineRegexCheck::canBeEmpty)
        .map(Collections::singletonList)
        .orElseGet(Collections::emptyList);
    }
  }

  private static Optional<Tree> getStringInMatcherFind(ExpressionTree mit) {
    return MethodTreeUtils.subsequentMethodInvocation(mit, PATTERN_MATCHER)
      .filter(matcherMit -> MethodTreeUtils.subsequentMethodInvocation(matcherMit, PATTERN_FIND).isPresent())
      .map(matcherMit -> matcherMit.arguments().get(0));
  }

  private static boolean canBeEmpty(Tree expressionTree) {
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      Symbol identifierSymbol = ((IdentifierTree) expressionTree).symbol();
      Symbol owner = identifierSymbol.owner();
      return owner != null && owner.isMethodSymbol() && identifierSymbol.usages().stream().noneMatch(EmptyLineRegexCheck::isIsEmpty);
    } else if (expressionTree.is(Tree.Kind.STRING_LITERAL, Tree.Kind.TEXT_BLOCK)) {
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

  private void reportWithSecondaries(Tree regex, List<Tree> secondaries) {
    List<JavaFileScannerContext.Location> secondariesLocation =
      secondaries.stream().map(secondary -> new JavaFileScannerContext.Location("This string can be empty.", secondary))
        .collect(Collectors.toList());
    reportIssue(regex, MESSAGE, secondariesLocation, null);
  }

  private static class EmptyLineMultilineVisitor extends RegexBaseVisitor {
    boolean visitedStart = false;
    boolean visitedEndAfterStart = false;
    boolean containEmptyLine = false;

    @Override
    public void visitSequence(SequenceTree tree) {
      List<RegexTree> items = tree.getItems().stream()
        .filter(item -> !isNonCapturingWithoutChild(item))
        .collect(Collectors.toList());

      if (items.size() == 1 && items.get(0).is(RegexTree.Kind.CAPTURING_GROUP)) {
        super.visitSequence(tree);
      } else if (items.size() == 2 && items.get(0).is(RegexTree.Kind.BOUNDARY) && items.get(1).is(RegexTree.Kind.BOUNDARY)) {
        super.visitSequence(tree);
        containEmptyLine |= visitedEndAfterStart;
      }
      visitedStart = false;
    }

    @Override
    public void visitBoundary(BoundaryTree boundaryTree) {
      if (boundaryTree.activeFlags().contains(Pattern.MULTILINE)) {
        if (boundaryTree.type().equals(BoundaryTree.Type.LINE_START)) {
          visitedStart = true;
        } else if (boundaryTree.type().equals(BoundaryTree.Type.LINE_END)) {
          visitedEndAfterStart = visitedStart;
        }
      }
    }

    private static boolean isNonCapturingWithoutChild(RegexTree tree) {
      return tree.is(RegexTree.Kind.NON_CAPTURING_GROUP) && ((NonCapturingGroupTree) tree).getElement() == null;
    }

  }

}
