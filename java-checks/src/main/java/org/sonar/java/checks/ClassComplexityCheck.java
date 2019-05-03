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
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "ClassCyclomaticComplexity")
@RspecKey("S1311")
public class ClassComplexityCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 200;

  @RuleProperty(
    description = "Maximum complexity allowed.",
    defaultValue = "" + DEFAULT_MAX)
  private int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    int size = context.getComplexityNodes(tree).size();
    if (size > max) {
      ClassTree classTree = (ClassTree) tree;
      Tree report = classTree.simpleName() == null ? classTree.openBraceToken() : classTree.simpleName();
      reportIssue(
        report,
        "The Cyclomatic Complexity of this class is " + size + " which is greater than " + max + " authorized.",
        Collections.emptyList(),
        size - max);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

}
