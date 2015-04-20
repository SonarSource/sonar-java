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

import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2701",
  name = "Literal boolean values should not be used in assertions",
  tags = {"junit"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class BooleanLiteralInAssertionsCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return Lists.newArrayList(
      MethodInvocationMatcher.create().typeDefinition("org.junit.Assert").name("assertTrue").withNoParameterConstraint(),
      MethodInvocationMatcher.create().typeDefinition("org.junit.Assert").name("assertFalse").withNoParameterConstraint(),
      MethodInvocationMatcher.create().typeDefinition("junit.framework.Assert").name("assertTrue").withNoParameterConstraint(),
      MethodInvocationMatcher.create().typeDefinition("junit.framework.Assert").name("assertFalse").withNoParameterConstraint()
      );
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    int arity = mit.arguments().size();
    if (arity == 1 || arity == 2) {
      ExpressionTree booleanArg = mit.arguments().get(arity - 1);
      if (booleanArg.is(Tree.Kind.BOOLEAN_LITERAL)) {
        raiseIssue(booleanArg);
      }
    }
  }

  private void raiseIssue(ExpressionTree expressionTree) {
    addIssue(expressionTree, "Remove or correct this assertion.");
  }
}
