/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.aws;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.MethodMatchers;

@Rule(key = "S6241")
public class AwsRegionShouldBeSetExplicitlyCheck extends AwsBuilderMethodFinder {
  private static final MethodMatchers REGION_METHOD = MethodMatchers.create()
    .ofSubTypes(AWS_CLIENT_BUILDER_TYPE)
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
