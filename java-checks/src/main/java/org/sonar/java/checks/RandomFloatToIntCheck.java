/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S2140")
public class RandomFloatToIntCheck extends IssuableSubscriptionVisitor {

  private static final String NEXT_FLOAT = "nextFloat";
  private static final String NEXT_DOUBLE = "nextDouble";

  private static final MethodMatchers MATH_RANDOM_METHOD_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.Math").names("random").addWithoutParametersMatcher().build();

  private static final MethodMatchers METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.util.concurrent.ThreadLocalRandom")
      .names(NEXT_DOUBLE)
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes(
        "java.util.Random",
        "org.apache.commons.lang.math.JVMRandom",
        "org.apache.commons.lang.math.RandomUtils",
        "org.apache.commons.lang3.RandomUtils")
      .names(NEXT_DOUBLE, NEXT_FLOAT)
      .addWithoutParametersMatcher()
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeCastTree castTree = (TypeCastTree) tree;
    Type castToType = castTree.type().symbolType();
    if (castToType.is("int")) {
      castTree.expression().accept(new RandomDoubleVisitor("nextInt()"));
    } else if (castToType.is("long")) {
      castTree.expression().accept(new RandomDoubleVisitor("nextLong()"));
    }
  }

  private class RandomDoubleVisitor extends BaseTreeVisitor {
    private final String methodToCall;

    public RandomDoubleVisitor(String methodToCall){
      this.methodToCall = methodToCall;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (MATH_RANDOM_METHOD_MATCHER.matches(tree)) {
        reportIssue(tree.methodSelect(), "Use \"java.util.Random."+methodToCall+"\" instead.");
      } else if (METHOD_MATCHERS.matches(tree)) {
        reportIssue(tree.methodSelect(), "Use \""+methodToCall+"\" instead.");
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      scan(tree.enclosingExpression());
      scan(tree.identifier());
      scan(tree.typeArguments());
      scan(tree.arguments());
      //do not scan body of anonymous classes.
    }
  }
}
