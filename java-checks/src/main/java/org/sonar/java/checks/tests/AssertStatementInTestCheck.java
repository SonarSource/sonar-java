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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.utils.UnitTestUtils.hasTestAnnotation;

@Rule(key = "S9427")
public class AssertStatementInTestCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Replace this \"assert\" statement with an assertion from the testing framework.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ASSERT_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    var assertStatement = (AssertStatementTree) tree;
    var enclosingTree = ExpressionUtils.getEnclosingTree(assertStatement, Tree.Kind.METHOD);
    if (context.getSemanticModel() != null && enclosingTree instanceof MethodTree method && hasTestAnnotation(method)) {
      reportIssue(assertStatement.assertKeyword(), MESSAGE);
    }
  }
}
