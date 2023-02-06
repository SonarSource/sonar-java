/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.matcher.MethodMatchersBuilder;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2142")
public class InterruptedExceptionCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Either re-interrupt this method or rethrow the \"%s\" that can be caught here.";

  private static final Predicate<Type> INTERRUPTING_TYPE_PREDICATE = catchType ->
    catchType.isSubtypeOf("java.lang.InterruptedException") ||
    catchType.isSubtypeOf("java.lang.ThreadDeath");

  private static final Predicate<Type> GENERIC_EXCEPTION_PREDICATE = catchType ->
    catchType.is("java.lang.Exception") ||
    catchType.is("java.lang.Throwable");

  private final Deque<Boolean> withinInterruptingFinally = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    withinInterruptingFinally.clear();
  }
  @Override
  public void leaveFile(JavaFileScannerContext context) {
    withinInterruptingFinally.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    TryStatementTree tryStatementTree = (TryStatementTree) tree;

    withinInterruptingFinally.addFirst(isFinallyInterrupting(tryStatementTree.finallyBlock()));
    for (CatchTree catchTree : tryStatementTree.catches()) {
      VariableTree catchParameter = catchTree.parameter();
      List<Type> caughtTypes = getCaughtTypes(catchParameter);

      Optional<Type> interruptType = caughtTypes.stream().filter(INTERRUPTING_TYPE_PREDICATE).findFirst();
      if (interruptType.isPresent()) {
        if (wasNotInterrupted(catchTree)) {
          reportIssue(catchParameter, String.format(MESSAGE, interruptType.get().name()));
        }
        return;
      } else if (caughtTypes.stream().anyMatch(GENERIC_EXCEPTION_PREDICATE)) {
        reportIfThrowInterruptInBlock(tryStatementTree.block(), catchTree);
        return;
      }
    }
  }

  private void reportIfThrowInterruptInBlock(BlockTree blockTree, CatchTree catchTree) {
    MethodTreeUtils.MethodInvocationCollector collector = new MethodTreeUtils.MethodInvocationCollector(InterruptedExceptionCheck::throwInterruptedException);
    blockTree.accept(collector);
    List<Tree> invocationInterrupting = collector.getInvocationTree();

    if (!invocationInterrupting.isEmpty() && wasNotInterrupted(catchTree)) {
      reportIssue(catchTree.parameter(), String.format(MESSAGE, "InterruptedException"),
        invocationInterrupting.stream()
          .map(t -> new JavaFileScannerContext.Location("Method invocation throwing InterruptedException.", t))
          .collect(Collectors.toList()),
        null);
    }
  }

  private boolean wasNotInterrupted(CatchTree catchTree) {
    BlockVisitor blockVisitor = new BlockVisitor();
    catchTree.block().accept(blockVisitor);
    return !blockVisitor.threadInterrupted && !isWithinInterruptingFinally();
  }

  private static List<Type> getCaughtTypes(VariableTree parameter) {
    if (parameter.type().is(Tree.Kind.UNION_TYPE)) {
      return ((UnionTypeTree) parameter.type()).typeAlternatives().stream()
        .map(TypeTree::symbolType)
        .collect(Collectors.toList());
    }
    return Collections.singletonList(parameter.symbol().type());
  }

  private boolean isWithinInterruptingFinally() {
    return withinInterruptingFinally.stream().anyMatch(Boolean.TRUE::equals);
  }

  @Override
  public void leaveNode(Tree tree) {
    withinInterruptingFinally.removeFirst();
  }

  private static boolean isFinallyInterrupting(@Nullable BlockTree blockTree) {
    if (blockTree == null) {
      return false;
    }
    BlockVisitor blockVisitor = new BlockVisitor();
    blockTree.accept(blockVisitor);
    return blockVisitor.threadInterrupted;
  }

  private static boolean throwInterruptedException(Symbol.MethodSymbol symbol) {
    return !symbol.isUnknown()
      && symbol.thrownTypes().stream().anyMatch(INTERRUPTING_TYPE_PREDICATE);
  }

  private static class BlockVisitor extends BaseTreeVisitor {
    boolean threadInterrupted = false;
    private int depth = 0;

    private static final int MAX_DEPTH = 3;

    private static final MethodMatchers INTERRUPT_MATCHERS = new MethodMatchersBuilder()
      .ofSubTypes("java.lang.Thread")
      .names("interrupt")
      .addWithoutParametersMatcher()
      .build();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (threadInterrupted || INTERRUPT_MATCHERS.matches(tree)) {
        threadInterrupted = true;
        return;
      }
      depth++;
      MethodTree declaration = tree.methodSymbol().declaration();
      if (declaration != null && depth <= MAX_DEPTH) {
        BlockTree block = declaration.block();
        if (block != null) {
          block.accept(this);
        }
      }
      depth--;
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      if (threadInterrupted || INTERRUPTING_TYPE_PREDICATE.test(tree.expression().symbolType())) {
        threadInterrupted = true;
      } else {
        super.visitThrowStatement(tree);
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Cut visit on anonymous and local classes, because we only want to analyze actual control flow.
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree tree) {
      // Cut visit on lambdas, because we only want to analyze actual control flow.
    }
  }
}
