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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
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
  public void visitMethod(MethodTree tree) {
    if (!isEquals(tree)) {
      super.visitMethod(tree);
    }
  }

  // TODO(Godin): It seems to be quite common need - operate with signature of methods, so this operation should be generalized and simplified.
  private boolean isEquals(MethodTree tree) {
    String methodName = tree.simpleName().name();
    return "equals".equals(methodName) && hasObjectParam(tree) && returnsBoolean(tree);
  }

  private boolean returnsBoolean(MethodTree tree) {
    Symbol.MethodSymbol methodSymbol = ((MethodTreeImpl) tree).getSymbol();
    // TODO(Godin): Not very convenient way to get a return type
    return (methodSymbol != null) && (methodSymbol.getReturnType().getType().isTagged(Type.BOOLEAN));
  }

  private boolean hasObjectParam(MethodTree tree) {
    boolean result = false;
    if (tree.parameters().size() == 1 && tree.parameters().get(0).type().is(Tree.Kind.IDENTIFIER)) {
      result = ((IdentifierTree) tree.parameters().get(0).type()).name().endsWith("Object");
    }
    return result;
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (tree.is(Tree.Kind.EQUAL_TO) || tree.is(Tree.Kind.NOT_EQUAL_TO)) {
      Type leftOperandType = ((AbstractTypedTree) tree.leftOperand()).getType();
      Type rightOperandType = ((AbstractTypedTree) tree.rightOperand()).getType();
      if (!isNullComparison(leftOperandType, rightOperandType)
          && !isNumericalComparison(leftOperandType, rightOperandType)
          && (isClass(leftOperandType) || isClass(rightOperandType))) {
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

  private boolean isNumericalComparison(Type leftOperandType, Type rightOperandType) {
    return leftOperandType.isNumerical() || rightOperandType.isNumerical();
  }
}
