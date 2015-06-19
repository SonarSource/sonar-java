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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Rule(
  key = "RedundantThrowsDeclarationCheck",
  name = "Throws declarations should not be superfluous",
  tags = {"error-handling", "security"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class RedundantThrowsDeclarationCheck extends SubscriptionBaseVisitor {

  private static final String ERROR_MESSAGE = "Remove the declaration of thrown exception '";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(
      Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE,
      Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD, Tree.Kind.NEW_CLASS,
      Tree.Kind.METHOD_INVOCATION, Tree.Kind.THROW_STATEMENT);
  }

  private final Deque<Set<Type>> thrownExceptions = new LinkedList<>();

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.THROW_STATEMENT)) {
      registerThrownType(((ThrowStatementTree) tree).expression().symbolType());
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol methodSymbol = ((MethodInvocationTree) tree).symbol();
      processMethodCall(methodSymbol);
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
      Symbol constructorSymbol = ((NewClassTree) tree).constructorSymbol();
      processMethodCall(constructorSymbol);
    } else {
      thrownExceptions.push(new HashSet<Type>());
    }
  }

  private void processMethodCall(Symbol methodSymbol) {
    if (methodSymbol.isMethodSymbol()) {
      for (Type thrownType : ((Symbol.MethodSymbol) methodSymbol).thrownTypes()) {
        registerThrownType(thrownType);
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      List<TypeTree> exceptionsTree = new ArrayList<>(methodTree.throwsClauses());
      checkRuntimeExceptions(methodTree, exceptionsTree);
      checkRedundantExceptions(methodTree, exceptionsTree);
      checkOtherExceptions(methodTree, exceptionsTree);
    }
    if (tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD)) {
      thrownExceptions.pop();
    }
  }

  private void checkRuntimeExceptions(Tree tree, List<TypeTree> exceptionsTree) {
    for (int i = exceptionsTree.size() - 1; i >= 0; i--) {
      TypeTree exceptionTree = exceptionsTree.get(i);
      Type exceptionType = exceptionTree.symbolType();
      if (exceptionType.isSubtypeOf("java.lang.RuntimeException")) {
        addIssue(tree, ERROR_MESSAGE + exceptionType.fullyQualifiedName() + "' which is a runtime exception.");
        exceptionsTree.remove(i);
      }
    }
  }

  private void checkRedundantExceptions(MethodTree tree, List<TypeTree> exceptionsTree) {
    for (int i1 = exceptionsTree.size() - 1; i1 >= 0; i1--) {
      TypeTree exceptionTree = exceptionsTree.get(i1);
      Type exceptionType = exceptionTree.symbolType();
      for (int i2 = i1 - 1; i2 >= 0; i2--) {
        Type secondExceptionType = exceptionsTree.get(i2).symbolType();
        if (exceptionType.equals(secondExceptionType) && !exceptionType.symbol().equals(Symbols.unknownSymbol)) {
          addIssue(tree, "Remove the redundant '" + exceptionType.fullyQualifiedName() + "' thrown exception declaration(s).");
          exceptionsTree.remove(i1);
          break;
        }
      }
    }
  }

  private void checkOtherExceptions(MethodTree tree, List<TypeTree> exceptionsTree) {
    for (int i1 = exceptionsTree.size() - 1; i1 >= 0; i1--) {
      TypeTree exceptionTree = exceptionsTree.get(i1);
      Type exceptionType = exceptionTree.symbolType();
      if (!exceptionType.symbol().equals(Symbols.unknownSymbol)
        && !checkRelatedExceptions(tree, exceptionTree, exceptionsTree)
        && shouldCheckExceptionsInBody(tree)
        && !isThrownFromBody(exceptionType)) {
        addIssue(tree, ERROR_MESSAGE + exceptionType.fullyQualifiedName() + "' which cannot be thrown from the body.");
      }
    }
  }

  private static boolean shouldCheckExceptionsInBody(MethodTree methodTree) {
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (!methodSymbol.isMethodSymbol()) {
      return false;
    }
    return methodSymbol.owner().isFinal() || methodSymbol.isPrivate() || methodSymbol.isStatic() || methodSymbol.isFinal();
  }

  private boolean checkRelatedExceptions(MethodTree tree, TypeTree exceptionTree, List<TypeTree> exceptionsTree) {
    Type exceptionType = exceptionTree.symbolType();
    for (int i = exceptionsTree.size() - 1; i >= 0; i--) {
      TypeTree otherExceptionTree = exceptionsTree.get(i);
      Type otherExceptionType = otherExceptionTree.symbolType();
      if (!exceptionTree.equals(otherExceptionTree) && exceptionType.isSubtypeOf(otherExceptionType)) {
        addIssue(tree, ERROR_MESSAGE + exceptionType.fullyQualifiedName() + "' which is a subclass of '" +
          otherExceptionType.fullyQualifiedName() + "'.");
        return true;
      }
    }
    return false;
  }

  private boolean isThrownFromBody(Type exceptionType) {
    return thrownExceptions.peek().contains(exceptionType);
  }

  private void registerThrownType(Type thrownException) {
    thrownExceptions.peek().add(thrownException);
  }

}
