/*
 * Copyright (C) 2012-2024 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
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
