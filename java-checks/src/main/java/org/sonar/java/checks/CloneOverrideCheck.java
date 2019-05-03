/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2975")
public class CloneOverrideCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    IdentifierTree identifierTree = methodTree.simpleName();
    if (methodTree.parameters().isEmpty() && "clone".equals(identifierTree.name()) && !isUnsupportedCloneOverride(methodTree)) {
      reportIssue(identifierTree, "Remove this \"clone\" implementation; use a copy constructor or copy factory instead.");
    }
  }

  private static boolean isUnsupportedCloneOverride(MethodTree methodTree) {
    if (isOneStatementMethod(methodTree)) {
      StatementTree statementTree = methodTree.block().body().get(0);
      return statementTree.is(Tree.Kind.THROW_STATEMENT) && ((ThrowStatementTree) statementTree).expression().symbolType().is("java.lang.CloneNotSupportedException");
    }
    return false;
  }

  private static boolean isOneStatementMethod(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    return block != null && block.body().size() == 1;
  }




}
