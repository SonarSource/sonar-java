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
package org.sonar.java.checks.methods;

import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractMethodDetection extends IssuableSubscriptionVisitor {

  private List<MethodMatcher> matchers;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_REFERENCE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      for (MethodMatcher invocationMatcher : matchers()) {
        checkInvocation(tree, invocationMatcher);
      }
    }
  }

  private void checkInvocation(Tree tree, MethodMatcher invocationMatcher) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (invocationMatcher.matches(mit)) {
        onMethodInvocationFound(mit);
      }
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) tree;
      if (invocationMatcher.matches(newClassTree)) {
        onConstructorFound(newClassTree);
      }
    } else if (tree.is(Tree.Kind.METHOD_REFERENCE)) {
      MethodReferenceTree methodReferenceTree = (MethodReferenceTree) tree;
      if (invocationMatcher.matches(methodReferenceTree)) {
        onMethodReferenceFound(methodReferenceTree);
      }
    }
  }

  protected abstract List<MethodMatcher> getMethodInvocationMatchers();

  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    // Do nothing by default
  }

  protected void onConstructorFound(NewClassTree newClassTree) {
    // Do nothing by default
  }

  protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
    // Do nothing by default
  }

  private List<MethodMatcher> matchers() {
    if (matchers == null) {
      matchers = getMethodInvocationMatchers();
    }
    return matchers;
  }
}
