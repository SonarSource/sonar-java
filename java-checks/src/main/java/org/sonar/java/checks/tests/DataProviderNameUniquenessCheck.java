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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9389")
public class DataProviderNameUniquenessCheck extends IssuableSubscriptionVisitor {

  private static final String DATAPROVIDER_ANNOTATION = "org.testng.annotations.DataProvider";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String ISSUE_MESSAGE = "Rename this data provider to make it unique within this class.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Map<String, IdentifierTree> firstOccurrences = new HashMap<>();

    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        String providerName = extractDataProviderName(method);
        if (providerName != null) {
          IdentifierTree previous = firstOccurrences.putIfAbsent(providerName, method.simpleName());
          if (previous != null) {
            reportDuplicate(method.simpleName(), previous);
          }
        }
      }
    }
  }

  @Nullable
  private static String extractDataProviderName(MethodTree method) {
    for (AnnotationTree annotation : method.modifiers().annotations()) {
      if (isDataProviderAnnotation(annotation)) {
        ExpressionTree nameExpression = findNameAttribute(annotation);
        if (nameExpression == null) {
          return method.simpleName().name();
        }
        return nameExpression.asConstant(String.class).orElse(null);
      }
    }
    return null;
  }

  private static boolean isDataProviderAnnotation(AnnotationTree annotation) {
    return annotation.annotationType().symbolType().is(DATAPROVIDER_ANNOTATION);
  }

  @Nullable
  private static ExpressionTree findNameAttribute(AnnotationTree annotation) {
    for (ExpressionTree argument : annotation.arguments()) {
      if (argument.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) argument;
        String attributeName = ((IdentifierTree) assignment.variable()).name();
        if (NAME_ATTRIBUTE.equals(attributeName)) {
          return assignment.expression();
        }
      }
    }
    return null;
  }

  private void reportDuplicate(IdentifierTree duplicate, IdentifierTree first) {
    List<JavaFileScannerContext.Location> secondaryLocations = new ArrayList<>();
    secondaryLocations.add(new JavaFileScannerContext.Location(
      "First data provider with this name", first));

    reportIssue(duplicate, ISSUE_MESSAGE, secondaryLocations, null);
  }
}
