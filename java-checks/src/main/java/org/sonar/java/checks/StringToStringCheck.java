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
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1858",
  name = "\"toString()\" should never be called on a String object",
  tags = {"clumsy", "pitfall"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class StringToStringCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(MethodMatcher.create()
      .typeDefinition("java.lang.String")
      .name("toString"));
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    ExpressionTree expressionTree = extractBaseExpression(((MemberSelectExpressionTree) tree.methodSelect()).expression());
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      addIssue(expressionTree, String.format("\"%s\" is already a string, there's no need to call \"toString()\" on it.",
        ((IdentifierTree) expressionTree).identifierToken().text()));
    } else if (expressionTree.is(Tree.Kind.STRING_LITERAL)) {
      addIssue(expressionTree, "there's no need to call \"toString()\" on a string literal.");
    } else if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      addIssue(expressionTree, String.format("\"%s\" returns a string, there's no need to call \"toString()\".",
        extractName(((MethodInvocationTree) expressionTree).methodSelect())));
    } else if (expressionTree.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
      String name = extractName(((ArrayAccessExpressionTree) expressionTree).expression());
      if(name.isEmpty()) {
        addIssue(expressionTree, "There's no need to call \"toString()\" on an array of String.");
      } else {
        addIssue(expressionTree, String.format("\"%s\" is an array of strings, there's no need to call \"toString()\".", name));
      }
    }
  }

  private static ExpressionTree extractBaseExpression(ExpressionTree tree) {
    ExpressionTree expressionTree = tree;
    while (true) {
      if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        expressionTree = ((MemberSelectExpressionTree) expressionTree).identifier();
      } else if (expressionTree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        expressionTree = ((ParenthesizedTree) expressionTree).expression();
      } else if (expressionTree.is(Tree.Kind.TYPE_CAST)) {
        expressionTree = ((TypeCastTree) expressionTree).expression();
      } else {
        return expressionTree;
      }
    }
  }

  private static String extractName(ExpressionTree tree) {
    ExpressionTree expressionTree = extractBaseExpression(tree);
    if(expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expressionTree).identifierToken().text();
    }
    return "";
  }

}
