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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.DisjunctionTree;
import org.sonar.java.regex.ast.NonCapturingGroupTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5850")
public class AnchorPrecedenceCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    new Visitor().visit(regexForLiterals);
  }

  private enum Position {
    BEGINNING, END
  }

  private class Visitor extends RegexBaseVisitor {

    @Override
    public void visitDisjunction(DisjunctionTree tree) {
      List<RegexTree> alternatives = tree.getAlternatives();
      if (onlyAnchoredAt(alternatives, Position.BEGINNING) || onlyAnchoredAt(alternatives, Position.END)) {
        reportIssue(tree, "Group the alternatives together to get the intended precedence.", null, Collections.emptyList());
      }
      super.visitDisjunction(tree);
    }

    private boolean onlyAnchoredAt(List<RegexTree> alternatives, Position position) {
      int itemIndex = position == Position.BEGINNING ? 0 : (alternatives.size() - 1);
      int othersStartIndex = position == Position.BEGINNING ? 1 : 0;
      int othersEndOffset = position == Position.BEGINNING ? 0 : 1;
      RegexTree firstOrLast = alternatives.get(itemIndex);
      List<RegexTree> others = alternatives.subList(othersStartIndex, alternatives.size() - othersEndOffset);
      return isAnchored(firstOrLast, position) && others.stream().noneMatch(alt -> isAnchored(alt, position));
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
      return items.get(index).is(RegexTree.Kind.BOUNDARY);
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
