/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1941")
public class VariableDeclarationScopeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    BlockTree block = (BlockTree) tree;
    List<StatementTree> body = block.body();
    int bodySize = body.size();
    for (int i = 0; i < bodySize; i++) {
      StatementTree statement = body.get(i);
      if (statement.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) statement;
        if (!variableTree.symbol().usages().isEmpty()) {
          check(variableTree, body, bodySize, i + 1);
        }
      }
    }
  }

  private void check(VariableTree variable, List<StatementTree> body, int bodySize, int next) {
    Symbol symbol = variable.symbol();
    ReferenceVisitor referenceVisitor = new ReferenceVisitor(symbol);
    for (int i = next; i < bodySize; i++) {
      referenceVisitor.visit(body.get(i));
      if (referenceVisitor.referencesSymbol) {
        return;
      }
      if (referenceVisitor.hasBreakingStatement) {
        reportIssue(variable.simpleName(), "Move the declaration of \"" + symbol.name() + "\" closer to the code that uses it.");
        return;
      }
    }
  }

  private static class ReferenceVisitor extends BaseTreeVisitor {
    private final Symbol symbol;
    boolean referencesSymbol;
    boolean hasBreakingStatement;

    ReferenceVisitor(Symbol symbol) {
      this.symbol = symbol;
    }

    void visit(StatementTree node) {
      referencesSymbol = false;
      hasBreakingStatement = false;
      node.accept(this);
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      hasBreakingStatement = true;
      super.visitReturnStatement(tree);
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      hasBreakingStatement = true;
      super.visitThrowStatement(tree);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      referencesSymbol |= symbol.equals(tree.symbol());
      super.visitIdentifier(tree);
    }
  }
}
