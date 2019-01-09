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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4602")
public class SpringScanDefaultPackageCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_ATTRIBUTE = "value";

  private static final Map<String, Set<String>> SCAN_PACKAGE_ATTRIBUTES = buildScanPackageAttributes();

  private static Map<String, Set<String>> buildScanPackageAttributes() {
    Map<String, Set<String>> map = new HashMap<>();

    map.put("org.springframework.context.annotation.ComponentScan",
      new HashSet<>(Arrays.asList(DEFAULT_ATTRIBUTE, "basePackages", "basePackageClasses")));

    map.put("org.springframework.boot.autoconfigure.SpringBootApplication",
      new HashSet<>(Arrays.asList("scanBasePackages", "scanBasePackageClasses")));

    map.put("org.springframework.boot.web.servlet.ServletComponentScan",
      new HashSet<>(Arrays.asList(DEFAULT_ATTRIBUTE, "basePackages", "basePackageClasses")));

    return map;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    AnnotationTree annotation = (AnnotationTree) tree;
    Set<String> scanPackageAttributeNames = SCAN_PACKAGE_ATTRIBUTES.get(annotation.symbolType().fullyQualifiedName());
    if (scanPackageAttributeNames != null) {
      List<ExpressionTree> scanPackageAttributeValues = annotation.arguments().stream()
        .filter(argument -> scanPackageAttributeNames.contains(attributeName(argument)))
        .flatMap(SpringScanDefaultPackageCheck::extractValues)
        .collect(Collectors.toList());

      checkAnnotationPackageAttributes(annotation, scanPackageAttributeValues);
    }
  }

  private void checkAnnotationPackageAttributes(AnnotationTree annotation, List<ExpressionTree> scanPackageAttributeValues) {
    if (scanPackageAttributeValues.isEmpty()) {
      if (isNodeInDefaultPackage(annotation)) {
        reportIssue(annotation.annotationType(), "Remove the annotation \"@" + annotation.symbolType().name() +
          "\" or move the annotated class out of the default package.");
      }
    } else {
      scanPackageAttributeValues.stream()
        .map(SpringScanDefaultPackageCheck::findEmptyString)
        .forEach(opt -> opt.ifPresent(expression -> reportIssue(expression, "Define packages to scan. Don't rely on the default package.")));

      scanPackageAttributeValues.stream()
        .map(SpringScanDefaultPackageCheck::findClassInDefaultPackage)
        .forEach(opt -> opt.ifPresent(identifier -> reportIssue(identifier, "Remove the annotation \"@" + annotation.symbolType().name() +
          "\" or move the \"" + identifier.name() + "\" class out of the default package.")));
    }
  }

  private static String attributeName(ExpressionTree expression) {
    if (expression.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
      // assignment.variable() in annotation is always a Tree.Kind.IDENTIFIER
      return ((IdentifierTree) assignment.variable()).name();
    }
    return DEFAULT_ATTRIBUTE;
  }

  private static Stream<ExpressionTree> extractValues(ExpressionTree argument) {
    ExpressionTree expression = argument;
    if (expression.is(Tree.Kind.ASSIGNMENT)) {
      expression = ((AssignmentExpressionTree) expression).expression();
    }
    if (expression.is(Tree.Kind.NEW_ARRAY)) {
      return ((NewArrayTree) expression).initializers().stream()
        .flatMap(SpringScanDefaultPackageCheck::extractValues);
    }
    return Stream.of(expression);
  }

  private static Optional<ExpressionTree> findEmptyString(ExpressionTree expression) {
    String stringValue = ConstantUtils.resolveAsStringConstant(expression);
    if (stringValue != null && stringValue.isEmpty()) {
      return Optional.of(expression);
    }
    return Optional.empty();
  }

  private static Optional<IdentifierTree> findClassInDefaultPackage(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expression;
      if ("class".equals(memberSelect.identifier().name()) && memberSelect.expression().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) memberSelect.expression();
        if (isTypeInDefaultPackage(identifier.symbol())) {
          return Optional.of(identifier);
        }
      }
    }
    return Optional.empty();
  }

  private static boolean isTypeInDefaultPackage(Symbol symbol) {
    if (!symbol.isTypeSymbol()) {
      return false;
    }
    Symbol parent = symbol.owner();
    while (!parent.isPackageSymbol()) {
      parent = parent.owner();
    }
    return parent.name().isEmpty();
  }

  private static boolean isNodeInDefaultPackage(Tree tree) {
    while (!tree.is(Tree.Kind.COMPILATION_UNIT)) {
      tree = tree.parent();
    }
    return ((CompilationUnitTree) tree).packageDeclaration() == null;
  }

}
