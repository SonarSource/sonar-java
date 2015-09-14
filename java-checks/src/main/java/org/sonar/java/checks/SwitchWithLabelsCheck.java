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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

import static org.sonar.plugins.java.api.tree.Tree.Kind.CASE_GROUP;
import static org.sonar.plugins.java.api.tree.Tree.Kind.LABELED_STATEMENT;

@Rule(
  key = "S1219",
  name = "\"switch\" statements should not contain non-case labels",
  tags = {"misra", "pitfall"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("10min")
public class SwitchWithLabelsCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(CASE_GROUP);
  }

  @Override
  public void visitNode(Tree tree) {
    CaseGroupTree cgt = (CaseGroupTree) tree;
    for (StatementTree statementTree : cgt.body()) {
      if (statementTree.is(LABELED_STATEMENT)) {
        LabeledStatementTree lst = (LabeledStatementTree) statementTree;
        addIssue(lst, "Remove this misleading \"" + lst.label().name() + "\" label.");
      }
    }
  }
}
