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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.BackReferenceTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassIntersectionTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassTree;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.ast.NonCapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;
import org.sonarsource.analyzer.commons.regex.ast.RegexToken;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonarsource.analyzer.commons.regex.ast.SourceCharacter;

@Rule(key = "S5843")
public class RegexComplexityCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Simplify this regular expression to reduce its complexity from %d to the %d allowed.";

  private static final int DEFAULT_MAX = 20;

  @RuleProperty(
    key = "maxComplexity",
    description = "The maximum authorized complexity.",
    defaultValue = "" + DEFAULT_MAX)
  private int max = DEFAULT_MAX;

  private final List<RegexConstructionInfo> regexConstructions = new ArrayList<>();

  private final Set<Integer> commentedLines = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> nodes = new ArrayList<>(super.nodesToVisit());
    nodes.add(Tree.Kind.TRIVIA);
    return nodes;
  }

  @Override
  public void checkRegex(RegexParseResult parseResult, ExpressionTree methodInvocationOrAnnotation) {
    // The parse result is not used except to get the initial flags. We find and parse the parts of the regex
    // ourselves because we want to count the complexity of each part individually if the regex is made out of
    // parts stored in variables.
    ExpressionTree regexArgument = getRegexLiteralExpression(methodInvocationOrAnnotation);
    // regexArgument can not be null when "checkRegex" is called
    regexConstructions.add(new RegexConstructionInfo(regexArgument, parseResult.getInitialFlags(), parseResult.containsComments()));
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    commentedLines.add(syntaxTrivia.range().start().line());
    int numLines = StringUtils.countMatches(syntaxTrivia.comment(), "\n");
    if (numLines > 0) {
      commentedLines.add(syntaxTrivia.range().start().line() + numLines);
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    for (RegexConstructionInfo regexInfo : regexConstructions) {
      FlagSet flags = regexInfo.initialFlags;
      for (LiteralTree[] regexPart : findRegexParts(regexInfo)) {
        new ComplexityCalculator().visit(regexForLiterals(flags, regexPart));
      }
    }
    regexConstructions.clear();
    commentedLines.clear();
  }

  List<LiteralTree[]> findRegexParts(RegexConstructionInfo regexInfo) {
    RegexPartFinder finder = new RegexPartFinder(regexInfo.initialFlags, regexInfo.containsComments);
    finder.find(regexInfo.regexArgument);
    return finder.parts;
  }

  public void setMax(int max) {
    this.max = max;
  }

  private class RegexPartFinder {

    final FlagSet initialFlags;

    final boolean regexContainsComments;

    List<LiteralTree[]> parts = new ArrayList<>();

    RegexPartFinder(FlagSet initialFlags, boolean regexContainsComments) {
      this.initialFlags = initialFlags;
      this.regexContainsComments = regexContainsComments;
    }

    void find(ExpressionTree expr) {
      switch (expr.kind()) {
        case PLUS:
          List<LiteralTree> literals = new ArrayList<>();
          findInStringConcatenation(expr, literals);
          if (!literals.isEmpty()) {
            parts.add(literals.toArray(new LiteralTree[0]));
          }
          break;
        case IDENTIFIER:
          getFinalVariableInitializer((IdentifierTree) expr).ifPresent(this::find);
          break;
        case PARENTHESIZED_EXPRESSION:
          find(ExpressionUtils.skipParentheses(expr));
          break;
        case STRING_LITERAL:
          parts.add(new LiteralTree[] {(LiteralTree) expr});
          break;
        default:
          // Do nothing
      }
    }

    void findInStringConcatenation(ExpressionTree expr, List<LiteralTree> literals) {
      if (expr.is(Tree.Kind.STRING_LITERAL)) {
        LiteralTree literal = (LiteralTree) expr;
        if (isCommented(literal)) {
          parts.add(new LiteralTree[] {literal});
        } else {
          literals.add(literal);
        }
      } else if (expr.is(Tree.Kind.PLUS)) {
        BinaryExpressionTree binExpr = (BinaryExpressionTree) expr;
        findInStringConcatenation(binExpr.leftOperand(), literals);
        findInStringConcatenation(binExpr.rightOperand(), literals);
      } else if (expr.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        findInStringConcatenation(ExpressionUtils.skipParentheses(expr), literals);
      } else {
        find(expr);
      }
    }

    private boolean isCommented(LiteralTree regexPart) {
      int line = regexPart.token().range().start().line();
      return regexContainsComments
        || commentedLines.contains(line)
        || commentedLines.contains(line - 1);
    }

  }

  private class ComplexityCalculator extends RegexBaseVisitor {

    int complexity = 0;

    int nesting = 1;

    List<RegexIssueLocation> components = new ArrayList<>();

    private void increaseComplexity(RegexSyntaxElement syntaxElement, int increment) {
      complexity += increment;
      String message = "+" + increment;
      if (increment > 1) {
        message += " (incl " + (increment - 1) + " for nesting)";
      }
      components.add(new RegexIssueLocation(syntaxElement, message));
    }

    @Override
    public void visitDisjunction(DisjunctionTree tree) {
      increaseComplexity(tree.getOrOperators().get(0), nesting);
      for (SourceCharacter orOperator : tree.getOrOperators().subList(1, tree.getOrOperators().size())) {
        increaseComplexity(orOperator, 1);
      }
      nesting++;
      super.visitDisjunction(tree);
      nesting--;
    }

    @Override
    public void visitRepetition(RepetitionTree tree) {
      increaseComplexity(tree.getQuantifier(), nesting);
      nesting++;
      super.visitRepetition(tree);
      nesting--;
    }

    // Character classes increase the complexity by only one regardless of nesting because they're not that complex by
    // themselves
    @Override
    public void visitCharacterClass(CharacterClassTree tree) {
      increaseComplexity(tree.getOpeningBracket(), 1);
      nesting++;
      super.visitCharacterClass(tree);
      nesting--;
    }

    // Intersections in character classes are a different matter though
    @Override
    public void visitCharacterClassIntersection(CharacterClassIntersectionTree tree) {
      // Subtract one from nesting because we want to treat [a-z&&0-9] as nesting level 1 and [[a-z&&0-9]otherstuff] as
      // nesting level 2
      increaseComplexity(tree.getAndOperators().get(0), nesting - 1);
      for (RegexToken andOperator : tree.getAndOperators().subList(1, tree.getAndOperators().size())) {
        increaseComplexity(andOperator, 1);
      }
      nesting++;
      super.visitCharacterClassIntersection(tree);
      nesting--;
    }

    // Regular groups, names groups and non-capturing groups without flags don't increase complexity because they don't
    // do anything by themselves. However lookarounds, atomic groups and non-capturing groups with flags do because
    // they're more complicated features
    @Override
    public void visitNonCapturingGroup(NonCapturingGroupTree tree) {
      if (tree.getEnabledFlags().isEmpty() && tree.getDisabledFlags().isEmpty()) {
        super.visitNonCapturingGroup(tree);
      } else {
        if (tree.getGroupHeader() == null) {
          increaseComplexity(tree, nesting);
        } else {
          increaseComplexity(tree.getGroupHeader(), nesting);
        }
        nesting++;
        super.visitNonCapturingGroup(tree);
        nesting--;
      }
    }

    @Override
    public void visitLookAround(LookAroundTree tree) {
      increaseComplexity(tree.getGroupHeader(), nesting);
      nesting++;
      super.visitLookAround(tree);
      nesting--;
    }

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      increaseComplexity(tree, 1);
    }

    @Override
    protected void after(RegexParseResult regexParseResult) {
      if (complexity > max) {
        reportIssue(regexParseResult.openingQuote(), String.format(MESSAGE, complexity, max), complexity - max , components);
      }
    }
  }

  private static class RegexConstructionInfo {

    final ExpressionTree regexArgument;

    final FlagSet initialFlags;

    final boolean containsComments;

    RegexConstructionInfo(ExpressionTree regexArgument, FlagSet initialFlags, boolean containsComments) {
      this.regexArgument = regexArgument;
      this.initialFlags = initialFlags;
      this.containsComments = containsComments;
    }

  }

}
