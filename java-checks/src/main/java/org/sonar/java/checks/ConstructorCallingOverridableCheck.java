/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S1699")
public class ConstructorCallingOverridableCheck extends IssuableSubscriptionVisitor {

  private SemanticModel semanticModel;

  @Override
  public void setContext(JavaFileScannerContext context) {
    semanticModel = (SemanticModel) context.getSemanticModel();
    super.setContext(context);
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      TypeJavaSymbol constructorType = (TypeJavaSymbol) semanticModel.getEnclosingClass(tree);
      if (!constructorType.isFinal()) {
        ((MethodTree) tree).block().accept(new ConstructorBodyVisitor(constructorType));
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
        isInvocationOnSelf = !isThisOrSuper(methodIdentifier);
      } else if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodSelect;
        methodIdentifier = memberSelect.identifier();
        isInvocationOnSelf = isThis(memberSelect.expression());
      }
      if (isInvocationOnSelf) {
        Symbol symbol = methodIdentifier.symbol();
        if (symbol.isMethodSymbol() && ((JavaSymbol.MethodJavaSymbol) symbol).isOverridable() && isMethodDefinedOnConstructedType(symbol)) {
          reportIssue(methodIdentifier, "Remove this call from a constructor to the overridable \"" + methodIdentifier.name() + "\" method.");
        }
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      // skip new class
    }

    private boolean is(ExpressionTree expression, String match) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        String targetName = ((IdentifierTree) expression).name();
        return match.equals(targetName);
      }
      return false;
    }

    private boolean isThis(ExpressionTree expression) {
      return is(expression, "this");
    }

    private boolean isThisOrSuper(ExpressionTree expression) {
      return is(expression, "this") || is(expression, "super");
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
  }

}
