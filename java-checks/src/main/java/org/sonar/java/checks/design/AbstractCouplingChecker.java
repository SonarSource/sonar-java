/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.design;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

public abstract class AbstractCouplingChecker extends BaseTreeVisitor implements JavaFileScanner {

  protected final Deque<Set<String>> nesting = new LinkedList<>();
  protected Set<String> types;
  protected JavaFileScannerContext context;

  /**
   * Implementations of this method should add the fully-qualified name of the type to the set {@link AbstractCouplingChecker#types}.
   */
  abstract void checkTypes(@Nullable Tree type, @Nullable Set<String> types);

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    checkTypes(tree.type(), types);
    super.visitVariable(tree);
  }

  @Override
  public void visitCatch(CatchTree tree) {
    // skip visit catch parameter for backward compatibility
    scan(tree.block());
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    checkTypes(tree.type(), types);
    super.visitTypeCast(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    checkTypes(tree.returnType(), types);
    super.visitMethod(tree);
  }

  @Override
  public void visitTypeParameter(TypeParameterTree typeParameter) {
    checkTypes(typeParameter.bounds());
    checkTypes(typeParameter.identifier(), types);
    super.visitTypeParameter(typeParameter);
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    checkTypes(tree.type(), types);
    checkTypes(tree.typeArguments());
    super.visitParameterizedType(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.typeArguments() != null) {
      checkTypes(tree.typeArguments());
    }
    if (tree.identifier().is(Tree.Kind.PARAMETERIZED_TYPE)) {
      scan(tree.enclosingExpression());
      checkTypes(((ParameterizedTypeTree) tree.identifier()).typeArguments());
      scan(tree.typeArguments());
      scan(tree.arguments());
      scan(tree.classBody());
    } else {
      super.visitNewClass(tree);
    }
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    checkTypes(tree.bound(), types);
    super.visitWildcard(tree);
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    checkTypes(tree.type(), types);
    super.visitArrayType(tree);
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    checkTypes(tree.type(), types);
    super.visitInstanceOf(tree);
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    checkTypes(tree.type(), types);
    super.visitNewArray(tree);
  }

  protected void checkTypes(List<? extends Tree> trees) {
    for (Tree type : trees) {
      checkTypes(type, types);
    }
  }

}
