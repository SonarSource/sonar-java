/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.RuleTemplate;

@RuleTemplate
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

  private void checkIfDisallowed(String className, Tree tree) {
    if (pattern == null) {
      try {
        pattern = Pattern.compile(disallowedClass);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("[" + getClass().getSimpleName() + "] Unable to compile the regular expression: " + disallowedClass, e);
      }
    }
    if (pattern.matcher(className).matches()) {
      context.reportIssue(this, tree, "Remove the use of this forbidden class.");
    }
  }
}
