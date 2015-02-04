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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2116",
  name = "\"hashCode\" and \"toString\" should not be called on array instances",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation(value = "5min")
public class ArrayHashCodeAndToStringCheck extends AbstractMethodDetection {

  private static final TypeCriteria IS_ARRAY = new IsArrayCriteria();

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      arrayMethodInvocation("toString"),
      arrayMethodInvocation("hashCode"));
  }

  private MethodInvocationMatcher arrayMethodInvocation(String methodName) {
    return MethodInvocationMatcher.create()
      .callSite(IS_ARRAY)
      .name(methodName);
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    MethodInvocationTreeImpl methodInvocationTreeImpl = (MethodInvocationTreeImpl) mit;
    String methodName = methodInvocationTreeImpl.getSymbol().getName();
    addIssue(mit, "Use \"Arrays." + methodName + "(array)\" instead.");
  }

  private static class IsArrayCriteria extends TypeCriteria {

    @Override
    public boolean matches(Type type) {
      return type.isTagged(Type.ARRAY);
    }

  }

}
