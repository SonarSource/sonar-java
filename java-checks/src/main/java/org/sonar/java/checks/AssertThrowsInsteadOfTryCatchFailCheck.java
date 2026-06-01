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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.*;

import java.util.List;

import static org.sonar.java.checks.helpers.UnitTestUtils.getJUnitVersion;

@Rule(key = "S8714")
public class AssertThrowsInsteadOfTryCatchFailCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.IMPLICIT_CLASS, Tree.Kind.RECORD, Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    List<MethodTree> methods = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .toList();

    int jUnitVersion = getJUnitVersion(methods);
    if (jUnitVersion < 5) {
      return;
    }

    methods.forEach(method -> method.accept(tryStatementsVisitor));
  }

  private final BaseTreeVisitor tryStatementsVisitor = new BaseTreeVisitor() {
    @Override
    public void visitTryStatement(TryStatementTree tree) {
      checkBlock(tree.block(), "Use assertThrows() instead of try/catch and fail() in the try block.");
      tree.catches().forEach(c -> checkBlock(c.block(), "Use assertDoesNotThrow() instead of try/catch and fail() in the catch block."));
      super.visitTryStatement(tree);
    }

    private void checkBlock(BlockTree block, String message) {
      UnitTestUtils.findFail(block).ifPresent(fail ->
        reportIssue(fail, message)
      );
    }
  };
}
