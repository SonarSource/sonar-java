/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S4434")
public class LDAPDeserializationCheck extends AbstractMethodDetection {
  private static final String CLASS_NAME = "javax.naming.directory.SearchControls";
  private static final int RET_OBJ_INDEX = 4;

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(CLASS_NAME)).name("<init>").withAnyParameters(),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(CLASS_NAME)).name("setReturningObjFlag").parameters("boolean"));
  }
  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (newClassTree.arguments().size() <= RET_OBJ_INDEX) {
      return;
    }
    ExpressionTree retObjArgument = newClassTree.arguments().get(RET_OBJ_INDEX);
    reportIfTrue(retObjArgument);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodTree) {
    ExpressionTree setValue = methodTree.arguments().get(0);
    reportIfTrue(setValue);
  }

  private void reportIfTrue(ExpressionTree toUnderline) {
    if (LiteralUtils.isTrue(toUnderline)) {
      reportIssue(toUnderline, "Disable object deserialization.");
    }
  }
}
