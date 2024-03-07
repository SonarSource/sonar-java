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
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S6915")
public class StringIndexOfRangesCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final String BEGIN_IDX_SMALLER_THAN_STR_LENGTH = "Begin index should be smaller than the length of the string.";
  private static final String END_IDX_AT_MOST_STR_LENGTH = "End index should be at most the length of the string.";
  private static final String BEGIN_IDX_NOT_LARGER_THAN_END_IDX = "Begin index should not be larger than endIndex.";
  private static final String END_INDEX = "End index";
  private static final String RECEIVER_STRING = "Receiver string";

  private static final MethodMatchers INDEX_OF_MATCHERS =
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("indexOf")
      .addParametersMatcher("int", "int", "int")
      .addParametersMatcher(JAVA_LANG_STRING, "int", "int")
      .build();

  private static final MethodMatchers LENGTH_MATCHERS =
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("length")
      .addWithoutParametersMatcher()
      .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return INDEX_OF_MATCHERS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodInvocation) {
    var issueFound = checkConstantBounds(methodInvocation);
    if (!issueFound && methodInvocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect
      && memberSelect.expression() instanceof IdentifierTree idTree) {
      checkBoundsThatDependOnLength(methodInvocation, idTree.name());
    }
  }

  /**
   * Return value indicates whether an issue has been found or not
   */
  private boolean checkConstantBounds(MethodInvocationTree methodInvocation) {
    var beginIdxExpr = methodInvocation.arguments().get(1);
    var endIdxExpr = methodInvocation.arguments().get(2);

    var beginIdxConst = beginIdxExpr.asConstant(Integer.class);
    var endIdxConst = endIdxExpr.asConstant(Integer.class);

    if (beginIdxConst.isPresent() && beginIdxConst.get() < 0) {
      reportIssue(beginIdxExpr, "Begin index should be non-negative.");
      return true;
    }

    if (beginIdxConst.isPresent() && endIdxConst.isPresent() && beginIdxConst.get() > endIdxConst.get()) {
      reportWithSecondaryLocation(beginIdxExpr, BEGIN_IDX_NOT_LARGER_THAN_END_IDX, endIdxExpr, END_INDEX);
      return true;
    }

    Optional<String> receiverConst = methodInvocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect
      ? memberSelect.expression().asConstant(String.class) : Optional.empty();

    if (receiverConst.isPresent() && beginIdxConst.isPresent() && beginIdxConst.get() >= receiverConst.get().length()) {
      reportWithSecondaryLocation(beginIdxExpr, BEGIN_IDX_SMALLER_THAN_STR_LENGTH, methodInvocation.methodSelect(), RECEIVER_STRING);
      return true;
    }

    if (receiverConst.isPresent() && endIdxConst.isPresent() && endIdxConst.get() > receiverConst.get().length()) {
      reportWithSecondaryLocation(endIdxExpr, END_IDX_AT_MOST_STR_LENGTH, methodInvocation.methodSelect(), RECEIVER_STRING);
      return true;
    }

    return false;
  }

  private void checkBoundsThatDependOnLength(MethodInvocationTree methodInvocation, String lengthReceiverVarName) {
    var beginIdxExpr = methodInvocation.arguments().get(1);
    var endIdxExpr = methodInvocation.arguments().get(2);
    var beginIdxDelta = lengthDelta(beginIdxExpr, lengthReceiverVarName);
    var endIdxDelta = lengthDelta(endIdxExpr, lengthReceiverVarName);
    if (beginIdxDelta.isPresent() && beginIdxDelta.get() >= 0) {
      reportIssue(beginIdxExpr, BEGIN_IDX_SMALLER_THAN_STR_LENGTH);
    } else if (endIdxDelta.isPresent() && endIdxDelta.get() > 0) {
      reportIssue(endIdxExpr, END_IDX_AT_MOST_STR_LENGTH);
    } else if (beginIdxDelta.isPresent() && endIdxDelta.isPresent() && beginIdxDelta.get() > endIdxDelta.get()) {
      reportWithSecondaryLocation(beginIdxExpr, BEGIN_IDX_NOT_LARGER_THAN_END_IDX, endIdxExpr, END_INDEX);
    }
  }

  private void reportWithSecondaryLocation(ExpressionTree tree, String msg, ExpressionTree secondaryLocation, String secondaryMsg) {
    reportIssue(tree, msg, List.of(new JavaFileScannerContext.Location(secondaryMsg, secondaryLocation)), null);
  }

  /**
   * @param expr    the expression
   * @param varName name of the variable
   * @return an Optional containing the difference delta such that the value of expr is var.length() + delta (delta may be negative),
   * or an empty Optional if the expression is too complex for delta to be computed
   */
  private static Optional<Integer> lengthDelta(ExpressionTree expr, String varName) {
    if (isCallToLengthOnVariable(expr, varName)) {
      // 1st pattern: var.length()
      return Optional.of(0);
    }
    if (expr instanceof BinaryExpressionTree binaryExpr) {
      var isPlus = binaryExpr.kind() == Tree.Kind.PLUS;
      var isMinus = binaryExpr.kind() == Tree.Kind.MINUS;
      if (!(isPlus || isMinus)) {
        return Optional.empty();
      }
      var leftCst = binaryExpr.leftOperand().asConstant(Integer.class);
      var rightCst = binaryExpr.rightOperand().asConstant(Integer.class);
      if (isCallToLengthOnVariable(binaryExpr.leftOperand(), varName) && rightCst.isPresent()) {
        // 2nd pattern:  var.length() + cst  or  var.length() - cst
        return isPlus ? rightCst : rightCst.map(x -> -x);
      } else if (isPlus && leftCst.isPresent() && isCallToLengthOnVariable(binaryExpr.rightOperand(), varName)) {
        // 3rd pattern: cst + var.length()
        return leftCst;
      }
    }
    return Optional.empty();
  }

  private static boolean isCallToLengthOnVariable(ExpressionTree expr, String varName) {
    if (expr instanceof MethodInvocationTree mit) {
      return LENGTH_MATCHERS.matches(mit.methodSymbol())
        && mit.methodSelect() instanceof MemberSelectExpressionTree memberSelect
        && memberSelect.expression() instanceof IdentifierTree idTree
        && idTree.symbol().name().equals(varName);
    }
    return false;
  }

}
