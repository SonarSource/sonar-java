/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java.api.query.ruleexamples;

import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.query.CommonTreeQuery;
import org.sonar.plugins.java.api.query.CompilationUnitQuery;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

public class S1181A implements JavaCheck {

  public CommonTreeQuery<JavaFileScannerContext, ? extends Tree> genQuery() {
    return new CompilationUnitQuery<JavaFileScannerContext>()
      .trees()
      .filterCatchTree()
      .filter((ctx, node) -> isBadCatchType(node.parameter().type()) )
      .visit((ctx, node) ->
        ctx.reportIssue(this, node.parameter().type(), "Catch Exception instead of "+node.parameter().type().symbolType().fullyQualifiedName()+"")
      );
  }

  private boolean isBadCatchType(TypeTree typeTree) {
    var type = typeTree.symbolType();
    if (type.fullyQualifiedName().equals("java.lang.Throwable")) return true;
    if (type.isSubtypeOf("java.lang.Error")) return true;

    if (typeTree instanceof UnionTypeTree union) {
      return union.typeAlternatives().stream().anyMatch(this::isBadCatchType);
    }
    return false;
  }

}
