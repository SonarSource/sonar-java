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
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S8795")
public class S8795Check extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Remove this unreachable code.";
  private static final String SECONDARY_MESSAGE = "Control flow is transferred here.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.METHOD,
      Tree.Kind.CONSTRUCTOR,
      Tree.Kind.STATIC_INITIALIZER,
      Tree.Kind.INITIALIZER);
  }

  @Override
  public void visitNode(Tree tree) {
    BlockTree block = extractBlock(tree);
    if (block != null) {
      checkStatements(block.body());
    }
  }

  private static BlockTree extractBlock(Tree tree) {
    if (tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
      return ((MethodTree) tree).block();
    } else if (tree.is(Tree.Kind.STATIC_INITIALIZER, Tree.Kind.INITIALIZER)) {
      return (BlockTree) tree;
    }
    return null;
  }

  private void checkStatements(List<StatementTree> statements) {
    StatementTree jumpStatement = null;
    for (StatementTree statement : statements) {
      if (jumpStatement != null) {
        reportIssue(
          statement,
          ISSUE_MESSAGE,
          List.of(new JavaFileScannerContext.Location(SECONDARY_MESSAGE, jumpStatement)),
          null);
        return;
      }
      if (isUnconditionalJump(statement)) {
        jumpStatement = statement;
      }
      visitNestedBlocks(statement);
    }
  }

  private static boolean isUnconditionalJump(StatementTree statement) {
    return statement.is(
      Tree.Kind.RETURN_STATEMENT,
      Tree.Kind.THROW_STATEMENT,
      Tree.Kind.BREAK_STATEMENT,
      Tree.Kind.CONTINUE_STATEMENT);
  }

  private void visitNestedBlocks(StatementTree statement) {
    switch (statement.kind()) {
      case IF_STATEMENT:
        IfStatementTree ifStmt = (IfStatementTree) statement;
        checkBlock(ifStmt.thenStatement());
        if (ifStmt.elseStatement() != null) {
          checkBlock(ifStmt.elseStatement());
        }
        break;
      case FOR_STATEMENT:
        checkBlock(((ForStatementTree) statement).statement());
        break;
      case FOR_EACH_STATEMENT:
        checkBlock(((ForEachStatement) statement).statement());
        break;
      case WHILE_STATEMENT:
        checkBlock(((WhileStatementTree) statement).statement());
        break;
      case DO_STATEMENT:
        checkBlock(((DoWhileStatementTree) statement).statement());
        break;
      case SYNCHRONIZED_STATEMENT:
        checkStatements(((SynchronizedStatementTree) statement).block().body());
        break;
      case LABELED_STATEMENT:
        checkBlock(((LabeledStatementTree) statement).statement());
        break;
      case TRY_STATEMENT:
        visitTryStatement((TryStatementTree) statement);
        break;
      case SWITCH_STATEMENT:
        visitSwitchStatement((SwitchStatementTree) statement);
        break;
      default:
        break;
    }
  }

  private void checkBlock(StatementTree statement) {
    if (statement.is(Tree.Kind.BLOCK)) {
      checkStatements(((BlockTree) statement).body());
    } else {
      // Single-statement body: no unreachable siblings possible in this block,
      // but we still recurse into any nested blocks it may contain
      visitNestedBlocks(statement);
    }
  }

  private void visitTryStatement(TryStatementTree tryStatement) {
    checkStatements(tryStatement.block().body());
    for (CatchTree catchTree : tryStatement.catches()) {
      checkStatements(catchTree.block().body());
    }
    if (tryStatement.finallyBlock() != null) {
      checkStatements(tryStatement.finallyBlock().body());
    }
  }

  private void visitSwitchStatement(SwitchStatementTree switchStatement) {
    for (CaseGroupTree caseGroup : switchStatement.cases()) {
      checkStatements(caseGroup.body());
    }
  }
}
