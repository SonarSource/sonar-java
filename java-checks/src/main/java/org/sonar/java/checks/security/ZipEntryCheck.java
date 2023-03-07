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
package org.sonar.java.checks.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5042")
public class ZipEntryCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Make sure that expanding this archive file is safe here.";

  private static final MethodMatchers SENSITIVE_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("java.util.zip.ZipFile")
      .names("entries")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.zip.ZipEntry")
      .names("getSize")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.zip.ZipInputStream")
      .names("getNextEntry")
      .addWithoutParametersMatcher()
      .build()
  );

  private static final MethodMatchers INPUT_STREAM_READ = MethodMatchers.create()
    .ofSubTypes("java.io.InputStream")
    .names("read")
    .withAnyParameters()
    .build();

  private boolean isSafe = false;

  private boolean insideMethod = false;

  private final List<MethodInvocationTree> calls = new ArrayList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      isSafe = false;
      calls.clear();
      insideMethod = true;
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (insideMethod && INPUT_STREAM_READ.matches(mit)) {
        isSafe = true;
      } else if (SENSITIVE_METHODS.matches(mit)) {
        if (insideMethod) {
          calls.add(mit);
        } else {
          report(mit);
        }
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      if (!isSafe) {
        for (MethodInvocationTree mit : calls) {
          report(mit);
        }
      }
      insideMethod = false;
    }
  }

  private void report(MethodInvocationTree mit) {
    reportIssue(ExpressionUtils.methodName(mit), ISSUE_MESSAGE);
  }
}
