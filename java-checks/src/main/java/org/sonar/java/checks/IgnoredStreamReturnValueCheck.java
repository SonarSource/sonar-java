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
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2674",
  name = "The value returned from a stream read should be checked",
  priority = Priority.BLOCKER,
  tags = {Tag.BUG})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("15min")
public class IgnoredStreamReturnValueCheck extends SubscriptionBaseVisitor {

  private static final MethodInvocationMatcherCollection MATCHERS = MethodInvocationMatcherCollection.create(
    inputStreamInvocationMatcher("skip", "long"),
    inputStreamInvocationMatcher("read", "byte[]"));

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ExpressionTree statement = ((ExpressionStatementTree) tree).expression();
    if (statement.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) statement;
      if (MATCHERS.anyMatch(mit)) {
        addIssue(mit, "Check the return value of the \"" + mit.symbol().name() + "\" call to see how many bytes were read.");
      }
    }
  }

  private static MethodMatcher inputStreamInvocationMatcher(String methodName, String parameterType) {
    return MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.io.InputStream"))
      .name(methodName)
      .addParameter(parameterType);
  }
}
