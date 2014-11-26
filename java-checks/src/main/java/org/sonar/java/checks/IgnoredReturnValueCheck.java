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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(
    key = "S2201",
    priority = Priority.CRITICAL,
    tags = {"bug"})
public class IgnoredReturnValueCheck extends SubscriptionBaseVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionStatementTree est = (ExpressionStatementTree) tree;
    if (est.expression().is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) est.expression();
      Type methodType = ((AbstractTypedTree) mit).getSymbolType();
      if (!returnsVoid(methodType) && !isFluentAPI(mit)) {
        addIssue(tree, "The return value of \"" + methodName(mit) + "\" is not used.");
      }
    }
  }

  private boolean returnsVoid(Type methodType) {
    return methodType.isTagged(Type.VOID) || methodType.isTagged(Type.UNKNOWN);
  }

  private boolean isFluentAPI(MethodInvocationTree mit) {
    Symbol method = getSemanticModel().getReference(getIdentifier(mit));
    Type methodType = ((AbstractTypedTree) mit).getSymbolType();
    //fluent api : owner type is return type.
    return method.owner().getType().equals(methodType);
  }

  private String methodName(MethodInvocationTree mit) {
    return getIdentifier(mit).name();
  }

  private IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }
}
