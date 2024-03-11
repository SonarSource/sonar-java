/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S6880")
public class PatternMatchUsingIfCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    var ifStat = (IfStatementTree) tree;
    if (isElseIf(ifStat)) {
      return;
    }
    if (ifShouldBePatternMatch(ifStat)) {
      reportIssue(ifStat.ifKeyword(), "Replace the chain of if/else with a switch expression.");
    }
  }

  private static boolean ifShouldBePatternMatch(IfStatementTree topLevelIf) {
    var leftInstanceofScrutinee = findLeftInstanceofScrutinee(topLevelIf.condition());
    if (leftInstanceofScrutinee.isEmpty()) {
      return false;
    }
    if (leftInstanceofScrutinee.get() instanceof IdentifierTree targetScrutineeVar) {
      for (var currStat = topLevelIf.elseStatement(); currStat instanceof IfStatementTree currIf; currStat = currIf.elseStatement()) {
        var currLeftScrutinee = findLeftInstanceofScrutinee(currIf.condition());
        if (!(currLeftScrutinee.isPresent() && currLeftScrutinee.get() instanceof IdentifierTree currLeftScrutineeVar
          && currLeftScrutineeVar.name().equals(targetScrutineeVar.name()))) {
          return false;
        }
      }
    }
    return true;
  }

  private static Optional<ExpressionTree> findLeftInstanceofScrutinee(ExpressionTree expr) {
    if (expr instanceof PatternInstanceOfTree patternInstanceOfTree) {
      return Optional.of(patternInstanceOfTree.expression());
    } else if (expr instanceof BinaryExpressionTree binary) {
      return findLeftInstanceofScrutinee(binary.leftOperand());
    } else {
      return Optional.empty();
    }
  }

  private static boolean isElseIf(IfStatementTree ifStat) {
    return ifStat.parent() instanceof IfStatementTree parentIf && parentIf.elseStatement() == ifStat;
  }

}
