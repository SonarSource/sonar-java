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

import com.google.common.collect.Iterables;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(
  key = SwitchCaseWithoutBreakCheck.RULE_KEY,
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class SwitchCaseWithoutBreakCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S128";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    scan(context.getTree());
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    super.visitCaseGroup(tree);

    if (tree.body().isEmpty() || !isBreakContinueReturnOrThrow(Iterables.getLast(tree.body()))) {
      context.addIssue(Iterables.getLast(tree.labels()), ruleKey, "End this switch case with an unconditional break, continue, return or throw statement.");
    }
  }

  private static boolean isBreakContinueReturnOrThrow(StatementTree tree) {
    return tree.is(Kind.BREAK_STATEMENT) ||
      tree.is(Kind.CONTINUE_STATEMENT) ||
      tree.is(Kind.RETURN_STATEMENT) ||
      tree.is(Kind.THROW_STATEMENT);
  }

}
