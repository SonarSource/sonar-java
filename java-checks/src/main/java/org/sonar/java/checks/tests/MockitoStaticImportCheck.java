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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8924")
public class MockitoStaticImportCheck extends IssuableSubscriptionVisitor {

  private static final String MOCKITO_CLASS = "org.mockito.Mockito";
  private static final String MOCKITO_IMPORT_PREFIX = MOCKITO_CLASS + ".";

  private static final MethodMatchers MOCKITO_METHODS = MethodMatchers.create()
    .ofTypes(MOCKITO_CLASS)
    .names("doReturn", "doThrow", "mock", "never", "spy", "times", "verify", "when")
    .withAnyParameters()
    .build();

  private Set<String> conflictingImportedNames = new HashSet<>();
  private final Deque<Set<String>> classMethodsStack = new ArrayDeque<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.COMPILATION_UNIT, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.RECORD, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case COMPILATION_UNIT -> collectConflictingImports((CompilationUnitTree) tree);
      case CLASS, ENUM, INTERFACE, RECORD -> pushClassMethods((ClassTree) tree);
      case METHOD_INVOCATION -> checkMethodInvocation((MethodInvocationTree) tree);
      default -> { /* not visited */ }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    switch (tree.kind()) {
      case CLASS, ENUM, INTERFACE, RECORD -> classMethodsStack.pop();
      default -> { /* nothing */ }
    }
  }

  private void collectConflictingImports(CompilationUnitTree compilationUnitTree) {
    conflictingImportedNames = compilationUnitTree.imports().stream()
      .filter(clause -> clause instanceof ImportTree importTree && importTree.isStatic())
      .map(clause -> ExpressionsHelper.concatenate((ExpressionTree) ((ImportTree) clause).qualifiedIdentifier()))
      .filter(fqn -> !fqn.startsWith(MOCKITO_IMPORT_PREFIX) && !fqn.endsWith(".*"))
      .map(fqn -> fqn.substring(fqn.lastIndexOf('.') + 1))
      .collect(Collectors.toSet());
  }

  private void pushClassMethods(ClassTree classTree) {
    Set<String> methodNames = new HashSet<>();
    for (Tree member : classTree.members()) {
      if (member instanceof MethodTree methodTree) {
        methodNames.add(methodTree.simpleName().name());
      }
    }
    classMethodsStack.push(methodNames);
  }

  private void checkMethodInvocation(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (!(methodSelect instanceof MemberSelectExpressionTree mset)) {
      return;
    }
    String methodName = mset.identifier().name();
    if (MOCKITO_METHODS.matches(mit.methodSymbol()) && !requiresTypeWitness(mit) && !isNameInConflict(methodName)) {
      reportIssue(methodSelect, "Use a static import for \"%s\".".formatted(methodName));
    }
  }

  private boolean isNameInConflict(String methodName) {
    return conflictingImportedNames.contains(methodName)
      || classMethodsStack.stream().anyMatch(methods -> methods.contains(methodName));
  }

  private static boolean requiresTypeWitness(MethodInvocationTree mit) {
    if (mit.typeArguments() == null) {
      return false;
    }
    Tree parent = mit.parent();
    if (parent instanceof VariableTree variableTree) {
      return variableTree.type().is(Tree.Kind.VAR_TYPE);
    }
    return true;
  }
}
