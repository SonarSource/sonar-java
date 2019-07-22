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

import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.IllegalRuleParameterException;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3688")
public class DisallowedClassCheck extends BaseTreeVisitor implements JavaFileScanner {

  @RuleProperty(
    key = "className",
    description = "Fully qualified name of the forbidden class. Use a regex to forbid a package.",
    defaultValue = "")
  public String disallowedClass = "";
  private Pattern pattern = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitImport(ImportTree tree) {
    String importName = ExpressionsHelper.concatenate((ExpressionTree) tree.qualifiedIdentifier());
    if (!checkIfDisallowed(importName, tree.qualifiedIdentifier())) {
      int separator = importName.lastIndexOf('.');
      if (separator != -1) {
        checkIfDisallowed(importName.substring(0, separator), tree.qualifiedIdentifier());
      }
    }
    super.visitImport(tree);
  }

  @Override
  public void visitVariable(VariableTree variableTree) {
    String variableTypeName = variableTree.type().symbolType().fullyQualifiedName();
    checkIfDisallowed(variableTypeName, variableTree.type());
    super.visitVariable(variableTree);
  }

  @Override
  public void visitMethod(MethodTree methodTree) {
    if (methodTree.returnType() != null ) {
      String returnTypeName = methodTree.returnType().symbolType().fullyQualifiedName();
      checkIfDisallowed(returnTypeName, methodTree.returnType());
    }
    super.visitMethod(methodTree);
  }

  @Override
  public void visitNewClass(NewClassTree newClassTree) {
    String newClassTypeName = newClassTree.identifier().symbolType().fullyQualifiedName();
    Tree parent = newClassTree.parent();
    if (parent != null && !parent.is(Tree.Kind.VARIABLE)) {
      checkIfDisallowed(newClassTypeName, newClassTree);
    }
    super.visitNewClass(newClassTree );
  }

  @Override
  public void visitClass(ClassTree classTree) {
    TypeTree superClass = classTree.superClass();
    if (superClass != null) {
      String superClassTypeName = superClass.symbolType().fullyQualifiedName();
      checkIfDisallowed(superClassTypeName, superClass);
    }
    super.visitClass(classTree);
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    String annotationTypeName = annotationTree.symbolType().fullyQualifiedName();
    checkIfDisallowed(annotationTypeName, annotationTree.annotationType());
    super.visitAnnotation(annotationTree);
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    // Disallowed new class are already reported in visitNewClass
    if(!tree.expression().is(Tree.Kind.NEW_CLASS)) {
      String memberSelectTypeName = tree.expression().symbolType().fullyQualifiedName();
      checkIfDisallowed(memberSelectTypeName, tree);
    }
    super.visitMemberSelectExpression(tree);
  }

  private boolean checkIfDisallowed(String className, Tree tree) {
    if (pattern == null) {
      try {
        pattern = Pattern.compile(disallowedClass);
      } catch (IllegalArgumentException e) {
        throw new IllegalRuleParameterException("[" + getClass().getSimpleName() + "] Unable to compile the regular expression: " + disallowedClass, e);
      }
    }
    if (pattern.matcher(className).matches() && !tree.is(Tree.Kind.INFERED_TYPE)) {
      context.reportIssue(this, tree, "Remove the use of this forbidden class.");
      return true;
    }
    return false;
  }
}
