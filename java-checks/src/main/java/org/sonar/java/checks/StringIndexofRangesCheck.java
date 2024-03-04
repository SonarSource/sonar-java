/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S6915")
public class StringIndexofRangesCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String INDEX_OF = "indexOf";
  private static final String LENGTH = "length";

  private static final MethodMatchers INDEX_OF_MATCHERS =
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names(INDEX_OF)
      .addParametersMatcher("int", "int", "int")
      .addParametersMatcher(JAVA_LANG_STRING, "int", "int")
      .build();

  private static final MethodMatchers LENGTH_MATCHERS =
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names(LENGTH)
      .addWithoutParametersMatcher()
      .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var methodInvocation = (MethodInvocationTree) tree;
    if (!INDEX_OF_MATCHERS.matches(methodInvocation)) {
      return;
    }

    checkConstantBounds(methodInvocation);

    if (methodInvocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect
      && memberSelect.expression() instanceof IdentifierTree idTree) {
      checkBoundsThatDependOnLength(methodInvocation, idTree.name());
    }

  }

  private void checkConstantBounds(MethodInvocationTree methodInvocation) {
    var beginIdxExpr = methodInvocation.arguments().get(1);
    var endIdxExpr = methodInvocation.arguments().get(2);

    var receiverConst = methodInvocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect
      ? memberSelect.expression().asConstant() : Optional.empty();
    var beginIdxConst = beginIdxExpr.asConstant();
    var endIdxConst = endIdxExpr.asConstant();

    if (beginIdxConst.isPresent() && ((int) beginIdxConst.get()) < 0) {
      reportIssue(beginIdxExpr, "Begin index should be non-negative.",
        callAsSingletonSecondaryLocation(methodInvocation), null);
    }

    if (beginIdxConst.isPresent() && endIdxConst.isPresent()
      && ((int) beginIdxConst.get()) > ((int) endIdxConst.get())) {
      reportBeginLargerThanEnd(methodInvocation, beginIdxExpr, endIdxExpr);
    }

    if (receiverConst.isPresent() && beginIdxConst.isPresent()
      && receiverConst.get() instanceof String s
      && ((int) beginIdxConst.get()) >= s.length()) {
      reportIssue(beginIdxExpr, "Begin index should be smaller than string length.",
        callAsSingletonSecondaryLocation(methodInvocation), null);
    }

    if (receiverConst.isPresent() && endIdxConst.isPresent()
      && receiverConst.get() instanceof String s
      && ((int) endIdxConst.get()) > s.length()) {
      reportIssue(endIdxExpr, "End index should not be larger than string length.",
        callAsSingletonSecondaryLocation(methodInvocation), null
      );
    }

  }

  private void checkBoundsThatDependOnLength(MethodInvocationTree methodInvocation, String lengthReceiverVarName) {
    var beginIdxExpr = methodInvocation.arguments().get(1);
    var endIdxExpr = methodInvocation.arguments().get(2);
    var beginIdxDelta = lengthDelta(beginIdxExpr, lengthReceiverVarName);
    var endIdxDelta = lengthDelta(endIdxExpr, lengthReceiverVarName);
    if (beginIdxDelta.isPresent() && beginIdxDelta.get() >= 0) {
      reportIssue(beginIdxExpr, "Begin index should be smaller than the length of the string.",
        callAsSingletonSecondaryLocation(methodInvocation), null);
    }
    if (endIdxDelta.isPresent() && endIdxDelta.get() > 0) {
      reportIssue(endIdxExpr, "End index should be at most the length of the string.",
        callAsSingletonSecondaryLocation(methodInvocation), null);
    }
    if (beginIdxDelta.isPresent() && endIdxDelta.isPresent() && beginIdxDelta.get() > endIdxDelta.get()) {
      reportBeginLargerThanEnd(methodInvocation, beginIdxExpr, endIdxExpr);
    }
  }

  private List<JavaFileScannerContext.Location> callAsSingletonSecondaryLocation(MethodInvocationTree methodInvocation) {
    return List.of(new JavaFileScannerContext.Location("Call", methodInvocation));
  }

  private void reportBeginLargerThanEnd(MethodInvocationTree methodInvocation, ExpressionTree beginIdxExpr, ExpressionTree endIdxExpr) {
    reportIssue(endIdxExpr, "Begin index should not be larger than endIndex.",
      List.of(
        new JavaFileScannerContext.Location("Call", methodInvocation),
        new JavaFileScannerContext.Location("Begin index", beginIdxExpr)
      ), null
    );
  }

  /**
   * @param expr    the expression
   * @param varName name of the variable
   * @return an Optional containing the difference delta such that the value of expr is var.length() + delta (delta may be negative),
   * or an empty Optional if the expression is too complex for delta to be computed
   */
  private Optional<Integer> lengthDelta(ExpressionTree expr, String varName) {
    if (expr instanceof BinaryExpressionTree binaryExpr) {
      var isPlus = binaryExpr.kind() == Tree.Kind.PLUS;
      var isMinus = binaryExpr.kind() == Tree.Kind.MINUS;
      if (!(isPlus || isMinus)) {
        return Optional.empty();
      }
      var leftCst = binaryExpr.leftOperand().asConstant();
      var rightCst = binaryExpr.rightOperand().asConstant();
      if (isCallToLengthOnVariable(binaryExpr.leftOperand(), varName) && rightCst.isPresent() && rightCst.get() instanceof Integer rightVal) {
        // 1st pattern: var.length() + cst  or  var.length() - cst
        return Optional.of(isPlus ? rightVal : -rightVal);
      } else if (isPlus && leftCst.isPresent() && leftCst.get() instanceof Integer leftVal
        && isCallToLengthOnVariable(binaryExpr.rightOperand(), varName)) {
        // 2nd pattern: cst + var.length()
        return Optional.of(leftVal);
      }
    }
    return Optional.empty();
  }

  private boolean isCallToLengthOnVariable(ExpressionTree expr, String varName) {
    if (expr instanceof MethodInvocationTree mit) {
      return LENGTH_MATCHERS.matches(mit.methodSymbol())
        && mit.methodSelect() instanceof MemberSelectExpressionTree memberSelect
        && memberSelect.expression() instanceof IdentifierTree idTree
        && idTree.symbol().name().equals(varName);
    }
    return false;
  }

}
