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
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(
  key = "S1481",
  priority = Priority.MAJOR,
  tags = {"unused"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class UnusedLocalVariableCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(
      Tree.Kind.BLOCK, Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.BLOCK)) {
      BlockTree blockTree = (BlockTree) tree;
      checkVariables(blockTree.body());
    } else if (tree.is(Tree.Kind.FOR_STATEMENT)) {
      ForStatementTree forStatementTree = (ForStatementTree) tree;
      checkVariables(forStatementTree.initializer());
    } else if (tree.is(Tree.Kind.FOR_EACH_STATEMENT)) {
      ForEachStatement forEachStatement = (ForEachStatement) tree;
      checkVariable(forEachStatement.variable());
    } else if (tree.is(Tree.Kind.TRY_STATEMENT)) {
      TryStatementTree tryStatementTree = (TryStatementTree) tree;
      for (VariableTree resource : tryStatementTree.resources()) {
        checkVariable(resource);
      }
    }
  }

  public void checkVariables(List<StatementTree> statementTrees) {
    for (StatementTree statementTree : statementTrees) {
      if (statementTree.is(Tree.Kind.VARIABLE)) {
        checkVariable((VariableTree) statementTree);
      }
    }
  }

  private void checkVariable(VariableTree variableTree) {
    Symbol symbol = getSemanticModel().getSymbol(variableTree);
    if (getSemanticModel().getUsages(symbol).isEmpty()) {
      addIssue(variableTree, "Remove this unused \"" + variableTree.simpleName() + "\" local variable.");
    }
  }

}
