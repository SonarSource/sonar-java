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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(
  key = "UselessParenthesesCheck",
  name = "Useless parentheses around expressions should be removed to prevent any misunderstanding",
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation(value = "1min")
public class UselessParenthesesCheck extends SubscriptionBaseVisitor {

  private final Deque<Tree> parent = new LinkedList<Tree>();
  private static final Kind[] PARENT_EXPRESSION =  {
      Kind.ANNOTATION,
      Kind.ARRAY_ACCESS_EXPRESSION,
      Kind.ASSERT_STATEMENT,
      Kind.CASE_LABEL,
      Kind.CONDITIONAL_EXPRESSION,
      Kind.DO_STATEMENT,
      Kind.EXPRESSION_STATEMENT,
      Kind.FOR_EACH_STATEMENT,
      Kind.FOR_STATEMENT,
      Kind.IF_STATEMENT,
      Kind.LAMBDA_EXPRESSION,
      Kind.METHOD_INVOCATION,
      Kind.METHOD,
      Kind.NEW_ARRAY,
      Kind.NEW_CLASS,
      Kind.PARENTHESIZED_EXPRESSION,
      Kind.RETURN_STATEMENT,
      Kind.SWITCH_STATEMENT,
      Kind.SYNCHRONIZED_STATEMENT,
      Kind.THROW_STATEMENT,
      Kind.VARIABLE,
      Kind.WHILE_STATEMENT
  };


  @Override
  public void scanFile(JavaFileScannerContext context) {
    parent.clear();
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if(tree.is(Kind.PARENTHESIZED_EXPRESSION) && hasParentExpression(tree)) {
      addIssue(tree, "Remove those useless parentheses.");
    }
    parent.push(tree);
  }

  private boolean hasParentExpression(Tree tree) {
    Tree parentTree = this.parent.peek();
    //Exclude condition of conditional expression
    if(parentTree.is(Kind.CONDITIONAL_EXPRESSION)) {
      ConditionalExpressionTree conditionalExpressionTree = (ConditionalExpressionTree) parentTree;
      return !(tree.equals(conditionalExpressionTree.condition()) || tree.equals(conditionalExpressionTree.falseExpression()));
    }
    //Exclude expression of array access expression
    if(parentTree.is(Kind.ARRAY_ACCESS_EXPRESSION) && tree.equals(((ArrayAccessExpressionTree) parentTree).expression()) ) {
      return false;
    }
    return parentTree.is(PARENT_EXPRESSION);
  }

  @Override
  public void leaveNode(Tree tree) {
    parent.pop();
  }


  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.values());
  }
}
