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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9389")
public class DataProviderNameUniquenessCheck extends IssuableSubscriptionVisitor {

  private static final String DATAPROVIDER_ANNOTATION = "org.testng.annotations.DataProvider";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String ISSUE_MESSAGE = "Rename this data provider to make it unique within this class.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Map<String, Tree> firstOccurrences = new HashMap<>();

    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        String providerName = extractDataProviderName(method);
        if (providerName != null) {
          Tree previous = firstOccurrences.putIfAbsent(providerName, method);
          if (previous != null) {
            reportDuplicate(providerName, method, previous);
          }
        }
      }
    }
  }

  private String extractDataProviderName(MethodTree method) {
    for (AnnotationTree annotation : method.modifiers().annotations()) {
      if (isDataProviderAnnotation(annotation)) {
        return extractNameAttribute(annotation);
      }
    }
    return null;
  }

  private boolean isDataProviderAnnotation(AnnotationTree annotation) {
    return annotation.annotationType().symbolType().is(DATAPROVIDER_ANNOTATION);
  }

  private String extractNameAttribute(AnnotationTree annotation) {
    List<ExpressionTree> arguments = annotation.arguments();
    if (arguments == null || arguments.isEmpty()) {
      return null;
    }

    for (ExpressionTree argument : arguments) {
      if (argument.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) argument;
        String attributeName = getAttributeName(assignment.variable());
        if (NAME_ATTRIBUTE.equals(attributeName)) {
          return extractStringValue(assignment.expression());
        }
      }
    }
    return null;
  }

  private String getAttributeName(ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expression).name();
    } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) expression).identifier().name();
    }
    return null;
  }

  private String extractStringValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      return ((LiteralTree) expression).value();
    }
    return null;
  }

  private void reportDuplicate(String name, Tree duplicate, Tree first) {
    List<JavaFileScannerContext.Location> secondaryLocations = new ArrayList<>();
    secondaryLocations.add(new JavaFileScannerContext.Location(
      "First data provider with this name", first));

    reportIssue(duplicate, ISSUE_MESSAGE, secondaryLocations, null);
  }
}
