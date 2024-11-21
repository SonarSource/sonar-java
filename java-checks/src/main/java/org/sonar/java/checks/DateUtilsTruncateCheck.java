/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2718")
public class DateUtilsTruncateCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final Set<String> FIRST_PARAMETER_TYPES = SetUtils.immutableSetOf(
    "java.util.Date", "java.util.Calendar", "java.lang.Object");

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("org.apache.commons.lang.time.DateUtils")
      .names("truncate")
      .addParametersMatcher(
        params -> params.size() == 2 &&
          FIRST_PARAMETER_TYPES.contains(params.get(0).fullyQualifiedName()) &&
          params.get(1).is("int"))
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(ExpressionUtils.methodName(mit), "Use \"ZonedDateTime.truncatedTo\" instead." + context.getJavaVersion().java8CompatibilityMessage());
  }

}
