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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5979")
public class MockitoAnnotatedObjectsShouldBeInitializedCheck extends IssuableSubscriptionVisitor {
  private static final List<String> TARGET_ANNOTATIONS = Arrays.asList(
    "Captor",
    "InjectMocks",
    "Mock",
    "Spy"
  );

  private static final List<String> EXPECTED_CLASS_ANNOTATIONS = Arrays.asList(
    "ExtendWith",
    "RunWith"
  );

  private static final List<String> BEFORE_ANNOTATIONS = Arrays.asList(
    "Before",
    "BeforeEach"
  );

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
    List<AnnotationTree> annotations = field.modifiers().annotations();
    return !getAnnotations(annotations, TARGET_ANNOTATIONS).isEmpty();
  }

  private static boolean mocksAreProperlyInitialized(ClassTree testClass) {
    return isClassProperlyAnnotated(testClass) ||
      isMockitoJUnitRuleInvoked(testClass) ||
      areMocksInitializedInSetup(testClass);
  }

  public static boolean isClassProperlyAnnotated(ClassTree clazz) {
    List<AnnotationTree> annotations = clazz.modifiers().annotations();
    return !getAnnotations(annotations, EXPECTED_CLASS_ANNOTATIONS).isEmpty();
  }

  private static boolean isMockitoJUnitRuleInvoked(ClassTree clazz) {
    List<VariableTree> collected = clazz.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .collect(Collectors.toList());
    for (VariableTree field : collected) {
      String type = field.type().symbolType().fullyQualifiedName();
      if (type.equals("org.mockito.junit.MockitoRule")) {
        ExpressionTree initializer = field.initializer();
        if (initializer != null && initializer.is(Tree.Kind.METHOD_INVOCATION) &&
          MOCKITO_JUNIT_RULE.matches((MethodInvocationTree) initializer) &&
          getAnnotation(field.modifiers().annotations(), "Rule").isPresent()) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean areMocksInitializedInSetup(ClassTree clazz) {
    List<MethodTree> methods = clazz.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .collect(Collectors.toList());
    for (MethodTree method : methods) {
      List<AnnotationTree> setupAnnotations = getAnnotations(method.modifiers().annotations(), BEFORE_ANNOTATIONS);
      if (!setupAnnotations.isEmpty()) {
        SetupMethodVisitor visitor = new SetupMethodVisitor();
        method.accept(visitor);
        if (visitor.initMocksIsInvoked) {
          return true;
        }
      }
    }
    return false;
  }

  private static List<AnnotationTree> getAnnotations(List<AnnotationTree> annotations, List<String> names) {
    return names.stream()
      .map(name -> getAnnotation(annotations, name))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
  }

  private static Optional<AnnotationTree> getAnnotation(List<AnnotationTree> annotations, String annotationName) {
    for (AnnotationTree annotation : annotations) {
      if (annotation.annotationType().toString().equals(annotationName)) {
        return Optional.of(annotation);
      }
    }
    return Optional.empty();
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
