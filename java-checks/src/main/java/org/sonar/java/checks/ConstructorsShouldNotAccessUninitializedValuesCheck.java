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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3037")
public class ConstructorsShouldNotAccessUninitializedValuesCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree recordTree = (ClassTree) tree;
    Symbol.TypeSymbol recordSymbol = recordTree.symbol();
    if (recordSymbol.isUnknown()) {
      return;
    }

    Set<String> componentNames = recordTree.recordComponents().stream()
      .map(VariableTree::simpleName)
      .map(IdentifierTree::name)
      .collect(Collectors.toSet());

    if (componentNames.isEmpty()) {
      return;
    }

    for (Tree member : recordTree.members()) {
      if (member.is(Tree.Kind.CONSTRUCTOR)) {
        MethodTree constructor = (MethodTree) member;
        if (isCompactConstructor(constructor) && constructor.block() != null) {
          constructor.block().accept(new CompactConstructorVisitor(recordSymbol, componentNames));
        }
      }
    }
  }

  private static boolean isCompactConstructor(MethodTree constructor) {
    return constructor.openParenToken() == null;
  }

  private class CompactConstructorVisitor extends BaseTreeVisitor {

    private final Symbol.TypeSymbol recordSymbol;
    private final Set<String> componentNames;

    public CompactConstructorVisitor(Symbol.TypeSymbol recordSymbol, Set<String> componentNames) {
      this.recordSymbol = recordSymbol;
      this.componentNames = componentNames;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit local/anonymous classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // Do not visit lambdas
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      // Do not visit anonymous class body in new class expressions
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (isInvocationOnRecordInstance(tree)) {
        Symbol methodSymbol = tree.methodSymbol();
        if (!methodSymbol.isUnknown()
          && recordSymbol.equals(methodSymbol.owner())
          && tree.arguments().isEmpty()
          && componentNames.contains(methodSymbol.name())
          && ((Symbol.MethodSymbol) methodSymbol).declaration() == null) {
          reportIssue(ExpressionUtils.methodName(tree),
            String.format("Remove this use of the uninitialized value \"%s()\".", methodSymbol.name()));
        }
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isInvocationOnRecordInstance(MethodInvocationTree invocation) {
      ExpressionTree methodSelect = invocation.methodSelect();
      if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
        return true;
      }
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree receiver = ExpressionUtils.skipParentheses(((MemberSelectExpressionTree) methodSelect).expression());
        while (receiver.is(Tree.Kind.TYPE_CAST)) {
          receiver = ExpressionUtils.skipParentheses(((TypeCastTree) receiver).expression());
        }
        if (ExpressionUtils.isThis(receiver)) {
          return true;
        }
        if (receiver.is(Tree.Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) receiver;
          if (ExpressionUtils.isThis(memberSelect.identifier())) {
            Symbol thisSymbol = memberSelect.identifier().symbol();
            return !thisSymbol.isUnknown() && recordSymbol.equals(thisSymbol.enclosingClass());
          }
        }
      }
      return false;
    }
  }
}
