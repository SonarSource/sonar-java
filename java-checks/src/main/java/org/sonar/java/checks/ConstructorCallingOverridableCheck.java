/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
import org.sonar.java.resolve.JavaType.ClassJavaType;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1699",
  name = "Constructors should only call non-overridable methods",
  tags = {"bug"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("10min")
public class ConstructorCallingOverridableCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      MethodTree methodTree = (MethodTree) tree;
      TypeJavaSymbol constructorType = (TypeJavaSymbol) getSemanticModel().getEnclosingClass(tree);
      if (!constructorType.isFinal()) {
        ConstructorBodyVisitor constructorBodyVisitor = new ConstructorBodyVisitor(constructorType);
        methodTree.block().accept(constructorBodyVisitor);
      }
    }
  }

  private class ConstructorBodyVisitor extends BaseTreeVisitor {

    private TypeJavaSymbol constructorType;

    public ConstructorBodyVisitor(TypeJavaSymbol constructorType) {
      this.constructorType = constructorType;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      IdentifierTree methodIdentifier = null;
      boolean isInvocationOnSelf = false;
      ExpressionTree methodSelect = tree.methodSelect();
      if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
        methodIdentifier = (IdentifierTree) methodSelect;
        isInvocationOnSelf = true;
      } else if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodSelect;
        methodIdentifier = memberSelect.identifier();
        isInvocationOnSelf = isThisOrSuper(memberSelect.expression());
      }
      if (isInvocationOnSelf) {
        Symbol symbol = methodIdentifier.symbol();
        if (isOverridableMethod(symbol) && isMethodDefinedOnConstructedType(symbol)) {
          addIssue(tree, "Remove this call from a constructor to the overridable \"" + methodIdentifier.name() + "\" method.");
        }
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isThisOrSuper(ExpressionTree expression) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        String targetName = ((IdentifierTree) expression).name();
        return "this".equals(targetName) || "super".equals(targetName);
      }
      return false;
    }

    private boolean isMethodDefinedOnConstructedType(Symbol symbol) {
      TypeJavaSymbol methodEnclosingClass = (TypeJavaSymbol) symbol.enclosingClass();
      for (ClassJavaType superType : constructorType.superTypes()) {
        if (superType.getSymbol().equals(methodEnclosingClass)) {
          return true;
        }
      }
      return constructorType.equals(methodEnclosingClass);
    }

    private boolean isOverridableMethod(Symbol symbol) {
      if (symbol.isMethodSymbol() && !symbol.isPrivate() && !symbol.isFinal() && !symbol.isStatic()) {
        return true;
      }
      return false;
    }

  }

}
