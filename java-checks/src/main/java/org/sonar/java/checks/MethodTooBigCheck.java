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
import org.sonar.java.ast.visitors.LinesOfCodeVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S138")
public class MethodTooBigCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 75;

  @RuleProperty(description = "Maximum authorized lines in a method", defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    BlockTree block = methodTree.block();
    if (block != null) {
      int lines = new LinesOfCodeVisitor().linesOfCode(block);
      if (lines > max) {
        reportIssue(methodTree.simpleName(),
          "This method has " + lines + " lines, which is greater than the " + max + " lines authorized. Split it into smaller methods.");
      }
    }
  }
}
