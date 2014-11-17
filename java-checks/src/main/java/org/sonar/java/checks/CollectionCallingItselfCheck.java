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
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(
    key = "S2114",
    priority = Priority.CRITICAL,
    tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class CollectionCallingItselfCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      Symbol symbolReference = null;
      Symbol.MethodSymbol method = null;
      String reportedName = "";
      if (methodInvocationTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) methodInvocationTree.methodSelect();
        IdentifierTree identifier = mse.identifier();
        reportedName = identifier.name();
        method = (Symbol.MethodSymbol) getSemanticModel().getReference(identifier);
        if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
          symbolReference = getSemanticModel().getReference((IdentifierTree) mse.expression());
        }
      }
      if (symbolReference != null && method != null && isMethodFromCollection(method)) {
        reportIssueForParameters(methodInvocationTree, symbolReference, reportedName);
      }
    }
  }

  private void reportIssueForParameters(MethodInvocationTree methodInvocationTree, Symbol symbolReference, String reportedName) {
    for (ExpressionTree arg : methodInvocationTree.arguments()) {
      if (arg.is(Tree.Kind.IDENTIFIER)) {
        Symbol reference = getSemanticModel().getReference((IdentifierTree) arg);
        if (reference == symbolReference) {
          addIssue(methodInvocationTree, "Remove or correct this \"" + reportedName + "\" call.");
        }
      }
    }
  }

  private boolean isMethodFromCollection(Symbol.MethodSymbol methodSymbol) {
    Symbol.TypeSymbol owner = (Symbol.TypeSymbol) methodSymbol.owner();
    for (Type.ClassType classType : owner.superTypes()) {
      if (classType.is("java.util.Collection")) {
        return true;
      }
    }
    return false;
  }

}
