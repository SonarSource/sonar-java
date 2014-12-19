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
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2197",
  priority = Priority.CRITICAL)
public class ModulusEqualityCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EQUAL_TO);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree equality = (BinaryExpressionTree) tree;
    checkModulusAndIntLiteral(equality.leftOperand(), equality.rightOperand());
    checkModulusAndIntLiteral(equality.rightOperand(), equality.leftOperand());
  }

  private void checkModulusAndIntLiteral(ExpressionTree operand1, ExpressionTree operand2) {
    if (operand1.is(Tree.Kind.REMAINDER)) {
      Integer intValue = LiteralUtils.intLiteralValue(operand2);
      if (intValue != null && intValue != 0) {
        String sign = intValue > 0 ? "positive" : "negative";
        addIssue(operand1, "The results of this modulus operation may not be " + sign + ".");
      }
    }
  }

}
