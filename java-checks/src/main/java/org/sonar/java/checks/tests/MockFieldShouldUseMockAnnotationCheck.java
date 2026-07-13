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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9015")
public class MockFieldShouldUseMockAnnotationCheck extends IssuableSubscriptionVisitor {

  private static final String MOCKITO_EXTENSION = "org.mockito.junit.jupiter.MockitoExtension";
  private static final String EXTEND_WITH_ANNOTATION = "org.junit.jupiter.api.extension.ExtendWith";
  private static final String RUN_WITH_ANNOTATION = "org.junit.runner.RunWith";
  private static final String MOCKITO_JUNIT_RUNNER_PREFIX = "org.mockito.junit.MockitoJUnitRunner";

  private static final MethodMatchers MOCK_METHOD = MethodMatchers.create()
    .ofTypes("org.mockito.Mockito")
    .names("mock")
    .withAnyParameters()
    .build();

  private static final String MESSAGE = "Use \"@Mock\" annotation instead of \"mock()\" for field declaration.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!isMockitoManagedClass(classTree)) {
      return;
    }
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .filter(MockFieldShouldUseMockAnnotationCheck::hasFieldMockInitializer)
      .forEach(field -> reportIssue(field.initializer(), MESSAGE));
  }

  private static boolean isMockitoManagedClass(ClassTree classTree) {
    Symbol classSymbol = classTree.symbol();
    return isAnnotatedWithMockitoExtension(classSymbol) || isAnnotatedWithMockitoJUnitRunner(classSymbol);
  }

  private static boolean isAnnotatedWithMockitoExtension(Symbol classSymbol) {
    return checkMockitoExtensionInMetadata(classSymbol.metadata(), new HashSet<>());
  }

  private static boolean checkMockitoExtensionInMetadata(SymbolMetadata metadata, Set<Symbol> visited) {
    List<SymbolMetadata.AnnotationValue> extendWithValues = metadata.valuesForAnnotation(EXTEND_WITH_ANNOTATION);
    if (extendWithValues != null) {
      for (SymbolMetadata.AnnotationValue av : extendWithValues) {
        if (isMockitoExtensionClass(av.value())) {
          return true;
        }
      }
    }
    for (SymbolMetadata.AnnotationInstance annotation : metadata.annotations()) {
      Symbol annotationSymbol = annotation.symbol();
      if (!visited.contains(annotationSymbol)) {
        visited.add(annotationSymbol);
        if (checkMockitoExtensionInMetadata(annotationSymbol.metadata(), visited)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isMockitoExtensionClass(Object value) {
    if (value instanceof Symbol symbol) {
      return symbol.type().is(MOCKITO_EXTENSION);
    }
    if (value instanceof Object[] values) {
      for (Object v : values) {
        if (v instanceof Symbol symbol && symbol.type().is(MOCKITO_EXTENSION)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isAnnotatedWithMockitoJUnitRunner(Symbol classSymbol) {
    List<SymbolMetadata.AnnotationValue> runWithValues = classSymbol.metadata().valuesForAnnotation(RUN_WITH_ANNOTATION);
    if (runWithValues != null && runWithValues.size() == 1) {
      Object value = runWithValues.get(0).value();
      if (value instanceof Symbol.TypeSymbol typeSymbol) {
        String fqn = typeSymbol.type().fullyQualifiedName();
        return fqn.equals(MOCKITO_JUNIT_RUNNER_PREFIX) || fqn.startsWith(MOCKITO_JUNIT_RUNNER_PREFIX + "$");
      }
    }
    return false;
  }

  private static boolean hasFieldMockInitializer(VariableTree field) {
    ExpressionTree initializer = field.initializer();
    return initializer != null
      && initializer.is(Tree.Kind.METHOD_INVOCATION)
      && MOCK_METHOD.matches(((MethodInvocationTree) initializer).methodSymbol());
  }
}
