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
import com.sonar.sslr.api.AstNode;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(
  key = "S2167",
  priority = Priority.CRITICAL,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class CompareToReturnValueCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isCompareToDeclaration(methodTree)) {
      methodTree.accept(new ReturnStatementVisitor(methodTree));
    }
  }

  private boolean isCompareToDeclaration(MethodTree tree) {
    return isMethodName(tree, "compareTo") && hasOneNonPrimitiveParameter(tree) && returnsInt(tree);
  }

  private boolean isMethodName(MethodTree methodTree, String name) {
    return name.equals(methodTree.simpleName().name());
  }

  private boolean hasOneNonPrimitiveParameter(MethodTree methodTree) {
    List<VariableTree> parameters = methodTree.parameters();
    return parameters.size() == 1 && !((VariableTreeImpl) parameters.get(0)).getSymbol().getType().isPrimitive();
  }

  private boolean returnsInt(MethodTree methodTree) {
    return ((MethodTreeImpl) methodTree).getSymbol().getReturnType().getType().isTagged(Type.INT);
  }

  private class ReturnStatementVisitor extends BaseTreeVisitor {

    private final AstNode callingMethod;

    public ReturnStatementVisitor(MethodTree callingMethod) {
      this.callingMethod = ((MethodTreeImpl) callingMethod).getAstNode();
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      if (isWithinCallingMethodBody(tree) && returnsIntegerMinValue(tree.expression())) {
        addIssue(tree, "Simply return -1");
      }
      super.visitReturnStatement(tree);
    }

    private boolean returnsIntegerMinValue(ExpressionTree expressionTree) {
      if (expressionTree.is(Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expressionTree;
        boolean isInteger = ((IdentifierTreeImpl) memberSelect.expression()).getSymbolType().is("java.lang.Integer");
        boolean isMinValue = "MIN_VALUE".equals(memberSelect.identifier().name());
        return isInteger && isMinValue;
      }
      return false;
    }

    private boolean isWithinCallingMethodBody(ReturnStatementTree tree) {
      return callingMethod.equals(((ReturnStatementTreeImpl) tree).getFirstAncestor(callingMethod.getType()));
    }
  }
}
