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
package org.sonar.java.filters;

import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;

public class GeneratedCodeFilter extends AnyRuleIssueFilter {

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (context.getSemanticModel() == null) {
      // Filter requires semantic
      return;
    }
    super.scanFile(context);
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (isGenerated(tree.symbol())) {
      excludeLines(tree);
    }
    super.visitClass(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (isGenerated(tree.symbol())) {
      excludeLines(tree);
    }
    super.visitMethod(tree);
  }

  private static boolean isGenerated(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith("javax.annotation.Generated");
  }
}
