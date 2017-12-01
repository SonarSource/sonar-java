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

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.util.*;

import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
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

  public static class TaintSummary {

    private final Set<TaintSource> result;
    private final Set<DirectTaintSource> methodCalls;

    public TaintSummary(Set<TaintSource> result, Set<DirectTaintSource> methodCalls) {
      this.result = result;
      this.methodCalls = methodCalls;
    }

    public boolean resultCanBeTaintedBy(String s) {
      for (TaintSource ts: result) {
        if (ts.toString().equals(s)) {
          return true;
        }
      }
      return false;
    }

    public int resultTaintedPaths() {
      return result.size();
    }

    public boolean callsMethod(String s) {
      for (DirectTaintSource ts: methodCalls) {
        if (ts.toString().equals(s)) {
          return true;
        }
      }
      return false;
    }

    public int methodCalls() {
      return methodCalls.size();
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

    private final String signature;
    private final List<TaintSource> arguments;

    public DirectTaintSource(String signature, List<TaintSource> arguments) {
      this.signature = signature;
      this.arguments = arguments;
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
      return signature + (arguments.isEmpty() ? "" : "(" + Joiner.on(", ").join(arguments) + ")");
    }

    @Override
    public int hashCode() {
      return signature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      DirectTaintSource other = (DirectTaintSource) obj;

      return signature.equals(other.signature) &&
        arguments.equals(other.arguments);
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

    private final ScannedFile src;
    private final Map<Symbol, TaintSource> taintSources = new HashMap<>();
    private final Set<DirectTaintSource> methodCalls = new HashSet<>();

    public TaintProgramState(ScannedFile src) {
      this.src = src;
    }

    private TaintProgramState(TaintProgramState other) {
      this.src = other.src;
      this.taintSources.putAll(other.taintSources);
      this.methodCalls.addAll(other.methodCalls);
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

    public static boolean isString(Type type) {
      return type.is("java.lang.String");
    }

    public TaintSource taintSourceFor(Symbol symbol) {
      return taintSourceFor(symbol, Collections.emptyList());
    }

    public TaintSource taintSourceFor(Symbol symbol, List<TaintSource> arguments) {
      if (symbol.isMethodSymbol()) {
        MethodSymbol method = (MethodSymbol)symbol;

        if (arguments.stream().anyMatch(ts -> ts.canBeTainted())) {
          methodCalls.add(new DirectTaintSource(fullyQualify(symbol), arguments));
        }

        if (isString(method.returnType().type())) {
          return new DirectTaintSource(fullyQualify(symbol), arguments);
        } else {
          return new TaintFreeSource();
        }
      }

      if (symbol.isVariableSymbol() && symbol.owner().isTypeSymbol()) {
        // Fields are not supported
        return new TaintFreeSource();
      }

      return taintSources.computeIfAbsent(
        symbol,
        s -> new DirectTaintSource(fullyQualify(s), arguments));
    }

    public void put(Symbol symbol, TaintSource ts) {
      taintSources.put(symbol, ts);
    }

    @Override
    public int hashCode() {
      return src.hashCode() +
        13 * taintSources.hashCode() +
        19 * methodCalls.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      TaintProgramState other = (TaintProgramState) obj;

      return src.equals(other.src) &&
        taintSources.equals(other.taintSources) &&
        methodCalls.equals(other.methodCalls);
    }

  }

  public static TaintSummary computeTaintConditions(ScannedFile src, CFG cfg) {
    Set<TaintSource> result = new HashSet<>();
    Set<DirectTaintSource> methodCalls = new HashSet<>();

    boolean returnsVoid = cfg.methodSymbol().returnType().type().isVoid();

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

      methodCalls.addAll(exitState.methodCalls);

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

    return new TaintSummary(result, methodCalls);
  }

  private static class BlockVisitor {

    private final TaintProgramState state;
    private final Deque<TaintSource> stack = new ArrayDeque<>();

    public BlockVisitor(TaintProgramState state) {
      this.state = state;
    }

    public TaintSource peek() {
      if (stack.isEmpty()) {
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
          executeMethodReference();
          break;
        case ASSERT_STATEMENT:
          executeAssertStatement();
          break;
        default:
          throw new IllegalArgumentException("Unhandled tree: " + tree.getClass().getSimpleName() + " - " + tree);
      }
    }

    private void executeLiteral() {
      stack.push(new TaintFreeSource());
    }

    private void executeIdentifier(IdentifierTree tree) {
      if (TaintProgramState.isString(tree.symbolType())) {
        stack.push(state.taintSourceFor(tree.symbol()));
      } else {
        stack.push(new TaintFreeSource());
      }
    }

    private void executeAssignment(AssignmentExpressionTree tree) {
      TaintSource ts;
      if (tree.is(Tree.Kind.ASSIGNMENT)) {
        ts = stack.pop();
        if (!ExpressionUtils.isSimpleAssignment(tree)) {
          stack.pop();
        }
      } else {
        ts = stack.pop();
        stack.pop();
      }

      Symbol symbol = null;
      if (tree.variable().is(Tree.Kind.IDENTIFIER) || ExpressionUtils.isSelectOnThisOrSuper(tree)) {
        symbol = ExpressionUtils.extractIdentifier(tree).symbol();
        // TODO How to handle any assignment?
        state.put(symbol, ts);
      }
      stack.push(ts);
    }

    private void executeMethodInvocation(MethodInvocationTree mit) {
      List<TaintSource> arguments = new ArrayList<>();
      for (int i = 0; i < mit.arguments().size(); i++) {
        arguments.add(stack.pop());
      }
      stack.pop();

      // TODO What if void method?
      stack.push(state.taintSourceFor(mit.symbol(), Lists.reverse(arguments)));
    }

    private void executeVariable(VariableTree variableTree) {
      if (variableTree.initializer() != null) {
        state.put(variableTree.symbol(), stack.pop());
      } else {
        state.put(variableTree.symbol(), new TaintFreeSource());
      }
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

    private void executeMethodReference() {
      // TODO
      throw new UnsupportedOperationException();
    }

    private void executeAssertStatement() {
      // TODO
      throw new UnsupportedOperationException();
    }

  }

}
