/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatchersBuilder;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2142")
public class InterruptedExceptionCheck extends IssuableSubscriptionVisitor {

  private static final Predicate<Type> INTERRUPTING_TYPE_PREDICATE = catchType ->
      catchType.is("java.lang.InterruptedException") ||
      catchType.is("java.lang.ThreadDeath");

  private Deque<Boolean> withinInterruptingFinally = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
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
      Optional<Type> interruptType = findInterruptingType(catchTree.parameter());
      if(interruptType.isPresent()) {
        BlockVisitor blockVisitor = new BlockVisitor(catchTree.parameter().symbol());
        catchTree.block().accept(blockVisitor);
        if(!blockVisitor.threadInterrupted && !isWithinInterruptingFinally()) {
          reportIssue(catchTree.parameter(), "Either re-interrupt this method" +
            " or rethrow the \""+interruptType.get().name()+"\".");
        }
      }
    }
  }

  private static Optional<Type> findInterruptingType(VariableTree parameter) {
    if (parameter.type().is(Tree.Kind.UNION_TYPE)) {
      return ((UnionTypeTree) parameter.type()).typeAlternatives().stream()
        .map(TypeTree::symbolType)
        .filter(INTERRUPTING_TYPE_PREDICATE)
        .findFirst();
    }
    return Optional.of(parameter)
      .map(VariableTree::symbol)
      .map(Symbol::type)
      .filter(INTERRUPTING_TYPE_PREDICATE);
  }

  private boolean isWithinInterruptingFinally() {
    return withinInterruptingFinally.stream().anyMatch(Boolean.TRUE::equals);
  }

  @Override
  public void leaveNode(Tree tree) {
    withinInterruptingFinally.removeFirst();
  }

  private static boolean isFinallyInterrupting(@Nullable BlockTree blockTree) {
    if(blockTree == null ){
      return false;
    }
    BlockVisitor blockVisitor = new BlockVisitor();
    blockTree.accept(blockVisitor);
    return blockVisitor.threadInterrupted;
  }

  private static class BlockVisitor extends BaseTreeVisitor {
    @Nullable
    private final Symbol catchedException;
    boolean threadInterrupted = false;
    private int depth = 0;
    
    private static final int MAX_DEPTH = 3;
    
    private static final MethodMatchers INTERRUPT_MATCHERS = new MethodMatchersBuilder()
      .ofSubTypes("java.lang.Thread")
      .names("interrupt")
      .addWithoutParametersMatcher()
      .build();

    public BlockVisitor() {
      this.catchedException = null;
    }

    public BlockVisitor(Symbol catchedException) {
      this.catchedException = catchedException;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (threadInterrupted || depth >= MAX_DEPTH) {
        return;
      }
      depth++;
      if (INTERRUPT_MATCHERS.matches(tree)) {
        threadInterrupted = true;
        return;
      }
      Tree declaration = tree.symbol().declaration();
      if (declaration != null) {
        //Declaration of MethodInvocationTree is MethodTree
        BlockTree block = ((MethodTree) declaration).block();
        if (block != null) {
          block.accept(this);
        }
      }
      depth--;
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      if (threadInterrupted || depth >= 3) {
        return;
      }
      if(tree.expression().is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) tree.expression()).symbol().equals(catchedException)) {
        threadInterrupted = true;
        return;
      }
      super.visitThrowStatement(tree);
    }
  }

}
