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
package org.sonar.samples.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "SpringControllerRequestMappingEntity")
public class SpringControllerRequestMappingEntityRule extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  /**
   * Overriding the visitor method to implement the logic of the rule.
   * @param tree AST of the visited method.
   */
  @Override
  public void visitMethod(MethodTree tree) {
    Symbol.MethodSymbol methodSymbol = tree.symbol();

    SymbolMetadata parentClassOwner = methodSymbol.owner().metadata();
    boolean isControllerContext = parentClassOwner.isAnnotatedWith("org.springframework.stereotype.Controller");

    if (isControllerContext && methodSymbol.metadata().isAnnotatedWith("org.springframework.web.bind.annotation.RequestMapping")) {

      for (VariableTree param : tree.parameters()) {
        TypeTree typeOfParam = param.type();
        if (typeOfParam.symbolType().symbol().metadata().isAnnotatedWith("javax.persistence.Entity")) {
          context.reportIssue(this, typeOfParam, String.format("Don't use %s here because it's an @Entity", typeOfParam.symbolType().name()));
        }
      }

    }
    super.visitMethod(tree);
  }

}
