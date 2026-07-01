/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8948")
public class OneToManyMappingCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> ONE_TO_MANY_ANNOTATIONS = Set.of(
    "jakarta.persistence.OneToMany",
    "javax.persistence.OneToMany");

  private static final Set<String> JOIN_COLUMN_ANNOTATIONS = Set.of(
    "jakarta.persistence.JoinColumn",
    "javax.persistence.JoinColumn");

  private static final Set<String> JOIN_TABLE_ANNOTATIONS = Set.of(
    "jakarta.persistence.JoinTable",
    "javax.persistence.JoinTable");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    var annotations = tree.is(Tree.Kind.METHOD)
      ? ((MethodTree) tree).modifiers().annotations()
      : ((VariableTree) tree).modifiers().annotations();
    checkAnnotations(annotations);
  }

  private void checkAnnotations(List<AnnotationTree> annotations) {
    annotations.stream()
      .filter(OneToManyMappingCheck::isOneToManyAnnotation)
      .filter(annotation -> !hasMappedBy(annotation))
      .filter(annotation -> annotations.stream().noneMatch(OneToManyMappingCheck::isJoinColumnAnnotation))
      .filter(annotation -> annotations.stream().noneMatch(OneToManyMappingCheck::isJoinTableAnnotation))
      .forEach(annotation -> reportIssue(annotation, "Add \"mappedBy\" or \"@JoinColumn\" to this \"@OneToMany\" relationship."));
  }

  private static boolean isOneToManyAnnotation(AnnotationTree annotation) {
    return ONE_TO_MANY_ANNOTATIONS.stream().anyMatch(annotation.annotationType().symbolType()::is);
  }

  private static boolean isJoinColumnAnnotation(AnnotationTree annotation) {
    return JOIN_COLUMN_ANNOTATIONS.stream().anyMatch(annotation.annotationType().symbolType()::is);
  }

  private static boolean isJoinTableAnnotation(AnnotationTree annotation) {
    return JOIN_TABLE_ANNOTATIONS.stream().anyMatch(annotation.annotationType().symbolType()::is);
  }

  private static boolean hasMappedBy(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .filter(arg -> arg.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .map(AssignmentExpressionTree::variable)
      .filter(v -> v.is(Tree.Kind.IDENTIFIER))
      .map(IdentifierTree.class::cast)
      .anyMatch(id -> "mappedBy".equals(id.name()));
  }
}
