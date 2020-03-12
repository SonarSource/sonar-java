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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5542")
public class EncryptionAlgorithmCheck extends AbstractMethodDetection {

  private static final Pattern ALGORITHM_PATTERN = Pattern.compile("(.+)/(.+)/(.+)");

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(
      MethodMatcher.create()
        .typeDefinition("javax.crypto.Cipher")
        .name("getInstance")
        .withAnyParameters());
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
    if (algorithmName != null && isInsecureAlgorithm(algorithmName)) {
      reportIssue(firstArgument, "Use secure mode and padding scheme.");
    }
  }

  private static boolean isInsecureAlgorithm(String algorithmName) {
    Matcher matcher = ALGORITHM_PATTERN.matcher(algorithmName);
    if (matcher.matches()) {
      String algorithm = matcher.group(1);
      String mode = matcher.group(2);
      String padding = matcher.group(3);

      if ("ECB".equalsIgnoreCase(mode)) {
        return true;
      } else if ("CBC".equalsIgnoreCase(mode)) {
        return "PKCS5Padding".equalsIgnoreCase(padding) || "PKCS7Padding".equalsIgnoreCase(padding);
      }

      if ("RSA".equalsIgnoreCase(algorithm)) {
        return !(padding.toUpperCase(Locale.ROOT).startsWith("OAEP"));
      }
      return false;
    }
    // By default, ECB is used.
    return true;
  }
}
