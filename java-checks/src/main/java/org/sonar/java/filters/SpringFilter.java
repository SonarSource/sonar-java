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
package org.sonar.java.filters;

import java.util.List;
import java.util.Set;
import org.sonar.java.checks.AtLeastOneConstructorCheck;
import org.sonar.java.checks.MethodOnlyCallsSuperCheck;
import org.sonar.java.checks.ServletInstanceFieldCheck;
import org.sonar.java.checks.TooManyParametersCheck;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class SpringFilter extends BaseTreeVisitorIssueFilter {

  private static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";

  private static final List<String> S107_METHOD_ANNOTATION_EXCEPTIONS = List.of(
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping",
    "org.springframework.context.annotation.Bean",
    AUTOWIRED);

  private static final List<String> S107_CLASS_ANNOTATION_EXCEPTIONS = List.of(
    "org.springframework.stereotype.Component",
    "org.springframework.context.annotation.Configuration",
    "org.springframework.stereotype.Service",
    "org.springframework.stereotype.Repository");

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return Set.of(
      /* S107_ */ TooManyParametersCheck.class,
      /* S1185 */ MethodOnlyCallsSuperCheck.class,
      /* S1258 */ AtLeastOneConstructorCheck.class,
      /* S2226 */ ServletInstanceFieldCheck.class);
  }

  @Override
  public void visitClass(ClassTree tree) {
    excludeLinesIfTrue(isTransactional(tree), tree, MethodOnlyCallsSuperCheck.class);
    excludeLinesIfTrue(hasAutowiredField(tree), tree.simpleName(), AtLeastOneConstructorCheck.class);
    super.visitClass(tree);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    excludeLinesIfTrue(isAutowired(tree), tree, ServletInstanceFieldCheck.class);
    super.visitVariable(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    MethodSymbol symbol = tree.symbol();
    if (tree.is(Tree.Kind.CONSTRUCTOR)) {
      SymbolMetadata ownerMetadata = symbol.owner().metadata();
      excludeLinesIfTrue(S107_CLASS_ANNOTATION_EXCEPTIONS.stream().anyMatch(ownerMetadata::isAnnotatedWith), tree.simpleName(), TooManyParametersCheck.class);
    } else {
      SymbolMetadata methodMetadata = symbol.metadata();
      excludeLinesIfTrue(S107_METHOD_ANNOTATION_EXCEPTIONS.stream().anyMatch(methodMetadata::isAnnotatedWith), tree.simpleName(), TooManyParametersCheck.class);
    }
    super.visitMethod(tree);
  }

  private static boolean hasAutowiredField(ClassTree tree) {
    return tree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .anyMatch(SpringFilter::isAutowired);
  }

  private static boolean isAutowired(VariableTree tree) {
    return tree.symbol().metadata().isAnnotatedWith(AUTOWIRED)
      // missing semantic
      || hasUnknownAnnotationWithName(tree.modifiers(), "Autowired");
  }

  private static boolean isTransactional(ClassTree tree) {
    return tree.symbol().metadata().isAnnotatedWith("org.springframework.transaction.annotation.Transactional")
      // missing semantic
      || hasUnknownAnnotationWithName(tree.modifiers(), "Transactional");
  }

  private static boolean hasUnknownAnnotationWithName(ModifiersTree modifiers, String annotation) {
    // Token based check in case of missing semantic
    List<AnnotationTree> annotations = modifiers.annotations();
    if (annotations.isEmpty()) {
      // avoid creation of streams for methods without annotations (should be a large majority of methods)
      return false;
    }
    return annotations.stream()
      .map(AnnotationTree::annotationType)
      .filter(a -> a.symbolType().isUnknown())
      // unknown annotation potentially matching - checking for tokens
      .filter(tree -> tree.is(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT))
      .map(ExpressionTree.class::cast)
      .map(ExpressionsHelper::concatenate)
      .anyMatch(annotationName -> annotationName.endsWith(annotation));
  }
}
