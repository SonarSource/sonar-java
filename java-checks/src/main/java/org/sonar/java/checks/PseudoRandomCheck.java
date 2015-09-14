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
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2245",
  name = "Pseudorandom number generators (PRNGs) should not be used in secure contexts",
  tags = {"cert", "cwe", "owasp-a6", "security"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("10min")
public class PseudoRandomCheck extends SubscriptionBaseVisitor {

  private MethodMatcher methodInvocationMatcher = MethodMatcher.create().typeDefinition("java.lang.Math").name("random");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (isMathRandom(tree) || isJavaUtilRandom((ExpressionTree) tree)) {
      addIssue(tree, "Use a cryptographically strong random number generator (RNG) like \"java.security.SecureRandom\" in place of this PRNG");
    }
  }

  private boolean isMathRandom(Tree tree) {
    return tree.is(Tree.Kind.METHOD_INVOCATION) && hasSemantic() && methodInvocationMatcher.matches((MethodInvocationTree) tree);
  }

  private static boolean isJavaUtilRandom(ExpressionTree tree) {
    return tree.is(Tree.Kind.NEW_CLASS) && tree.symbolType().is("java.util.Random");
  }
}
