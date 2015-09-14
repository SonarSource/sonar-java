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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.NameCriteria;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S2698",
  name = "JUnit assertions should include messages",
  tags = {"junit"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class AssertionsWithoutMessageCheck extends AbstractMethodDetection {

  private static final String GENERIC_ASSERT = "org.fest.assertions.GenericAssert";
  private static final MethodMatcher FEST_AS_METHOD = MethodMatcher.create()
        .typeDefinition(GENERIC_ASSERT).name("as").addParameter("java.lang.String");
  private static final Set<String> ASSERT_METHODS_WITH_ONE_PARAM = Sets.newHashSet("assertNull", "assertNotNull");
  private static final Set<String> ASSERT_METHODS_WITH_TWO_PARAMS = Sets.newHashSet("assertEquals", "assertSame", "assertNotSame", "assertThat");

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Lists.newArrayList(
        MethodMatcher.create().typeDefinition("org.junit.Assert").name(NameCriteria.startsWith("assert")).withNoParameterConstraint(),
        MethodMatcher.create().typeDefinition("org.junit.Assert").name("fail").withNoParameterConstraint(),
        MethodMatcher.create().typeDefinition("junit.framework.Assert").name(NameCriteria.startsWith("assert")).withNoParameterConstraint(),
        MethodMatcher.create().typeDefinition("junit.framework.Assert").name(NameCriteria.startsWith("fail")).withNoParameterConstraint(),
        MethodMatcher.create().typeDefinition("org.fest.assertions.Fail").name(NameCriteria.startsWith("fail")).withNoParameterConstraint(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(GENERIC_ASSERT)).name(NameCriteria.any()).withNoParameterConstraint()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if(mit.symbol().owner().type().isSubtypeOf(GENERIC_ASSERT) && !FEST_AS_METHOD.matches(mit)) {
      FestVisitor visitor = new FestVisitor();
      mit.methodSelect().accept(visitor);
      if(!visitor.useDescription) {
        addIssue(mit, "Add a message to this assertion.");
      }
    } else {
      if(mit.arguments().isEmpty() || !isString(mit.arguments().get(0)) || isAssertingOnStringWithNoMessage(mit)) {
        addIssue(mit, "Add a message to this assertion.");
      }
    }
  }

  private static boolean isAssertingOnStringWithNoMessage(MethodInvocationTree mit) {
    return isAssertWithTwoParams(mit) || isAssertWithOneParam(mit);
  }

  private static boolean isAssertWithOneParam(MethodInvocationTree mit) {
    return ASSERT_METHODS_WITH_ONE_PARAM.contains(mit.symbol().name()) && mit.arguments().size() == 1;
  }

  private static boolean isAssertWithTwoParams(MethodInvocationTree mit) {
    return ASSERT_METHODS_WITH_TWO_PARAMS.contains(mit.symbol().name()) && mit.arguments().size() == 2;
  }

  private static boolean isString(ExpressionTree expressionTree) {
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
