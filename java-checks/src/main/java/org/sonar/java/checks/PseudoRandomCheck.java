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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2245")
public class PseudoRandomCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure that using this pseudorandom number generator is safe here.";

  private static final MethodMatchers STATIC_RANDOM_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.lang.Math").names("random").addWithoutParametersMatcher().build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.concurrent.ThreadLocalRandom",
        "org.apache.commons.lang.math.RandomUtils",
        "org.apache.commons.lang3.RandomUtils",
        "org.apache.commons.lang.RandomStringUtils",
        "org.apache.commons.lang3.RandomStringUtils")
      .anyName()
      .withAnyParameters()
      .build()
  );

  private static final MethodMatchers RANDOM_STRING_UTILS_RANDOM_WITH_RANDOM_SOURCE = MethodMatchers.create()
    .ofSubTypes("org.apache.commons.lang.RandomStringUtils", "org.apache.commons.lang3.RandomStringUtils")
    .names("random")
    .addParametersMatcher("int", "int", "int", "boolean", "boolean", "char[]", "java.util.Random")
    .build();

  private static final Set<String> RANDOM_CONSTRUCTOR_TYPES = SetUtils.immutableSetOf(
    "java.util.Random",
    "org.apache.commons.lang.math.JVMRandom"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      IdentifierTree reportLocation = ExpressionUtils.methodName(mit);

      if (isStaticCallToInsecureRandomMethod(mit)) {
        reportIssue(reportLocation, MESSAGE);
      }
    } else {
      NewClassTree newClass = (NewClassTree) tree;
      if (RANDOM_CONSTRUCTOR_TYPES.contains(newClass.symbolType().fullyQualifiedName())) {
        reportIssue(newClass.identifier(), MESSAGE);
      }
    }
  }

  private static boolean isStaticCallToInsecureRandomMethod(MethodInvocationTree mit) {
    return STATIC_RANDOM_METHODS.matches(mit)
      && !RANDOM_STRING_UTILS_RANDOM_WITH_RANDOM_SOURCE.matches(mit)
      && mit.methodSymbol().isStatic();
  }

}
