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

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2245")
public class PseudoRandomCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure that using this pseudorandom number generator is safe here.";
  private static final MethodMatcher MATH_RANDOM_MATCHER = MethodMatcher.create().typeDefinition("java.lang.Math").name("random").withoutParameter();

  private static final Set<String> RANDOM_STATIC_TYPES = ImmutableSet.of(
    "java.util.concurrent.ThreadLocalRandom",
    "org.apache.commons.lang.math.RandomUtils",
    "org.apache.commons.lang3.RandomUtils",
    "org.apache.commons.lang.RandomStringUtils",
    "org.apache.commons.lang3.RandomStringUtils"
  );

  private static final Set<String> RANDOM_CONSTRUCTOR_TYPES = ImmutableSet.of(
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

      if (MATH_RANDOM_MATCHER.matches(mit)) {
        reportIssue(reportLocation, MESSAGE);
      } else if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT) && !isChainedMethodInvocation(mit)) {
        Type expressionType = ((MemberSelectExpressionTree) mit.methodSelect()).expression().symbolType();
        if (RANDOM_STATIC_TYPES.contains(expressionType.fullyQualifiedName())) {
          reportIssue(reportLocation, MESSAGE);
        }
      }
    } else {
      NewClassTree newClass = (NewClassTree) tree;
      if (RANDOM_CONSTRUCTOR_TYPES.contains(newClass.symbolType().fullyQualifiedName())) {
        reportIssue(newClass.identifier(), MESSAGE);
      }
    }
  }

  private static boolean isChainedMethodInvocation(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    return parent != null && parent.is(Tree.Kind.MEMBER_SELECT);
  }

}
