/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

@Rule(key = "S4968")
public class TypeUpperBoundNotFinalCheck extends IssuableSubscriptionVisitor {

  private CheckVariable checkVariable = new CheckVariable();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      ClassTree classTree = (ClassTree) tree;
      handleTypeParameters(classTree.typeParameters());
      handleClassFields(classTree);
    } else if (tree.is(Tree.Kind.METHOD)) {
      MethodTree method = (MethodTree) tree;
      if (isNotOverriding(method)) {
        handleTypeParameters(method.typeParameters());
        handleMethodReturn(method.returnType());
        handleMethodParameters(method);
        handleMethodBody(method);
      }
    }
  }

  private void handleMethodParameters(MethodTree method) {
    for (VariableTree variable : method.parameters()) {
      variable.accept(checkVariable);
    }
  }

  private void handleMethodReturn(TypeTree returnType) {
    if (returnType.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      handleParameterizedType((ParameterizedTypeTree) returnType);
    }
  }

  private void handleMethodBody(MethodTree method) {
    if (method.block() != null) {
      for (StatementTree stmt : method.block().body()) {
        stmt.accept(checkVariable);
      }
    }
  }

  private void handleClassFields(ClassTree classTree) {
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        member.accept(checkVariable);
      }
    }
  }

  private void handleTypeParameters(TypeParameters tree) {
    for (TypeParameterTree typeParameterTree : tree) {
      for (TypeTree bound : typeParameterTree.bounds()) {
        if (reportIssueIfBoundIsFinal(bound, typeParameterTree))
          return;
      }
    }
  }

  private void handleParameterizedType(ParameterizedTypeTree type) {
    handleTypeArguments(type);
  }

  private boolean reportIssueIfBoundIsFinal(TypeTree bound, Tree treeToReport) {
    if (bound.is(Tree.Kind.IDENTIFIER)) {
      if (((IdentifierTree) bound).symbol().isFinal()) {
        reportIssue(treeToReport, "Replace this type parametrization by the 'final' type.");
        return true;
      }
    } else if (bound.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      ParameterizedTypeTree type = (ParameterizedTypeTree) bound;
      if (reportIssueIfBoundIsFinal(type.type(), treeToReport)) {
        return true;
      }
      handleTypeArguments(type);
    }
    return false;
  }

  private void handleTypeArguments(ParameterizedTypeTree type) {
    for (TypeTree typeArg : type.typeArguments()) {
      if (typeArg.is(Tree.Kind.EXTENDS_WILDCARD)) {
        TypeTree bound = ((WildcardTree) typeArg).bound();
        if (reportIssueIfBoundIsFinal(bound, typeArg)) {
          return;
        }
      }
    }
  }

  private class CheckVariable extends BaseTreeVisitor {
    @Override
    public void visitVariable(VariableTree tree) {
      TypeTree type = tree.type();
      if (type.is(Tree.Kind.PARAMETERIZED_TYPE)) {
        handleParameterizedType((ParameterizedTypeTree) type);
      }
    }
  }

  private static boolean isNotOverriding(MethodTree tree) {
    return Boolean.FALSE.equals(tree.isOverriding());
  }
}
