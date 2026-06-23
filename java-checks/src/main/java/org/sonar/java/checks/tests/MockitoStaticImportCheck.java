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
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8924")
public class MockitoStaticImportCheck extends IssuableSubscriptionVisitor {

  private static final String MOCKITO_CLASS = "org.mockito.Mockito";

  private static final Set<String> MOCKITO_METHODS = Set.of(
    "doReturn", "doThrow", "mock", "never", "spy", "times", "verify", "when"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    ExpressionTree methodSelect = mit.methodSelect();
    if (!methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      return;
    }
    MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodSelect;
    String methodName = mset.identifier().name();
    if (MOCKITO_METHODS.contains(methodName) && mset.expression().symbolType().is(MOCKITO_CLASS)) {
      reportIssue(methodSelect, "Use a static import for \"" + methodName + "\".");
    }
  }
}
