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
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2116")
public class ArrayHashCodeAndToStringCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofType(Type::isArray)
      .names("toString", "hashCode")
      .addWithoutParametersMatcher()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    reportIssue(ExpressionUtils.methodName(mit), "Use \"Arrays." + methodName + "(array)\" instead.");
  }
}
