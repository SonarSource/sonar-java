/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.LinkedList;

@Rule(
  key = "S1143",
  name = "\"return\" statements should not occur in \"finally\" blocks",
  priority = Priority.BLOCKER,
  tags = {Tag.BUG, Tag.CWE, Tag.ERROR_HANDLING})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("30min")
public class ReturnInFinallyCheck extends BaseTreeVisitor implements JavaFileScanner{

  private final Deque<Boolean> isInFinally = new LinkedList<>();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    isInFinally.clear();
    scan(context.getTree());
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    scan(tree.resources());
    scan(tree.block());
    scan(tree.catches());
    if(tree.finallyBlock()!=null) {
      isInFinally.push(true);
      scan(tree.finallyBlock());
      isInFinally.pop();
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    isInFinally.push(false);
    super.visitMethod(tree);
    isInFinally.pop();
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    if(isInFinally()) {
      context.addIssue(tree, this, "Remove this return statement from this finally block.");
    }
    super.visitReturnStatement(tree);
  }

  private boolean isInFinally() {
    return !isInFinally.isEmpty() && isInFinally.peek();
  }


}
