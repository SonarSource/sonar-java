/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3740")
public class RawTypeCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    Boolean overridesParentMethod = tree.isOverriding();
    if (overridesParentMethod == null || Boolean.TRUE.equals(overridesParentMethod)) {
      // only scan body of the method
      scan(tree.block());
    } else {
      checkTypeTree(tree.returnType());
      super.visitMethod(tree);
    }
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    tree.typeArguments()
      .forEach(this::checkTypeTree);
    super.visitParameterizedType(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    checkTypeTree(tree.identifier());
    super.visitNewClass(tree);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    checkTypeTree(tree.type());
    super.visitVariable(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    tree.superInterfaces().forEach(this::checkTypeTree);
    checkTypeTree(tree.superClass());
    super.visitClass(tree);
  }

  private void checkTypeTree(@Nullable TypeTree typeTree) {
    if (typeTree == null) {
      return;
    }
    if (typeTree.is(Tree.Kind.IDENTIFIER)) {
      checkIdentifier((IdentifierTree) typeTree);
    } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
      checkIdentifier(((MemberSelectExpressionTree) typeTree).identifier());
    }
  }

  private void checkIdentifier(IdentifierTree identifier) {
    Type type = identifier.symbolType();
    if (JUtils.isRawType(type) && !type.equals(JUtils.declaringType(type))) {
      context.reportIssue(this, identifier, "Provide the parametrized type for this generic.");
    }
  }
}
