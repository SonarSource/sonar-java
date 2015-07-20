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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1695",
  name = "\"NullPointerException\" should not be explicitly thrown",
  tags = {"pitfall"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("10min")
public class NPEThrowCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.THROW_STATEMENT, Kind.METHOD, Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Kind.THROW_STATEMENT)) {
        ExpressionTree expressionTree = ((ThrowStatementTree) tree).expression();
        raiseIssueOnNpe(expressionTree, expressionTree.symbolType());
      } else {
        for (TypeTree throwClause : ((MethodTree) tree).throwsClauses()) {
          raiseIssueOnNpe(throwClause, throwClause.symbolType());
        }
      }
    }
  }

  private void raiseIssueOnNpe(Tree tree, Type type) {
    if (type.is("java.lang.NullPointerException")) {
      addIssue(treeAtFault(tree), "Throw some other exception here, such as \"IllegalArgumentException\".");
    }
  }

  private static Tree treeAtFault(Tree tree) {
    return tree.is(Kind.NEW_CLASS) ? ((NewClassTree) tree).identifier() : tree;
  }

}
