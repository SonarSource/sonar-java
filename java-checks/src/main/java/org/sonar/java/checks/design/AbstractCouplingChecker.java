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
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

public abstract class AbstractCouplingChecker extends BaseTreeVisitor implements JavaFileScanner {

  protected final Deque<Set<String>> nesting = new LinkedList<>();
  protected Set<String> types;
  protected JavaFileScannerContext context;

  /**
   * This method adds the fully-qualified class name to a set of types that are being tracked.
   */
  abstract void checkTypes(@Nullable Tree type);

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    checkTypes(tree.type());
    super.visitVariable(tree);
  }

  @Override
  public void visitCatch(CatchTree tree) {
    // skip visit catch parameter for backward compatibility
    scan(tree.block());
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    checkTypes(tree.type());
    super.visitTypeCast(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    checkTypes(tree.returnType());
    super.visitMethod(tree);
  }

  @Override
  public void visitTypeParameter(TypeParameterTree typeParameter) {
    checkTypes((List<? extends Tree>) typeParameter.bounds());
    checkTypes(typeParameter.identifier());
    super.visitTypeParameter(typeParameter);
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    // can not be visited because of visitCatch excluding exceptions
    checkTypes((List<? extends Tree>) tree.typeAlternatives());
    super.visitUnionType(tree);
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    checkTypes(tree.type());
    checkTypes((List<TypeTree>) tree.typeArguments());
    super.visitParameterizedType(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.typeArguments() != null) {
      checkTypes((List<TypeTree>) tree.typeArguments());
    }
    if (tree.identifier().is(Tree.Kind.PARAMETERIZED_TYPE)) {
      scan(tree.enclosingExpression());
      checkTypes((List<TypeTree>) ((ParameterizedTypeTree) tree.identifier()).typeArguments());
      scan(tree.typeArguments());
      scan(tree.arguments());
      scan(tree.classBody());
    } else {
      super.visitNewClass(tree);
    }
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    checkTypes(tree.bound());
    super.visitWildcard(tree);
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    checkTypes(tree.type());
    super.visitArrayType(tree);
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    checkTypes(tree.type());
    super.visitInstanceOf(tree);
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    checkTypes(tree.type());
    super.visitNewArray(tree);
  }

  private void checkTypes(List<? extends Tree> types) {
    for (Tree type : types) {
      checkTypes(type);
    }
  }

}