/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S4512")
public class PopulateBeansCheck extends AbstractMethodDetection {

  private static final List<MethodMatcher> METHOD_MATCHERS = Arrays.asList(
    MethodMatcher.create().typeDefinition("org.apache.commons.beanutils.BeanUtils").name("populate").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.apache.commons.beanutils.BeanUtils").name("setProperty").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.apache.commons.beanutils.BeanUtilsBean").name("populate").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.apache.commons.beanutils.BeanUtilsBean").name("setProperty").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.springframework.beans.PropertyAccessor").name("setPropertyValue").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.springframework.beans.PropertyAccessor").name("setPropertyValues").withAnyParameters());

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return METHOD_MATCHERS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(mit, "Make sure that setting JavaBean properties is safe here.");
  }

}
