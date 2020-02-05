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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S5547")
public class StrongCipherAlgorithmCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Use a strong cipher algorithm.";

  private static final Set<String> VULNERABLE_ALGORITHMS = Stream.of("DES", "DESede", "RC2", "RC4", "Blowfish")
    .map(name -> name.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition("javax.crypto.Cipher").name("getInstance").withAnyParameters(),
      MethodMatcher.create().typeDefinition("javax.crypto.NullCipher").name("<init>").withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree.identifier(), MESSAGE);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArg = mit.arguments().get(0);
    ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(firstArg);
    String firstArgStringValue = ExpressionsHelper.getConstantValueAsString(defaultValue != null ? defaultValue : firstArg).value();
    if (firstArgStringValue != null) {
      checkIssue(firstArg, firstArgStringValue);
    }
  }

  private void checkIssue(ExpressionTree argumentForReport, String algorithm) {
    String[] transformationElements = algorithm.split("/");
    if (transformationElements.length > 0 && VULNERABLE_ALGORITHMS.contains(transformationElements[0].toUpperCase(Locale.ROOT))) {
      reportIssue(argumentForReport, MESSAGE);
    }
  }

}
