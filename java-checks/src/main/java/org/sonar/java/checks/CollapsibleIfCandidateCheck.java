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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

import java.util.ArrayDeque;
import java.util.Deque;

@Rule(
  key = CollapsibleIfCandidateCheck.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CollapsibleIfCandidateCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1066";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;
  private Deque<Boolean> outerIf = new ArrayDeque<Boolean>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {

    if (!outerIf.isEmpty() && !hasElseClause(tree)) {
      context.addIssue(tree, ruleKey, "Merge this if statement with the enclosing one.");
    }

    if (!hasElseClause(tree) && hasBodySingleIfStatement(tree.thenStatement())) {
      // children of this if statement are eligible for issues
      outerIf.push(Boolean.TRUE);
      // recurse into sub-tree
      super.visitIfStatement(tree);
      if (!outerIf.isEmpty()) {
        outerIf.pop();
      }
    } else {
      // direct children of this if statement not eligible for issues. Reset nesting count
      outerIf.clear();
      super.visitIfStatement(tree);
    }
  }

  private static boolean hasElseClause(IfStatementTree tree) {
    return tree.elseStatement() != null;
  }

  private static boolean hasBodySingleIfStatement(StatementTree thenStatement) {

    if (thenStatement.is(Tree.Kind.BLOCK)) {
      // thenStatement has curly braces. Let's see what's inside...
      BlockTree block = (BlockTree) thenStatement;
      return block.body().size() == 1 && block.body().get(0).is(Tree.Kind.IF_STATEMENT);
    } else if (thenStatement.is(Tree.Kind.IF_STATEMENT)) {
      // no curlys on thenStatement; it's a bare if statement
      return true;
    }

    return false;
  }
}
