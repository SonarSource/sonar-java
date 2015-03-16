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
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = NPEThrowCheck.RULE_KEY,
  name = "\"NullPointerException\" should not be explicitly thrown",
  tags = {"pitfall"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("10min")
public class NPEThrowCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1695";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    raiseIssueOnNpe((AbstractTypedTree) tree.expression());
    super.visitThrowStatement(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    for (TypeTree throwClause : tree.throwsClauses()) {
      raiseIssueOnNpe((AbstractTypedTree) throwClause);
    }
    super.visitMethod(tree);
  }

  private void raiseIssueOnNpe(AbstractTypedTree tree) {
    if (isNPE(tree)) {
      context.addIssue(treeAtFault(tree), ruleKey, "Throw some other exception here, such as \"IllegalArgumentException\".");
    }
  }

  private Tree treeAtFault(AbstractTypedTree tree) {
    return tree.is(Kind.NEW_CLASS) ? ((NewClassTree) tree).identifier() : tree;
  }

  private boolean isNPE(AbstractTypedTree tree) {
    if (tree.getSymbolType().isTagged(Type.CLASS)) {
      Type.ClassType type = (Type.ClassType) tree.getSymbolType();
      return "NullPointerException".equals(type.getSymbol().getName());
    }
    return false;
  }
}
