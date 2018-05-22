/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.security;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3329")
public class CipherBlockChainingCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition("javax.crypto.spec.IvParameterSpec").name("<init>").withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Tree mTree = findEnclosingMethod(newClassTree);
    if (mTree != null) {
      MethodInvocationVisitor mitVisit = new MethodInvocationVisitor();
      mTree.accept(mitVisit);
      if (!mitVisit.secureRandomFound) {
        reportIssue(newClassTree, "Use a dynamically-generated, random IV.");
      }
    }
  }

  private static MethodTree findEnclosingMethod(Tree tree) {
    while (!tree.is(Tree.Kind.CLASS, Tree.Kind.METHOD)) {
      tree = tree.parent();
    }
    if (tree.is(Tree.Kind.CLASS)) {
      return null;
    }
    return (MethodTree) tree;
  }

  private static class MethodInvocationVisitor extends BaseTreeVisitor {

    private boolean secureRandomFound = false;

    private static final MethodMatcher SECURE_RANDOM_NEXT_BYTES = MethodMatcher.create()
      .typeDefinition("java.security.SecureRandom")
      .name("nextBytes")
      .withAnyParameters();

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (SECURE_RANDOM_NEXT_BYTES.matches(methodInvocation)) {
        secureRandomFound = true;
      }
      super.visitMethodInvocation(methodInvocation);
    }
  }
}
