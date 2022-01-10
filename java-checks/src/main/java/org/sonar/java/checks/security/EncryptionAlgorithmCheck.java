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
package org.sonar.java.checks.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5542")
public class EncryptionAlgorithmCheck extends AbstractMethodDetection {

  private static final Pattern ALGORITHM_PATTERN = Pattern.compile("([^/]+)/([^/]+)/([^/]+)");

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
    ExpressionTree algorithmTree = firstArgument;
    // Improve the review experience by helping to understand what is inside the first argument in case it's not hardcoded.
    List<JavaFileScannerContext.Location> transformationDefinition = new ArrayList<>();

    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(firstArgument);
    if (defaultPropertyValue != null) {
      algorithmTree = defaultPropertyValue;
      transformationDefinition.add(new JavaFileScannerContext.Location("Default transformation", defaultPropertyValue));
    } else if (firstArgument.is(Tree.Kind.IDENTIFIER)) {
      Tree declaration = ((IdentifierTree) firstArgument).symbol().declaration();
      if (declaration != null) {
        // We expect that most of the time, the identifier will directly lead to a constant, so this is already enough.
        // "getConstantValueAsString" can be smarter and rebuild a constant from more complex tree (concatenation, ...)
        // in this case, the secondary will not be perfect, but still a first step to understand easily the issue.
        transformationDefinition.add(new JavaFileScannerContext.Location("Transformation definition", declaration));
      }
    }
    String algorithmName = ExpressionsHelper.getConstantValueAsString(algorithmTree).value();
    if (algorithmName != null && isInsecureAlgorithm(algorithmName)) {
      reportIssue(firstArgument, "Use secure mode and padding scheme.", transformationDefinition, null);
    }
  }

  private static boolean isInsecureAlgorithm(String algorithmName) {
    Matcher matcher = ALGORITHM_PATTERN.matcher(algorithmName);
    if (matcher.matches()) {
      String algorithm = matcher.group(1);
      String mode = matcher.group(2);
      String padding = matcher.group(3);

      boolean isRSA = "RSA".equalsIgnoreCase(algorithm);

      if ("ECB".equalsIgnoreCase(mode) && !isRSA) {
        return true;
      }
      if ("CBC".equalsIgnoreCase(mode)) {
        return false;
      }

      return isRSA && !(padding.toUpperCase(Locale.ROOT).startsWith("OAEP"));
    }
    // By default, ECB is used.
    return true;
  }
}
