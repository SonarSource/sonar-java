/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2278",
  name = "Neither DES (Data Encryption Standard) nor DESede (3DES) should be used",
  tags = {"cwe", "owasp-a6", "security"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("20min")
public class AvoidDESCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(MethodMatcher.create().typeDefinition("javax.crypto.Cipher").name("getInstance").withNoParameterConstraint());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArg = mit.arguments().get(0);
    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(firstArg);
    if (defaultPropertyValue != null) {
      firstArg = defaultPropertyValue;
    }
    if (firstArg != null && firstArg.is(Tree.Kind.STRING_LITERAL)) {
      String algo = LiteralUtils.trimQuotes(((LiteralTree) firstArg).value());
      checkIssue(mit, algo);
    }
  }

  private void checkIssue(Tree tree, String algorithm) {
    String[] transformationElements = algorithm.split("/");
    if (transformationElements.length > 0 && isExcludedAlgorithm(transformationElements[0])) {
      addIssue(tree, "Use the recommended AES (Advanced Encryption Standard) instead.");
    }
  }

  private static boolean isExcludedAlgorithm(String algorithm) {
    return "DES".equals(algorithm) || "DESede".equals(algorithm);
  }

}
