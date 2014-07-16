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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(
  key = DuplicateConditionIfElseIfCheck.RULE_KEY,
  priority = Priority.CRITICAL,
  tags = {"bug", "unused"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class DuplicateConditionIfElseIfCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1862";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    ExpressionTree condition = tree.condition();

    StatementTree statement = tree.elseStatement();
    while (statement != null && statement.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatement = (IfStatementTree) statement;
      if (SyntacticEquivalence.areEquivalent(condition, ifStatement.condition())) {
        context.addIssue(
          ifStatement.condition(),
          ruleKey,
          "This branch can not be reached because the condition duplicates a previous condition in the same sequence of \"if/else if\" statements"
        );
      }
      statement = ifStatement.elseStatement();
    }

    super.visitIfStatement(tree);
  }

}
