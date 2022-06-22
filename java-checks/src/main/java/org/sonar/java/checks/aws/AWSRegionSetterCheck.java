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
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

// TODO rename to AwsXxxx
@Rule(key = "S6262")
public class AWSRegionSetterCheck extends AbstractMethodDetection {

  private static final String STRING_TYPE = "java.lang.String";
  private static final String MESSAGE = "Use an Enum not a String to set the region.";

  private static final MethodMatchers REGION_SETTER_MATCHER = MethodMatchers.create()
    .ofTypes("com.amazonaws.services.s3.AmazonS3ClientBuilder")
    .names("withRegion")
    .addParametersMatcher(STRING_TYPE)
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return REGION_SETTER_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    process(tree.arguments());
  }

  private void process(Arguments arguments) {
    // TODO assert that argument size == 1
    if (arguments.isEmpty()) {
      return;
    }
    ExpressionTree firstArgument = arguments.get(0);
    processArgument(firstArgument);
  }

  private void processArgument(ExpressionTree argument) {
    switch (argument.kind()) {
      case STRING_LITERAL:
        reportIssue(argument, MESSAGE);
        break;
      case IDENTIFIER:
        if (((IdentifierTree) argument).symbol().type().is(STRING_TYPE)) {
          reportIssue(argument, MESSAGE);
        }
        break;
      default:
        break;
    }
  }

}
