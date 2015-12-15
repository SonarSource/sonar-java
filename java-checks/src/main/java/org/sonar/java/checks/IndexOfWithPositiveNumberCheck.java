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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2692",
  name = "\"indexOf\" checks should not be for positive numbers",
  priority = Priority.CRITICAL,
  tags = {Tag.PITFALL})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("2min")
public class IndexOfWithPositiveNumberCheck extends SubscriptionBaseVisitor {

  private static final String INDEXOF = "indexOf";

  private static final MethodInvocationMatcherCollection CHECKED_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create()
      .typeDefinition(String.class.getName())
      .name(INDEXOF)
      .addParameter("int"),
    MethodMatcher.create()
      .typeDefinition(String.class.getName())
      .name(INDEXOF)
      .addParameter(String.class.getName()),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.List"))
      .name(INDEXOF)
      .addParameter("java.lang.Object")
    );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.GREATER_THAN, Tree.Kind.LESS_THAN);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryTree = (BinaryExpressionTree) tree;
    if (tree.is(Tree.Kind.GREATER_THAN)) {
      checkForIssue(tree, binaryTree.leftOperand(), LiteralUtils.longLiteralValue(binaryTree.rightOperand()));
    } else {
      checkForIssue(tree, binaryTree.rightOperand(), LiteralUtils.longLiteralValue(binaryTree.leftOperand()));
    }
  }

  private void checkForIssue(Tree tree, ExpressionTree operand, @Nullable Long constant) {
    if (constant != null && constant == 0 && isIndexOfOnArrayOrString(operand)) {
      addIssue(tree, "0 is a valid index, but is ignored by this check.");
    }
  }

  private static boolean isIndexOfOnArrayOrString(Tree tree) {
    return tree.is(Tree.Kind.METHOD_INVOCATION) && CHECKED_METHODS.anyMatch((MethodInvocationTree) tree);
  }

}
