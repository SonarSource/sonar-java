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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.LiveVariables;
import org.sonar.java.cfg.LocalVariableReadExtractor;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(
  key = "S1854",
  name = "Dead stores should be removed",
  priority = Priority.MAJOR,
  tags = {Tag.CERT, Tag.CWE, Tag.SUSPICIOUS, Tag.UNUSED})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("15min")
public class DeadStoreCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (methodTree.block() == null) {
      return;
    }

    // TODO(npe) Exclude try statements with finally as CFG is incorrect for those and lead to false positive
    if (hasTryFinallyWithLocalVar(methodTree.block(), methodTree.symbol())) {
      return;
    }

    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    CFG cfg = CFG.build(methodTree);
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    // Liveness analysis provides information only for block boundaries, so we should do analysis between elements within blocks
    for (CFG.Block block : cfg.blocks()) {
      checkElements(block, liveVariables.getOut(block), methodSymbol);
    }
  }

  private void checkElements(CFG.Block block, Set<Symbol> blockOut, Symbol.MethodSymbol methodSymbol) {
    List<Tree> elements = Lists.reverse(block.elements());
    Set<Symbol> out = new HashSet<>(blockOut);
    Set<Tree> assignmentLHS = new HashSet<>();
    for (Tree element : elements) {
      Symbol symbol;
      switch (element.kind()) {
        case ASSIGNMENT:
          ExpressionTree lhs = ExpressionsHelper.skipParentheses(((AssignmentExpressionTree) element).variable());
          if (lhs.is(Tree.Kind.IDENTIFIER)) {
            symbol = ((IdentifierTree) lhs).symbol();
            if (isLocalVariable(symbol) && !out.contains(symbol)) {
              createIssue(lhs, symbol);
            }
            assignmentLHS.add(lhs);
            out.remove(symbol);
          }
          break;
        case IDENTIFIER:
          symbol = ((IdentifierTree) element).symbol();
          if (!assignmentLHS.contains(element) && isLocalVariable(symbol)) {
            out.add(symbol);
          }
          break;
        case VARIABLE:
          out = handleVariable(out, (VariableTree) element);
          break;
        case NEW_CLASS:
          ClassTree body = ((NewClassTree) element).classBody();
          if (body != null) {
            out.addAll(getUsedLocalVarInSubTree(body, methodSymbol));
          }
          break;
        case LAMBDA_EXPRESSION:
          LambdaExpressionTree lambda = (LambdaExpressionTree) element;
          out.addAll(getUsedLocalVarInSubTree(lambda.body(), methodSymbol));
          break;
        case TRY_STATEMENT:
          TryStatementTree tryStatement = (TryStatementTree) element;
          AssignedLocalVarVisitor visitor = new AssignedLocalVarVisitor();
          tryStatement.block().accept(visitor);
          out.addAll(visitor.assignedLocalVars);
          for (CatchTree catchTree : tryStatement.catches()) {
            out.addAll(getUsedLocalVarInSubTree(catchTree, methodSymbol));
          }
          break;
        case PREFIX_DECREMENT:
        case PREFIX_INCREMENT:
          // within each block, each produced value is consumed or by following elements or by terminator
          ExpressionTree prefixExpression = ExpressionsHelper.skipParentheses(((UnaryExpressionTree) element).expression());
          if (isParentExpressionStatement(element) && prefixExpression.is(Tree.Kind.IDENTIFIER)) {
            symbol = ((IdentifierTree) prefixExpression).symbol();
            if (isLocalVariable(symbol) && !out.contains(symbol)) {
              createIssue(element, symbol);
            }
          }
          break;
        case POSTFIX_INCREMENT:
        case POSTFIX_DECREMENT:
          ExpressionTree expression = ExpressionsHelper.skipParentheses(((UnaryExpressionTree) element).expression());
          if (expression.is(Tree.Kind.IDENTIFIER)) {
            symbol = ((IdentifierTree) expression).symbol();
            if (isLocalVariable(symbol) && !out.contains(symbol)) {
              createIssue(element, symbol);
            }
          }
          break;
        case CLASS:
        case ENUM:
        case ANNOTATION_TYPE:
        case INTERFACE:
          ClassTree classTree = (ClassTree) element;
          out.addAll(getUsedLocalVarInSubTree(classTree, methodSymbol));
          break;
        default:
          // Ignore instructions that does not affect liveness of variables
      }
    }
  }

  private static boolean isParentExpressionStatement(Tree element) {
    return element.parent().is(Tree.Kind.EXPRESSION_STATEMENT);
  }

  private void createIssue(Tree element, Symbol symbol) {
    reportIssue(element, "Remove this useless assignment to local variable \"" + symbol.name() + "\".");
  }

  private Set<Symbol> handleVariable(Set<Symbol> out, VariableTree localVar) {
    Symbol symbol = localVar.symbol();
    if (localVar.initializer() != null && !out.contains(symbol)) {
      createIssue(localVar.simpleName(), symbol);
    }
    out.remove(symbol);
    return out;
  }

  private static List<Symbol> getUsedLocalVarInSubTree(Tree tree, Symbol.MethodSymbol methodSymbol) {
    LocalVariableReadExtractor localVarExtractor = new LocalVariableReadExtractor(methodSymbol);
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
      hasTryFinally |= finallyBlock != null && !getUsedLocalVarInSubTree(finallyBlock, methodSymbol).isEmpty();
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
      ExpressionTree lhs = ExpressionsHelper.skipParentheses(tree.variable());
      if (lhs.is(Tree.Kind.IDENTIFIER)) {
        Symbol symbol = ((IdentifierTree) lhs).symbol();
        if (isLocalVariable(symbol)) {
          assignedLocalVars.add(symbol);
        }
        super.visitAssignmentExpression(tree);
      }
    }
  }

  private static boolean isLocalVariable(Symbol symbol) {
    return symbol.owner().isMethodSymbol();
  }
}
