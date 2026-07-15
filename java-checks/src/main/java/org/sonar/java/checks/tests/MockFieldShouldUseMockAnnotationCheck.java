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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9015")
public class MockFieldShouldUseMockAnnotationCheck extends IssuableSubscriptionVisitor {

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
    if (!MockitoManagedClassHelper.isMockitoManagedClass(classTree)) {
      return;
    }
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .filter(MockFieldShouldUseMockAnnotationCheck::hasFieldMockInitializer)
      .forEach(field -> reportIssue(field.initializer(), MESSAGE));
  }

  private static boolean hasFieldMockInitializer(VariableTree field) {
    ExpressionTree initializer = field.initializer();
    return initializer != null
      && initializer.is(Tree.Kind.METHOD_INVOCATION)
      && MOCK_METHOD.matches(((MethodInvocationTree) initializer).methodSymbol());
  }
}
