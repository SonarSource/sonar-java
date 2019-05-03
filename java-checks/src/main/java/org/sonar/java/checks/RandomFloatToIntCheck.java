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

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2140")
public class RandomFloatToIntCheck extends IssuableSubscriptionVisitor {

  private final MethodMatcherCollection methodMatchers  = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("java.util.Random").name("nextDouble").withoutParameter(),
    MethodMatcher.create().typeDefinition("java.util.Random").name("nextFloat").withoutParameter(),
    MethodMatcher.create().typeDefinition("java.lang.Math").name("random").withoutParameter()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeCastTree castTree = (TypeCastTree) tree;
    if(castTree.type().symbolType().is("int")) {
      castTree.expression().accept(new RandomDoubleVisitor());
    }
  }

  private class RandomDoubleVisitor extends BaseTreeVisitor {

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if(methodMatchers.anyMatch(tree)) {
        reportIssue(tree.methodSelect(), "Use \"java.util.Random.nextInt()\" instead.");
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
