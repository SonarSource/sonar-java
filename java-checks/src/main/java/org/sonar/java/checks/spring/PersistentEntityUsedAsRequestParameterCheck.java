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
package org.sonar.java.checks.spring;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4684")
public class PersistentEntityUsedAsRequestParameterCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  private static final List<String> REQUEST_ANNOTATIONS = Arrays.asList(
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping");

  private static final List<String> ENTITY_ANNOTATIONS = Arrays.asList(
    "javax.persistence.Entity",
    "org.springframework.data.mongodb.core.mapping.Document",
    "org.springframework.data.elasticsearch.annotations.Document");

  private static final String PATH_VARIABLE_ANNOTATION = "org.springframework.web.bind.annotation.PathVariable";

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();

    if (isRequestMappingAnnotated(methodSymbol)) {
      methodTree.parameters().stream()
        .filter(PersistentEntityUsedAsRequestParameterCheck::hasNoPathVariableAnnotation)
        .filter(PersistentEntityUsedAsRequestParameterCheck::isPersistentEntity)
        .forEach(p -> reportIssue(p.simpleName(), "Replace this persistent entity with a simple POJO or DTO object."));
    }
  }

  private static boolean isRequestMappingAnnotated(Symbol.MethodSymbol methodSymbol) {
    return REQUEST_ANNOTATIONS.stream().anyMatch(methodSymbol.metadata()::isAnnotatedWith);
  }

  private static boolean isPersistentEntity(VariableTree variableTree) {
    return ENTITY_ANNOTATIONS.stream().anyMatch(variableTree.type().symbolType().symbol().metadata()::isAnnotatedWith);
  }

  private static boolean hasNoPathVariableAnnotation(VariableTree variableTree) {
    return !variableTree.symbol().metadata().isAnnotatedWith(PATH_VARIABLE_ANNOTATION);
  }
}
