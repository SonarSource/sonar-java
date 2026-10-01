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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
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
        BlockTree body = constructor.block();
        if (isCompactConstructor(constructor)) {
          body.accept(new AccessorCallVisitor(recordSymbol, componentNames, Collections.emptyMap()));
        } else if (!delegatesToAnotherConstructor(body)) {
          body.accept(new AccessorCallVisitor(recordSymbol, componentNames, fieldAssignmentEnds(body, componentNames)));
        }
      }
    }
  }

  private static boolean isCompactConstructor(MethodTree constructor) {
    return constructor.openParenToken() == null;
  }

  /**
   * Non-canonical record constructors must invoke "this(...)" as a top-level statement of their body, possibly preceded by other
   * statements since Java 25, so a constructor without such an invocation is the explicit canonical constructor.
   */
  private static boolean delegatesToAnotherConstructor(BlockTree body) {
    return body.body().stream()
      .filter(statement -> statement.is(Tree.Kind.EXPRESSION_STATEMENT))
      .map(statement -> ((ExpressionStatementTree) statement).expression())
      .filter(expression -> expression.is(Tree.Kind.METHOD_INVOCATION))
      .map(expression -> ((MethodInvocationTree) expression).methodSelect())
      .anyMatch(ExpressionUtils::isThis);
  }

  /**
   * Returns, for each record component, the end position of the first "this.component = ..." assignment of the canonical constructor body.
   * A final field cannot be assigned in a loop, so any accessor call located before this position runs before the field is assigned.
   */
  private static Map<String, Position> fieldAssignmentEnds(BlockTree body, Set<String> componentNames) {
    Map<String, Position> assignmentEnds = new HashMap<>();
    body.accept(new BaseTreeVisitor() {
      @Override
      public void visitClass(ClassTree tree) {
        // Record fields cannot be assigned from local or anonymous classes
      }

      @Override
      public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
        // Record fields cannot be assigned from lambdas
      }

      @Override
      public void visitAssignmentExpression(AssignmentExpressionTree tree) {
        ExpressionTree variable = ExpressionUtils.skipParentheses(tree.variable());
        if (variable.is(Tree.Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) variable;
          String name = memberSelect.identifier().name();
          if (ExpressionUtils.isThis(memberSelect.expression()) && componentNames.contains(name)) {
            assignmentEnds.putIfAbsent(name, Position.endOf(tree));
          }
        }
        super.visitAssignmentExpression(tree);
      }
    });
    return assignmentEnds;
  }

  private class AccessorCallVisitor extends BaseTreeVisitor {

    private final Symbol.TypeSymbol recordSymbol;
    private final Set<String> componentNames;
    private final Map<String, Position> assignmentEnds;

    public AccessorCallVisitor(Symbol.TypeSymbol recordSymbol, Set<String> componentNames, Map<String, Position> assignmentEnds) {
      this.recordSymbol = recordSymbol;
      this.componentNames = componentNames;
      this.assignmentEnds = assignmentEnds;
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
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol methodSymbol = tree.methodSymbol();
      String name = methodSymbol.name();
      if (tree.arguments().isEmpty()
        && componentNames.contains(name)
        && recordSymbol.equals(methodSymbol.owner())
        && isInvocationOnRecordInstance(tree)
        && isBeforeFieldAssignment(name, tree)) {
        reportIssue(ExpressionUtils.methodName(tree),
          "Replace this call to \"" + name + "()\" with the \"" + name + "\" parameter; the field is not assigned yet.");
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isBeforeFieldAssignment(String componentName, MethodInvocationTree invocation) {
      Position assignmentEnd = assignmentEnds.get(componentName);
      return assignmentEnd == null || Position.startOf(invocation).isBefore(assignmentEnd);
    }
  }

  private static boolean isInvocationOnRecordInstance(MethodInvocationTree invocation) {
    ExpressionTree methodSelect = invocation.methodSelect();
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      return true;
    }
    ExpressionTree receiver = ExpressionUtils.skipParentheses(((MemberSelectExpressionTree) methodSelect).expression());
    while (receiver.is(Tree.Kind.TYPE_CAST)) {
      receiver = ExpressionUtils.skipParentheses(((TypeCastTree) receiver).expression());
    }
    return isThisReference(receiver);
  }

  /**
   * Records are implicitly static, so both "this" and a qualified "X.this" denote the record being constructed.
   */
  private static boolean isThisReference(ExpressionTree expression) {
    return ExpressionUtils.isThis(expression)
      || (expression.is(Tree.Kind.MEMBER_SELECT) && ExpressionUtils.isThis(((MemberSelectExpressionTree) expression).identifier()));
  }
}
