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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

@Rule(
    key = "S2077",
    priority = Priority.CRITICAL,
    tags = {"cwe", "owasp-top10", "security", "sql"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class SQLInjectionCheck extends SubscriptionBaseVisitor {

  private String parameterName;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodTree = (MethodInvocationTree) tree;
    boolean isHibernateCall = isHibernateCall(methodTree);
    if (isHibernateCall(methodTree) || isExecuteQueryOrPrepareStatement(methodTree)) {
      //We want to check the argument for the three methods.
      ExpressionTree arg = methodTree.arguments().get(0);
      parameterName = "";
      if (isDynamicString(methodTree, arg, null, true)) {
        String message = "\""+parameterName+"\" is provided externally to the method and not sanitized before use.";
        if(isHibernateCall) {
          message = "Use Hibernate's parameter binding instead of concatenation.";
        }
        addIssue(methodTree, message);
      }
    }
  }

  private boolean isDynamicString(MethodInvocationTree methodTree, ExpressionTree arg, @Nullable Symbol currentlyChecking) {
    return isDynamicString(methodTree, arg, currentlyChecking, false);
  }
  private boolean isDynamicString(MethodInvocationTree methodTree, ExpressionTree arg, @Nullable Symbol currentlyChecking, boolean firstLevel) {
    if (arg.is(Tree.Kind.IDENTIFIER)) {
      return isIdentifierDynamicString(methodTree, (IdentifierTree) arg, currentlyChecking, firstLevel);
    } else if (arg.is(Tree.Kind.PLUS)) {
      BinaryExpressionTree binaryArg = (BinaryExpressionTree) arg;
      return isDynamicString(methodTree, binaryArg.rightOperand(), currentlyChecking) || isDynamicString(methodTree, binaryArg.leftOperand(), currentlyChecking);

    } else if(arg.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    return !arg.is(Tree.Kind.STRING_LITERAL);
  }

  private boolean isIdentifierDynamicString(MethodInvocationTree methodTree, IdentifierTree arg, @Nullable Symbol currentlyChecking, boolean firstLevel) {
    Symbol symbol = getSemanticModel().getReference(arg);
    if(symbol.equals(currentlyChecking) || isConstant(symbol)) {
      return false;
    }

    Tree enclosingBlockTree = getSemanticModel().getTree(getSemanticModel().getEnv(methodTree));
    Tree argEnclosingDeclarationTree = getSemanticModel().getTree(getSemanticModel().getEnv(symbol));
    if(enclosingBlockTree.equals(argEnclosingDeclarationTree)) {
      //symbol is a local variable, check it is not a dynamic string.

      //Check declaration
      VariableTree declaration = (VariableTree) getSemanticModel().getTree(symbol);
      if(declaration.initializer() != null && isDynamicString(methodTree, declaration.initializer(), currentlyChecking)) {
        return true;
      }
      //check usages by revisiting the enclosing tree.
      Collection<IdentifierTree> usages = getSemanticModel().getUsages(symbol);
      LocalVariableDynamicStringVisitor visitor = new LocalVariableDynamicStringVisitor(symbol, usages, methodTree);
      argEnclosingDeclarationTree.accept(visitor);
      return visitor.dynamicString;
    }
    //arg is not a local variable nor a constant, so it is a parameter or a field.
    parameterName =  arg.name();
    return symbol.owner().isKind(Symbol.MTH) && !firstLevel;
  }

  private boolean isConstant(Symbol symbol) {
    return symbol.isStatic() && symbol.isFinal();
  }

  private boolean isExecuteQueryOrPrepareStatement(MethodInvocationTree methodTree) {
    if (methodTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) methodTree.methodSelect();
      return !methodTree.arguments().isEmpty() && (isMethodCall("java.sql.Statement", "executeQuery", memberSelectExpressionTree)
          || isMethodCall("java.sql.Connection", "prepareStatement", memberSelectExpressionTree)
          || isMethodCall("java.sql.Connection", "prepareCall", memberSelectExpressionTree)
      );
    }
    return false;
  }

  private boolean isHibernateCall(MethodInvocationTree methodTree) {
    if (methodTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) methodTree.methodSelect();
      return !methodTree.arguments().isEmpty() && isMethodCall("org.hibernate.Session", "createQuery", memberSelectExpressionTree);
    }
    return false;
  }

  private boolean isMethodCall(String typeName, String methodName, MemberSelectExpressionTree memberSelectExpressionTree) {
    return methodName.equals(memberSelectExpressionTree.identifier().name()) && isInvokedOnType(typeName, memberSelectExpressionTree.expression());
  }

  private boolean isInvokedOnType(String type, ExpressionTree expressionTree) {
    Type selectorType = ((AbstractTypedTree) expressionTree).getSymbolType();
    if (selectorType.isTagged(Type.CLASS)) {
      Symbol.TypeSymbol symbol = ((Type.ClassType) selectorType).getSymbol();
      String selector = symbol.owner().getName() + "." + symbol.getName();
      return type.equals(selector) || checkInterfaces(type, symbol);
    }
    return false;
  }

  private boolean checkInterfaces(String type, Symbol.TypeSymbol symbol) {
    for (Type interfaceType : symbol.getInterfaces()) {
      Symbol.TypeSymbol interfaceSymbol = ((Type.ClassType) interfaceType).getSymbol();
      if (type.equals(interfaceSymbol.owner().getName() + "." + interfaceSymbol.getName()) || checkInterfaces(type, interfaceSymbol)) {
        return true;
      }
    }
    return false;
  }

  private class LocalVariableDynamicStringVisitor extends BaseTreeVisitor {

    private final Collection<IdentifierTree> usages;
    private final MethodInvocationTree methodInvocationTree;
    private final Symbol currentlyChecking;
    private boolean stopInspection;
    boolean dynamicString;

    public LocalVariableDynamicStringVisitor(Symbol currentlyChecking, Collection<IdentifierTree> usages, MethodInvocationTree methodInvocationTree) {
      this.currentlyChecking = currentlyChecking;
      stopInspection = false;
      this.usages = usages;
      this.methodInvocationTree = methodInvocationTree;
      dynamicString = false;
    }


    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if(!stopInspection && tree.variable().is(Tree.Kind.IDENTIFIER) && usages.contains(tree.variable())) {
        dynamicString |= isDynamicString(methodInvocationTree, tree.expression(), currentlyChecking);
      }
      super.visitAssignmentExpression(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if(tree.equals(methodInvocationTree)) {
        //stop inspection, all concerned usages have been visited.
        stopInspection = true;
      } else {
        super.visitMethodInvocation(tree);
      }
    }
  }

}
