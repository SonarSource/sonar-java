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
package org.sonar.java.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

class WarningMapper extends SubscriptionVisitor {

  private PriorityQueue<JWarning> warnings = new PriorityQueue<>((p1, p2) -> {
    if (p1.getStartLine() == p2.getStartLine()) {
      return p1.getStartColumn() - p2.getStartColumn();
    }
    return p1.getStartLine() - p2.getStartLine();
  });

  public WarningMapper(CompilationUnit astRoot) {
    Arrays.stream(astRoot.getProblems())
      .map(problem -> JWarning.ofIProblem(problem, astRoot))
      .filter(Objects::nonNull)
      .forEach(warnings::add);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.stream(JWarning.Type.values())
      .map(JWarning.Type::treeKind)
      .collect(Collectors.toList());
  }

  @Override
  public void visitNode(Tree tree) {
    JWarning nextWarning = warnings.peek();
    while (nextWarning != null && matches(nextWarning, tree)) {
      ((JavaTree) tree).addWarning(nextWarning);
      warnings.poll();
      nextWarning = warnings.peek();
    }
  }

  private static boolean matches(JWarning warning, Tree tree) {
    return warning.contains(tree.firstToken()) && warning.contains(tree.lastToken());
  }
}
