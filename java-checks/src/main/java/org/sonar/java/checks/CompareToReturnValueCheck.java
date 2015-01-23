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
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2167",
  priority = Priority.CRITICAL,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class CompareToReturnValueCheck extends SubscriptionBaseVisitor {

  private boolean insideCompareTo = false;

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.RETURN_STATEMENT, Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.METHOD)) {
      insideCompareTo = isCompareToDeclaration((MethodTree) tree);
    } else if (insideCompareTo && returnIntegerMinValue((ReturnStatementTree) tree)) {
      addIssue(tree, "Simply return -1");
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      insideCompareTo = false;
    }
  }

  private boolean isCompareToDeclaration(MethodTree tree) {
    Tree returnType = tree.returnType();
    return "compareTo".equals(tree.simpleName().name()) &&
      returnType.is(Kind.PRIMITIVE_TYPE) &&
      "int".equals(((PrimitiveTypeTree) returnType).keyword().text());
  }

  private boolean returnIntegerMinValue(ReturnStatementTree tree) {
    ExpressionTree expressionTree = tree.expression();
    if (expressionTree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) expressionTree;
      return ((IdentifierTreeImpl) memberSelectExpressionTree.expression()).getSymbolType().is("java.lang.Integer") &&
        "MIN_VALUE".equals(memberSelectExpressionTree.identifier().name());
    }
    return false;
  }
}
