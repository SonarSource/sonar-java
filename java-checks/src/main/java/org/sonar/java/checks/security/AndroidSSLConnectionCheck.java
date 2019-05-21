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
package org.sonar.java.checks.security;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import java.util.Collections;
import java.util.List;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5326")
public class AndroidSSLConnectionCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  private static final String MESSAGE = "Make sure that SSL/TLS connections are validated safely here.";

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      MethodTree methodTree = (MethodTree) tree;
      List<VariableTree> parameters = methodTree.parameters();
      if (methodTree.simpleName().name().equals("onReceivedSslError") &&
        !parameters.isEmpty() &&
        parameters.get(0).symbol().type().is("android.webkit.WebView")) {
        reportIssue(methodTree.simpleName(), MESSAGE);
      }
    }
  }
}
