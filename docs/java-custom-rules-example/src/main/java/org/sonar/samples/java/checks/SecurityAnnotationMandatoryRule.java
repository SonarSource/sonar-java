/*
 * Copyright (C) 2012-2023 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java.checks;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAnnotationMandatoryRule.class);

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
