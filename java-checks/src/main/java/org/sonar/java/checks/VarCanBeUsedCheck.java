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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6212")
public class VarCanBeUsedCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {
  
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.VARIABLE);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava10Compatible();
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    ExpressionTree initializer = variableTree.initializer();
    
    if (initializer == null ||
      variableTree.type().is(Tree.Kind.VAR_TYPE) ||
      !JUtils.isLocalVariable(variableTree.symbol()) ||
      isMultiAssignment(variableTree) ||
      initializer.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
      return;
    }

    Type type = variableTree.type().symbolType();
    Type initializerType = initializer.symbolType();
    
    if (type.fullyQualifiedName().equals(initializerType.fullyQualifiedName())) {
      reportIssue(variableTree.simpleName(), "");
    }
  }

  private static boolean isMultiAssignment(VariableTree variableTree) {
    return variableTree.parent().is(Tree.Kind.LIST) && ((ListTree<StatementTree>) variableTree.parent()).size() > 1;
  }
}
