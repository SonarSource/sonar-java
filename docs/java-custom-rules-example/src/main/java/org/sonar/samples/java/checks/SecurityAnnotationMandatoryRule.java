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

import java.util.List;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "SecurityAnnotationMandatory")
public class SecurityAnnotationMandatoryRule extends BaseTreeVisitor implements JavaFileScanner {

  private static final Logger LOGGER = Loggers.get(SecurityAnnotationMandatoryRule.class);

  private static final String DEFAULT_VALUE = "MySecurityAnnotation";

  private boolean implementsSpecificInterface = false;

  private JavaFileScannerContext context;

  @RuleProperty(
    defaultValue = DEFAULT_VALUE,
    description = "Name of the mandatory annotation")
  protected String name;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    implementsSpecificInterface = false;
    for (TypeTree typeTree : tree.superInterfaces()) {
      LOGGER.debug("implements Interface: {}", typeTree);
      if ("MySecurityInterface".equals(typeTree.toString())) {
        implementsSpecificInterface = true;
      }
    }

    super.visitClass(tree);
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    PackageDeclarationTree packageDeclaration = tree.packageDeclaration();
    if (packageDeclaration != null) {
      printPackageName(packageDeclaration.packageName());
    }

    super.visitCompilationUnit(tree);
  }

  private static void printPackageName(ExpressionTree packageName) {
    StringBuilder sb = new StringBuilder();
    ExpressionTree expr = packageName;
    while (expr.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
      sb.insert(0, mse.identifier().name());
      sb.insert(0, mse.operatorToken().text());
      expr = mse.expression();
    }
    IdentifierTree idt = (IdentifierTree) expr;
    sb.insert(0, idt.name());

    LOGGER.debug("PackageName: {}", sb);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (implementsSpecificInterface) {
      List<AnnotationTree> annotations = tree.modifiers().annotations();

      boolean isHavingMandatoryAnnotation = Boolean.FALSE;

      for (AnnotationTree annotationTree : annotations) {
        TypeTree annotationType = annotationTree.annotationType();
        if (annotationType.is(Tree.Kind.IDENTIFIER)) {
          String annotationName = ((IdentifierTree) annotationType).name();
          LOGGER.debug("Method Name {}", annotationName);

          if (annotationName.equals(name)) {
            isHavingMandatoryAnnotation = Boolean.TRUE;
          }
        }
      }
      if (!isHavingMandatoryAnnotation) {
        // report on the method name, not on everything
        context.reportIssue(this, tree.simpleName(), String.format("Mandatory Annotation not set @%s", name));
      }

    }
    // The call to the super implementation allows to continue the visit of the AST.
    // Be careful to always call this method to visit every node of the tree.
    super.visitMethod(tree);
  }
}
