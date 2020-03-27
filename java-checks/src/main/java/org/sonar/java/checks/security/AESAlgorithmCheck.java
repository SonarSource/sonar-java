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
package org.sonar.java.checks.security;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S4432")
public class AESAlgorithmCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
        .ofTypes("javax.crypto.Cipher")
        .names("getInstance")
        .withAnyParameters()
        .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.arguments().isEmpty()) {
      return;
    }
    ExpressionTree firstArgument = mit.arguments().get(0);
    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(firstArgument);
    ExpressionTree algorithmTree = defaultPropertyValue == null ? firstArgument : defaultPropertyValue;
    String algorithmName = ExpressionsHelper.getConstantValueAsString(algorithmTree).value();
    if (algorithmName != null && isInsecureAESAlgorithm(algorithmName)) {
      reportIssue(firstArgument, "Use Galois/Counter Mode (GCM/NoPadding) instead.");
    }
  }

  private static boolean isInsecureAESAlgorithm(String algorithmName) {
    return algorithmName.startsWith("AES/ECB/") || "AES/CBC/PKCS5Padding".equals(algorithmName);
  }
}
