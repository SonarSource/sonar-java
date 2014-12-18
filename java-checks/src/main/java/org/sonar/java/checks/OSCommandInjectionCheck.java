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
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

@Rule(
    key = "S2076",
    priority = Priority.CRITICAL,
    tags = {"cwe", "owasp-top10", "sans-top25", "security"})
public class OSCommandInjectionCheck extends AbstractInjectionChecker {

  private static final MethodInvocationMatcher RUNTIME_EXEC_MATCHER = MethodInvocationMatcher.create()
      .typeDefinition("java.lang.Runtime")
      .name("exec").withNoParameterConstraint();

  private String parameterName;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree arg = null;
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.METHOD_INVOCATION) && RUNTIME_EXEC_MATCHER.matches((MethodInvocationTree) tree, getSemanticModel())) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        checkForIssue(tree, mit.arguments().get(0));
      } else if (tree.is(Tree.Kind.NEW_CLASS) && ((AbstractTypedTree) tree).getSymbolType().is("java.lang.ProcessBuilder")) {
        for (ExpressionTree expressionTree : ((NewClassTree) tree).arguments()) {
          checkForIssue(tree, expressionTree);
        }
      }
    }
  }

  private void checkForIssue(Tree tree, ExpressionTree arg) {
    if (isDynamicArray(arg, tree)) {
      addIssue(arg, "Make sure \""+parameterName+"\" is properly sanitized before use in this OS command.");
    }
  }

  private boolean isDynamicArray(@Nullable ExpressionTree arg, Tree mit) {
    if (arg == null) {
      return false;
    }
    if (arg.is(Tree.Kind.NEW_ARRAY)) {
      NewArrayTree nat = (NewArrayTree) arg;
      for (ExpressionTree expressionTree : nat.initializers()) {
        if (isDynamicString(mit, expressionTree, null)) {
          return true;
        }
      }
      return false;
    }
    if (arg.is(Tree.Kind.IDENTIFIER)) {
      parameterName = ((IdentifierTree) arg).name();
    } else if (arg.is(Tree.Kind.MEMBER_SELECT)) {
      parameterName = ((MemberSelectExpressionTree) arg).identifier().name();
    }
    boolean argIsString = ((AbstractTypedTree) arg).getSymbolType().is("java.lang.String");
    return !argIsString || isDynamicString(mit, arg, null);
  }

  @Override
  protected boolean isDynamicString(Tree methodTree, ExpressionTree arg, @Nullable Symbol currentlyChecking) {
    if (arg.is(Tree.Kind.IDENTIFIER)) {
      return isIdentifierDynamicString(methodTree, (IdentifierTree) arg, currentlyChecking);
    } else if (arg.is(Tree.Kind.PLUS)) {
      BinaryExpressionTree binaryArg = (BinaryExpressionTree) arg;
      return isDynamicString(methodTree, binaryArg.rightOperand(), currentlyChecking) || isDynamicString(methodTree, binaryArg.leftOperand(), currentlyChecking);

    } else if (arg.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    return !arg.is(Tree.Kind.STRING_LITERAL);
  }

  private boolean isIdentifierDynamicString(Tree methodTree, IdentifierTree arg, @Nullable Symbol currentlyChecking) {
    Symbol symbol = getSemanticModel().getReference(arg);
    if (symbol.equals(currentlyChecking) || isConstant(symbol)) {
      return false;
    }

    Tree enclosingBlockTree = getSemanticModel().getTree(getSemanticModel().getEnv(methodTree));
    Tree argEnclosingDeclarationTree = getSemanticModel().getTree(getSemanticModel().getEnv(symbol));
    if (enclosingBlockTree.equals(argEnclosingDeclarationTree)) {
      //symbol is a local variable, check it is not a dynamic string.

      //Check declaration
      VariableTree declaration = (VariableTree) getSemanticModel().getTree(symbol);
      if (declaration.initializer() != null && isDynamicString(methodTree, declaration.initializer(), currentlyChecking)) {
        return true;
      }
      //check usages by revisiting the enclosing tree.
      Collection<IdentifierTree> usages = getSemanticModel().getUsages(symbol);
      AbstractInjectionChecker.LocalVariableDynamicStringVisitor visitor = new AbstractInjectionChecker.LocalVariableDynamicStringVisitor(symbol, usages, methodTree);
      argEnclosingDeclarationTree.accept(visitor);
      return visitor.dynamicString;
    }
    //arg is not a local variable nor a constant, so it is a parameter or a field.
    parameterName = arg.name();
    return symbol.owner().isKind(Symbol.MTH);
  }


  private boolean isConstant(Symbol symbol) {
    return symbol.isStatic() && symbol.isFinal();
  }


}
