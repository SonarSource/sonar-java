/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.BackReferenceTree;
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree;
import org.sonarsource.analyzer.commons.regex.ast.CapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.GroupTree;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S5840")
public class ImpossibleRegexCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new ImpossiblePatternFinder().visit(regexForLiterals);
  }

  private static class ImpossibleSubPattern {

    RegexTree tree;

    String description;

    public ImpossibleSubPattern(RegexTree tree, String description) {
      this.tree = tree;
      this.description = description;
    }

  }

  private class ImpossiblePatternFinder extends RegexBaseVisitor {

    int groupCount = 0;
    boolean isAtBeginning = true;
    boolean isAtEnd = true;
    List<ImpossibleSubPattern> impossibleSubPatterns = new ArrayList<>();

    @Override
    public void visitCapturingGroup(CapturingGroupTree tree) {
      groupCount++;
      super.visitCapturingGroup(tree);
    }

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      if (tree.isNumerical() && tree.groupNumber() > groupCount) {
        impossibleSubPatterns.add(new ImpossibleSubPattern(tree, "illegal back reference"));
      }
      super.visitBackReference(tree);
    }

    @Override
    public void visitSequence(SequenceTree tree) {
      List<RegexTree> items = tree.getItems();
      if (items.isEmpty()) {
        return;
      }

      boolean wasAtEnd = isAtEnd;
      isAtEnd = false;
      int lastConsumingItemIndex = findLastConsumingIndex(items);
      for (int i = 0; i < items.size(); i++) {
        if (i >= lastConsumingItemIndex) {
          isAtEnd = wasAtEnd;
        }
        RegexTree item = items.get(i);
        visit(item);
        if (canConsumeInput(item)) {
          isAtBeginning = false;
        }
      }
    }

    @Override
    public void visitDisjunction(DisjunctionTree tree) {
      for (RegexTree alternative : tree.getAlternatives()) {
        restoreLocationAfter(() -> visit(alternative));
      }
    }

    @Override
    public void visitLookAround(LookAroundTree tree) {
      restoreLocationAfter(() -> super.visitLookAround(tree));
    }

    @Override
    public void visitBoundary(BoundaryTree tree) {
      switch (tree.type()) {
        case LINE_END:
        case INPUT_END:
        case INPUT_END_FINAL_TERMINATOR:
          if (!isAtEnd) {
            impossibleSubPatterns.add(new ImpossibleSubPattern(tree, "boundary"));
          }
          break;
        case LINE_START:
        case INPUT_START:
          if (!isAtBeginning) {
            impossibleSubPatterns.add(new ImpossibleSubPattern(tree, "boundary"));
          }
          break;
        default:
          // Do nothing
      }
    }


    @Override
    public void after(RegexParseResult result) {
      if (impossibleSubPatterns.size() == 1) {
        ImpossibleSubPattern pattern = impossibleSubPatterns.get(0);
        reportIssue(pattern.tree, "Remove this " + pattern.description + " that can never match or rewrite the regex.", null, Collections.emptyList());
      } else if (impossibleSubPatterns.size() > 1) {
        List<RegexIssueLocation> secondaries = impossibleSubPatterns.stream()
          .map(pattern -> new RegexIssueLocation(pattern.tree, pattern.description))
          .collect(Collectors.toList());
        reportIssue(impossibleSubPatterns.get(0).tree, "Remove these subpatterns that can never match or rewrite the regex.", null, secondaries);
      }
    }

    void restoreLocationAfter(Runnable action) {
      boolean wasAtEnd = isAtEnd;
      boolean wasAtBeginning = isAtBeginning;
      action.run();
      isAtEnd = wasAtEnd;
      isAtBeginning = wasAtBeginning;
    }

    int findLastConsumingIndex(List<RegexTree> items) {
      for (int i = items.size() - 1; i >= 0; i--) {
        if (canConsumeInput(items.get(i))) {
          return i;
        }
      }
      return -1;
    }

    boolean canConsumeInput(RegexTree tree) {
      if ((tree.is(RegexTree.Kind.SEQUENCE) && ((SequenceTree) tree).getItems().isEmpty())
        || tree.is(RegexTree.Kind.LOOK_AROUND, RegexTree.Kind.BOUNDARY)) {
        return false;
      }
      if (tree instanceof GroupTree) {
        RegexTree element = ((GroupTree) tree).getElement();
        return element != null && canConsumeInput(element);
      }
      return true;
    }

  }

}
