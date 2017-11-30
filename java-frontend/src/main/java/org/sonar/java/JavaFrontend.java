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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.util.*;

import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.*;

public class JavaFrontend {

  public static class ScannedFile {

    private final CompilationUnitTree tree;
    private final SemanticModel semantic;

    public ScannedFile(CompilationUnitTree tree, SemanticModel semantic) {
      this.tree = tree;
      this.semantic = semantic;
    }

    public CompilationUnitTree tree() {
      return tree;
    }

    public SemanticModel semantic() {
      return semantic;
    }
  }

  private final ActionParser<Tree> parser = JavaParser.createParser();

  public ScannedFile scan(File source, ClassLoader classLoader) {
    CompilationUnitTree tree = (CompilationUnitTree) parser.parse(source);
    SemanticModel model = SemanticModel.createFor(tree, classLoader);

    return new ScannedFile(tree, model);
  }

  private static class IdGenerator {

    private int current = 0;

    public int next() {
      return current++;
    }

  }

  public interface TaintSource {

    boolean canBeTainted();
    TaintSource unionWith(TaintSource other);

  }

  /**
   * Literals
   * Non-string operands
   */
  public static class TaintFreeSource implements TaintSource {

    @Override
    public boolean canBeTainted() {
      return false;
    }

    @Override
    public TaintSource unionWith(TaintSource other) {
      return other;
    }

    @Override
    public String toString() {
      return "taint-free";
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      return true;
    }

  }

  /**
   * Read-from method parameter
   * Read-from field
   * Read-from method-call return value
   */
  public static class DirectTaintSource implements TaintSource {

    private final int id;
    private final String signature;

    public DirectTaintSource(int id, String signature) {
      this.id = id;
      this.signature = signature;
    }

    @Override
    public boolean canBeTainted() {
      return true;
    }

    @Override
    public TaintSource unionWith(TaintSource other) {
      return IndirectTaintSource.union(this, other);
    }

    @Override
    public String toString() {
      return "$" + id + ": " + signature;
    }

    @Override
    public int hashCode() {
      return id;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      DirectTaintSource other = (DirectTaintSource) obj;

      return id == other.id;
    }

  }

  /**
   * String concatenation
   */
  public static class IndirectTaintSource implements TaintSource {

    private final TaintSource left;
    private final TaintSource right;

    private IndirectTaintSource(TaintSource left, TaintSource right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean canBeTainted() {
      return true;
    }

    @Override
    public TaintSource unionWith(TaintSource other) {
      return union(this, other);
    }

    public static TaintSource union(TaintSource left, TaintSource right) {
      if (!left.canBeTainted()) {
        return right;
      }

      if (!right.canBeTainted()) {
        return left;
      }

      if (left.equals(right)) {
        return left;
      }

      return new IndirectTaintSource(left, right);
    }

    @Override
    public String toString() {
      return "left: " + left.toString() + ", right = " + right.toString();
    }

    @Override
    public int hashCode() {
      return left.hashCode() + right.hashCode();
    }

    // TODO equals is not symmetrical
    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      IndirectTaintSource other = (IndirectTaintSource) obj;
      return left.equals(other.left) &&
        right.equals(other.right);
    }

  }

  private static class TaintProgramState {

    private final IdGenerator idGenerator;
    private final ScannedFile src;
    private final Map<Symbol, TaintSource> taintSources = new HashMap<>();

    public TaintProgramState(IdGenerator idGenerator, ScannedFile src) {
      this.idGenerator = idGenerator;
      this.src = src;
    }

    private TaintProgramState(TaintProgramState other) {
      this.idGenerator = other.idGenerator;
      this.src = other.src;
      this.taintSources.putAll(other.taintSources);
    }

    public TaintProgramState dup() {
      return new TaintProgramState(this);
    }

    public SemanticModel semantic() {
      return src.semantic();
    }

    private static String fullyQualify(Symbol s) {
      if (s.isVariableSymbol()) {
        Symbol owner = s.owner();
        String ownerSignature;
        if (owner.isTypeSymbol()) {
          // field
          ownerSignature = ((JavaSymbol.TypeJavaSymbol)owner).getFullyQualifiedName();
        } else if (owner.isMethodSymbol()) {
          // parameter
          ownerSignature = ((JavaSymbol.MethodJavaSymbol)s.owner()).completeSignature();
        } else {
          throw new IllegalArgumentException();
        }

        return ownerSignature + "#" + s.name();
      } else if (s.isMethodSymbol()) {
        return ((JavaSymbol.MethodJavaSymbol)s).completeSignature();
      } else {
        throw new IllegalArgumentException();
      }
    }

    public TaintSource taintSourceFor(Symbol symbol) {
      return taintSources.computeIfAbsent(
        symbol,
        s -> new DirectTaintSource(idGenerator.next(), fullyQualify(s)));
    }

    @Override
    public int hashCode() {
      return src.hashCode() +
        13 * taintSources.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      TaintProgramState other = (TaintProgramState) obj;

      return Objects.equals(src, other.src) &&
        Objects.equals(taintSources, other.taintSources);
    }

  }

  public static Set<TaintSource> computeTaintConditions(ScannedFile src, CFG cfg) {
    IdGenerator idGenerator = new IdGenerator();
    Set<TaintSource> result = new HashSet<>();

    boolean returnsVoid = cfg.methodSymbol().returnType().type().isVoid();

    Multimap<Block, TaintProgramState> visited = HashMultimap.create();
    Multimap<Block, TaintProgramState> workList = HashMultimap.create();

    Block entry = cfg.entry();
    TaintProgramState entryState = new TaintProgramState(idGenerator, src);
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
        if (!returnsVoid) {
          TaintSource ts = visitor.peek();
          if (ts.canBeTainted()) {
            result.add(visitor.peek());
          }
        }
      }

      for (Block successor: block.successors()) {
        if (!visited.containsEntry(successor, exitState)) {
          workList.put(successor, exitState);
        }
      }
    }

    return result;
  }

  private static class BlockVisitor {

    private final TaintProgramState state;
    private final Deque<TaintSource> stack = new ArrayDeque<>();

    public BlockVisitor(TaintProgramState state) {
      this.state = state;
    }

    public TaintSource peek() {
      if (stack.size() != 1) {
        throw new IllegalStateException();
      }

      return stack.getFirst();
    }

    public void visit(Block block) {
      for (Tree tree: block.elements()) {
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
      stack.push(new TaintFreeSource());
    }

    private void executeIdentifier(IdentifierTree tree) {
      if (isString(tree.symbolType())) {
        stack.push(state.taintSourceFor(tree.symbol()));
      } else {
        stack.push(new TaintFreeSource());
      }
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

    private boolean isString(Type type) {
      return type.is("java.lang.String");
    }

  }

}
