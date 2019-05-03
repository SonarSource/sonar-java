/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2245")
public class PseudoRandomCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure that using this pseudorandom number generator is safe here.";
  private static final MethodMatcher MATH_RANDOM_MATCHER = MethodMatcher.create().typeDefinition("java.lang.Math").name("random").withoutParameter();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (MATH_RANDOM_MATCHER.matches(mit)) {
        reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
      }
    } else {
      NewClassTree newClass = (NewClassTree) tree;
      if (newClass.symbolType().is("java.util.Random")) {
        reportIssue(newClass.identifier(), MESSAGE);
      }
    }
  }

}
