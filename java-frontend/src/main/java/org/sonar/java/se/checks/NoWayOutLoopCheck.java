/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.se.checks;

import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFGLoop;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Rule(key = "S2189")
public class NoWayOutLoopCheck extends SECheck {

  private static final MethodMatcher THREAD_RUN_MATCHER = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.lang.Thread"))
    .name("run").withoutParameter();

  private enum UpdateType {
    INCREMENT, DECREMENT, INDETERMINATE
  }

  private final Deque<MethodContext> contexts = new LinkedList<>();

  @Override
  public void init(MethodTree tree, CFG cfg) {
    MethodContext context = new MethodContext(tree, cfg);
    contexts.push(context);
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (contexts.peek().isThreadRunMethod()) {
      // It is OK to have an endless Thread run method
      return context.getState();
    }
    final PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    context.alwaysTrueOrFalseExpressions().alwaysTrue().forEach(tree -> {
      Tree statementParent = firstStatementParent(tree);
      if (statementParent != null && statementParent.is(Tree.Kind.WHILE_STATEMENT)) {
        checkLoopWithAlwaysTrueCondition(context, statementParent);
      }
    });
    contexts.pop();
  }

  private void checkLoopWithAlwaysTrueCondition(CheckerContext context, Tree statementParent) {
    CFGLoop loopBlocks = contexts.peek().getLoop(statementParent);
    if (loopBlocks != null && loopBlocks.hasNoWayOut()) {
      context.reportIssue(statementParent, NoWayOutLoopCheck.this, "Add an end condition to this loop.");
    }
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    contexts.pop();
  }

  private class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private final CheckerContext context;

    protected PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.context = context;
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      if (LiteralUtils.isTrue(tree.condition())) {
        checkLoopWithAlwaysTrueCondition(context, tree);
      }
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      if (tree.condition() == null) {
        checkLoopWithAlwaysTrueCondition(context, tree);
      } else if (isConditionUnreachable(tree)) {
        context.reportIssue(tree, NoWayOutLoopCheck.this, "Correct this loop's end condition.");
      }
    }

    private boolean isConditionUnreachable(ForStatementTree tree) {
      UpdatesCollector collector = new UpdatesCollector();
      tree.accept(collector);
      ConditionType condition = new ConditionType(tree.condition(), collector);
      return !condition.isMatched();
    }
  }

  private static class Update {

    private Symbol symbol = null;
    private UpdateType type = null;

    Update(Symbol symbol, UpdateType type) {
      this.symbol = symbol;
      this.type = type;
    }

    UpdateType type() {
      return type;
    }

    boolean concerns(ExpressionTree operand) {
      if (operand.is(Tree.Kind.IDENTIFIER)) {
        return symbol.equals(((IdentifierTree) operand).symbol());
      } else if (operand.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT)) {
        UnaryExpressionTree unary = (UnaryExpressionTree) operand;
        return concerns(unary.expression());
      }
      return false;
    }
  }

  private static class ConditionType {

    private final boolean matched;

    public ConditionType(ExpressionTree condition, UpdatesCollector collector) {
      if (condition.is(Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO)) {
        matched = canBeMatched(((BinaryExpressionTree) condition).leftOperand(), ((BinaryExpressionTree) condition).rightOperand(), collector);
      } else if (condition.is(Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO)) {
        matched = canBeMatched(((BinaryExpressionTree) condition).rightOperand(), ((BinaryExpressionTree) condition).leftOperand(), collector);
      } else {
        matched = true;
      }
    }

    protected boolean canBeMatched(ExpressionTree leftOperand, ExpressionTree rightOperand, UpdatesCollector collector) {
      boolean matchFound = false;
      for (Update update : collector) {
        if (update.concerns(leftOperand)) {
          if (!UpdateType.DECREMENT.equals(update.type())) {
            return true;
          }
          matchFound = true;
        }
        if (update.concerns(rightOperand)) {
          if (!UpdateType.INCREMENT.equals(update.type())) {
            return true;
          }
          matchFound = true;
        }
      }
      return !matchFound;
    }

    public boolean isMatched() {
      return matched;
    }
  }

  private static class UpdatesCollector extends BaseTreeVisitor implements Iterable<Update> {

    private List<Update> updates = new ArrayList<>();

    @Override
    public void visitForStatement(ForStatementTree tree) {
      // Updates in initializer are not of interest
      scan(tree.condition());
      scan(tree.update());
      scan(tree.statement());
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      ExpressionTree assign = tree.variable();
      if (assign.is(Tree.Kind.IDENTIFIER)) {
        UpdateType type;
        if (tree.is(Tree.Kind.PLUS_ASSIGNMENT)) {
          // Will not work if the target is negative
          type = UpdateType.INCREMENT;
        } else if (tree.is(Tree.Kind.MINUS_ASSIGNMENT)) {
          // Will not work if the target is negative
          type = UpdateType.DECREMENT;
        } else {
          // Other assignments are to complex to decide between increment or decrement
          type = UpdateType.INDETERMINATE;
        }
        updates.add(new Update(((IdentifierTree) assign).symbol(), type));
      }
      super.visitAssignmentExpression(tree);
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree expression) {
      ExpressionTree unary = expression.expression();
      if (unary.is(Tree.Kind.IDENTIFIER)) {
        Symbol symbol = ((IdentifierTree) unary).symbol();
        if (expression.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_INCREMENT)) {
          updates.add(new Update(symbol, UpdateType.INCREMENT));
        } else if (expression.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.PREFIX_DECREMENT)) {
          updates.add(new Update(symbol, UpdateType.DECREMENT));
        }
      }
      super.visitUnaryExpression(expression);
    }

    @Override
    public Iterator<Update> iterator() {
      return updates.iterator();
    }
  }

  @CheckForNull
  private static Tree firstStatementParent(Tree node) {
    Tree current = node.parent();
    while (current != null) {
      if (current instanceof StatementTree) {
        break;
      }
      current = current.parent();
    }
    return current;
  }

  private static class MethodContext {

    private final Map<Tree, CFGLoop> loopStarts;
    private final boolean threadRunMethod;

    MethodContext(MethodTree tree, CFG cfg) {
      loopStarts = CFGLoop.getCFGLoops(cfg);
      threadRunMethod = THREAD_RUN_MATCHER.matches(tree);
    }

    boolean isThreadRunMethod() {
      return threadRunMethod;
    }

    CFGLoop getLoop(Tree tree) {
      return loopStarts.get(tree);
    }
  }
}
