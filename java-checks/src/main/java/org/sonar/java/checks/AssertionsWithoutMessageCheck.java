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
import org.sonar.java.checks.methods.NameCriteria;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2698",
  name = "Literal boolean values should not be used in assertions",
  tags = {"junit"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class AssertionsWithoutMessageCheck extends AbstractMethodDetection {

  private static final String GENERIC_ASSERT = "org.fest.assertions.GenericAssert";
  private static final MethodInvocationMatcher FEST_AS_METHOD = MethodInvocationMatcher.create()
        .typeDefinition(GENERIC_ASSERT).name("as").addParameter("java.lang.String");

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return Lists.newArrayList(
        MethodInvocationMatcher.create().typeDefinition("org.junit.Assert").name(NameCriteria.startsWith("assert")).withNoParameterConstraint(),
        MethodInvocationMatcher.create().typeDefinition("junit.framework.Assert").name(NameCriteria.startsWith("assert")).withNoParameterConstraint(),
        MethodInvocationMatcher.create().typeDefinition(TypeCriteria.subtypeOf(GENERIC_ASSERT)).name(NameCriteria.any()).withNoParameterConstraint()
    );
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    if(mit.symbol().owner().type().isSubtypeOf(GENERIC_ASSERT) && !FEST_AS_METHOD.matches(mit)) {
      FestVisitor visitor = new FestVisitor();
      mit.methodSelect().accept(visitor);
      if(!visitor.useDescription) {
        addIssue(mit, "Add a message to this assertion.");
      }
    } else {
      if(!isString(mit.arguments().get(0)) || isAssertEqualsOnString(mit)) {
        addIssue(mit, "Add a message to this assertion.");
      }
    }
  }

  private boolean isAssertEqualsOnString(MethodInvocationTree mit) {
    if("assertEquals".equals(mit.symbol().name())) {
      return mit.arguments().size() == 2 && isString(mit.arguments().get(0)) && isString(mit.arguments().get(1));
    }
    return false;
  }

  private boolean isString(ExpressionTree expressionTree) {
    return expressionTree.symbolType().is("java.lang.String");
  }

  private static class FestVisitor extends BaseTreeVisitor {
    boolean useDescription = false;
    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      useDescription |= FEST_AS_METHOD.matches(tree);
      super.visitMethodInvocation(tree);
    }

  }


}
