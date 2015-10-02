/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.cfg;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CFG {

  private final Block exitBlock;
  private final Symbol.MethodSymbol methodSymbol;
  private Block currentBlock;

  /**
   * List of all blocks in order they were created.
   */
  private final List<Block> blocks = new ArrayList<>();

  private final Deque<Block> breakTargets = new LinkedList<>();
  private final Deque<Block> continueTargets = new LinkedList<>();

  private final Deque<Block> switches = new LinkedList<>();
  private Map<String, Block> labels = Maps.newHashMap();
  private final List<Block> gotos = new LinkedList<>();

  public Symbol.MethodSymbol methodSymbol() {
    return methodSymbol;
  }

  public Block entry() {
    return currentBlock;
  }

  public List<Block> blocks() {
    return Lists.reverse(blocks);
  }

  public List<Block> reversedBlocks() {
    return blocks;
  }

  public static class Block {
    public final int id;
    private final List<Tree> elements = new ArrayList<>();
    private final List<Block> successors = new ArrayList<>();
    private final List<Block> predecessors = new ArrayList<>();
    private Tree terminator;

    public Block(int id) {
      this.id = id;
    }

    public List<Tree> elements() {
      return Lists.reverse(elements);
    }

    public List<Block> predecessors() {
      return predecessors;
    }

    public List<Block> successors() {
      return successors;
    }

    @CheckForNull
    public Tree terminator() {
      return terminator;
    }
  }

  private CFG(BlockTree tree, Symbol.MethodSymbol symbol) {
    methodSymbol = symbol;
    exitBlock = createBlock();
    currentBlock = createBlock(exitBlock);
    for (StatementTree statementTree : Lists.reverse(tree.body())) {
      build(statementTree);
    }

    for (Block b : gotos) {
      assert b.successors.isEmpty();
      Tree s = b.terminator;
      assert s != null;
      String label;
      if (s.is(Tree.Kind.BREAK_STATEMENT)) {
        label = ((BreakStatementTree) s).label().name();
      } else {
        label = ((ContinueStatementTree) s).label().name();
      }
      Block target = labels.get(label);
      if (target == null) {
        throw new IllegalStateException("Undeclared label: " + label);
      }
      b.successors.add(target);
    }

    for (Block b : blocks) {
      for (Block successor : b.successors) {
        successor.predecessors.add(b);
      }
    }
  }

  private Block createBlock(Block successor) {
    Block result = createBlock();
    result.successors.add(successor);
    return result;
  }

  private Block createBlock() {
    Block result = new Block(blocks.size());
    blocks.add(result);
    return result;
  }

  public static CFG build(MethodTree tree) {
    Preconditions.checkArgument(tree.block() != null, "Cannot build CFG for method with no body.");
    return new CFG(tree.block(), tree.symbol());
  }

  private void build(List<? extends Tree> trees) {
    for (Tree tree : Lists.reverse(trees)) {
      build(tree);
    }
  }

  private void build(Tree tree) {
    switch (tree.kind()) {
      case BLOCK:
        build(((BlockTree) tree).body());
        break;
      case RETURN_STATEMENT:
        buildReturnStatement((ReturnStatementTree) tree);
        break;
      case EXPRESSION_STATEMENT:
        build(((ExpressionStatementTree) tree).expression());
        break;
      case METHOD_INVOCATION:
        buildMethodInvocation((MethodInvocationTree) tree);
        break;
      case IF_STATEMENT:
        buildIfStatement((IfStatementTree) tree);
        break;
      case CONDITIONAL_EXPRESSION:
        buildConditionalExpression((ConditionalExpressionTree) tree);
        break;
      case VARIABLE:
        buildVariable((VariableTree) tree);
        break;
      case MULTIPLY:
      case DIVIDE:
      case REMAINDER:
      case PLUS:
      case MINUS:
      case LEFT_SHIFT:
      case RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT:
      case AND:
      case XOR:
      case OR:
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL_TO:
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL_TO:
      case EQUAL_TO:
      case NOT_EQUAL_TO:
        buildBinaryExpression(tree);
        break;
      case ASSIGNMENT:
      case LEFT_SHIFT_ASSIGNMENT:
      case RIGHT_SHIFT_ASSIGNMENT:
      case AND_ASSIGNMENT:
      case REMAINDER_ASSIGNMENT:
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
      case OR_ASSIGNMENT:
      case XOR_ASSIGNMENT:
      case DIVIDE_ASSIGNMENT:
      case MULTIPLY_ASSIGNMENT:
      case PLUS_ASSIGNMENT:
      case MINUS_ASSIGNMENT:
        buildAssignment((AssignmentExpressionTree) tree);
        break;
      case MEMBER_SELECT:
        buildMemberSelect((MemberSelectExpressionTree) tree);
        break;
      case CONDITIONAL_AND:
        buildConditionalAnd((BinaryExpressionTree) tree);
        break;
      case CONDITIONAL_OR:
        buildConditionalOr((BinaryExpressionTree) tree);
        break;
      case LABELED_STATEMENT:
        buildLabeledStatement((LabeledStatementTree) tree);
        break;
      case SWITCH_STATEMENT:
        buildSwitchStatement((SwitchStatementTree) tree);
        break;
      case BREAK_STATEMENT:
        buildBreakStatement((BreakStatementTree) tree);
        break;
      case CONTINUE_STATEMENT:
        buildContinueStatement((ContinueStatementTree) tree);
        break;
      case WHILE_STATEMENT:
        buildWhileStatement((WhileStatementTree) tree);
        break;
      case DO_STATEMENT:
        buildDoWhileStatement((DoWhileStatementTree) tree);
        break;
      case FOR_EACH_STATEMENT:
        buildForEachStatement((ForEachStatement) tree);
        break;
      case FOR_STATEMENT:
        buildForStatement((ForStatementTree) tree);
        break;
      case TRY_STATEMENT:
        buildTryStatement((TryStatementTree) tree);
        break;
      case THROW_STATEMENT:
        buildThrowStatement((ThrowStatementTree) tree);
        break;
      case SYNCHRONIZED_STATEMENT:
        buildSynchronizedStatement((SynchronizedStatementTree) tree);
        break;
      case POSTFIX_INCREMENT:
      case POSTFIX_DECREMENT:
      case PREFIX_INCREMENT:
      case PREFIX_DECREMENT:
      case UNARY_MINUS:
      case UNARY_PLUS:
      case BITWISE_COMPLEMENT:
      case LOGICAL_COMPLEMENT:
        buildUnaryExpression((UnaryExpressionTree) tree);
        break;
      case PARENTHESIZED_EXPRESSION:
        build(((ParenthesizedTree) tree).expression());
        break;
      case ARRAY_ACCESS_EXPRESSION:
        buildArrayAccessExpression((ArrayAccessExpressionTree) tree);
        break;
      case ARRAY_DIMENSION:
        buildArrayDimension((ArrayDimensionTree) tree);
        break;
      case NEW_CLASS:
        buildNewClass((NewClassTree) tree);
        break;
      case TYPE_CAST:
        buildTypeCast(tree);
        break;
      case INSTANCE_OF:
        buildInstanceOf((InstanceOfTree) tree);
        break;
      case NEW_ARRAY:
        buildNewArray((NewArrayTree) tree);
        break;
      // Java 8 constructions : ignored for now.
      case METHOD_REFERENCE:
      // assert can be ignored by VM so skip them for now.
      case ASSERT_STATEMENT:
        break;
      // store declarations as complete blocks.
      case EMPTY_STATEMENT:
      case CLASS:
      case ENUM:
      case ANNOTATION_TYPE:
      case INTERFACE:
      case LAMBDA_EXPRESSION:
      // simple instructions
      case IDENTIFIER:
      case INT_LITERAL:
      case LONG_LITERAL:
      case DOUBLE_LITERAL:
      case CHAR_LITERAL:
      case FLOAT_LITERAL:
      case STRING_LITERAL:
      case BOOLEAN_LITERAL:
      case NULL_LITERAL:
        currentBlock.elements.add(tree);
        break;
      default:
        throw new UnsupportedOperationException(tree.kind().name() + " " + ((JavaTree) tree).getLine());
    }
  }

  private void buildReturnStatement(ReturnStatementTree tree) {
    ReturnStatementTree s = tree;
    currentBlock = createUnconditionalJump(s, exitBlock);
    ExpressionTree expression = s.expression();
    if (expression != null) {
      build(expression);
    }
  }

  private void buildMethodInvocation(MethodInvocationTree tree) {
    MethodInvocationTree mit = tree;
    currentBlock.elements.add(mit);
    build(mit.methodSelect());
    for (ExpressionTree arg : Lists.reverse(mit.arguments())) {
      build(arg);
    }
  }

  private void buildIfStatement(IfStatementTree tree) {
    IfStatementTree ifStatementTree = tree;
    Block next = currentBlock;
    // process else-branch
    Block elseBlock = next;
    StatementTree elseStatement = ifStatementTree.elseStatement();
    if (elseStatement != null) {
      // if statement will create the required block.
      if (!elseStatement.is(Tree.Kind.IF_STATEMENT)) {
        currentBlock = createBlock(next);
      }
      build(elseStatement);
      elseBlock = currentBlock;
    }
    // process then-branch
    currentBlock = createBlock(next);
    build(ifStatementTree.thenStatement());
    Block thenBlock = currentBlock;
    // process condition
    currentBlock = createBranch(ifStatementTree, thenBlock, elseBlock);
    buildCondition(ifStatementTree.condition(), thenBlock, elseBlock);
  }

  private void buildConditionalExpression(ConditionalExpressionTree tree) {
    ConditionalExpressionTree cond = tree;
    Block next = currentBlock;
    // process else-branch
    ExpressionTree elseStatement = cond.falseExpression();
    currentBlock = createBlock(next);
    build(elseStatement);
    Block elseBlock = currentBlock;
    // process then-branch
    currentBlock = createBlock(next);
    build(cond.trueExpression());
    Block thenBlock = currentBlock;
    // process condition
    currentBlock = createBranch(cond, thenBlock, elseBlock);
    buildCondition(cond.condition(), thenBlock, elseBlock);
  }

  private void buildVariable(VariableTree tree) {
    currentBlock.elements.add(tree);
    ExpressionTree initializer = tree.initializer();
    if (initializer != null) {
      build(initializer);
    }
  }

  private void buildBinaryExpression(Tree tree) {
    BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
    currentBlock.elements.add(tree);
    build(binaryExpressionTree.rightOperand());
    build(binaryExpressionTree.leftOperand());
  }

  private void buildAssignment(AssignmentExpressionTree tree) {
    currentBlock.elements.add(tree);
    build(tree.variable());
    build(tree.expression());
  }

  private void buildMemberSelect(MemberSelectExpressionTree tree) {
    MemberSelectExpressionTree mse = tree;
    currentBlock.elements.add(mse);
    // int.class or String[].class are memberSelectExpression which expression part is not an expression.
    if (!"class".equals(mse.identifier().name())) {
      build(mse.expression());
    }
  }

  private void buildConditionalAnd(BinaryExpressionTree tree) {
    // process RHS
    Block falseBlock = currentBlock;
    currentBlock = createBlock(falseBlock);
    build(tree.rightOperand());
    Block trueBlock = currentBlock;
    // process LHS
    currentBlock = createBranch(tree, trueBlock, falseBlock);
    build(tree.leftOperand());
  }

  private void buildConditionalOr(BinaryExpressionTree tree) {
    // process RHS
    Block trueBlock = currentBlock;
    currentBlock = createBlock(trueBlock);
    build(tree.rightOperand());
    Block falseBlock = currentBlock;
    // process LHS
    currentBlock = createBranch(tree, trueBlock, falseBlock);
    build(tree.leftOperand());
  }

  private void buildLabeledStatement(LabeledStatementTree tree) {
    LabeledStatementTree s = tree;
    build(s.statement());
    currentBlock = createBlock(currentBlock);
    labels.put(s.label().name(), currentBlock);
    return;
  }

  private void buildSwitchStatement(SwitchStatementTree tree) {
    // FIXME useless node created for default cases.
    SwitchStatementTree switchStatementTree = tree;
    Block switchSuccessor = currentBlock;
    // process condition
    currentBlock = createBlock();
    currentBlock.terminator = switchStatementTree;
    switches.addLast(currentBlock);
    build(switchStatementTree.expression());
    // process body
    currentBlock = createBlock(switchSuccessor);
    breakTargets.addLast(switchSuccessor);
    if (!switchStatementTree.cases().isEmpty()) {
      CaseGroupTree firstCase = switchStatementTree.cases().get(0);
      for (CaseGroupTree caseGroupTree : Lists.reverse(switchStatementTree.cases())) {
        build(caseGroupTree.body());
        switches.getLast().successors.add(currentBlock);
        if (!caseGroupTree.equals(firstCase)) {
          // No block predecessing the first case group.
          currentBlock = createBlock(currentBlock);
        }
      }
    }
    breakTargets.removeLast();
    // process condition
    currentBlock = switches.removeLast();
  }

  private void buildBreakStatement(BreakStatementTree tree) {
    if (tree.label() == null) {
      if (breakTargets.isEmpty()) {
        throw new IllegalStateException("'break' statement not in loop or switch statement");
      }
      currentBlock = createUnconditionalJump(tree, breakTargets.getLast());
    } else {
      currentBlock = createUnconditionalJump(tree, null);
      gotos.add(currentBlock);
    }
  }

  private void buildContinueStatement(ContinueStatementTree tree) {
    if (tree.label() == null) {
      if (continueTargets.isEmpty()) {
        throw new IllegalStateException("'continue' statement not in loop or switch statement");
      }
      currentBlock = createUnconditionalJump(tree, continueTargets.getLast());
    } else {
      currentBlock = createUnconditionalJump(tree, null);
      gotos.add(currentBlock);
    }
  }

  private void buildWhileStatement(WhileStatementTree tree) {
    WhileStatementTree s = tree;
    Block falseBranch = currentBlock;
    Block loopback = createBlock();
    // process body
    currentBlock = createBlock(loopback);
    continueTargets.addLast(loopback);
    breakTargets.addLast(falseBranch);
    build(s.statement());
    breakTargets.removeLast();
    continueTargets.removeLast();
    Block bodyBlock = currentBlock;
    // process condition
    currentBlock = createBranch(s, bodyBlock, falseBranch);
    buildCondition(s.condition(), bodyBlock, falseBranch);
    loopback.successors.add(currentBlock);
    currentBlock = createBlock(currentBlock);
  }

  private void buildDoWhileStatement(DoWhileStatementTree tree) {
    DoWhileStatementTree s = tree;
    Block falseBranch = currentBlock;
    Block loopback = createBlock();
    // process condition
    currentBlock = createBranch(s, loopback, falseBranch);
    buildCondition(s.condition(), loopback, falseBranch);
    // process body
    currentBlock = createBlock(currentBlock);
    continueTargets.addLast(loopback);
    breakTargets.addLast(falseBranch);
    build(s.statement());
    breakTargets.removeLast();
    continueTargets.removeLast();
    loopback.successors.add(currentBlock);
    currentBlock = createBlock(currentBlock);
  }

  private void buildForEachStatement(ForEachStatement tree) {
    // TODO(npe) One solution is to create a forstatement node depending on type of expression (iterable or array) and build CFG from it.
    Block afterLoop = currentBlock;
    currentBlock = createBlock();
    Block loopback = currentBlock;
    continueTargets.addLast(loopback);
    breakTargets.addLast(afterLoop);
    build(tree.statement());
    breakTargets.removeLast();
    continueTargets.removeLast();
    currentBlock = createBranch(tree, currentBlock, afterLoop);
    loopback.successors.add(currentBlock);
    build(tree.variable());
    build(tree.expression());
    currentBlock = createBlock(currentBlock);
  }

  private void buildForStatement(ForStatementTree tree) {
    Block falseBranch = currentBlock;
    // process step
    currentBlock = createBlock();
    Block updateBlock = currentBlock;
    for (StatementTree updateTree : Lists.reverse(tree.update())) {
      build(updateTree);
    }
    continueTargets.addLast(currentBlock);
    // process body
    currentBlock = createBlock(currentBlock);
    breakTargets.addLast(falseBranch);
    build(tree.statement());
    breakTargets.removeLast();
    continueTargets.removeLast();
    Block body = currentBlock;
    // process condition
    ExpressionTree condition = tree.condition();
    if (condition != null) {
      currentBlock = createBranch(tree, body, falseBranch);
      buildCondition(condition, body, falseBranch);
    } else {
      currentBlock = createUnconditionalJump(tree, body);
    }
    updateBlock.successors.add(currentBlock);
    // process init
    currentBlock = createBlock(currentBlock);
    for (StatementTree init : Lists.reverse(tree.initializer())) {
      build(init);
    }
  }

  private void buildTryStatement(TryStatementTree tree) {
    // FIXME only path with no failure constructed for now, (not taking try with resources into consideration).
    TryStatementTree tryStatementTree = tree;
    currentBlock = createBlock(currentBlock);
    BlockTree finallyBlock = tryStatementTree.finallyBlock();
    if (finallyBlock != null) {
      build(finallyBlock);
    }
    currentBlock = createBlock(currentBlock);
    build(tryStatementTree.block());
    build((List<? extends Tree>) tryStatementTree.resources());
    currentBlock = createBlock(currentBlock);
    currentBlock.elements.add(tree);
  }

  private void buildThrowStatement(ThrowStatementTree tree) {
    // FIXME this won't work if it is intended to be caught by a try statement.
    ThrowStatementTree throwStatementTree = tree;
    currentBlock = createUnconditionalJump(throwStatementTree, exitBlock);
    build(throwStatementTree.expression());
  }

  private void buildSynchronizedStatement(SynchronizedStatementTree tree) {
    SynchronizedStatementTree sst = tree;
    // Naively build synchronized statement.
    build(sst.block());
    build(sst.expression());
  }

  private void buildUnaryExpression(UnaryExpressionTree tree) {
    currentBlock.elements.add(tree);
    build(tree.expression());
  }

  private void buildArrayAccessExpression(ArrayAccessExpressionTree tree) {
    currentBlock.elements.add(tree);
    build(tree.expression());
    build(tree.dimension());
  }

  private void buildArrayDimension(ArrayDimensionTree tree) {
    ExpressionTree expression = tree.expression();
    if (expression != null) {
      build(expression);
    }
  }

  private void buildNewClass(NewClassTree tree) {
    currentBlock.elements.add(tree);
    ExpressionTree enclosingExpression = tree.enclosingExpression();
    if (enclosingExpression != null) {
      build(enclosingExpression);
    }
    build(Lists.reverse(tree.arguments()));
  }

  private void buildTypeCast(Tree tree) {
    currentBlock.elements.add(tree);
    TypeCastTree typeCastTree = (TypeCastTree) tree;
    build(typeCastTree.expression());
  }

  private void buildInstanceOf(InstanceOfTree instanceOfTree) {
    currentBlock.elements.add(instanceOfTree);
    build(instanceOfTree.expression());
  }

  private void buildNewArray(NewArrayTree tree) {
    currentBlock.elements.add(tree);
    build(Lists.reverse(tree.dimensions()));
    build(Lists.reverse(tree.initializers()));
  }

  private Block createUnconditionalJump(Tree terminator, @Nullable Block target) {
    Block result = createBlock();
    result.terminator = terminator;
    if (target != null) {
      result.successors.add(target);
    }
    return result;
  }

  private void buildCondition(Tree syntaxNode, Block trueBlock, Block falseBlock) {
    switch (syntaxNode.kind()) {
      case CONDITIONAL_OR:
        buildConditionalOr((BinaryExpressionTree) syntaxNode, trueBlock, falseBlock);
        break;
      case CONDITIONAL_AND:
        // process RHS
        buildConditionalAnd((BinaryExpressionTree) syntaxNode, trueBlock, falseBlock);
        break;
      // Skip syntactic sugar:
      case PARENTHESIZED_EXPRESSION:
        buildCondition(((ParenthesizedTree) syntaxNode).expression(), trueBlock, falseBlock);
        break;
      default:
        build(syntaxNode);
        break;
    }
  }

  private void buildConditionalOr(BinaryExpressionTree conditionalOr, Block trueBlock, Block falseBlock) {
    // process RHS
    buildCondition(conditionalOr.rightOperand(), trueBlock, falseBlock);
    Block newFalseBlock = currentBlock;
    // process LHS
    currentBlock = createBranch(conditionalOr, trueBlock, newFalseBlock);
    buildCondition(conditionalOr.leftOperand(), trueBlock, newFalseBlock);
  }

  private void buildConditionalAnd(BinaryExpressionTree conditionalAnd, Block trueBlock, Block falseBlock) {
    buildCondition(conditionalAnd.rightOperand(), trueBlock, falseBlock);
    Block newTrueBlock = currentBlock;
    // process LHS
    currentBlock = createBranch(conditionalAnd, newTrueBlock, falseBlock);
    buildCondition(conditionalAnd.leftOperand(), newTrueBlock, falseBlock);
  }

  private Block createBranch(Tree terminator, Block trueBranch, Block falseBranch) {
    Block result = createBlock();
    result.terminator = terminator;
    result.successors.add(trueBranch);
    result.successors.add(falseBranch);
    return result;
  }

  public void debugTo(PrintStream out) {
    for (Block block : Lists.reverse(blocks)) {
      if (block.id != 0) {
        out.println("B" + block.id + ":");
      } else {
        out.println("B" + block.id + " (Exit) :");
      }
      int i = 0;
      for (Tree tree : block.elements()) {
        out.println("  " + i + ": " + syntaxNodeToDebugString(tree));
        i++;
      }
      if (block.terminator != null) {
        out.println("  T: " + syntaxNodeToDebugString(block.terminator));
      }
      if (!block.successors.isEmpty()) {
        out.print("  Successors:");
        for (Block successor : block.successors) {
          out.print(" B" + successor.id);
        }
        out.println();
      }
    }
    out.println();
  }

  private static String syntaxNodeToDebugString(Tree syntaxNode) {
    StringBuilder sb = new StringBuilder(syntaxNode.kind().name())
      .append(' ').append(Integer.toHexString(syntaxNode.hashCode()));
    switch (syntaxNode.kind()) {
      case VARIABLE:
        sb.append(' ').append(((VariableTree) syntaxNode).simpleName().name());
        break;
      case IDENTIFIER:
        sb.append(' ').append(((IdentifierTree) syntaxNode).identifierToken().text());
        break;
      case INT_LITERAL:
        sb.append(' ').append(((LiteralTree) syntaxNode).token().text());
        break;
      default:
        //no need to debug other syntaxNodes
    }
    return sb.toString();
  }

}
