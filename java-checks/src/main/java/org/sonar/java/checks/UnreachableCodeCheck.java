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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8795")
public class UnreachableCodeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.BLOCK, Tree.Kind.CASE_GROUP);
  }

  @Override
  public void visitNode(Tree tree) {
    List<StatementTree> statements;
    if (tree.is(Tree.Kind.BLOCK)) {
      statements = ((BlockTree) tree).body();
    } else {
      statements = ((CaseGroupTree) tree).body();
    }

    for (int i = 0; i < statements.size() - 1; i++) {
      StatementTree statement = statements.get(i);
      if (isUnconditionalJump(statement)) {
        StatementTree unreachableStatement = statements.get(i + 1);
        reportIssue(unreachableStatement, "Remove this unreachable code.");
        break;
      }
    }
  }

  private static boolean isUnconditionalJump(StatementTree statement) {
    return statement.is(
      Tree.Kind.RETURN_STATEMENT,
      Tree.Kind.THROW_STATEMENT,
      Tree.Kind.BREAK_STATEMENT,
      Tree.Kind.CONTINUE_STATEMENT
    );
  }
}
