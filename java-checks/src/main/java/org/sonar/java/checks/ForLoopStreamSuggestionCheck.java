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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9391")
public class ForLoopStreamSuggestionCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Use a stream instead of this loop.";
  private static final Set<String> ADD_METHODS = Set.of("add", "addFirst", "addLast", "offer", "offerFirst", "offerLast");
  private static final Pattern COLLECTION_NAME_PATTERN = Pattern.compile(
      "^(result|list|set|collection|queue|deque|items|elements|entries|values|keys|children|nodes|names|ids|users|strings)$",
      Pattern.CASE_INSENSITIVE);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!tree.is(Tree.Kind.BLOCK)) {
      return;
    }
    
    BlockTree block = (BlockTree) tree;
    for (StatementTree stmt : block.body()) {
      if (stmt.is(Tree.Kind.FOR_EACH_STATEMENT)) {
        analyzeLoop((ForEachStatement) stmt, block);
      } else if (stmt.is(Tree.Kind.FOR_STATEMENT)) {
        analyzeLoop((ForStatementTree) stmt, block);
      }
    }
  }

  private void analyzeLoop(ForEachStatement forEach, BlockTree methodBody) {
    StatementTree body = forEach.statement();
    if (body == null) {
      return;
    }
    BlockTree loopBody = toBlock(body);
    
    // If loop body is an if statement, analyze it directly
    if (loopBody == null && body.is(Tree.Kind.IF_STATEMENT)) {
      analyzeIfAsLoopBody((IfStatementTree) body, methodBody, ExpressionUtils.skipParentheses(forEach.expression()), forEach);
      return;
    }
    
    if (loopBody == null || loopBody.body().isEmpty()) {
      return;
    }

    // Collect collection symbols from the current block (method body)
    Set<Symbol> collectionSymbols = collectCollectionSymbols(methodBody);
    if (collectionSymbols.isEmpty()) {
      return;
    }

    // Check if loop body is a block containing a single if statement
    if (loopBody.body().size() == 1) {
      StatementTree singleStmt = loopBody.body().get(0);
      if (singleStmt.is(Tree.Kind.IF_STATEMENT)) {
        analyzeIfAsLoopBody((IfStatementTree) singleStmt, methodBody, ExpressionUtils.skipParentheses(forEach.expression()), forEach);
        return;
      }
    }

    LoopContext ctx = new LoopContext(collectionSymbols);
    ctx.iterable = ExpressionUtils.skipParentheses(forEach.expression());
    body.accept(new BodyAnalyzer(ctx));

    if (ctx.collectionSymbol != null && !ctx.hasMultipleStatements && canDetectPattern(ctx)) {
      reportIssue(forEach, MESSAGE);
    }
  }

  private void analyzeIfAsLoopBody(IfStatementTree ifStmt, BlockTree methodBody, ExpressionTree iterable, ForEachStatement forEach) {
    Set<Symbol> collectionSymbols = collectCollectionSymbols(methodBody);
    if (collectionSymbols.isEmpty()) {
      return;
    }

    LoopContext ctx = new LoopContext(collectionSymbols);
    ctx.iterable = iterable;
    ctx.condition = ExpressionUtils.skipParentheses(ifStmt.condition());
    
    StatementTree thenBranch = ifStmt.thenStatement();
    if (thenBranch != null && thenBranch.is(Tree.Kind.BLOCK)) {
      BlockTree block = (BlockTree) thenBranch;
      if (block.body().size() == 1) {
        StatementTree singleStmt = block.body().get(0);
        if (singleStmt.is(Tree.Kind.EXPRESSION_STATEMENT)) {
          ExpressionTree expr = ((ExpressionStatementTree) singleStmt).expression();
          checkAddStatement(ctx, expr);
        }
      }
    }

    if (ctx.collectionSymbol != null && !ctx.hasMultipleStatements && canDetectPattern(ctx)) {
      reportIssue(forEach, MESSAGE);
    }
  }

  private void analyzeLoop(ForStatementTree forStmt, BlockTree methodBody) {
    StatementTree stmt = forStmt.statement();
    if (stmt == null) {
      return;
    }
    BlockTree loopBody = toBlock(stmt);
    if (loopBody == null || loopBody.body().isEmpty()) {
      return;
    }

    Set<Symbol> collectionSymbols = collectCollectionSymbols(methodBody);
    if (collectionSymbols.isEmpty()) {
      return;
    }

    LoopContext ctx = new LoopContext(collectionSymbols);
    ctx.iterable = extractIterableFromFor(forStmt);
    if (ctx.iterable == null) {
      return;
    }
    stmt.accept(new BodyAnalyzer(ctx));

    if (ctx.collectionSymbol != null && !ctx.hasMultipleStatements && canDetectPattern(ctx)) {
      reportIssue(forStmt, MESSAGE);
    }
  }

  private Set<Symbol> collectCollectionSymbols(BlockTree methodBody) {
    Set<Symbol> symbols = new HashSet<>();
    methodBody.accept(new CollectionCollector(symbols));
    return symbols;
  }

  private static class CollectionCollector extends BaseTreeVisitor {
    private final Set<Symbol> collectionSymbols;

    CollectionCollector(Set<Symbol> collectionSymbols) {
      this.collectionSymbols = collectionSymbols;
    }

    @Override
    public void visitMethod(MethodTree tree) {
      // Don't recurse into nested methods, but do visit the current method's body
      // The method body is already visited by the outer loop in visitNode
    }

    @Override
    public void visitVariable(VariableTree tree) {
      ExpressionTree initializer = tree.initializer();
      if (initializer == null) {
        return;
      }
      Symbol symbol = tree.symbol();
      Type type = initializer.symbolType();
      
      // Check by type first (semantic), then by name (syntax-only)
      if (isCollectionType(type) || isCollectionName(tree.simpleName().name())) {
        collectionSymbols.add(symbol);
      }
    }
  }

  private static BlockTree toBlock(StatementTree stmt) {
    if (stmt.is(Tree.Kind.BLOCK)) {
      return (BlockTree) stmt;
    }
    return null;
  }

  private static ExpressionTree extractIterableFromFor(ForStatementTree forStmt) {
    ListTree<StatementTree> initList = forStmt.initializer();
    if (initList == null || initList.isEmpty()) {
      return null;
    }

    StatementTree firstInit = initList.get(0);
    if (firstInit.is(Tree.Kind.VARIABLE)) {
      VariableTree var = (VariableTree) firstInit;
      return var.initializer();
    }
    return null;
  }

  private static boolean canDetectPattern(LoopContext ctx) {
    return ctx.pattern == DetectionPattern.FILTER
        || ctx.pattern == DetectionPattern.MAP
        || ctx.pattern == DetectionPattern.COLLECT
        || ctx.pattern == DetectionPattern.FIND_FIRST;
  }

  private static class LoopContext {
    final Set<Symbol> collectionSymbols;
    Symbol collectionSymbol;
    ExpressionTree iterable;
    ExpressionTree condition;
    DetectionPattern pattern = DetectionPattern.NONE;
    boolean hasMultipleStatements = false;
    boolean hasBreak = false;
    boolean hasContinue = false;
    boolean hasNestedIf = false;
    boolean hasNestedLoop = false;
    boolean hasSourceModification = false;

    LoopContext(Set<Symbol> collectionSymbols) {
      this.collectionSymbols = collectionSymbols;
    }
  }

  private static void checkAddStatement(LoopContext ctx, ExpressionTree expr) {
    if (ctx.collectionSymbol != null) {
      return; // Already found a match
    }
    if (expr.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expr;
      if (isAddMethod(mit) && isCollectionTarget(ctx, mit)) {
        ctx.collectionSymbol = ((IdentifierTree) ((MemberSelectExpressionTree) mit.methodSelect()).expression()).symbol();
        if (ctx.condition == null) {
          ctx.pattern = DetectionPattern.COLLECT;
        } else {
          ctx.pattern = DetectionPattern.MAP;
        }
      }
    } else if (expr.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assign = (AssignmentExpressionTree) expr;
      if (isCollectionIndexAccess(ctx, assign.variable())) {
        ctx.collectionSymbol = ((IdentifierTree) ((MemberSelectExpressionTree) assign.variable()).expression()).symbol();
        ctx.pattern = DetectionPattern.COLLECT;
      }
    }
  }

  private enum DetectionPattern {
    NONE, FILTER, MAP, COLLECT, FIND_FIRST
  }

  private static class BodyAnalyzer extends BaseTreeVisitor {
    private final LoopContext ctx;

    BodyAnalyzer(LoopContext ctx) {
      this.ctx = ctx;
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      if (ctx.pattern == DetectionPattern.FIND_FIRST) {
        return;
      }
      if (ctx.pattern != DetectionPattern.NONE) {
        ctx.hasNestedIf = true;
        return;
      }
      ExpressionTree condition = ExpressionUtils.skipParentheses(tree.condition());
      ctx.condition = condition;
      StatementTree thenBranch = tree.thenStatement();
      if (thenBranch != null) {
        if (thenBranch.is(Tree.Kind.BLOCK)) {
          BlockTree block = (BlockTree) thenBranch;
          if (block.body().size() == 1) {
            StatementTree singleStmt = block.body().get(0);
            if (singleStmt.is(Tree.Kind.EXPRESSION_STATEMENT)) {
              ExpressionTree expr = ((ExpressionStatementTree) singleStmt).expression();
              ForLoopStreamSuggestionCheck.checkAddStatement(ctx, expr);
            }
          }
        } else if (thenBranch.is(Tree.Kind.EXPRESSION_STATEMENT)) {
          ForLoopStreamSuggestionCheck.checkAddStatement(ctx, ((ExpressionStatementTree) thenBranch).expression());
        }
      }
    }

    @Override
    public void visitBreakStatement(BreakStatementTree tree) {
      ctx.hasBreak = true;
      if (ctx.condition != null) {
        ctx.pattern = DetectionPattern.FIND_FIRST;
      }
    }

    @Override
    public void visitContinueStatement(ContinueStatementTree tree) {
      ctx.hasContinue = true;
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      ctx.hasNestedLoop = true;
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      ctx.hasNestedLoop = true;
    }

    @Override
    public void visitBlock(BlockTree tree) {
      super.visitBlock(tree);
    }

    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
      if (ctx.pattern != DetectionPattern.NONE) {
        ctx.hasMultipleStatements = true;
        return;
      }
      ForLoopStreamSuggestionCheck.checkAddStatement(ctx, tree.expression());
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (ctx.pattern != DetectionPattern.NONE) {
        ctx.hasMultipleStatements = true;
        return;
      }
      ForLoopStreamSuggestionCheck.checkAddStatement(ctx, tree);
    }
  }

  private static boolean isCollectionType(Type type) {
    if (type == null) {
      return false;
    }
    if (type.is("java.util.List") || type.is("java.util.ArrayList") || type.is("java.util.LinkedList")
        || type.is("java.util.Set") || type.is("java.util.HashSet") || type.is("java.util.LinkedHashSet")
        || type.is("java.util.Collection") || type.is("java.util.Queue") || type.is("java.util.Deque")) {
      return true;
    }
    return type.isSubtypeOf("java.util.Collection");
  }

  private static boolean isCollectionName(String name) {
    return COLLECTION_NAME_PATTERN.matcher(name).matches();
  }

  private static boolean isAddMethod(MethodInvocationTree mit) {
    String methodName = ExpressionUtils.methodName(mit).name();
    return ADD_METHODS.contains(methodName);
  }

  private static boolean isCollectionTarget(LoopContext ctx, MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree target = ((MemberSelectExpressionTree) methodSelect).expression();
      if (target.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree id = (IdentifierTree) target;
        return ctx.collectionSymbols.contains(id.symbol());
      }
    }
    return false;
  }

  private static boolean isCollectionIndexAccess(LoopContext ctx, ExpressionTree expr) {
    if (expr.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree target = ((MemberSelectExpressionTree) expr).expression();
      if (target.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree id = (IdentifierTree) target;
        return ctx.collectionSymbols.contains(id.symbol());
      }
    }
    return false;
  }
}
