/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.LiveVariables;
import org.sonar.java.cfg.VariableReadExtractor;
import org.sonar.java.checks.helpers.UnresolvedIdentifiersVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1854")
public class DeadStoreCheck extends IssuableSubscriptionVisitor {

  private static final UnresolvedIdentifiersVisitor UNRESOLVED_IDENTIFIERS_VISITOR = new UnresolvedIdentifiersVisitor();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (methodTree.block() == null) {
      return;
    }

    // TODO(npe) Exclude try statements with finally as CFG is incorrect for those and lead to false positive
    if (hasTryFinallyWithLocalVar(methodTree.block(), methodTree.symbol())) {
      return;
    }

    UNRESOLVED_IDENTIFIERS_VISITOR.check(methodTree);

    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    CFG cfg = (CFG) methodTree.cfg();
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    // Liveness analysis provides information only for block boundaries, so we should do analysis between elements within blocks
    for (CFG.Block block : cfg.blocks()) {
      checkElements(block, liveVariables.getOut(block), methodSymbol);
    }
  }

  private void checkElements(CFG.Block block, Set<Symbol> blockOut, Symbol.MethodSymbol methodSymbol) {
    Set<Symbol> out = new HashSet<>(blockOut);
    Set<Tree> assignmentLHS = new HashSet<>();
    new ArrayDeque<>(block.elements()).descendingIterator().forEachRemaining(
      element -> checkElement(methodSymbol, out, assignmentLHS, element)
    );
  }

  private Set<Symbol> checkElement(Symbol.MethodSymbol methodSymbol, Set<Symbol> outVar, Set<Tree> assignmentLHS, Tree element) {
    Set<Symbol> out = outVar;
    switch (element.kind()) {
      case PLUS_ASSIGNMENT,
        DIVIDE_ASSIGNMENT,
        MINUS_ASSIGNMENT,
        MULTIPLY_ASSIGNMENT,
        OR_ASSIGNMENT,
        XOR_ASSIGNMENT,
        AND_ASSIGNMENT,
        LEFT_SHIFT_ASSIGNMENT,
        RIGHT_SHIFT_ASSIGNMENT,
        UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
        REMAINDER_ASSIGNMENT,
        ASSIGNMENT:
        handleAssignment(out, assignmentLHS, (AssignmentExpressionTree) element);
        break;
      case IDENTIFIER:
        handleIdentifier(out, assignmentLHS, (IdentifierTree) element);
        break;
      case VARIABLE:
        handleVariable(out, (VariableTree) element);
        break;
      case NEW_CLASS:
        handleNewClass(out, methodSymbol, (NewClassTree) element);
        break;
      case LAMBDA_EXPRESSION:
        LambdaExpressionTree lambda = (LambdaExpressionTree) element;
        out.addAll(getUsedLocalVarInSubTree(lambda.body(), methodSymbol));
        break;
      case METHOD_REFERENCE:
        MethodReferenceTree methodRef = (MethodReferenceTree) element;
        out.addAll(getUsedLocalVarInSubTree(methodRef.expression(), methodSymbol));
        break;
      case TRY_STATEMENT:
        handleTryStatement(out, methodSymbol, (TryStatementTree) element);
        break;
      case PREFIX_DECREMENT,
        PREFIX_INCREMENT:
        handlePrefixExpression(out, (UnaryExpressionTree) element);
        break;
      case POSTFIX_INCREMENT,
        POSTFIX_DECREMENT:
        handlePostfixExpression(out, (UnaryExpressionTree) element);
        break;
      case CLASS,
        ENUM,
        ANNOTATION_TYPE,
        INTERFACE:
        ClassTree classTree = (ClassTree) element;
        out.addAll(getUsedLocalVarInSubTree(classTree, methodSymbol));
        break;
      default:
        // Ignore instructions that does not affect liveness of variables
    }
    return out;
  }

  private void handleAssignment(Set<Symbol> out, Set<Tree> assignmentLHS, AssignmentExpressionTree element) {
    ExpressionTree lhs = ExpressionUtils.skipParentheses(element.variable());
    if (lhs.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) lhs).symbol();
      if (symbol.isLocalVariable()
        && !out.contains(symbol)
        && (element.is(Tree.Kind.ASSIGNMENT) || isParentExpressionStatement(element))
        && !UNRESOLVED_IDENTIFIERS_VISITOR.isUnresolved(symbol.name())) {
        createIssue(element.operatorToken(), element.expression(), symbol);
      }
      assignmentLHS.add(lhs);
      if (element.is(Tree.Kind.ASSIGNMENT)) {
        out.remove(symbol);
      } else {
        out.add(symbol);
      }
    }
  }

  private static boolean isParentExpressionStatement(Tree element) {
    return element.parent().is(Tree.Kind.EXPRESSION_STATEMENT);
  }

  private static void handleIdentifier(Set<Symbol> out, Set<Tree> assignmentLHS, IdentifierTree element) {
    Symbol symbol = element.symbol();
    if (!assignmentLHS.contains(element) && symbol.isLocalVariable()) {
      out.add(symbol);
    }
  }

  private void handleVariable(Set<Symbol> out, VariableTree localVar) {
    Symbol symbol = localVar.symbol();
    ExpressionTree initializer = localVar.initializer();
    if (initializer != null
      && !localVar.symbol().isUnknown()
      && !isUsualDefaultValue(initializer)
      && !out.contains(symbol)
      && !UNRESOLVED_IDENTIFIERS_VISITOR.isUnresolved(symbol.name())) {
      createIssue(localVar.equalToken(), initializer, symbol);
    }
    out.remove(symbol);
  }

  private static boolean isUsualDefaultValue(ExpressionTree tree) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(tree);
    switch (expr.kind()) {
      case BOOLEAN_LITERAL,
        NULL_LITERAL:
        return true;
      case STRING_LITERAL:
        return LiteralUtils.isEmptyString(expr);
      case INT_LITERAL:
        String value = ((LiteralTree) expr).value();
        return "0".equals(value) || "1".equals(value);
      case UNARY_MINUS,
        UNARY_PLUS:
        return isUsualDefaultValue(((UnaryExpressionTree) expr).expression());
      default:
        return false;
    }
  }

  private static void handleNewClass(Set<Symbol> out, Symbol.MethodSymbol methodSymbol, NewClassTree element) {
    ClassTree body = element.classBody();
    if (body != null) {
      out.addAll(getUsedLocalVarInSubTree(body, methodSymbol));
    }
  }

  private static void handleTryStatement(Set<Symbol> out, Symbol.MethodSymbol methodSymbol, TryStatementTree element) {
    AssignedLocalVarVisitor visitor = new AssignedLocalVarVisitor();
    element.block().accept(visitor);
    out.addAll(visitor.assignedLocalVars);
    for (CatchTree catchTree : element.catches()) {
      out.addAll(getUsedLocalVarInSubTree(catchTree, methodSymbol));
    }
  }

  private void handlePrefixExpression(Set<Symbol> out, UnaryExpressionTree element) {
    // within each block, each produced value is consumed or by following elements or by terminator
    ExpressionTree expression = element.expression();
    if (isParentExpressionStatement(element) && expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      if (symbol.isLocalVariable() && !out.contains(symbol)) {
        createIssue(element, symbol);
      }
    }
  }

  private void handlePostfixExpression(Set<Symbol> out, UnaryExpressionTree element) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(element.expression());
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      if (symbol.isLocalVariable() && !out.contains(symbol)) {
        createIssue(element, symbol);
      }
    }
  }

  private void createIssue(Tree element, Symbol symbol) {
    reportIssue(element, getMessage(symbol));
  }

  private void createIssue(Tree startTree, Tree endTree, Symbol symbol) {
    reportIssue(startTree, endTree, getMessage(symbol));
  }

  private static String getMessage(Symbol symbol) {
    return "Remove this useless assignment to local variable \"" + symbol.name() + "\".";
  }

  private static Set<Symbol> getUsedLocalVarInSubTree(Tree tree, Symbol.MethodSymbol methodSymbol) {
    VariableReadExtractor localVarExtractor = new VariableReadExtractor(methodSymbol, false);
    tree.accept(localVarExtractor);
    return localVarExtractor.usedVariables();
  }

  private static boolean hasTryFinallyWithLocalVar(BlockTree block, Symbol.MethodSymbol methodSymbol) {
    TryVisitor tryVisitor = new TryVisitor(methodSymbol);
    block.accept(tryVisitor);
    return tryVisitor.hasTryFinally;
  }

  private static class TryVisitor extends BaseTreeVisitor {

    boolean hasTryFinally = false;
    Symbol.MethodSymbol methodSymbol;

    TryVisitor(Symbol.MethodSymbol methodSymbol) {
      this.methodSymbol = methodSymbol;
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      BlockTree finallyBlock = tree.finallyBlock();
      hasTryFinally |= (finallyBlock != null && !getUsedLocalVarInSubTree(finallyBlock, methodSymbol).isEmpty()) || !tree.resourceList().isEmpty();
      if (!hasTryFinally) {
        super.visitTryStatement(tree);
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // ignore inner classes
    }
  }

  private static class AssignedLocalVarVisitor extends BaseTreeVisitor {
    List<Symbol> assignedLocalVars = new ArrayList<>();

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      ExpressionTree lhs = ExpressionUtils.skipParentheses(tree.variable());
      if (lhs.is(Tree.Kind.IDENTIFIER)) {
        Symbol symbol = ((IdentifierTree) lhs).symbol();
        if (symbol.isLocalVariable()) {
          assignedLocalVars.add(symbol);
        }
        super.visitAssignmentExpression(tree);
      }
    }
  }
}
