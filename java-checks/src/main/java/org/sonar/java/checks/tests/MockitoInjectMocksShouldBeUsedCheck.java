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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9024")
public class MockitoInjectMocksShouldBeUsedCheck extends IssuableSubscriptionVisitor {

  private static final String MOCK_ANNOTATION = "org.mockito.Mock";
  private static final String SPY_ANNOTATION = "org.mockito.Spy";
  private static final String MOCKITO_EXTENSION = "org.mockito.junit.jupiter.MockitoExtension";
  private static final String EXTEND_WITH_ANNOTATION = "org.junit.jupiter.api.extension.ExtendWith";
  private static final String RUN_WITH_ANNOTATION = "org.junit.runner.RunWith";
  private static final String MOCKITO_JUNIT_RUNNER_PREFIX = "org.mockito.junit.MockitoJUnitRunner";

  private static final List<String> SETUP_ANNOTATIONS = List.of(
    "org.junit.Before",
    "org.junit.jupiter.api.BeforeEach");

  private static final MethodMatchers OPEN_OR_INIT_MOCKS = MethodMatchers.create()
    .ofTypes("org.mockito.MockitoAnnotations")
    .names("openMocks", "initMocks")
    .addParametersMatcher("java.lang.Object")
    .build();

  private static final String MESSAGE = "Use \"@InjectMocks\" to inject these mock fields instead of manually constructing the object.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    Set<Symbol> mockAndSpyFields = collectMockAndSpyFields(classTree);
    if (mockAndSpyFields.isEmpty()) {
      return;
    }

    List<MethodTree> setupMethods = getSetupMethods(classTree);
    if (setupMethods.isEmpty()) {
      return;
    }

    if (!isMockitoManagedClass(classTree, setupMethods)) {
      return;
    }

    Symbol classSymbol = classTree.symbol();
    setupMethods.forEach(method -> checkSetupMethod(method, mockAndSpyFields, classSymbol));
  }

  private void checkSetupMethod(MethodTree method, Set<Symbol> mockFields, Symbol classSymbol) {
    SetupMethodVisitor visitor = new SetupMethodVisitor(mockFields, classSymbol);
    method.accept(visitor);
    visitor.issues.forEach(node -> reportIssue(node, MESSAGE));
  }

  private static Set<Symbol> collectMockAndSpyFields(ClassTree classTree) {
    return classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .filter(MockitoInjectMocksShouldBeUsedCheck::isMockOrSpyAnnotated)
      .map(VariableTree::symbol)
      .collect(Collectors.toSet());
  }

  private static boolean isMockOrSpyAnnotated(VariableTree field) {
    SymbolMetadata metadata = field.symbol().metadata();
    return metadata.isAnnotatedWith(MOCK_ANNOTATION) || metadata.isAnnotatedWith(SPY_ANNOTATION);
  }

  private static List<MethodTree> getSetupMethods(ClassTree classTree) {
    return classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(MockitoInjectMocksShouldBeUsedCheck::isSetupMethod)
      .toList();
  }

  private static boolean isSetupMethod(MethodTree method) {
    SymbolMetadata metadata = method.symbol().metadata();
    return SETUP_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }

  private static boolean isMockitoManagedClass(ClassTree classTree, List<MethodTree> setupMethods) {
    Symbol classSymbol = classTree.symbol();
    return isAnnotatedWithMockitoExtension(classSymbol)
      || isAnnotatedWithMockitoJUnitRunner(classSymbol)
      || callsOpenOrInitMocksInSetup(setupMethods);
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

  private static boolean callsOpenOrInitMocksInSetup(List<MethodTree> setupMethods) {
    for (MethodTree method : setupMethods) {
      if (method.block() == null) {
        return false;
      }
      for (StatementTree statement : method.block().body()) {
        if (statement instanceof ExpressionStatementTree expressionStatementTree
          && expressionStatementTree.expression() instanceof MethodInvocationTree mit
          && OPEN_OR_INIT_MOCKS.matches(mit)) {
          return true;
        }
      }
    }
    return false;
  }

  private static class SetupMethodVisitor extends BaseTreeVisitor {
    private final Set<Symbol> mockFields;
    private final Symbol classSymbol;
    final List<NewClassTree> issues = new ArrayList<>();

    SetupMethodVisitor(Set<Symbol> mockFields, Symbol classSymbol) {
      this.mockFields = mockFields;
      this.classSymbol = classSymbol;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      ExpressionTree expression = tree.expression();
      if (expression instanceof NewClassTree newClass
        && isFieldAssignment(tree.variable())
        && allArgsMockFields(newClass)) {
        issues.add(newClass);
      }
      super.visitAssignmentExpression(tree);
    }

    private boolean isFieldAssignment(ExpressionTree variable) {
      Symbol symbol = extractSymbol(variable);
      return symbol != null && symbol.isVariableSymbol() && symbol.owner().equals(classSymbol);
    }

    private static Symbol extractSymbol(ExpressionTree expression) {
      if (expression instanceof IdentifierTree identifierTree) {
        return identifierTree.symbol();
      }
      if (expression instanceof MemberSelectExpressionTree memberSelect
        && memberSelect.expression() instanceof IdentifierTree identifierTree
        && "this".equals(identifierTree.name())) {
        return memberSelect.identifier().symbol();
      }
      return null;
    }

    private boolean allArgsMockFields(NewClassTree newClass) {
      List<ExpressionTree> args = newClass.arguments();
      if (args.isEmpty()) {
        return false;
      }
      return args.stream().allMatch(arg -> arg.is(Tree.Kind.IDENTIFIER) && mockFields.contains(((IdentifierTree) arg).symbol()));
    }

    @Override
    public void visitClass(ClassTree tree) {
      // don't descend into nested/anonymous classes
    }
  }
}
