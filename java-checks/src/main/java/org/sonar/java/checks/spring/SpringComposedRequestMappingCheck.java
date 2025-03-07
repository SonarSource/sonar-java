/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.classpath.DependencyVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4488")
public class SpringComposedRequestMappingCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  private static final Map<String, String> PREFERRED_METHOD_MAP = buildPreferredMethodMap();

  private static Map<String, String> buildPreferredMethodMap() {
    Map<String, String> map = new HashMap<>();
    map.put("GET", "@GetMapping");
    map.put("POST", "@PostMapping");
    map.put("PUT", "@PutMapping");
    map.put("PATCH", "@PatchMapping");
    map.put("DELETE", "@DeleteMapping");
    return map;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree annotation = (AnnotationTree) tree;
    if (annotation.symbolType().is("org.springframework.web.bind.annotation.RequestMapping")) {
      List<ExpressionTree> methodValues = annotation.arguments().stream()
        .filter(argument -> "method".equals(attributeName(argument)))
        .flatMap(SpringComposedRequestMappingCheck::extractValues)
        .toList();

      if (methodValues.size() == 1) {
        ExpressionTree requestMethod = methodValues.get(0);
        String currentMethod = getRequestMethodEnumEntry(requestMethod);
        String preferredMethod = PREFERRED_METHOD_MAP.get(currentMethod);
        if (preferredMethod != null) {
          reportIssue(annotation.annotationType(),
            "Replace \"@RequestMapping(method = RequestMethod." + currentMethod + ")\" with \"" + preferredMethod + "\"",
            Collections.singletonList(new JavaFileScannerContext.Location("", requestMethod)),
            null);
        }
      }
    }
  }

  private static String getRequestMethodEnumEntry(ExpressionTree requestMethod) {
    ExpressionTree expression = requestMethod;
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) requestMethod).identifier();
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      if (symbol.type().is("org.springframework.web.bind.annotation.RequestMethod")) {
        return symbol.name();
      }
    }
    return "";
  }

  private static String attributeName(ExpressionTree expression) {
    if (expression.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
      // assignment.variable() in annotation is always a Tree.Kind.IDENTIFIER
      return ((IdentifierTree) assignment.variable()).name();
    }
    return "value";
  }

  private static Stream<ExpressionTree> extractValues(ExpressionTree argument) {
    ExpressionTree expression = argument;
    if (expression.is(Tree.Kind.ASSIGNMENT)) {
      expression = ((AssignmentExpressionTree) expression).expression();
    }
    if (expression.is(Tree.Kind.NEW_ARRAY)) {
      return ((NewArrayTree) expression).initializers().stream()
        .flatMap(SpringComposedRequestMappingCheck::extractValues);
    }
    return Stream.of(expression);
  }

  @Override
  public boolean isCompatibleWithDependencies(BiFunction<String, String, Optional<DependencyVersion>> dependencyFinder) {
    return dependencyFinder.apply("org.springframework", "spring-web")
      .map(v -> v.isGreaterThanOrEqualTo("4.3"))
      .orElse(false);
  }
}
