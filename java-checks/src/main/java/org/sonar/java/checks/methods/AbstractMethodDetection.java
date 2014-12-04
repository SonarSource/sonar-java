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
package org.sonar.java.checks.methods;

import com.google.common.collect.ImmutableList;
import org.sonar.java.checks.SubscriptionBaseVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public abstract class AbstractMethodDetection extends SubscriptionBaseVisitor {

  private List<MethodInvocationMatcher> matchers;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      for (MethodInvocationMatcher invocationMatcher : matchers()) {
        if (invocationMatcher.matches(mit, getSemanticModel())) {
          onMethodFound(mit);
        }
      }
    }
  }

  protected abstract List<MethodInvocationMatcher> getMethodInvocationMatchers();

  protected void onMethodFound(MethodInvocationTree mit) {
    //Do nothing by default
  }

  private List<MethodInvocationMatcher> matchers() {
    if (matchers == null) {
      matchers = getMethodInvocationMatchers();
    }
    return matchers;
  }
}
