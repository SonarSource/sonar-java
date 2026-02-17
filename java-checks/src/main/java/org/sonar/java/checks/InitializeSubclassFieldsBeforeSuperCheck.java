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

  private static boolean isFieldAssignment(AssignmentExpressionTree tree) {
    return tree.variable() instanceof MemberSelectExpressionTree mseTree
      && mseTree.expression() instanceof IdentifierTree idTree
      && THIS.getValue().equals(idTree.name());
  }


  private static boolean isFieldUsedInBlock(@Nullable BlockTree methodBlock, Symbol symbol, @Nullable Symbol.TypeSymbol childClass) {
    if (methodBlock == null) {
      // Can't resolve body, conservatively assume field may be used.
      return true;
    }
    SymbolUsedVisitor symbolUsedVisitor = new SymbolUsedVisitor(symbol, childClass);
    methodBlock.body().forEach(statement -> statement.accept(symbolUsedVisitor));
    return symbolUsedVisitor.isSymbolUsed();
  }

  private static boolean isFieldUsedInMethod(MethodTree mt, Symbol symbol, @Nullable Symbol.TypeSymbol childClass) {
    return isFieldUsedInBlock(mt.block(), symbol, childClass);
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

  private static class SymbolUsedVisitor extends StatementVisitor {
    private boolean symbolUsed = false;
    private final Symbol symbol;
    @Nullable
    private final Symbol.TypeSymbol childClass;

    private SymbolUsedVisitor(Symbol symbol, @Nullable Symbol.TypeSymbol childClass) {
      this.symbol = symbol;
      this.childClass = childClass;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      symbolUsed |= tree.symbol() == symbol;
      super.visitIdentifier(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol.MethodSymbol calledMethod = tree.methodSymbol();
      MethodTree childOverride = findChildOverride(childClass, calledMethod);
      if (childOverride != null) {
        symbolUsed |= isFieldUsedInMethod(childOverride, symbol, childClass);
      } else {
        var methodDeclaration = calledMethod.declaration();
        if (methodDeclaration != null) {
          symbolUsed |= isFieldUsedInMethod(methodDeclaration, symbol, childClass);
        }
      }
      super.visitMethodInvocation(tree);
    }

    @Nullable
    private static MethodTree findChildOverride(@Nullable Symbol.TypeSymbol childClass, Symbol.MethodSymbol method) {
      if (childClass == null) {
        return null;
      }
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
