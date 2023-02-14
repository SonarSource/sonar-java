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
package org.sonar.java.checks.tests;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5973")
public class TestsStabilityCheck extends IssuableSubscriptionVisitor {

  private static final String ANNOTATION = "org.testng.annotations.Test";
  private static final String SUCCESS_PERCENTAGE_NAME = "successPercentage";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;

    List<AnnotationTree> annotations = methodTree.modifiers().annotations();
    Optional<Arguments> arguments = annotations.stream()
      .filter(annotationTree -> annotationTree.symbolType().is(ANNOTATION))
      .map(AnnotationTree::arguments)
      .findFirst();

    if (arguments.isPresent()) {
      for (ExpressionTree argument : arguments.get()) {
        if (argument.is(Tree.Kind.ASSIGNMENT)) {
          AssignmentExpressionTree assignmentTree = (AssignmentExpressionTree) argument;
          IdentifierTree nameTree = (IdentifierTree) assignmentTree.variable();
          if (nameTree.name().equals(SUCCESS_PERCENTAGE_NAME) ) {
            reportIssue(argument, "Make this test stable and remove this \"successPercentage\" argument.");
          }
        }
      }
    }
  }
}
