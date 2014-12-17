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

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Collection;

@Rule(
    key = "S2078",
    priority = Priority.CRITICAL,
    tags = {"cwe", "owasp-top10", "security"})
public class LDAPInjectionCheck extends AbstractInjectionChecker {

  private static final MethodInvocationMatcher LDAP_SEARCH_MATCHER = MethodInvocationMatcher.create()
      .typeDefinition("javax.naming.directory.DirContext")
      .name("search").withNoParameterConstraint();

  private static final MethodInvocationMatcher SEARCH_CONTROLS_MATCHER = MethodInvocationMatcher.create()
      .typeDefinition("javax.naming.directory.SearchControls")
      .name("setReturningAttributes").addParameter("java.lang.String[]");

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (isDirContextSearchCall(mit)) {
      //Check the first two arguments of search method
      checkDirContextArg(mit.arguments().get(0), mit);
      checkDirContextArg(mit.arguments().get(1), mit);
    } else if (isSearchControlCall(mit)) {
      ExpressionTree arg = mit.arguments().get(0);
      if (isDynamicArray(arg, mit)) {
        createIssue(arg);
      }
    }
  }

  private void checkDirContextArg(ExpressionTree arg1, MethodInvocationTree mit) {
    if (((AbstractTypedTree) arg1).getSymbolType().is("java.lang.String") && isDynamicString(mit, arg1, null)) {
      createIssue(arg1);
    }
  }

  private void createIssue(Tree tree) {
    addIssue(tree, "Make sure that all elements used to build this LDAP request are properly sanitized.");
  }


  private boolean isDynamicArray(ExpressionTree arg, MethodInvocationTree mit) {
    if (arg.is(Tree.Kind.NEW_ARRAY)) {
      NewArrayTree nat = (NewArrayTree) arg;
      for (ExpressionTree expressionTree : nat.initializers()) {
        if (isDynamicString(mit, expressionTree, null)) {
          return true;
        }
      }
      return false;
    }
    return true;
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
      LocalVariableDynamicStringVisitor visitor = new LocalVariableDynamicStringVisitor(symbol, usages, methodTree);
      argEnclosingDeclarationTree.accept(visitor);
      return visitor.dynamicString;
    }
    //arg is not a local variable nor a constant, so it is a parameter or a field.
    return symbol.owner().isKind(Symbol.MTH);
  }


  private boolean isConstant(Symbol symbol) {
    return symbol.isStatic() && symbol.isFinal();
  }

  private boolean isDirContextSearchCall(MethodInvocationTree methodTree) {
    return hasSemantic() && LDAP_SEARCH_MATCHER.matches(methodTree, getSemanticModel());
  }

  private boolean isSearchControlCall(MethodInvocationTree methodTree) {
    return hasSemantic() && SEARCH_CONTROLS_MATCHER.matches(methodTree, getSemanticModel());
  }

}
