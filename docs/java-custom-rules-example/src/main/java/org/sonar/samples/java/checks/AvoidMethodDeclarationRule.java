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
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;

/**
 * This class is an example of how to implement your own rules.
 * The (stupid) rule raises a minor issue each time a method is encountered.
 */
@Rule(key = "AvoidMethodDeclaration")
/**
 * The class extends BaseTreeVisitor: the visitor for the Java AST.
 * The logic of the rule is implemented by overriding its methods.
 * It also implements the JavaFileScanner interface to be injected with the JavaFileScannerContext to attach issues to this context.
 */
public class AvoidMethodDeclarationRule extends BaseTreeVisitor implements JavaFileScanner {

  /**
   * Private field to store the context: this is the object used to create issues.
   */
  private JavaFileScannerContext context;

  /**
   * Implementation of the method of the JavaFileScanner interface.
   * @param context Object used to attach issues to source file.
   */
  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    // The call to the scan method on the root of the tree triggers the visit of the AST by this visitor
    scan(context.getTree());
  }

  /**
   * Overriding the visitor method to implement the logic of the rule.
   * @param tree AST of the visited method.
   */
  @Override
  public void visitMethod(MethodTree tree) {
    // All the code located before the call to the overridden method is executed before visiting the node

    // Adds an issue by attaching it with the tree and the rule
    context.reportIssue(this, tree, "Avoid declaring methods (don't ask why)");

    // The call to the super implementation allows to continue the visit of the AST.
    // Be careful to always call this method to visit every node of the tree.
    super.visitMethod(tree);

    // All the code located after the call to the overridden method is executed when leaving the node
  }

}
