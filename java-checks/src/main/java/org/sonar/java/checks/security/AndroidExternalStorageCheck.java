/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.SetUtils;

@Rule(key = "S5324")
public class AndroidExternalStorageCheck extends IssuableSubscriptionVisitor {

  private static final String ANDROID_CONTENT_CONTEXT = "android.content.Context";

  private static final MethodMatchers SENSITIVE_METHODS_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("android.os.Environment")
      .names("getExternalStorageDirectory", "getExternalStoragePublicDirectory")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes(ANDROID_CONTENT_CONTEXT)
      .names("getExternalFilesDir", "getExternalFilesDirs", "getExternalMediaDirs", "getExternalCacheDir", "getExternalCacheDirs", "getObbDir", "getObbDirs")
      .withAnyParameters()
      .build()
  );

  private static final Set<String> SENSITIVE_FIELDS = SetUtils.immutableSetOf(
    "externalCacheDir",
    "externalCacheDirs",
    "externalMediaDirs",
    "obbDir",
    "obbDirs");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.MEMBER_SELECT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (SENSITIVE_METHODS_MATCHER.matches(mit)) {
        reportIssue(mit, "Make sure that external files are accessed safely here.");
      }
    } else {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree;
      IdentifierTree identifier = memberSelect.identifier();
      if (SENSITIVE_FIELDS.contains(identifier.name()) && memberSelect.expression().symbolType().is(ANDROID_CONTENT_CONTEXT)) {
        reportIssue(identifier, "Make sure that external files are accessed safely here.");
      }
    }
  }
}
