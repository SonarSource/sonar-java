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
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.cfg.CFG;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S128")
public class SwitchCaseWithoutBreakCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    List<CaseGroupTree> caseGroupTrees = switchStatementTree.cases();
    CFG cfg = CFG.buildCFG(Collections.singletonList(tree), true);
    CFG.Block entry = cfg.entry();
    List<CFG.Block> cfgCases = new ArrayList<>(entry.successors());

    if (!hasDefaultClause(switchStatementTree)) {
      cfgCases.remove(cfgCases.size() - 1);
    }

    List<CFG.Block> cases = Lists.reverse(cfgCases);

    IntStream.range(1, cases.size())
      .filter(i -> cases.get(i).predecessors().stream().anyMatch(predecessor -> !tree.equals(predecessor.terminator())))
      .filter(i -> !intentionalFallThrough(caseGroupTrees.get(i - 1), caseGroupTrees.get(i)))
      .mapToObj(i -> caseGroupTrees.get(i - 1).labels())
      .map(caseGroupLabels -> caseGroupLabels.get(caseGroupLabels.size() - 1))
      .forEach(label -> reportIssue(label, "End this switch case with an unconditional break, return or throw statement."));
  }

  private static boolean hasDefaultClause(SwitchStatementTree switchStatement) {
    return switchStatement.cases().stream()
      .flatMap(caseGroupTree -> caseGroupTree.labels().stream())
      .anyMatch(caseLabelTree -> caseLabelTree.caseOrDefaultKeyword().text().equals("default"));
  }

  private static boolean intentionalFallThrough(CaseGroupTree caseGroup, CaseGroupTree nextCaseGroup) {
    // Check first token of next case group when comment is last element of case group it is attached to next group.
    FallThroughCommentVisitor visitor = new FallThroughCommentVisitor();
    List<Tree> treesToScan = ImmutableList.<Tree>builder().addAll(caseGroup.body()).add(nextCaseGroup.firstToken()).build();
    visitor.scan(treesToScan);
    return visitor.hasComment;
  }

  private static class FallThroughCommentVisitor extends SubscriptionVisitor {
    private static final Pattern FALL_THROUGH_PATTERN = Pattern.compile("falls?[\\-\\s]?thro?u[gh]?", Pattern.CASE_INSENSITIVE);
    boolean hasComment = false;

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      if (!hasComment && FALL_THROUGH_PATTERN.matcher(syntaxTrivia.comment()).find()) {
        hasComment = true;
      }
    }

    private void scan(List<Tree> trees) {
      for (Tree tree : trees) {
        if (hasComment) {
          return;
        }
        scanTree(tree);
      }
    }
  }
}
