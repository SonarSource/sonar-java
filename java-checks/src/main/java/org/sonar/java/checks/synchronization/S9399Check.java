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
package org.sonar.java.checks.synchronization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.ExpressionUtils.isThis;

@Rule(key = "S9399")
public class S9399Check extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SynchronizedStatementTree sst = (SynchronizedStatementTree) tree;
    Symbol lockSymbol = resolveFieldSymbol(sst.expression());
    if (lockSymbol == null || !lockSymbol.isStatic()) {
      return;
    }
    InstanceFieldAccessVisitor visitor = new InstanceFieldAccessVisitor(lockSymbol.owner());
    sst.block().accept(visitor);
    List<IdentifierTree> instanceFieldAccesses = visitor.getInstanceFieldAccesses();
    if (!instanceFieldAccesses.isEmpty()) {
      List<JavaFileScannerContext.Location> secondaries = instanceFieldAccesses.stream()
        .map(id -> new JavaFileScannerContext.Location("Instance field", id))
        .toList();
      reportIssue(sst.expression(), "Use an instance-level lock to guard instance fields.", secondaries, null);
    }
  }

  @CheckForNull
  private static Symbol resolveFieldSymbol(ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      if (!symbol.isUnknown() && symbol.owner().isTypeSymbol()) {
        return symbol;
      }
    } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expression;
      return resolveFieldSymbol(mse.identifier());
    }
    return null;
  }

  private static class InstanceFieldAccessVisitor extends BaseTreeVisitor {

    private final Symbol ownerType;
    private final List<IdentifierTree> instanceFieldAccesses = new ArrayList<>();

    InstanceFieldAccessVisitor(Symbol ownerType) {
      this.ownerType = ownerType;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol symbol = tree.symbol();
      if (!symbol.isUnknown() && symbol.owner().isTypeSymbol() && !symbol.isStatic()
        && isOwnedBySameOrEnclosingClass(symbol)) {
        instanceFieldAccesses.add(tree);
      }
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      ExpressionTree expression = tree.expression();
      if (isThis(expression)) {
        visitIdentifier(tree.identifier());
      } else if (!expression.is(Tree.Kind.IDENTIFIER) || isThis(expression)) {
        super.visitMemberSelectExpression(tree);
      }
    }

    private boolean isOwnedBySameOrEnclosingClass(Symbol symbol) {
      Symbol current = symbol.owner();
      while (current != null && current.isTypeSymbol()) {
        if (current.equals(ownerType)) {
          return true;
        }
        current = current.owner();
      }
      return false;
    }

    List<IdentifierTree> getInstanceFieldAccesses() {
      return instanceFieldAccesses;
    }
  }
}
