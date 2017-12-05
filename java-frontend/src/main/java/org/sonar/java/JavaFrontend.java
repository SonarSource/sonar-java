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

    private final TaintSource result;
    private final Set<TaintSource> methodCalls;

    public TaintSummary(TaintSource result, Set<TaintSource> methodCalls) {
      this.result = result;
      this.methodCalls = methodCalls;
    }

    public boolean resultCanBeTaintedBy(String s) {
      for (TaintSource ts: result.flatten()) {
        if (ts.toString().equals(s)) {
          return true;
        }
      }
      return false;
    }

    public int resultTaintedPaths() {
      return result.flatten().size();
    }

    public boolean callsMethod(String s) {
      for (TaintSource ts: methodCalls) {
        if (ts.toString().equals(s)) {
          return true;
        }
      }
      return false;
    }

  }

  public interface TaintSource {

    boolean canBeTainted();
    Set<DirectTaintSource> flatten();
    TaintSource unionWith(TaintSource other);

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
    public Set<DirectTaintSource> flatten() {
      return Collections.singleton(this);
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
   * Literals (taint-free)
   * Non-string operands (taint-free)
   * String concatenation
   * Multi-paths return values
   * Multi-paths method argument taint conditions
   */
  public static class IndirectTaintSource implements TaintSource {

    private Set<DirectTaintSource> sources;

    private IndirectTaintSource() {
      this.sources = Collections.emptySet();
    }

    private IndirectTaintSource(TaintSource ts1, TaintSource ts2) {
      Set<DirectTaintSource> sources = new HashSet<>();
      sources.addAll(ts1.flatten());
      sources.addAll(ts2.flatten());
      this.sources = Collections.unmodifiableSet(sources);
    }

    @Override
    public boolean canBeTainted() {
      return !sources.isEmpty();
    }

    @Override
    public Set<DirectTaintSource> flatten() {
      return sources;
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
      if (!canBeTainted()) {
        return "taint-free";
      }

      return "(" + Joiner.on(" | ").join(sources) + ")";
    }

    @Override
    public int hashCode() {
      return sources.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      IndirectTaintSource other = (IndirectTaintSource) obj;
      return sources.equals(other.sources);
    }

  }

  private static class TaintSourceFactory {

    private Map<TaintSource, TaintSource> singletons = new HashMap<>();

    public TaintSource taintFree() {
      return singletonFor(new IndirectTaintSource());
    }

    public TaintSource taintedIif(String fqn, List<TaintSource> arguments) {
      return singletonFor(new DirectTaintSource(fqn, arguments));
    }

    private TaintSource singletonFor(TaintSource taintSource) {
      return singletons.computeIfAbsent(taintSource, ts -> ts);
    }

  }

  private static class TaintProgramState {

    private final ScannedFile src;
    private final TaintSourceFactory tsf;
    private final Map<Symbol, TaintSource> taintSources = new HashMap<>();
    private final Set<TaintSource> methodCalls = new HashSet<>();

    public TaintProgramState(ScannedFile src, TaintSourceFactory tsf) {
      this.src = src;
      this.tsf = tsf;
    }

    private TaintProgramState(TaintProgramState other) {
      this.src = other.src;
      this.tsf = other.tsf;
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
        if (owner.isTypeSymbol()) {
          // field
          return ((JavaSymbol.TypeJavaSymbol)owner).getFullyQualifiedName() + "#" + s.name();
        } else if (owner.isMethodSymbol()) {
          // parameter
          JavaSymbol.MethodJavaSymbol m = (JavaSymbol.MethodJavaSymbol)s.owner();
          int paramId = m.getParameters().scopeSymbols().indexOf(s);
          if (paramId == -1) {
            throw new IllegalStateException();
          }
          return ((JavaSymbol.MethodJavaSymbol)s.owner()).completeSignature() + "#" + paramId;
        } else {
          throw new IllegalArgumentException();
        }
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
        TaintSource methodTaintSource = tsf.taintedIif(fullyQualify(symbol), arguments);

        if (arguments.stream().anyMatch(ts -> ts.canBeTainted())) {
          methodCalls.add(methodTaintSource);
        }

        if (isString(method.returnType().type())) {
          return methodTaintSource;
        } else {
          return tsf.taintFree();
        }
      } else if (symbol.isVariableSymbol() && symbol.owner().isTypeSymbol()) {
        // Fields are not supported
        return tsf.taintFree();
      } else {
        // Local variables, re-assigned parameters, etc.
        return taintSources.computeIfAbsent(
          symbol,
          s -> tsf.taintedIif(fullyQualify(s), arguments));
      }
    }

    public void put(Symbol symbol, TaintSource ts) {
      taintSources.put(symbol, ts);
    }

    @Override
    public int hashCode() {
      return src.hashCode() +
        13 * tsf.hashCode() +
        17 * taintSources.hashCode() +
        19 * methodCalls.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj.getClass().equals(this.getClass()))) {
        return false;
      }

      TaintProgramState other = (TaintProgramState) obj;

      return src.equals(other.src) &&
        tsf.equals(other.tsf) &&
        taintSources.equals(other.taintSources) &&
        methodCalls.equals(other.methodCalls);
    }

  }

  public static TaintSummary computeTaintConditions(ScannedFile src, CFG cfg) {
    TaintSourceFactory tsf = new TaintSourceFactory();

    TaintSource result = tsf.taintFree();
    Set<TaintSource> methodCalls = new HashSet<>();

    boolean returnsVoid = cfg.methodSymbol().returnType().type().isVoid();

    Multimap<Block, TaintProgramState> visited = HashMultimap.create();
    Multimap<Block, TaintProgramState> workList = HashMultimap.create();

    Block entry = cfg.entry();
    TaintProgramState entryState = new TaintProgramState(src, tsf);
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
          result = result.unionWith(ts);
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
      stack.push(state.tsf.taintFree());
    }

    private void executeIdentifier(IdentifierTree tree) {
      if (TaintProgramState.isString(tree.symbolType())) {
        stack.push(state.taintSourceFor(tree.symbol()));
      } else {
        stack.push(state.tsf.taintFree());
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
        state.put(variableTree.symbol(), state.tsf.taintFree());
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
