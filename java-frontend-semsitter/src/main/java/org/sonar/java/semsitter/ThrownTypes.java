/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

/** The exception types the statements of a try body can throw, not looking inside lambdas and nested classes. */
final class ThrownTypes extends BaseTreeVisitor {
  final List<Type> thrown = new ArrayList<>();
  boolean unknown;

  void scanTry(List<Tree> resources, Tree block) {
    for (Tree resource : resources) {
      scan(resource);
      addClose(resource instanceof org.sonar.plugins.java.api.tree.VariableTree variable ? variable.type().symbolType() : ((ExpressionTree) resource).symbolType());
    }
    scan(block);
  }

  private void addClose(Type type) {
    if (type.isUnknown()) {
      unknown = true;
      return;
    }
    List<Type> candidates = new ArrayList<>(List.of(type));
    type.symbol().superTypes().forEach(candidates::add);
    for (Type candidate : candidates) {
      for (Symbol member : candidate.symbol().memberSymbols()) {
        if (member instanceof Symbol.MethodSymbol method && "close".equals(method.name()) && method.parameterTypes().isEmpty()) {
          addAll(method);
          return;
        }
      }
    }
    unknown = true;
  }

  private void addAll(Symbol.MethodSymbol method) {
    if (method.isUnknown()) {
      unknown = true;
      return;
    }
    method.thrownTypes().forEach(this::add);
  }

  private void add(Type type) {
    if (type.isUnknown() || !type.isClass()) {
      unknown = true;
    } else {
      thrown.add(type);
    }
  }

  @Override
  public void visitMethodInvocation(org.sonar.plugins.java.api.tree.MethodInvocationTree tree) {
    super.visitMethodInvocation(tree);
    addAll(tree.methodSymbol());
  }

  @Override
  public void visitNewClass(org.sonar.plugins.java.api.tree.NewClassTree tree) {
    scan(tree.enclosingExpression());
    scan(tree.arguments());
    addAll(tree.methodSymbol());
  }

  @Override
  public void visitThrowStatement(org.sonar.plugins.java.api.tree.ThrowStatementTree tree) {
    super.visitThrowStatement(tree);
    add(tree.expression().symbolType());
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    ThrownTypes inner = new ThrownTypes();
    inner.scanTry(tree.resourceList(), tree.block());
    unknown |= inner.unknown;
    List<Type> caught = new ArrayList<>();
    tree.catches().forEach(catchTree -> {
      TypeTree parameterType = catchTree.parameter().type();
      (parameterType instanceof UnionTypeTree union ? union.typeAlternatives() : List.of(parameterType)).forEach(alternative -> caught.add(alternative.symbolType()));
      scan(catchTree.block());
    });
    inner.thrown.stream().filter(type -> caught.stream().noneMatch(type::isSubtypeOf)).forEach(thrown::add);
    scan(tree.finallyBlock());
  }

  @Override
  public void visitLambdaExpression(org.sonar.plugins.java.api.tree.LambdaExpressionTree tree) {
    // not thrown by the enclosing statements
  }

  @Override
  public void visitMethodReference(org.sonar.plugins.java.api.tree.MethodReferenceTree tree) {
    scan(tree.expression());
  }

  @Override
  public void visitClass(org.sonar.plugins.java.api.tree.ClassTree tree) {
    // a nested class has its own callables
  }
}
