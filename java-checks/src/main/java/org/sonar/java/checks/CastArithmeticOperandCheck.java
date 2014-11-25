/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;
import java.util.Map;

@Rule(
    key = "S2184",
    priority = Priority.MAJOR,
    tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CastArithmeticOperandCheck extends SubscriptionBaseVisitor {


  private static final Map<Tree.Kind, String> OPERATION_BY_KIND = ImmutableMap.<Tree.Kind, String>builder()
      .put(Tree.Kind.MULTIPLY, "multiplication")
      .put(Tree.Kind.DIVIDE, "division")
      .build();


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.ASSIGNMENT, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    Type varType;
    ExpressionTree expr;
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree aet = (AssignmentExpressionTree) tree;
      varType = ((AbstractTypedTree) aet.variable()).getSymbolType();
      expr = aet.expression();
    } else {
      VariableTree variableTree = (VariableTree) tree;
      varType = ((AbstractTypedTree) variableTree.type()).getSymbolType();
      expr = variableTree.initializer();
    }
    if (expr != null && expr.is(Tree.Kind.MULTIPLY, Tree.Kind.DIVIDE) && isVarTypeErrorProne(varType)) {
      Type exprType = ((AbstractTypedTree) expr).getSymbolType();
      if (exprType.isTagged(Type.INT)) {
        addIssue(tree, "Cast one of the operands of this " + OPERATION_BY_KIND.get(((JavaTree) expr).getKind()) + " operation to a \"" + varType.getSymbol().getName() + "\".");
      }
    }
  }

  private boolean isVarTypeErrorProne(Type varType) {
    return varType.isTagged(Type.LONG) || varType.isTagged(Type.FLOAT) || varType.isTagged(Type.DOUBLE);
  }
}

