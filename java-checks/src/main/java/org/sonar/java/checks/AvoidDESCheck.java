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

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2278")
public class AvoidDESCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("javax.crypto.Cipher").names("getInstance").withAnyParameters().build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArg = mit.arguments().get(0);
    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(firstArg);
    if (defaultPropertyValue == null) {
      defaultPropertyValue = firstArg;
    }
    if (defaultPropertyValue.is(Tree.Kind.STRING_LITERAL)) {
      checkIssue(firstArg, (LiteralTree) defaultPropertyValue);
    }
  }

  private void checkIssue(ExpressionTree argumentForReport, LiteralTree argument) {
    String[] transformationElements = LiteralUtils.trimQuotes(argument.value()).split("/");
    if (transformationElements.length > 0 && isExcludedAlgorithm(transformationElements[0])) {
      reportIssue(argumentForReport, "Use the recommended AES (Advanced Encryption Standard) instead.");
    }
  }

  private static boolean isExcludedAlgorithm(String algorithm) {
    return "DES".equals(algorithm)
      || "DESede".equals(algorithm)
      || "RC2".equals(algorithm);
  }

}
