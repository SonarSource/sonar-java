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
package org.sonar.java.checks.aws;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.MethodMatchers;

@Rule(key = "S6241")
public class AwsRegionShouldBeSetExplicitlyCheck extends AwsBuilderMethodFinder {
  private static final MethodMatchers REGION_METHOD = MethodMatchers.create()
    .ofSubTypes(SDK_CLIENT_BUILDER_TYPE)
    .names("region")
    .addParametersMatcher("software.amazon.awssdk.regions.Region")
    .build();

  @Override
  protected MethodMatchers getTargetMethod() {
    return REGION_METHOD;
  }

  @Override
  String getIssueMessage() {
    return "Set the region explicitly on this builder.";
  }
}
