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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1656")
public class SelfAssignementCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Remove or correct this useless self-assignment.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    AssignmentExpressionTree node = (AssignmentExpressionTree) tree;
    if (SyntacticEquivalence.areEquivalent(node.expression(), node.variable()) || ((JavaTree) tree).hasWarning(JWarning.Type.ASSIGNMENT_HAS_NO_EFFECT)) {
      SyntaxToken reportTree = node.operatorToken();
      reportIssue(reportTree, ISSUE_MESSAGE);
    }
  }
}
