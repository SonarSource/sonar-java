/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import com.google.common.collect.Lists;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(
    key = "S1067",
    priority = Priority.MAJOR,
    tags = {"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ExpressionComplexityCheck extends SubscriptionBaseVisitor {


  private static final int DEFAULT_MAX = 3;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private final Deque<Integer> count = new LinkedList<Integer>();
  private final Deque<Integer> level = new LinkedList<Integer>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    count.clear();
    level.clear();
    level.push(0);
    count.push(0);
    super.scanFile(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    ImmutableList.Builder<Class<? extends Tree>> builder = ImmutableList.builder();
    builder.add(ArrayAccessExpressionTree.class);
    builder.add(ArrayTypeTree.class);
    builder.add(AssignmentExpressionTree.class);
    builder.add(BinaryExpressionTree.class);
    builder.add(ConditionalExpressionTree.class);
    builder.add(IdentifierTree.class);
    builder.add(InstanceOfTree.class);
    builder.add(LambdaExpressionTree.class);
    builder.add(LiteralTree.class);
    builder.add(MemberSelectExpressionTree.class);
    builder.add(MethodInvocationTree.class);
    builder.add(NewArrayTree.class);
    builder.add(NewClassTree.class);
    builder.add(JavaTree.ParameterizedTypeTreeImpl.class);
    builder.add(ParenthesizedTree.class);
    builder.add(PrimitiveTypeTree.class);
    builder.add(TypeCastTree.class);
    builder.add(UnaryExpressionTree.class);
    Collection<Tree.Kind> kinds = Lists.newArrayList(getKinds(builder.build()));
    return ImmutableList.<Tree.Kind>builder().addAll(kinds).add(Tree.Kind.CLASS).add(Tree.Kind.NEW_ARRAY).add(Tree.Kind.CONDITIONAL_EXPRESSION).build();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.NEW_ARRAY)) {
      count.push(0);
      level.push(0);
    } else {
      if (tree.is(Tree.Kind.CONDITIONAL_OR) || tree.is(Tree.Kind.CONDITIONAL_AND) || tree.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
        count.push(count.pop() + 1);
      }
      level.push(level.pop() + 1);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.NEW_ARRAY)) {
      count.pop();
      level.pop();
    } else {
      int currentLevel = level.peek();
      if (currentLevel == 1) {
        int opCount = count.pop();
        if (opCount > max) {
          addIssue(tree, "Reduce the number of conditional operators (" + opCount + ") used in the expression (maximum allowed " + max + ").");
        }
        count.push(0);
      }
      level.push(level.pop() - 1);
    }
  }

}
