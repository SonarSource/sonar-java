/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.LinkedList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.quickfixes.QuickFix;

@Rule(key = "S6831")
public class AvoidQualifierOnBeanMethodsCheck extends IssuableSubscriptionVisitor {
  private static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";
  private static final String QUALIFIER_ANNOTATION = "org.springframework.beans.factory.annotation.Qualifier";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  /**
   * This rule reports an issue when @Bean methods are annotated with @Qualifier.
   */
  @Override
  public void visitNode(Tree tree) {
    var methodTree = (MethodTree) tree;

    var beanAnnotation = getAnnotation(methodTree, BEAN_ANNOTATION);
    var qualifierAnnotation = getAnnotation(methodTree, QUALIFIER_ANNOTATION);

    if (beanAnnotation != null && qualifierAnnotation != null) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(qualifierAnnotation)
        .withMessage("Remove this redundant \"@Qualifier\" annotation and rely on the @Bean method.")
        .withQuickFixes(() -> getQuickFix(methodTree, qualifierAnnotation))
        .report();
    }
  }

  private static AnnotationTree getAnnotation(MethodTree methodTree, String annotation) {
    return methodTree.modifiers()
      .annotations()
      .stream()
      .filter(annotationTree -> annotationTree.symbolType().is(annotation))
      .findFirst()
      .orElse(null);
  }

  private static List<QuickFix> getQuickFix(MethodTree methodTree, AnnotationTree qualifierAnnotation) {
    List<QuickFix> quickFixes = new LinkedList<>();

    // quick fix only for @Qualifier annotations without arguments or with argument that matches the method name
    if (isFixable(methodTree, qualifierAnnotation)) {
      var quickFix = QuickFix.newQuickFix("Remove \"@Qualifier\"")
        .addTextEdit(AnalyzerMessage.removeTree(qualifierAnnotation))
        .build();
      quickFixes.add(quickFix);
    }

    return quickFixes;
  }

  private static boolean isFixable(MethodTree methodTree, AnnotationTree qualifierAnnotation) {
    var arguments = qualifierAnnotation.arguments();

    // @Qualifier annotation without argument can be always removed
    if (arguments.isEmpty()) {
      return true;
    }

    // @Qualifier that matches the method name is redundant and can be removed
    var methodName = methodTree.simpleName().name();
    return getQualifierAnnotationValue(arguments).equals(methodName);
  }

  private static String getQualifierAnnotationValue(Arguments arguments) {
    var argument = arguments.get(0);
    String qualifierAnnotationValue;

    if (argument.is(Tree.Kind.ASSIGNMENT)) {
      qualifierAnnotationValue = ((LiteralTreeImpl) ((AssignmentExpressionTreeImpl) argument).expression()).value();
    } else if (argument.is(Tree.Kind.STRING_LITERAL)) {
      qualifierAnnotationValue = ((LiteralTreeImpl) argument).token().text();
    } else {
      // case when argument is an identifier: don't suggest a quick fix
      qualifierAnnotationValue = "";
    }

    return removeQuotes(qualifierAnnotationValue);
  }

  private static String removeQuotes(String value) {
    return value.replace("\"", "");
  }
}
