/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S134",
  name = "Control flow statements \"if\", \"for\", \"while\", \"switch\" and \"try\" should not be nested too deeply",
  tags = {"brain-overload"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_CHANGEABILITY)
@SqaleConstantRemediation("10min")
public class NestedIfStatementsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX,
  description = "Maximum allowed control flow statement nesting depth.")
  public int max = DEFAULT_MAX;

  private JavaFileScannerContext context;
  private int nestingLevel;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    this.nestingLevel = 0;
    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    nestingLevel++;
    checkNesting(tree);
    visit(tree);
    nestingLevel--;
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    nestingLevel++;
    checkNesting(tree);
    super.visitForStatement(tree);
    nestingLevel--;
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    nestingLevel++;
    checkNesting(tree);
    super.visitForEachStatement(tree);
    nestingLevel--;
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    nestingLevel++;
    checkNesting(tree);
    super.visitWhileStatement(tree);
    nestingLevel--;
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    nestingLevel++;
    checkNesting(tree);
    super.visitDoWhileStatement(tree);
    nestingLevel--;
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    nestingLevel++;
    checkNesting(tree);
    super.visitSwitchStatement(tree);
    nestingLevel--;
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    nestingLevel++;
    checkNesting(tree);
    scan(tree.block());
    nestingLevel--;
    scan(tree.resources());
    scan(tree.catches());
    scan(tree.finallyBlock());
  }

  private void checkNesting(Tree tree) {
    if (nestingLevel == max + 1) {
      context.addIssue(tree, this, "Refactor this code to not nest more than " + max + " if/for/while/switch/try statements.");
    }
  }

  private void visit(IfStatementTree tree) {
    scan(tree.condition());
    scan(tree.thenStatement());

    StatementTree elseStatementTree = tree.elseStatement();
    if (elseStatementTree != null && elseStatementTree.is(Tree.Kind.IF_STATEMENT)) {
      visit((IfStatementTree) elseStatementTree);
    } else {
      scan(elseStatementTree);
    }
  }

}
