/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5979")
public class MockitoAnnotatedObjectsShouldBeInitializedCheck extends IssuableSubscriptionVisitor {
  private static final List<String> TARGET_ANNOTATIONS = Arrays.asList(
    "org.mockito.Captor",
    "org.mockito.InjectMocks",
    "org.mockito.Mock",
    "org.mockito.Spy"
  );

  private static final List<String> EXPECTED_CLASS_ANNOTATIONS = Arrays.asList(
    "org.junit.jupiter.api.extension.ExtendWith",
    "org.junit.runner.RunWith"
  );

  private static final List<String> BEFORE_ANNOTATIONS = Arrays.asList(
    "org.junit.Before",
    "org.junit.jupiter.api.BeforeEach"
  );

  private static final String RULE_ANNOTATION = "org.junit.Rule";

  private static final MethodMatchers MOCKITO_JUNIT_RULE = MethodMatchers.create()
    .ofAnyType()
    .names("rule")
    .addWithoutParametersMatcher()
    .build();

  private static final String MESSAGE = "Initialize mocks before using them.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree testClass = (ClassTree) tree;
    List<VariableTree> mocksToInitialize = testClass.members().stream()
      .filter(MockitoAnnotatedObjectsShouldBeInitializedCheck::isFieldWithTargetAnnotation)
      .map(VariableTree.class::cast)
      .collect(Collectors.toList());
    if (!mocksToInitialize.isEmpty() && !mocksAreProperlyInitialized(testClass)) {
      AnnotationTree firstAnnotation = mocksToInitialize.get(0).modifiers().annotations().get(0);
      reportIssue(firstAnnotation, MESSAGE);
    }
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
    return isClassProperlyAnnotated(testClass) ||
      isMockitoJUnitRuleInvoked(testClass) ||
      areMocksInitializedInSetup(testClass);
  }

  public static boolean isClassProperlyAnnotated(ClassTree clazz) {
    SymbolMetadata metadata = clazz.symbol().metadata();
    return EXPECTED_CLASS_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
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
          MOCKITO_JUNIT_RULE.matches((MethodInvocationTree) initializer) &&
          field.symbol().metadata().isAnnotatedWith(RULE_ANNOTATION)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean areMocksInitializedInSetup(ClassTree clazz) {
    List<MethodTree> methods = new ArrayList<>();
    Optional<ClassTree> parent = Optional.of(clazz);
    while (parent.isPresent()) {
      ClassTree tree = parent.get();
      methods.addAll(getSetupMethods(tree));
      parent = getParentClass(tree);
    }
    for (MethodTree method : methods) {
      SetupMethodVisitor visitor = new SetupMethodVisitor();
      method.accept(visitor);
      if (visitor.initMocksIsInvoked) {
        return true;
      }
    }
    return false;
  }

  private static Optional<ClassTree> getParentClass(ClassTree tree) {
    TypeTree parentTree = tree.superClass();
    if (parentTree == null) {
      return Optional.empty();
    }
    IdentifierTree identifier = (IdentifierTree) parentTree;
    Tree declaration = identifier.symbol().declaration();
    if (declaration == null) {
      return Optional.empty();
    }
    return Optional.of((ClassTree) declaration);
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
