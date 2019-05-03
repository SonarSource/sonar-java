/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1941")
public class VariableDeclarationScopeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    BlockTree block = (BlockTree) tree;
    List<StatementTree> body = block.body();
    int bodySize = body.size();
    for (int i = 0; i < bodySize; i++) {
      StatementTree statement = body.get(i);
      if (statement.is(Kind.VARIABLE)) {
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
      if (referenceVisitor.referenceSymbol) {
        return;
      } else if (referenceVisitor.hasBreakingStatement) {
        reportIssue(variable.simpleName(), "Move the declaration of \"" + symbol.name() + "\" closer to the code that uses it.");
        return;
      }
    }
  }

  private static class ReferenceVisitor extends BaseTreeVisitor {
    Symbol symbol;
    boolean referenceSymbol;
    boolean hasBreakingStatement;

    ReferenceVisitor(Symbol symbol) {
      this.symbol = symbol;
    }

    void visit(StatementTree node) {
      referenceSymbol = false;
      hasBreakingStatement = false;
      node.accept(this);
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      if (!hasBreakingStatement) {
        hasBreakingStatement = true;
      }
      super.visitReturnStatement(tree);
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      if (!hasBreakingStatement) {
        hasBreakingStatement = true;
      }
      super.visitThrowStatement(tree);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (!referenceSymbol && symbol.equals(tree.symbol())) {
        referenceSymbol = true;
      }
      super.visitIdentifier(tree);
    }
  }
}
