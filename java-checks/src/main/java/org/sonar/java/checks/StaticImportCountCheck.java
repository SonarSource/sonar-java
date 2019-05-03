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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Rule(key = "S3030")
public class StaticImportCountCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_THRESHOLD = 4;

  @RuleProperty(key = "threshold", description = "The maximum number of static imports allowed", defaultValue = "" + DEFAULT_THRESHOLD)
  private int threshold = DEFAULT_THRESHOLD;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    CompilationUnitTree cut = (CompilationUnitTree) tree;
    List<ImportClauseTree> staticImports = cut.imports().stream()
      .filter(importClauseTree -> importClauseTree.is(Tree.Kind.IMPORT) && ((ImportTree) importClauseTree).isStatic())
      .collect(Collectors.toList());
    int staticImportsCount = staticImports.size();

    if (staticImportsCount > threshold) {
      List<JavaFileScannerContext.Location> flow = staticImports.stream()
        .map(importStatement -> new JavaFileScannerContext.Location("+1", importStatement))
        .collect(Collectors.toList());
      String message = String.format("Reduce the number of \"static\" imports in this class from %d to the maximum allowed %d.", staticImportsCount, threshold);
      reportIssue(staticImports.get(0), message, flow, null);
    }
  }


  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }
}
