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
package org.sonar.java.checks.spring;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3752")
public class SpringRequestMappingMethodCheck extends IssuableSubscriptionVisitor {

  private static final String REQUEST_MAPPING_CLASS = "org.springframework.web.bind.annotation.RequestMapping";

  private static final String REQUEST_METHOD = "method";
  public static final String MESSAGE = "Do not use @RequestMapping without specifying the allowed HTTP methods.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .forEach(member -> checkMethod((MethodTree) member, classTree.symbol()));
  }

  private void checkMethod(MethodTree method, Symbol.TypeSymbol classSymbol) {
    findRequestMappingAnnotation(method.modifiers()).ifPresent(annotation -> {
      if (findRequestMethods(annotation).isEmpty() && !inheritRequestMethod(classSymbol)) {
        reportIssue(annotation.annotationType(), MESSAGE);
      }
    });
  }

  private static Optional<AnnotationTree> findRequestMappingAnnotation(ModifiersTree modifiers) {
    return modifiers.annotations().stream()
      .filter(annotation -> annotation.symbolType().is(REQUEST_MAPPING_CLASS))
      .findFirst();
  }

  private static Optional<ExpressionTree> findRequestMethods(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .filter(argument -> argument.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .filter(assignment -> REQUEST_METHOD.equals(((IdentifierTree) assignment.variable()).name()))
      .map(AssignmentExpressionTree::expression)
      .findFirst();
  }

  private static boolean inheritRequestMethod(Symbol.TypeSymbol symbol) {
    List<SymbolMetadata.AnnotationValue> annotationValues = symbol.metadata().valuesForAnnotation(REQUEST_MAPPING_CLASS);
    if (annotationValues != null && annotationValues.stream().anyMatch(value -> REQUEST_METHOD.equals(value.name()))) {
      return true;
    }
    Type superClass = symbol.superClass();
    if (superClass != null && inheritRequestMethod(superClass.symbol())) {
      return true;
    }
    for (Type type : symbol.interfaces()) {
      if (inheritRequestMethod(type.symbol())) {
        return true;
      }
    }
    return false;
  }

}
