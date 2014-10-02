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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(
    key = "S1244",
    priority = Priority.CRITICAL,
    tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class FloatEqualityCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
    if(isFloat(binaryExpressionTree.leftOperand()) && isFloat(binaryExpressionTree.rightOperand()) && !isNanTest(binaryExpressionTree)) {
      addIssue(binaryExpressionTree, "float comparison should rely on absolute value of their difference compared to an arbitrary epsilon.");
    }
  }

  private boolean isNanTest(BinaryExpressionTree binaryExpressionTree) {
    return SyntacticEquivalence.areEquivalent(binaryExpressionTree.leftOperand(), binaryExpressionTree.rightOperand());
  }

  private boolean isFloat(ExpressionTree expressionTree){
    return ((AbstractTypedTree) expressionTree).getSymbolType().isTagged(Type.FLOAT);
  }

}
