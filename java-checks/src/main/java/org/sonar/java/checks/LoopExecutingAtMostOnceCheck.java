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
package org.sonar.java.checks;


import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S1751")
public class LoopExecutingAtMostOnceCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcherCollection NEXT_ELEMENT = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.util.Enumeration")).name("hasMoreElements").withoutParameter(),
    MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.util.Iterator")).name("hasNext").withoutParameter());

  private static final Tree.Kind[] LOOP_KINDS = {
    Tree.Kind.DO_STATEMENT,
    Tree.Kind.WHILE_STATEMENT,
    Tree.Kind.FOR_STATEMENT,
    Tree.Kind.FOR_EACH_STATEMENT
  };

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.BREAK_STATEMENT,
      Tree.Kind.RETURN_STATEMENT,
      Tree.Kind.THROW_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    Tree parent = tree.parent();
    while (parent.is(Tree.Kind.BLOCK)) {
      parent = parent.parent();
    }

    if (!parent.is(LOOP_KINDS)) {
      return;
    }

    if (!isWhileNextElementLoop(parent) && !isEmptyConditionLoop(parent) && executeUnconditionnally(parent)) {
      SyntaxToken jumpKeyword = jumpKeyword(tree);
      reportIssue(jumpKeyword, String.format("Remove this \"%s\" statement or make it conditional.", jumpKeyword.text()));
    }
  }

  /**
   * For or while loops are sometimes used as short-cut simulating goto's, when having always true conditions:
   * <code>
   * for(;;) {
   *  if (...) {
   *    // ...
   *    break;
   *  }
   *  while (...) {
   *    // ...
   *    if (...) {
   *      break;
   *    }
   *  }
   *  break; // last unconditional jump to exit the infinite loop
   * }
   * </code>
   */
  private static boolean isEmptyConditionLoop(Tree loopTree) {
    switch (loopTree.kind()) {
      case FOR_STATEMENT:
        ForStatementTree fst = (ForStatementTree) loopTree;
        return fst.initializer().isEmpty() && fst.condition() == null && fst.update().isEmpty();
      case WHILE_STATEMENT:
        // 'while(false)' does not compile, unreachable code
        return isTrue(((WhileStatementTree) loopTree).condition());
      case DO_STATEMENT:
        // only target true literal, which needs a break statement.
        // For a 'do {...} while (false);', loop the jump is useless and issue should be raised.
        return isTrue(((DoWhileStatementTree) loopTree).condition());
      default:
        // variable and expression of For-Each statement can not be empty
        return false;
    }
  }

  private static boolean isTrue(ExpressionTree expressionTree) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(expressionTree);
    return LiteralUtils.isTrue(expr);
  }

  /**
   * While loops are sometimes used to get only the first element of an enumeration/collection, using code similar to:
   * <code>
   * while(myIterator.hasNext()) {
   *   // ...
   *   return myIterator.next(); // unconditional jump
   * }
   * </code>
   */
  private static boolean isWhileNextElementLoop(Tree loopTree) {
    if (loopTree.is(Tree.Kind.WHILE_STATEMENT)) {
      ExpressionTree condition = ExpressionUtils.skipParentheses(((WhileStatementTree) loopTree).condition());
      return condition.is(Tree.Kind.METHOD_INVOCATION) && NEXT_ELEMENT.anyMatch((MethodInvocationTree) condition);
    }
    return false;
  }

  private static boolean executeUnconditionnally(Tree loopTree) {
    CFG cfg = getCFG(loopTree);
    CFG.Block loopBlock = getLoopBlock(cfg, loopTree);
    // we cannot find a path in the CFG that goes twice through this instruction.
    return !hasPredecessorInBlock(loopBlock, loopTree);
  }

  private static CFG.Block getLoopBlock(CFG cfg, Tree loopTree) {
    return cfg.blocks().stream()
      .filter(block -> loopTree.equals(block.terminator()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("CFG necessarily contains the loop block."));
  }

  private static boolean hasPredecessorInBlock(CFG.Block block, Tree loop) {
    for (CFG.Block predecessor : block.predecessors()) {
      List<Tree> predecessorElements = predecessor.elements();
      if (predecessorElements.isEmpty()) {
        return hasPredecessorInBlock(predecessor, loop);
      } else {
        Tree predecessorFirstElement = predecessorElements.get(0);

        if (isForStatementInitializer(predecessorFirstElement, loop)) {
          // skip 'for' loops initializers
          continue;
        }

        if (isForStatementUpdate(predecessorFirstElement, loop)) {
          // there is no way to reach the 'for' loop update
          return !predecessor.predecessors().isEmpty();
        }

        if (isDescendant(predecessorFirstElement, loop)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isForStatementInitializer(Tree lastElement, Tree loop) {
    if (loop.is(Tree.Kind.FOR_STATEMENT)) {
      return isDescendant(lastElement, ((ForStatementTree) loop).initializer());
    }
    return loop.is(Tree.Kind.FOR_EACH_STATEMENT) && isDescendant(lastElement, ((ForEachStatement) loop).expression());
  }

  private static boolean isForStatementUpdate(Tree lastElement, Tree loop) {
    return loop.is(Tree.Kind.FOR_STATEMENT) && isDescendant(lastElement, ((ForStatementTree) loop).update());
  }

  private static boolean isDescendant(Tree descendant, Tree target) {
    Tree parent = descendant;
    while (parent != null) {
      if (parent.equals(target)) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }

  private static SyntaxToken jumpKeyword(Tree jumpStatement) {
    switch (jumpStatement.kind()) {
      case BREAK_STATEMENT:
        return ((BreakStatementTree) jumpStatement).breakKeyword();
      case CONTINUE_STATEMENT:
        return ((ContinueStatementTree) jumpStatement).continueKeyword();
      case RETURN_STATEMENT:
        return ((ReturnStatementTree) jumpStatement).returnKeyword();
      case THROW_STATEMENT:
        return ((ThrowStatementTree) jumpStatement).throwKeyword();
      default:
        throw new IllegalStateException("Unexpected jump statement.");
    }
  }

  private static CFG getCFG(Tree loop) {
    Tree currentTree = loop;
    do {
      currentTree = currentTree.parent();
    } while (!currentTree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.INITIALIZER, Tree.Kind.STATIC_INITIALIZER));

    if (currentTree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
      return CFG.build((MethodTree) currentTree);
    }
    if (currentTree.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      currentTree = ((LambdaExpressionTree) currentTree).body();
      if (!currentTree.is(Tree.Kind.BLOCK)) {
        throw new IllegalStateException("Block statement was expected");
      }
    }

    return CFG.buildCFG(((BlockTree) currentTree).body());
  }

}
