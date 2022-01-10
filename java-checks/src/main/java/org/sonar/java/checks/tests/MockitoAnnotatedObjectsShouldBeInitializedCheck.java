/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;

@Rule(key = "S5979")
public class MockitoAnnotatedObjectsShouldBeInitializedCheck extends IssuableSubscriptionVisitor {
  private static final List<String> TARGET_ANNOTATIONS = Arrays.asList(
    "org.mockito.Captor",
    "org.mockito.InjectMocks",
    "org.mockito.Mock",
    "org.mockito.Spy"
  );

  private static final String EXTEND_WITH_ANNOTATION = "org.junit.jupiter.api.extension.ExtendWith";
  private static final String RUN_WITH_ANNOTATION = "org.junit.runner.RunWith";

  private static final List<String> BEFORE_ANNOTATIONS = Arrays.asList(
    "org.junit.Before",
    "org.junit.jupiter.api.BeforeEach"
  );

  private static final String RULE_ANNOTATION = "org.junit.Rule";

  private static final MethodMatchers MOCKITO_JUNIT_RULE = MethodMatchers.create()
    .ofSubTypes("org.mockito.junit.MockitoJUnit")
    .names("rule")
    .addWithoutParametersMatcher()
    .build();

  private static final String MESSAGE = "Initialize mocks before using them.";

  private final Set<ClassTree> coveredByExtendWithAnnotation = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree testClass = (ClassTree) tree;
    if (coveredByExtendWithAnnotation.contains(testClass)) {
      return;
    }

    if (isMetaAnnotated(testClass.symbol(), EXTEND_WITH_ANNOTATION, new HashSet<>())) {
      List<ClassTree> classes = getInnerClassesCoveredByAnnotation(testClass);
      coveredByExtendWithAnnotation.addAll(classes);
      return;
    }

    List<VariableTree> mocksToInitialize = testClass.members().stream()
      .filter(MockitoAnnotatedObjectsShouldBeInitializedCheck::isFieldWithTargetAnnotation)
      .map(VariableTree.class::cast)
      .collect(Collectors.toList());

    if (!mocksToInitialize.isEmpty() && !mocksAreProperlyInitialized(testClass)) {
      AnnotationTree firstAnnotation = mocksToInitialize.get(0).modifiers().annotations().get(0);
      reportIssue(firstAnnotation, MESSAGE);
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    super.leaveFile(context);
    coveredByExtendWithAnnotation.clear();
  }

  private static boolean isMetaAnnotated(Symbol symbol, String annotation, Set<Symbol> visited) {
    if (visited.contains(symbol)) {
      return false;
    }
    for (AnnotationInstance a : symbol.metadata().annotations()) {
      visited.add(symbol);
      if (a.symbol().type().is(annotation) || isMetaAnnotated(a.symbol(), annotation, visited)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAnnotation(ClassTree tree, String annotation) {
    return tree.symbol().metadata().isAnnotatedWith(annotation);
  }

  private static boolean isFieldWithTargetAnnotation(Tree tree) {
    if (!tree.is(Tree.Kind.VARIABLE)) {
      return false;
    }
    VariableTree field = (VariableTree) tree;
    SymbolMetadata metadata = field.symbol().metadata();
    return TARGET_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }

  private static boolean mocksAreProperlyInitialized(ClassTree testClass) {
    return hasAnnotation(testClass, RUN_WITH_ANNOTATION) ||
      isMockitoJUnitRuleInvoked(testClass) ||
      areMocksInitializedInSetup(testClass);
  }


  private static List<ClassTree> getInnerClassesCoveredByAnnotation(ClassTree tree) {
    NestedClassesCollector collector = new NestedClassesCollector();
    tree.accept(collector);
    return collector.classes;
  }

  private static boolean isMockitoJUnitRuleInvoked(ClassTree clazz) {
    List<VariableTree> collected = clazz.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .collect(Collectors.toList());
    for (VariableTree field : collected) {
      if (field.type().symbolType().is("org.mockito.junit.MockitoRule")) {
        ExpressionTree initializer = field.initializer();
        if (initializer != null && initializer.is(Tree.Kind.METHOD_INVOCATION) &&
          field.symbol().metadata().isAnnotatedWith(RULE_ANNOTATION) &&
          isInitializedWithRule((MethodInvocationTree) initializer)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isInitializedWithRule(MethodInvocationTree mit) {
    MethodInvocationTree current = mit;
    while (true) {
      if (MOCKITO_JUNIT_RULE.matches(current)) {
        return true;
      }
      ExpressionTree expressionTree = current.methodSelect();
      if (!expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        return false;
      }
      ExpressionTree expression = ((MemberSelectExpressionTree) expressionTree).expression();
      if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
        return false;
      }
      current = (MethodInvocationTree) expression;
    }
  }

  private static boolean areMocksInitializedInSetup(ClassTree clazz) {
    List<MethodTree> methods = getSetupMethods(clazz);
    for (MethodTree method : methods) {
      SetupMethodVisitor visitor = new SetupMethodVisitor();
      method.accept(visitor);
      if (visitor.initMocksIsInvoked) {
        return true;
      }
    }
    return hasParentClass(clazz);
  }

  private static boolean hasParentClass(ClassTree tree) {
    return tree.superClass() != null;
  }

  private static List<MethodTree> getSetupMethods(ClassTree tree) {
    return tree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(MockitoAnnotatedObjectsShouldBeInitializedCheck::isTaggedWithBefore)
      .collect(Collectors.toList());
  }

  private static boolean isTaggedWithBefore(MethodTree method) {
    SymbolMetadata metadata = method.symbol().metadata();
    return BEFORE_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }


  /**
   * Traverses a tree looking for classes annotated with JUnit5's Nested
   */
  static class NestedClassesCollector extends BaseTreeVisitor {
    private static final String NESTED_ANNOTATION = "org.junit.jupiter.api.Nested";
    private final List<ClassTree> classes = new ArrayList<>();

    @Override
    public void visitClass(ClassTree tree) {
      if (tree.symbol().metadata().isAnnotatedWith(NESTED_ANNOTATION)) {
        classes.add(tree);
      }
      tree.members().stream()
        .filter(member -> member.is(Tree.Kind.CLASS))
        .map(ClassTree.class::cast)
        .forEach(child -> child.accept(this));
    }
  }

  /**
   * Traverses a tree looking for an invocation of org.mockito.MockitoAnnotations.initMocks
   */
  private static class SetupMethodVisitor extends BaseTreeVisitor {
    private static final MethodMatchers OPEN_OR_INIT_MOCKS = MethodMatchers.create()
      .ofTypes("org.mockito.MockitoAnnotations")
      .names("openMocks", "initMocks")
      .addParametersMatcher("java.lang.Object")
      .build();

    private boolean initMocksIsInvoked = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (OPEN_OR_INIT_MOCKS.matches(tree)) {
        initMocksIsInvoked = true;
      }
    }
  }
}
