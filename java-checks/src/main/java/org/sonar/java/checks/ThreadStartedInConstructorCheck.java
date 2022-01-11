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
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2693")
public class ThreadStartedInConstructorCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers THREAD_START = MethodMatchers.create()
    .ofSubTypes("java.lang.Thread")
    .names("start")
    .addWithoutParametersMatcher()
    .build();

  private final Deque<Boolean> inMethodOrStaticInitializerOrFinalClass = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.RECORD, Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION, Tree.Kind.STATIC_INITIALIZER);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS, Tree.Kind.RECORD)) {
      inMethodOrStaticInitializerOrFinalClass.push(((ClassTree) tree).symbol().isFinal());
    } else if (tree.is(Tree.Kind.METHOD, Tree.Kind.STATIC_INITIALIZER)) {
      inMethodOrStaticInitializerOrFinalClass.push(Boolean.TRUE);
    } else if (BooleanUtils.isFalse(inMethodOrStaticInitializerOrFinalClass.peek()) && THREAD_START.matches((MethodInvocationTree) tree)) {
      reportIssue(ExpressionUtils.methodName((MethodInvocationTree) tree), "Move this \"start\" call to another method.");
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS, Tree.Kind.METHOD, Tree.Kind.STATIC_INITIALIZER)) {
      inMethodOrStaticInitializerOrFinalClass.pop();
    }
  }

}
