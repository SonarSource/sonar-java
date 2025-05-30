/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import javax.annotation.Nullable;
import org.sonar.check.Rule;
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
    if (!Boolean.FALSE.equals(tree.isOverriding())) {
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
    if (type.isRawType() && !type.equals(type.declaringType())) {
      context.reportIssue(this, identifier, "Provide the parametrized type for this generic.");
    }
  }
}
