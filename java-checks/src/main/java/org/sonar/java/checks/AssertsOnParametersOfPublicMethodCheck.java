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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4274")
public class AssertsOnParametersOfPublicMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  private Set<AssertStatementTree> assertReported = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    assertReported.clear();
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (!methodTree.symbol().isPublic()) {
      return;
    }
    for (VariableTree parameter : methodTree.parameters()) {
      for (IdentifierTree paramUsage : parameter.symbol().usages()) {
        Tree paramParent = paramUsage.identifierToken().parent();
        while (!paramParent.equals(methodTree) && !assertReported.contains(paramParent)) {
          if (parentType(paramParent)) {
            break;
          }
          paramParent = paramParent.parent();
        }
      }
    }
  }

  private boolean parentType(Tree parameterParent) {
    if (parameterParent.is(Tree.Kind.ASSERT_STATEMENT)) {
      assertReported.add((AssertStatementTree) parameterParent);
      reportIssue(parameterParent, "Replace this assert with a proper check.");
      return true;
    } else {
      return parameterParent.is(Tree.Kind.METHOD_INVOCATION) ? true : false;
    }
  }
}
