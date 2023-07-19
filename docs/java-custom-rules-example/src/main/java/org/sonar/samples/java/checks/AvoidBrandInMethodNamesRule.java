/*
 * Copyright (C) 2012-2023 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.samples.java.utils.PrinterVisitor;

@Rule(key = "AvoidBrandInMethodNames")
public class AvoidBrandInMethodNamesRule extends BaseTreeVisitor implements JavaFileScanner {

  private static final Logger LOGGER = LoggerFactory.getLogger(AvoidBrandInMethodNamesRule.class);

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
