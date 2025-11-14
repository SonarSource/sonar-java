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
package org.sonar.java.checks;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6904")
public class JpaEagerFetchTypeCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> FETCH_TYPE_ENUMS = Set.of(
    "jakarta.persistence.FetchType",
    "javax.persistence.FetchType");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var at = (AnnotationTree) tree;

    // Report an issue if any argument in the annotation is FetchType.EAGER
    at.arguments().stream()
      .map(JpaEagerFetchTypeCheck::getEagerArgument)
      .filter(Objects::nonNull)
      .findAny()
      .ifPresent(arg -> reportIssue(arg, "Use lazy fetching instead."));
  }

  @Nullable
  private static IdentifierTree getEagerArgument(ExpressionTree tree) {
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      var assignmentTree = (AssignmentExpressionTree) tree;
      var assignedExpr = assignmentTree.expression();
      IdentifierTree fetchType;
      if (assignedExpr.is(Tree.Kind.MEMBER_SELECT)) {
        fetchType = ((MemberSelectExpressionTree) assignedExpr).identifier();
      } else if (assignedExpr.is(Tree.Kind.IDENTIFIER)) {
        fetchType = (IdentifierTree) assignedExpr;
      } else {
        return null;
      }

      if ("fetch".equals(ExpressionUtils.extractIdentifier(assignmentTree).name()) &&
        "EAGER".equals(fetchType.name()) &&
        FETCH_TYPE_ENUMS.contains(assignedExpr.symbolType().fullyQualifiedName())) {

        return fetchType;
      }
    }

    return null;
  }
}
