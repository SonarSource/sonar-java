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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.NonCapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S5850")
public class AnchorPrecedenceCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new Visitor().visit(regexForLiterals);
  }

  private enum Position {
    BEGINNING, END
  }

  private class Visitor extends RegexBaseVisitor {

    @Override
    public void visitDisjunction(DisjunctionTree tree) {
      List<RegexTree> alternatives = tree.getAlternatives();
      if ((anchoredAt(alternatives, Position.BEGINNING) || anchoredAt(alternatives, Position.END))
        && notAnchoredElseWhere(alternatives)) {
        reportIssue(tree, "Group parts of the regex together to make the intended operator precedence explicit.", null, Collections.emptyList());
      }
      super.visitDisjunction(tree);
    }

    private boolean anchoredAt(List<RegexTree> alternatives, Position position) {
      int itemIndex = position == Position.BEGINNING ? 0 : (alternatives.size() - 1);
      RegexTree firstOrLast = alternatives.get(itemIndex);
      return isAnchored(firstOrLast, position);
    }

    private boolean notAnchoredElseWhere(List<RegexTree> alternatives) {
      if (isAnchored(alternatives.get(0), Position.END)
        || isAnchored(alternatives.get(alternatives.size() - 1), Position.BEGINNING)) {
        return false;
      }
      for (RegexTree alternative : alternatives.subList(1, alternatives.size() - 1)) {
        if (isAnchored(alternative, Position.BEGINNING) || isAnchored(alternative, Position.END)) {
          return false;
        }
      }
      return true;
    }

    private boolean isAnchored(RegexTree tree, Position position) {
      if (!tree.is(RegexTree.Kind.SEQUENCE)) {
        return false;
      }
      SequenceTree sequence = (SequenceTree) tree;
      List<RegexTree> items = sequence.getItems().stream()
        .filter(item -> !isFlagSetter(item))
        .collect(Collectors.toList());
      if (items.isEmpty()) {
        return false;
      }
      int index = position == Position.BEGINNING ? 0 : (items.size() - 1);
      RegexTree firstOrLast = items.get(index);
      return firstOrLast.is(RegexTree.Kind.BOUNDARY) && isAnchor((BoundaryTree) firstOrLast);
    }

    private boolean isAnchor(BoundaryTree tree) {
      switch (tree.type()) {
        case INPUT_START:
        case LINE_START:
        case INPUT_END:
        case INPUT_END_FINAL_TERMINATOR:
        case LINE_END:
          return true;
        default:
          return false;
      }
    }

    /**
     * Return whether the given regex is a non-capturing group without contents, i.e. one that only sets flags for the
     * rest of the expression
     */
    private boolean isFlagSetter(RegexTree tree) {
      return tree.is(RegexTree.Kind.NON_CAPTURING_GROUP) && ((NonCapturingGroupTree) tree).getElement() == null;
    }

  }

}
