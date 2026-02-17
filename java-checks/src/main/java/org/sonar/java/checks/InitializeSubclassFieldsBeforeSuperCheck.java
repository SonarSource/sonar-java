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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.StatementVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BlockTree;
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
      AssignmentsUsedInSuperVisitor assignmentsUsedInSuperVisitor = new AssignmentsUsedInSuperVisitor(superMethod, childClass);
      body.subList(constructorCallIndex + 1, body.size()).forEach(statement -> statement.accept(assignmentsUsedInSuperVisitor));
    }
  }

  private static Optional<Symbol> fieldAssignmentSymbol(AssignmentExpressionTree tree, Symbol.TypeSymbol childClass) {
    // field assignment
    if (tree.variable() instanceof MemberSelectExpressionTree mseTree
      && mseTree.expression() instanceof IdentifierTree idTree
      && THIS.getValue().equals(idTree.name())) {
      return Optional.of(mseTree.identifier().symbol());
    }
    // direct assignment
    if (tree.variable() instanceof IdentifierTree idTree
      && idTree.symbol().owner() instanceof Symbol.TypeSymbol typeSymbol
      && childClass.type().isSubtypeOf(typeSymbol.type())
    ) {
      return Optional.of(idTree.symbol());
    }
    return Optional.empty();
  }


  private static boolean isFieldUsedInMethod(
    @Nullable MethodTree method,
    Symbol symbol,
    Symbol.TypeSymbol childClass,
    Set<MethodTree> visitedMethods
  ) {
    BlockTree methodBlock;
    if (method == null || (methodBlock = method.block()) == null) {
      // Can't resolve body, conservatively assume field may be used.
      return true;
    }
    SymbolUsedVisitor symbolUsedVisitor = new SymbolUsedVisitor(symbol, childClass, visitedMethods);
    methodBlock.body().forEach(statement -> statement.accept(symbolUsedVisitor));
    return symbolUsedVisitor.isSymbolUsed();
  }

  private final class AssignmentsUsedInSuperVisitor extends StatementVisitor {
    private final MethodTree superMethod;
    private final Symbol.TypeSymbol childClass;

    private AssignmentsUsedInSuperVisitor(MethodTree superMethod, Symbol.TypeSymbol childClass) {
      this.superMethod = superMethod;
      this.childClass = childClass;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      fieldAssignmentSymbol(tree, childClass).ifPresent(symbol -> {
        if (isFieldUsedInMethod(superMethod, symbol, childClass, new HashSet<>()))
          reportIssue(tree, "Initialize subclass fields before calling super constructor.");
      });
      super.visitAssignmentExpression(tree);
    }
  }

  private static class SymbolUsedVisitor extends StatementVisitor {
    private boolean symbolUsed = false;
    private final Symbol symbol;
    private final Symbol.TypeSymbol childClass;
    private final Set<MethodTree> visitedMethods;

    private SymbolUsedVisitor(
      Symbol symbol,
      Symbol.TypeSymbol childClass,
      Set<MethodTree> visitedMethods
    ) {
      this.symbol = symbol;
      this.childClass = childClass;
      this.visitedMethods = visitedMethods;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (tree.parent() instanceof MemberSelectExpressionTree) {
        // handled in visitMemberSelectExpression to avoid double counting symbol usage
        return;
      }
      boolean isSymbolUsed = tree.symbol() == symbol;
      boolean isSymbolUsedAsAssignmentTarget =
        tree.parent() instanceof AssignmentExpressionTree assignment
          && assignment.variable() instanceof IdentifierTree identifierTree
          && identifierTree.symbol() == symbol;
      boolean isSymbolUsedAsExpression = tree.parent() instanceof AssignmentExpressionTree assignment && assignment.expression() == tree;
      symbolUsed |= isSymbolUsed && (!isSymbolUsedAsAssignmentTarget || isSymbolUsedAsExpression);
      super.visitIdentifier(tree);
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      if (tree.expression() instanceof IdentifierTree idTree
        && THIS.getValue().equals(idTree.name())
        && tree.identifier().symbol() == symbol) {
        boolean isSymbolUsedOnlyAsAssignmentTarget = tree.parent() instanceof AssignmentExpressionTree assignment && assignment.variable() == tree;
        boolean isSymbolUsedAsExpression = tree.parent() instanceof AssignmentExpressionTree assignment && assignment.expression() == tree;
        symbolUsed |= !isSymbolUsedOnlyAsAssignmentTarget || isSymbolUsedAsExpression;
      }
      super.visitMemberSelectExpression(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol.MethodSymbol calledMethod = tree.methodSymbol();
      MethodTree targetMethod = findChildOverride(childClass, calledMethod);
      if (targetMethod == null) {
        targetMethod = calledMethod.declaration();
      }
      if (targetMethod != null && visitedMethods.add(targetMethod)) {
        symbolUsed |= isFieldUsedInMethod(targetMethod, symbol, childClass, visitedMethods);
      }
      super.visitMethodInvocation(tree);
    }

    @Nullable
    private static MethodTree findChildOverride(Symbol.TypeSymbol childClass, Symbol.MethodSymbol method) {
      Symbol.TypeSymbol declaringClass = method.enclosingClass();
      Symbol.TypeSymbol currentClass = childClass;
      while (currentClass != null && currentClass != declaringClass) {
        for (Symbol member : currentClass.lookupSymbols(method.name())) {
          if (member instanceof Symbol.MethodSymbol memberMethod
            && memberMethod.overriddenSymbols().stream().anyMatch(s -> s == method)) {
            return memberMethod.declaration();
          }
        }
        Type superType = currentClass.superClass();
        currentClass = superType != null ? superType.symbol() : null;
      }
      return null;
    }

    public boolean isSymbolUsed() {
      return symbolUsed;
    }
  }
}
