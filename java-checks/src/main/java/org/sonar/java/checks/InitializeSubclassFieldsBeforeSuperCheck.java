/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.StatementVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import static org.sonar.java.ast.api.JavaKeyword.THIS;

@Rule(key = "S8447")
public final class InitializeSubclassFieldsBeforeSuperCheck extends FlexibleConstructorVisitor {
  @Override
  void validateConstructor(MethodTree constructor, List<StatementTree> body, int constructorCallIndex) {
    if (constructorCallIndex < 0) {
      // Super called implicitly, so we disable the rule to avoid false positives.
      return;
    }
    MethodTree superMethod;
    Symbol.TypeSymbol childClass;
    if (body.get(constructorCallIndex) instanceof ExpressionStatementTree esTree
      && esTree.expression() instanceof MethodInvocationTree miTree
      && (superMethod = miTree.methodSymbol().declaration()) != null
      && (childClass = constructor.symbol().enclosingClass()) != null
    ) {
      AssignmentsUsedInSuperCheck assignmentsUsedInSuperCheck = new AssignmentsUsedInSuperCheck(superMethod, childClass);
      body.subList(constructorCallIndex + 1, body.size()).forEach(statement -> statement.accept(assignmentsUsedInSuperCheck));
    }
  }

  private static boolean isFieldAssignment(AssignmentExpressionTree tree) {
    return tree.variable() instanceof MemberSelectExpressionTree mseTree
      && mseTree.expression() instanceof IdentifierTree idTree
      && THIS.getValue().equals(idTree.name());
  }

  private static boolean isFieldUsedInMethod(MethodTree mt, Symbol symbol) {
    var methodBlock = mt.block();
    if (methodBlock == null) {
      // Can't resolve override, conservatively assume field may be used.
      return true;
    }
    SymbolUsedCheck symbolUsedCheck = new SymbolUsedCheck(symbol);
    methodBlock.body().forEach(statement -> statement.accept(symbolUsedCheck));
    return symbolUsedCheck.isSymbolUsed();
  }

  private static boolean isFieldUsedInMethod(MethodTree mt, Symbol symbol, Symbol.TypeSymbol childClass) {
    // Try to resolve the concrete override in the child class.
    return findOverrideIn(childClass, mt.symbol())
      .map(methodTree -> isFieldUsedInMethod(methodTree, symbol))
      .orElseGet(() -> isFieldUsedInMethod(mt, symbol));
  }

  private static Optional<MethodTree> findOverrideIn(Symbol.TypeSymbol childClass, Symbol.MethodSymbol abstractMethod) {
    String methodName = abstractMethod.name();
    return childClass.memberSymbols().stream()
      .filter(s -> s.isMethodSymbol() && s.name().equals(methodName))
      .map(Symbol.MethodSymbol.class::cast)
      .filter(m -> m.declaration() != null && m.overriddenSymbols().stream().anyMatch(s -> s == abstractMethod))
      .findFirst()
      .map(Symbol.MethodSymbol::declaration);
  }

  private final class AssignmentsUsedInSuperCheck extends StatementVisitor {
    private final MethodTree superMethod;
    private final Symbol.TypeSymbol childClass;

    private AssignmentsUsedInSuperCheck(MethodTree superMethod, Symbol.TypeSymbol childClass) {
      this.superMethod = superMethod;
      this.childClass = childClass;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (
        isFieldAssignment(tree)
          && tree.variable() instanceof MemberSelectExpressionTree mseTree
          && isFieldUsedInMethod(superMethod, mseTree.identifier().symbol(), childClass)
      ) {
        reportIssue(tree, "Initialize subclass fields before calling super constructor.");
      }
      super.visitAssignmentExpression(tree);
    }
  }

  private static class SymbolUsedCheck extends StatementVisitor {
    private boolean symbolUsed = false;
    private final Symbol symbol;

    private SymbolUsedCheck(Symbol symbol) {
      this.symbol = symbol;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      symbolUsed |= tree.symbol() == symbol;
      super.visitIdentifier(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      var methodDeclaration = tree.methodSymbol().declaration();
      if (methodDeclaration != null) {
        symbolUsed |= isFieldUsedInMethod(methodDeclaration, symbol);
      }
      super.visitMethodInvocation(tree);
    }

    public boolean isSymbolUsed() {
      return symbolUsed;
    }
  }
}
