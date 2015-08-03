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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S106",
  name = "Standard outputs should not be used directly to log anything",
  tags = {"bad-practice"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("10min")
public class SystemOutOrErrUsageCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.MEMBER_SELECT);
  }

  @Override
  public void visitNode(Tree tree) {
    MemberSelectExpressionTree mset = (MemberSelectExpressionTree) tree;
    if (isOutOrErr(mset) && isSystem(mset.expression())) {
      addIssue(tree, "Replace this usage of System.out or System.err by a logger.");
    }
  }

  private static boolean isSystem(ExpressionTree expression) {
    IdentifierTree identifierTree = null;
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      identifierTree = (IdentifierTree) expression;
    } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      identifierTree = ((MemberSelectExpressionTree) expression).identifier();
    }
    return identifierTree != null && "System".equals(identifierTree.name());
  }

  private static boolean isOutOrErr(MemberSelectExpressionTree mset) {
    return "out".equals(mset.identifier().name()) || "err".equals(mset.identifier().name());
  }
}
