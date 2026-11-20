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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import java.util.List;

@Rule(key = "S106")
public class SystemOutOrErrUsageCheck extends IssuableSubscriptionVisitor {

  private boolean isCompactSourceFile = false;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(
      Tree.Kind.COMPILATION_UNIT,
      Tree.Kind.IMPLICIT_CLASS,
      Tree.Kind.MEMBER_SELECT
    );
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      isCompactSourceFile = false;
    } else if (tree.is(Tree.Kind.IMPLICIT_CLASS)) {
      // System.out or System.err is allowed in compact source files.
      isCompactSourceFile = true;
    } else if (!isCompactSourceFile && tree instanceof MemberSelectExpressionTree mset) {
      visitMemberSelectExpression(mset);
    }
  }

  private void visitMemberSelectExpression(MemberSelectExpressionTree mset) {
    if (isIoPrintFunction(mset)) {
      reportIssue(mset, "Replace this use of IO." + mset.identifier().name() + " by a logger.");
    }
  }

  private static boolean isIoPrintFunction(MemberSelectExpressionTree mset) {
    boolean isIoFunction = mset.expression().parent() instanceof MemberSelectExpressionTree tree &&
      tree.expression() instanceof IdentifierTree identifierTree &&
      "IO".equals(identifierTree.name());
    // use contains to cover both print and println methods
    return isIoFunction && mset.identifier().name().contains("print");
  }
}
