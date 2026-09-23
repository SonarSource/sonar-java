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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.ExpressionUtils.getEnclosingTree;
import static org.sonar.java.model.ExpressionUtils.isThisOrSuper;
import static org.sonar.java.model.ExpressionUtils.skipParentheses;

@Rule(key = "S9399")
public class S9399Check extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SynchronizedStatementTree sst = (SynchronizedStatementTree) tree;
    if (!isStaticLock(sst.expression())) {
      return;
    }
    Tree enclosingClassTree = getEnclosingTree(sst, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.RECORD);
    if (enclosingClassTree == null) {
      return;
    }
    Symbol enclosingClassSymbol = ((ClassTree) enclosingClassTree).symbol();
    InstanceFieldAccessVisitor visitor = new InstanceFieldAccessVisitor(enclosingClassSymbol);
    sst.block().accept(visitor);
    List<IdentifierTree> instanceFieldAccesses = visitor.getInstanceFieldAccesses();
    if (!instanceFieldAccesses.isEmpty()) {
      List<JavaFileScannerContext.Location> secondaries = instanceFieldAccesses.stream()
        .map(id -> new JavaFileScannerContext.Location("Instance field", id))
        .toList();
      reportIssue(sst.expression(), "Use an instance-level lock to guard instance fields.", secondaries, null);
    }
  }

  private static boolean isStaticLock(ExpressionTree expression) {
    ExpressionTree expr = skipParentheses(expression);
    if (expr.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
      if ("class".equals(mse.identifier().name())) {
        return true;
      }
      return isStaticLock(mse.identifier());
    }
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expr).symbol();
      return !symbol.isUnknown() && symbol.owner().isTypeSymbol() && symbol.isStatic();
    }
    return false;
  }

  private static class InstanceFieldAccessVisitor extends BaseTreeVisitor {

    private final Symbol ownerType;
    private final List<IdentifierTree> instanceFieldAccesses = new ArrayList<>();

    InstanceFieldAccessVisitor(Symbol ownerType) {
      this.ownerType = ownerType;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (isThisOrSuper(tree.name())) {
        return;
      }
      Symbol symbol = tree.symbol();
      if (!symbol.isUnknown() && symbol.isVariableSymbol() && symbol.owner().isTypeSymbol() && !symbol.isStatic()
        && isOwnedBySameOrEnclosingClass(symbol)) {
        instanceFieldAccesses.add(tree);
      }
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      ExpressionTree expression = skipParentheses(tree.expression());
      boolean isBareThisOrSuper = expression.is(Tree.Kind.IDENTIFIER) && isThisOrSuper(((IdentifierTree) expression).name());
      if (isBareThisOrSuper || isQualifiedThis(expression)) {
        visitIdentifier(tree.identifier());
      } else {
        scan(expression);
      }
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      if (isPassedAsArgument(lambdaExpressionTree)) {
        scan(lambdaExpressionTree.body());
      }
    }

    private static boolean isPassedAsArgument(LambdaExpressionTree lambda) {
      Tree parent = lambda.parent();
      return parent != null && parent.is(Tree.Kind.ARGUMENTS);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit inner classes as field accesses inside them are not guarded by the outer synchronized block
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      scan(tree.enclosingExpression());
      scan(tree.arguments());
    }

    private static boolean isQualifiedThis(ExpressionTree expression) {
      ExpressionTree expr = skipParentheses(expression);
      if (expr.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
        return "this".equals(mse.identifier().name()) || "super".equals(mse.identifier().name());
      }
      return false;
    }

    private boolean isOwnedBySameOrEnclosingClass(Symbol symbol) {
      Symbol fieldOwner = symbol.owner();
      Symbol current = ownerType;
      while (current != null && !current.isPackageSymbol()) {
        if (current.isTypeSymbol() && current.type().isSubtypeOf(fieldOwner.type().fullyQualifiedName())) {
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
