/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S4512")
public class PopulateBeansCheck extends AbstractMethodDetection {

  private static final MethodMatchers METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("org.apache.commons.beanutils.BeanUtils", "org.apache.commons.beanutils.BeanUtilsBean")
      .names("populate", "setProperty")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("org.springframework.beans.PropertyAccessor")
      .names("setPropertyValue", "setPropertyValues")
      .withAnyParameters()
      .build());

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return METHOD_MATCHERS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(mit, "Make sure that setting JavaBean properties is safe here.");
  }

}
