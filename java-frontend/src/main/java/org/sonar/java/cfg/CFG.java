/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CFG {

  private final Symbol.MethodSymbol methodSymbol;
  private Block currentBlock;

  /**
   * List of all blocks in order they were created.
   */
  private final List<Block> blocks = new ArrayList<>();

  private final Deque<Block> breakTargets = new LinkedList<>();
  private final Deque<Block> continueTargets = new LinkedList<>();
  private final Deque<Block> exitBlocks = new LinkedList<>();

  private final Deque<Block> switches = new LinkedList<>();
  private String pendingLabel = null;
  private Map<String, Block> labelsBreakTarget = Maps.newHashMap();
  private Map<String, Block> labelsContinueTarget = Maps.newHashMap();

  private CFG(BlockTree tree, Symbol.MethodSymbol symbol) {
    methodSymbol = symbol;
    exitBlocks.add(createBlock());
    currentBlock = createBlock(exitBlock());
    for (StatementTree statementTree : Lists.reverse(tree.body())) {
      build(statementTree);
    }
    prune();
    computePredecessors(blocks);
  }

  private Block exitBlock() {
    return exitBlocks.peek();
  }

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
    private int id;
    private final List<Tree> elements = new ArrayList<>();
    private final Set<Block> successors = new LinkedHashSet<>();
    private final Set<Block> predecessors = new LinkedHashSet<>();
    private Block trueBlock;
    private Block falseBlock;
    private Block exitBlock;

    private Tree terminator;

    private boolean isFinallyBlock;

    public Block(int id) {
      this.id = id;
    }

    public int id() {
      return id;
    }

    public List<Tree> elements() {
      return Lists.reverse(elements);
    }

    public Block trueBlock() {
      return trueBlock;
    }

    public Block falseBlock() {
      return falseBlock;
    }

    public Block exitBlock() {
      return exitBlock;
    }

    public boolean isFinallyBlock() {
      return isFinallyBlock;
    }

    void addSuccessor(Block successor) {
      successors.add(successor);
    }

    public void addTrueSuccessor(Block successor) {
      if (trueBlock != null) {
        throw new IllegalStateException("Attempt to re-assign a true successor");
      }
      successors.add(successor);
      trueBlock = successor;
    }

    public void addFalseSuccessor(Block successor) {
      if (falseBlock != null) {
        throw new IllegalStateException("Attempt to re-assign a false successor");
      }
      successors.add(successor);
      falseBlock = successor;
    }

    public void addExitSuccessor(Block block) {
      successors.add(block);
      exitBlock = block;
    }

    public Set<Block> predecessors() {
      return predecessors;
    }

    public Set<Block> successors() {
      return successors;
    }

    @CheckForNull
    public Tree terminator() {
      return terminator;
    }

    public boolean isInactive() {
      return terminator == null && elements.isEmpty() && successors.size() == 1;
    }

    private void prune(Block inactiveBlock) {
      if (inactiveBlock.equals(trueBlock)) {
        if (inactiveBlock.successors.size() != 1) {
          throw new IllegalStateException("True successor must be replaced by a unique successor!");
        }
        trueBlock = inactiveBlock.successors.iterator().next();
      }
      if (inactiveBlock.equals(falseBlock)) {
        if (inactiveBlock.successors.size() != 1) {
          throw new IllegalStateException("False successor must be replaced by a unique successor!");
        }
        falseBlock = inactiveBlock.successors.iterator().next();
      }
      if (successors.remove(inactiveBlock)) {
        successors.addAll(inactiveBlock.successors);
      }
      if (inactiveBlock.equals(exitBlock)) {
        exitBlock = inactiveBlock.successors.iterator().next();
      }
    }
  }

  private static void computePredecessors(List<Block> blocks) {
    for (Block b : blocks) {
      for (Block successor : b.successors) {
        successor.predecessors.add(b);
      }
    }
  }

  private void prune() {
    List<Block> inactiveBlocks = new ArrayList<>();
    boolean first = true;
    for (Block block : blocks) {
      if (!first && isInactive(block)) {
        inactiveBlocks.add(block);
      }
      first = false;
    }
    if (!inactiveBlocks.isEmpty()) {
      removeInactiveBlocks(inactiveBlocks);
      if (inactiveBlocks.contains(currentBlock)) {
        currentBlock = currentBlock.successors.iterator().next();
      }
      int id = 0;
      for (Block block : blocks) {
        block.id = id;
        id += 1;
      }
    }
  }

  private boolean isInactive(Block block) {
    if (block.equals(currentBlock) && block.successors.size() > 1) {
      return false;
    }
    return block.isInactive();
  }

  private void removeInactiveBlocks(List<Block> inactiveBlocks) {
    for (Block inactiveBlock : inactiveBlocks) {
      for (Block block : blocks) {
        block.prune(inactiveBlock);
      }
    }
    blocks.removeAll(inactiveBlocks);
  }

  private Block createBlock(Block successor) {
    Block result = createBlock();
    result.addSuccessor(successor);
    return result;
  }

  private Block createBlock() {
    Block result = new Block(blocks.size());
    blocks.add(result);
    return result;
  }

  public static CFG build(MethodTree tree) {
    BlockTree block = tree.block();
    Preconditions.checkArgument(block != null, "Cannot build CFG for method with no body.");
    return new CFG(block, tree.symbol());
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
      // assert can be ignored by VM so skip them for now.
      case ASSERT_STATEMENT:
        // Ignore assert statement as they are disabled by default in JVM
        break;
      // store declarations as complete blocks.
      case EMPTY_STATEMENT:
      case CLASS:
      case ENUM:
      case ANNOTATION_TYPE:
      case INTERFACE:
      case METHOD_REFERENCE:
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

  private void buildReturnStatement(ReturnStatementTree returnStatement) {
    currentBlock = createUnconditionalJump(returnStatement, exitBlock());
    ExpressionTree expression = returnStatement.expression();
    if (expression != null) {
      build(expression);
    }
  }

  private void buildMethodInvocation(MethodInvocationTree mit) {
    currentBlock.elements.add(mit);
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) mit.methodSelect();
      build(memberSelect.expression());
    } else {
      build(mit.methodSelect());
    }
    for (ExpressionTree arg : Lists.reverse(mit.arguments())) {
      build(arg);
    }
  }

  private void buildIfStatement(IfStatementTree ifStatementTree) {
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

  private void buildConditionalExpression(ConditionalExpressionTree cond) {
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

  private void buildMemberSelect(MemberSelectExpressionTree mse) {
    currentBlock.elements.add(mse);
    // int.class or String[].class are memberSelectExpression which expression part is not an expression.
    if (!"class".equals(mse.identifier().name())) {
      build(mse.expression());
    }
  }

  private void buildConditionalAnd(BinaryExpressionTree tree) {
    Block falseBlock = currentBlock;
    currentBlock = createBlock(falseBlock);
    // process RHS
    build(tree.rightOperand());
    // process LHS
    buildConditionalBinaryLHS(tree, currentBlock, falseBlock);
  }

  private void buildConditionalOr(BinaryExpressionTree tree) {
    Block trueBlock = currentBlock;
    currentBlock = createBlock(trueBlock);
    // process RHS
    build(tree.rightOperand());
    // process LHS
    buildConditionalBinaryLHS(tree, trueBlock, currentBlock);
  }

  private void buildConditionalBinaryLHS(BinaryExpressionTree tree, Block trueBlock, Block falseBlock) {
    currentBlock = createBlock();
    Block toComplete = currentBlock;
    build(tree.leftOperand());
    toComplete.terminator = tree;
    toComplete.addFalseSuccessor(falseBlock);
    toComplete.addTrueSuccessor(trueBlock);
  }

  private void buildLabeledStatement(LabeledStatementTree labeledStatement) {
    String name = labeledStatement.label().name();
    labelsBreakTarget.put(name, currentBlock);
    pendingLabel = name;
    build(labeledStatement.statement());
    currentBlock = createBlock(currentBlock);
  }

  private void buildSwitchStatement(SwitchStatementTree switchStatementTree) {
    Block switchSuccessor = currentBlock;
    // process condition
    currentBlock = createBlock();
    currentBlock.terminator = switchStatementTree;
    switches.addLast(currentBlock);
    build(switchStatementTree.expression());
    Block conditionBlock = currentBlock;
    // process body
    currentBlock = createBlock(switchSuccessor);
    breakTargets.addLast(switchSuccessor);
    boolean hasDefaultCase = false;
    if (!switchStatementTree.cases().isEmpty()) {
      CaseGroupTree firstCase = switchStatementTree.cases().get(0);
      for (CaseGroupTree caseGroupTree : Lists.reverse(switchStatementTree.cases())) {
        build(caseGroupTree.body());
        if (!hasDefaultCase) {
          hasDefaultCase = containsDefaultCase(caseGroupTree.labels());
        }
        switches.getLast().addSuccessor(currentBlock);
        if (!caseGroupTree.equals(firstCase)) {
          // No block predecessing the first case group.
          currentBlock = createBlock(currentBlock);
        }
      }
    }
    breakTargets.removeLast();
    // process condition
    currentBlock = switches.removeLast();
    if (!hasDefaultCase) {
      currentBlock.addSuccessor(switchSuccessor);
    }
    currentBlock = conditionBlock;
  }

  private static boolean containsDefaultCase(List<CaseLabelTree> labels) {
    for (CaseLabelTree caseLabel : labels) {
      if ("default".equals(caseLabel.caseOrDefaultKeyword().text())) {
        return true;
      }
    }
    return false;
  }

  private void buildBreakStatement(BreakStatementTree tree) {
    IdentifierTree label = tree.label();
    Block targetBlock;
    if (label == null) {
      if (breakTargets.isEmpty()) {
        throw new IllegalStateException("'break' statement not in loop or switch statement");
      }
      targetBlock = breakTargets.getLast();
    } else {
      targetBlock = labelsBreakTarget.get(label.name());
    }
    currentBlock = createUnconditionalJump(tree, targetBlock);
  }

  private void buildContinueStatement(ContinueStatementTree tree) {
    IdentifierTree label = tree.label();
    Block targetBlock;
    if (label == null) {
      if (continueTargets.isEmpty()) {
        throw new IllegalStateException("'continue' statement not in loop or switch statement");
      }
      targetBlock = continueTargets.getLast();
    } else {
      targetBlock = labelsContinueTarget.get(label.name());
    }
    currentBlock = createUnconditionalJump(tree, targetBlock);
  }

  private void buildWhileStatement(WhileStatementTree whileStatement) {
    Block falseBranch = currentBlock;
    Block loopback = createBlock();
    // process body
    currentBlock = createBlock(loopback);
    addContinueTarget(loopback);
    breakTargets.addLast(falseBranch);
    build(whileStatement.statement());
    breakTargets.removeLast();
    continueTargets.removeLast();
    Block bodyBlock = currentBlock;
    // process condition
    currentBlock = createBranch(whileStatement, bodyBlock, falseBranch);
    buildCondition(whileStatement.condition(), bodyBlock, falseBranch);
    loopback.addSuccessor(currentBlock);
    currentBlock = createBlock(currentBlock);
  }

  private void buildDoWhileStatement(DoWhileStatementTree doWhileStatementTree) {
    Block falseBranch = currentBlock;
    Block loopback = createBlock();
    // process condition
    currentBlock = createBranch(doWhileStatementTree, loopback, falseBranch);
    buildCondition(doWhileStatementTree.condition(), loopback, falseBranch);
    // process body
    currentBlock = createBlock(currentBlock);
    addContinueTarget(loopback);
    breakTargets.addLast(falseBranch);
    build(doWhileStatementTree.statement());
    breakTargets.removeLast();
    continueTargets.removeLast();
    loopback.addSuccessor(currentBlock);
    currentBlock = createBlock(currentBlock);
  }

  private void buildForEachStatement(ForEachStatement tree) {
    // TODO(npe) One solution is to create a forstatement node depending on type of expression (iterable or array) and build CFG from it.
    Block afterLoop = currentBlock;
    Block statementBlock = createBlock();
    Block loopback = createBranch(tree, statementBlock, afterLoop);
    currentBlock = createBlock(loopback);
    addContinueTarget(loopback);
    breakTargets.addLast(afterLoop);
    build(tree.statement());
    breakTargets.removeLast();
    continueTargets.removeLast();
    statementBlock.addSuccessor(currentBlock);
    currentBlock = loopback;
    build(tree.variable());
    currentBlock = createBlock(currentBlock);
    build(tree.expression());
    currentBlock = createBlock(currentBlock);
  }

  private void addContinueTarget(Block target) {
    continueTargets.addLast(target);
    if (pendingLabel != null) {
      labelsContinueTarget.put(pendingLabel, target);
      pendingLabel = null;
    }
  }

  private void buildForStatement(ForStatementTree tree) {
    Block falseBranch = currentBlock;
    // process step
    currentBlock = createBlock();
    Block updateBlock = currentBlock;
    for (StatementTree updateTree : Lists.reverse(tree.update())) {
      build(updateTree);
    }
    addContinueTarget(currentBlock);
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
    updateBlock.addSuccessor(currentBlock);
    // process init
    currentBlock = createBlock(currentBlock);
    for (StatementTree init : Lists.reverse(tree.initializer())) {
      build(init);
    }
  }

  private void buildTryStatement(TryStatementTree tryStatementTree) {
    // FIXME only path with no failure constructed for now, (not taking try with resources into consideration).
    currentBlock = createBlock(currentBlock);
    BlockTree finallyBlockTree = tryStatementTree.finallyBlock();
    if (finallyBlockTree != null) {
      currentBlock.isFinallyBlock = true;
      Block finallyBlock = currentBlock;
      build(finallyBlockTree);
      finallyBlock.addExitSuccessor(exitBlock());
      exitBlocks.push(currentBlock);
    }
    Block finallyOrEndBlock = currentBlock;
    Block beforeFinally = createBlock(currentBlock);
    List<Block> catches = new ArrayList<>();
    for (CatchTree catchTree : tryStatementTree.catches()) {
      currentBlock = createBlock(finallyOrEndBlock);
      build(catchTree.block());
      catches.add(currentBlock);
    }
    currentBlock = beforeFinally;
    build(tryStatementTree.block());
    if(currentBlock.exitBlock != null && currentBlock.exitBlock.isFinallyBlock) {
      for (Block catchBlock : catches) {
        currentBlock.exitBlock.addSuccessor(catchBlock);
      }
    } else {
      for (Block catchBlock : catches) {
        currentBlock.addSuccessor(catchBlock);
      }
    }
    build((List<? extends Tree>) tryStatementTree.resources());
    currentBlock = createBlock(currentBlock);
    currentBlock.elements.add(tryStatementTree);
    if (finallyBlockTree != null) {
      exitBlocks.pop();
      if(catches.isEmpty()) {
        currentBlock.addExitSuccessor(finallyOrEndBlock);
      }
    }
    for (Block catchBlock : catches) {
      currentBlock.addSuccessor(catchBlock);
    }
  }

  private void buildThrowStatement(ThrowStatementTree throwStatementTree) {
    // FIXME this won't work if it is intended to be caught by a try statement.
    currentBlock = createUnconditionalJump(throwStatementTree, exitBlock());
    build(throwStatementTree.expression());
  }

  private void buildSynchronizedStatement(SynchronizedStatementTree sst) {
    // First create the block of the statement,
    build(sst.block());
    // Then create a single block with the SYNCHRONIZED tree as terminator
    currentBlock = createUnconditionalJump(sst, currentBlock);
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
      if (target == exitBlock()) {
        result.addExitSuccessor(target);
      } else {
        result.addSuccessor(target);
      }
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
    result.addFalseSuccessor(falseBranch);
    result.addTrueSuccessor(trueBranch);
    return result;
  }

}
