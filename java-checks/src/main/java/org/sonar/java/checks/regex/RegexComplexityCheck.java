/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import org.apache.commons.lang3.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.finders.ComplexRegexFinder;

@Rule(key = "S5843")
public class RegexComplexityCheck extends AbstractRegexCheck {

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
    int startLine = LineUtils.startLine(syntaxTrivia);
    commentedLines.add(startLine);
    int numLines = StringUtils.countMatches(syntaxTrivia.comment(), "\n");
    if (numLines > 0) {
      commentedLines.add(startLine + numLines);
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    for (RegexConstructionInfo regexInfo : regexConstructions) {
      FlagSet flags = regexInfo.initialFlags;
      for (LiteralTree[] regexPart : findRegexParts(regexInfo)) {
        new ComplexRegexFinder(this::reportIssueFromCommons, max).visit(regexForLiterals(flags, regexPart));
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
      int line = LineUtils.startLine(regexPart);
      return regexContainsComments
        || commentedLines.contains(line)
        || commentedLines.contains(line - 1);
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
