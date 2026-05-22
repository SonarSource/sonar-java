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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.TypeUtils.isValueBasedType;

@Rule(key = "S8696")
public class ValueBasedObjectIdentityCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String EQUALS_MESSAGE = "Use \".equals()\" instead of \"==\" or \"!=\" to compare objects of value-based types.";
  private static final String HASHCODE_MESSAGE = "Use \".hashCode()\" instead of \"System.identityHashCode()\" to compute the hash code for objects of value-based types.";

  private static final MethodMatchers IDENTITY_HASHCODE_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.System")
    .names("identityHashCode")
    .addParametersMatcher("java.lang.Object")
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof BinaryExpressionTree binaryExpr &&
      isValueBasedTypeExcludingPrimitiveWrappers(binaryExpr.leftOperand().symbolType()) &&
      isValueBasedTypeExcludingPrimitiveWrappers(binaryExpr.rightOperand().symbolType())) {
      reportIssue(binaryExpr, EQUALS_MESSAGE);
    } else if (tree instanceof MethodInvocationTree methodInvocation &&
      IDENTITY_HASHCODE_MATCHER.matches(methodInvocation) &&
      isValueBasedType(methodInvocation.arguments().get(0).symbolType())) {
      reportIssue(methodInvocation, HASHCODE_MESSAGE);
    }
  }

  private static boolean isValueBasedTypeExcludingPrimitiveWrappers(Type type) {
    // We need to exclude wrappers for primitive classes to avoid duplicating issues with S4973.
    return !type.isPrimitiveWrapper() && isValueBasedType(type);
  }

}
