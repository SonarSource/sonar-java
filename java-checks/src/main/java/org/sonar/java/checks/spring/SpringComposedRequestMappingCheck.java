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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4488")
public class SpringComposedRequestMappingCheck extends IssuableSubscriptionVisitor {

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
        .collect(Collectors.toList());

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
}
