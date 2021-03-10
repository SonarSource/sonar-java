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

import java.util.Locale;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.samples.java.utils.PrinterVisitor;

@Rule(key = "AvoidBrandInMethodNames")
public class AvoidBrandInMethodNamesRule extends BaseTreeVisitor implements JavaFileScanner {

  private static final Logger LOGGER = Loggers.get(AvoidBrandInMethodNamesRule.class);

  private JavaFileScannerContext context;

  protected static final String COMPANY_NAME = "MyCompany";

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    // The call to the scan method on the root of the tree triggers the visit of the AST by this visitor
    scan(context.getTree());

    // For debugging purpose, you can print out the entire AST of the analyzed file
    // In production, this will display all the syntax trees, as soon as the log level is set to DEBUG
    PrinterVisitor.print(context.getTree(), LOGGER::debug);
  }

  /**
   * Overriding the visitor method to implement the logic of the rule.
   * @param tree AST of the visited method.
   */
  @Override
  public void visitMethod(MethodTree tree) {

    if (tree.simpleName().name().toUpperCase(Locale.ROOT).contains(COMPANY_NAME.toUpperCase(Locale.ROOT))) {
      // Adds an issue by attaching it with the tree and the rule
      context.reportIssue(this, tree, "Avoid using Brand in method name");
    }
    // The call to the super implementation allows to continue the visit of the AST.
    // Be careful to always call this method to visit every node of the tree.
    super.visitMethod(tree);

    // All the code located after the call to the overridden method is executed when leaving the node
  }

}
