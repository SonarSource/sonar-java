/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S3011")
public class AccessibilityChangeCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_REFLECT_FIELD = "java.lang.reflect.Field";
  private static final MethodMatchers METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create().ofSubTypes("java.lang.reflect.AccessibleObject").names("setAccessible").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("set").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setBoolean").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setByte").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setChar").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setDouble").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setFloat").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setInt").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setLong").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setShort").withAnyParameters().build()
  );

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return METHOD_MATCHERS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if(mit.symbol().name().equals("setAccessible")) {
      checkAccessibilityUpdate(mit);
    } else {
      reportIssue(mit, "Make sure that this accessibility bypass is safe here.");
    }
  }

  private void checkAccessibilityUpdate(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    ExpressionTree arg = arguments.get(0);
    if (arguments.size() > 1) {
      arg = arguments.get(1);
    }
    if (Boolean.TRUE.equals(ExpressionsHelper.getConstantValueAsBoolean(arg).value())) {
      reportIssue(mit, "Make sure that this accessibility update is safe here.");
    }
  }
}
