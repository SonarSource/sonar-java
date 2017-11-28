/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;
import org.sonar.java.JavaFrontend.ScannedFile;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.*;

public class JavaFrontendTest {

  private final JavaFrontend front = new JavaFrontend();

  @Test
  public void parse() {
    ScannedFile src = front.scan(new File("src/test/files/JavaFrontend.java"), this.getClass().getClassLoader());
    for (MethodTree m: getMethods(src.tree())) {
      System.out.println("visiting: " + m.simpleName());
      CFG cfg = CFG.build(m);
      simplifyCFG(src, cfg);
    }
  }

  private static Collection<MethodTree> getMethods(Tree tree) {
    List<MethodTree> result = new ArrayList<>();
    new BaseTreeVisitor() {
      {
        scan(tree);
      }

      @Override public void visitMethod(MethodTree methodTree) {
        super.visitMethod(methodTree);
        result.add(methodTree);
      }
    };
    return result;
  }

  private static class TaintSymbolicValue {

    private final String symbol;

    public TaintSymbolicValue(Symbol symbol) {
      if (symbol == null) {
        throw new IllegalArgumentException();
      }
      this.symbol = symbol.toString();
    }

    @Override
    public String toString() {
      return symbol;
    }
  }

  private static class TaintProgramState {

    private final ScannedFile src;

    public TaintProgramState(ScannedFile src) {
      this.src = src;
    }

    private TaintProgramState(TaintProgramState other) {
      this.src = other.src;
    }

    public TaintProgramState dup() {
      return new TaintProgramState(this);
    }

    public SemanticModel semantic() {
      return src.semantic();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(src);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      TaintProgramState other = (TaintProgramState) obj;

      return Objects.equals(src, other.src);
    }
  }

  private static void simplifyCFG(ScannedFile src, CFG cfg) {
    Multimap<Block, TaintProgramState> visited = HashMultimap.create();
    Multimap<Block, TaintProgramState> workList = HashMultimap.create();

    Block entry = cfg.entry();
    TaintProgramState entryState = new TaintProgramState(src);
    workList.put(entry, entryState);

    while (!workList.isEmpty()) {
      Block block = workList.keys().iterator().next();
      TaintProgramState state = workList.get(block).iterator().next();
      visited.put(block, state);
      if (!workList.remove(block, state)) {
        throw new IllegalStateException();
      }

      BlockVisitor visitor = new BlockVisitor(state.dup());
      visitor.visit(block);
      TaintProgramState exitState = visitor.state;
      if (block.successors().contains(cfg.exitBlock())) {
        /* TODO */
      }

      for (Block successor: block.successors()) {
        if (!visited.containsEntry(successor, exitState)) {
          workList.put(successor, exitState);
        }
      }
    }
  }

  private static class BlockVisitor {

    private final TaintProgramState state;

    public BlockVisitor(TaintProgramState state) {
      this.state = state;
    }

    public void visit(Block block) {
      for (Tree tree: block.elements()) {
        String fqtn;
        if (tree instanceof ExpressionTree) {
          ExpressionTree expr = (ExpressionTree) tree;
          fqtn = expr.symbolType().fullyQualifiedName();
        } else {
          fqtn = state.semantic().getSymbol(tree).type().fullyQualifiedName();
        }
        System.out.println(String.format("    %s: %s of type %s", tree.kind(), tree, fqtn));

        visit(tree);
      }
    }

    private void visit(Tree tree) {
      switch (tree.kind()) {
        case METHOD_INVOCATION:
          MethodInvocationTree mit = (MethodInvocationTree) tree;
          executeMethodInvocation(mit);
          return;
        case LABELED_STATEMENT:
        case SWITCH_STATEMENT:
        case EXPRESSION_STATEMENT:
        case PARENTHESIZED_EXPRESSION:
          throw new IllegalStateException("Cannot appear in CFG: " + tree.kind().name());
        case VARIABLE:
          executeVariable((VariableTree) tree);
          break;
        case TYPE_CAST:
          executeTypeCast((TypeCastTree) tree);
          break;
        case ASSIGNMENT:
        case PLUS_ASSIGNMENT:
        case MULTIPLY_ASSIGNMENT:
        case DIVIDE_ASSIGNMENT:
        case REMAINDER_ASSIGNMENT:
        case MINUS_ASSIGNMENT:
        case LEFT_SHIFT_ASSIGNMENT:
        case RIGHT_SHIFT_ASSIGNMENT:
        case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
          executeAssignment((AssignmentExpressionTree) tree);
          break;
        case AND_ASSIGNMENT:
        case XOR_ASSIGNMENT:
        case OR_ASSIGNMENT:
          executeLogicalAssignment((AssignmentExpressionTree) tree);
          break;
        case ARRAY_ACCESS_EXPRESSION:
          executeArrayAccessExpression((ArrayAccessExpressionTree) tree);
          break;
        case NEW_ARRAY:
          executeNewArray((NewArrayTree) tree);
          break;
        case NEW_CLASS:
          executeNewClass((NewClassTree) tree);
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
          executeBinaryExpression(tree);
          break;
        case POSTFIX_INCREMENT:
        case POSTFIX_DECREMENT:
        case PREFIX_INCREMENT:
        case PREFIX_DECREMENT:
        case UNARY_MINUS:
        case UNARY_PLUS:
        case BITWISE_COMPLEMENT:
        case LOGICAL_COMPLEMENT:
        case INSTANCE_OF:
          executeUnaryExpression(tree);
          break;
        case IDENTIFIER:
          executeIdentifier((IdentifierTree) tree);
          break;
        case MEMBER_SELECT:
          executeMemberSelect((MemberSelectExpressionTree) tree);
          break;
        case INT_LITERAL:
        case LONG_LITERAL:
        case FLOAT_LITERAL:
        case DOUBLE_LITERAL:
        case CHAR_LITERAL:
        case STRING_LITERAL:
        case BOOLEAN_LITERAL:
        case NULL_LITERAL:
          executeLiteral();
          break;
        case LAMBDA_EXPRESSION:
        case METHOD_REFERENCE:
          // TODO
          throw new UnsupportedOperationException();
        case ASSERT_STATEMENT:
          // TODO
          throw new UnsupportedOperationException();
        default:
          throw new IllegalArgumentException("Unhandled tree: " + tree.getClass().getSimpleName() + " - " + tree);
      }
    }

    private void executeLiteral() {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeIdentifier(IdentifierTree tree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeAssignment(AssignmentExpressionTree tree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeMethodInvocation(MethodInvocationTree mit) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeVariable(VariableTree variableTree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeTypeCast(TypeCastTree typeCast) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeLogicalAssignment(AssignmentExpressionTree tree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeArrayAccessExpression(ArrayAccessExpressionTree tree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeNewArray(NewArrayTree newArrayTree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeNewClass(NewClassTree tree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeBinaryExpression(Tree tree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeUnaryExpression(Tree tree) {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeMemberSelect(MemberSelectExpressionTree mse) {
      // TODO
      throw new UnsupportedOperationException();
    }

  }

}
