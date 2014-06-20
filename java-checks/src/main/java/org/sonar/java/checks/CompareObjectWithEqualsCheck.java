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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(
    key = CompareObjectWithEqualsCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"error-handling"})
public class CompareObjectWithEqualsCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1698";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (tree.is(Tree.Kind.EQUAL_TO)) {
      Type leftOperandType = ((JavaTree.AbstractExpressionTree) tree.leftOperand()).getType();
      Type rightOperandType = ((JavaTree.AbstractExpressionTree) tree.rightOperand()).getType();
      if (leftOperandType == null || rightOperandType == null) {
        //FIXME null type should not happen.
        return;
      }
      if (!isNullComparison(leftOperandType, rightOperandType) && (isClass(leftOperandType) || isClass(rightOperandType))) {
        context.addIssue(tree, ruleKey, "Change this comparison to use the equals method.");
      }
    }
  }

  private boolean isClass(Type operandType) {
    return operandType.isTagged(Type.CLASS) && !((Type.ClassType) operandType).getSymbol().isEnum();
  }

  private boolean isNullComparison(Type leftOperandType, Type rightOperandType) {
    return leftOperandType.isTagged(Type.BOT) || rightOperandType.isTagged(Type.BOT);
  }
}
