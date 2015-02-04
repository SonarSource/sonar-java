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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = NestedTryCatchCheck.RULE_KEY,
  name = "Try-catch blocks should not be nested",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation(value = "20min")
public class NestedTryCatchCheck extends BaseTreeVisitor implements JavaFileScanner {


  public static final String RULE_KEY = "S1141";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private JavaFileScannerContext context;
  private int nestingLevel;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    nestingLevel = 0;
    scan(context.getTree());
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    scan(tree.resources());
    if (!tree.catches().isEmpty()) {
      nestingLevel++;
      if (nestingLevel > 1) {
        context.addIssue(tree.block(), ruleKey, "Extract this nested try block into a separate method.");
      }
    }
    scan(tree.block());
    if (!tree.catches().isEmpty()) {
      nestingLevel--;
    }
    scan(tree.catches());
    scan(tree.finallyBlock());
  }
}
