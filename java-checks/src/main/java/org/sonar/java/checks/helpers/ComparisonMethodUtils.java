/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;

public final class ComparisonMethodUtils {

  private static final MethodMatchers COMPARE_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("java.lang.Comparable")
      .names("compareTo")
      .addParametersMatcher(MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.Comparator")
      .names("compare")
      .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
      .build());

  private ComparisonMethodUtils() {
  }

  public static boolean isCompareMethod(MethodTree methodTree) {
    return methodTree.block() != null && COMPARE_METHODS.matches(methodTree);
  }

  public static boolean isComparatorLambda(LambdaExpressionTree lambda) {
    return lambda.symbolType().isSubtypeOf("java.util.Comparator");
  }
}
