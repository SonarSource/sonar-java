/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class AbstractInjectionChecker extends IssuableSubscriptionVisitor {

  protected String parameterName;
  private SemanticModel semanticModel;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    semanticModel = (SemanticModel) context.getSemanticModel();
    super.scanFile(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  protected boolean isDynamicString(Tree methodTree, ExpressionTree arg, @Nullable Symbol currentlyChecking) {
    return isDynamicString(methodTree, arg, currentlyChecking, false);
  }

  protected boolean isDynamicString(Tree methodTree, ExpressionTree arg, @Nullable Symbol currentlyChecking, boolean firstLevel) {
    if (arg.is(Tree.Kind.MEMBER_SELECT)) {
      return !isConstant(((MemberSelectExpressionTree) arg).identifier().symbol());
    } else if (arg.is(Tree.Kind.IDENTIFIER)) {
      return isIdentifierDynamicString(methodTree, (IdentifierTree) arg, currentlyChecking, firstLevel);
    } else if (arg.is(Tree.Kind.PLUS)) {
      BinaryExpressionTree binaryArg = (BinaryExpressionTree) arg;
      return isDynamicString(methodTree, binaryArg.rightOperand(), currentlyChecking) || isDynamicString(methodTree, binaryArg.leftOperand(), currentlyChecking);

    } else if (arg.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    return !arg.is(Tree.Kind.STRING_LITERAL);
  }

  protected boolean isIdentifierDynamicString(Tree methodTree, IdentifierTree arg, @Nullable Symbol currentlyChecking, boolean firstLevel) {
    Symbol symbol = arg.symbol();
    if (isExcluded(currentlyChecking, symbol)) {
      return false;
    }

    Tree enclosingBlockTree = semanticModel.getTree(semanticModel.getEnv(methodTree));
    Tree argEnclosingDeclarationTree = semanticModel.getTree(semanticModel.getEnv(symbol));
    if (enclosingBlockTree.equals(argEnclosingDeclarationTree)) {
      //symbol is a local variable, check it is not a dynamic string.

      //Check declaration
      VariableTree declaration = ((Symbol.VariableSymbol) symbol).declaration();
      ExpressionTree initializer = declaration.initializer();
      if (initializer != null && isDynamicString(methodTree, initializer, currentlyChecking)) {
        return true;
      }
      //check usages by revisiting the enclosing tree.
      Collection<IdentifierTree> usages = symbol.usages();
      LocalVariableDynamicStringVisitor visitor = new LocalVariableDynamicStringVisitor(symbol, usages, methodTree);
      argEnclosingDeclarationTree.accept(visitor);
      return visitor.dynamicString;
    }
    //arg is not a local variable nor a constant, so it is a parameter or a field.
    parameterName = arg.name();
    return symbol.owner().isMethodSymbol() && !firstLevel;
  }

  private boolean isExcluded(@Nullable Symbol currentlyChecking, Symbol symbol) {
    return !symbol.isVariableSymbol() || symbol.equals(currentlyChecking) || isConstant(symbol);
  }

  public boolean isConstant(Symbol symbol) {
    return symbol.isStatic() && symbol.isFinal();
  }

  protected class LocalVariableDynamicStringVisitor extends BaseTreeVisitor {

    private final Collection<IdentifierTree> usages;
    private final Tree methodInvocationTree;
    private final Symbol currentlyChecking;
    boolean dynamicString;
    private boolean stopInspection;

    public LocalVariableDynamicStringVisitor(Symbol currentlyChecking, Collection<IdentifierTree> usages, Tree methodInvocationTree) {
      this.currentlyChecking = currentlyChecking;
      stopInspection = false;
      this.usages = usages;
      this.methodInvocationTree = methodInvocationTree;
      dynamicString = false;
    }


    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (!stopInspection && tree.variable().is(Tree.Kind.IDENTIFIER) && usages.contains(tree.variable())) {
        dynamicString |= isDynamicString(methodInvocationTree, tree.expression(), currentlyChecking);
      }
      super.visitAssignmentExpression(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.equals(methodInvocationTree)) {
        //stop inspection, all concerned usages have been visited.
        stopInspection = true;
      } else {
        super.visitMethodInvocation(tree);
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (tree.equals(methodInvocationTree)) {
        //stop inspection, all concerned usages have been visited.
        stopInspection = true;
      } else {
        super.visitNewClass(tree);
      }
    }
  }

  protected void setParameterNameFromArgument(ExpressionTree arg) {
    if (arg.is(Tree.Kind.IDENTIFIER)) {
      parameterName = ((IdentifierTree) arg).name();
    } else if (arg.is(Tree.Kind.MEMBER_SELECT)) {
      parameterName = ((MemberSelectExpressionTree) arg).identifier().name();
    }
  }

}
