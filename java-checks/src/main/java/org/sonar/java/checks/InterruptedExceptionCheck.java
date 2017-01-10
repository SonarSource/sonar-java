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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import javax.annotation.Nullable;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S2142")
public class InterruptedExceptionCheck extends IssuableSubscriptionVisitor {

  private Deque<Boolean> withinInterruptingFinally = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if(context.getSemanticModel() != null) {
      super.scanFile(context);
    }
    withinInterruptingFinally.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    TryStatementTree tryStatementTree = (TryStatementTree) tree;
    withinInterruptingFinally.addFirst(isFinallyInterrupting(tryStatementTree.finallyBlock()));
    for (CatchTree catchTree : tryStatementTree.catches()) {
      if(catchTree.parameter().symbol().type().is("java.lang.InterruptedException")) {
        BlockVisitor blockVisitor = new BlockVisitor(catchTree.parameter().symbol());
        catchTree.block().accept(blockVisitor);
        if(!blockVisitor.threadInterrupted && !isWithinInterruptingFinally()) {
          reportIssue(catchTree.parameter(), "Either re-interrupt this method or rethrow the \"InterruptedException\".");
        }
      }
    }
  }

  private boolean isWithinInterruptingFinally() {
    for (Boolean aBoolean : withinInterruptingFinally) {
      if(aBoolean) {
        return true;
      }
    }
    return false;
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

    public BlockVisitor() {
      this.catchedException = null;
    }

    public BlockVisitor(Symbol catchedException) {
      this.catchedException = catchedException;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol symbol = tree.symbol();
      if (!symbol.isUnknown() && symbol.owner().type().isSubtypeOf("java.lang.Thread") && "interrupt".equals(symbol.name()) && tree.arguments().isEmpty()) {
        threadInterrupted = true;
        return;
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      if(tree.expression().is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) tree.expression()).symbol().equals(catchedException)) {
        threadInterrupted = true;
        return;
      }
      super.visitThrowStatement(tree);
    }
  }

}
