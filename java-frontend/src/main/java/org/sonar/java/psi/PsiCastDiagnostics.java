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
package org.sonar.java.psi;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFunctionalExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;

final class PsiCastDiagnostics {
  private PsiCastDiagnostics() {
  }

  static boolean isRedundant(PsiTypeCastExpression cast) {
    var operand = PsiUtil.skipParenthesizedExprDown(cast.getOperand());
    if (operand == null || operand instanceof PsiFunctionalExpression || cast.getType() == null || operand.getType() == null) {
      return false;
    }
    var from = operand.getType();
    var to = cast.getCastType().getType();
    if (!resolved(from) || !resolved(to)) {
      return false;
    }
    var anchor = anchor(cast);
    var parent = anchor.getParent();
    if (parent instanceof com.intellij.psi.PsiReferenceExpression reference && reference.getParent() instanceof PsiMethodCallExpression call
      && call.getArgumentList().isEmpty() && "getClass".equals(reference.getReferenceName())) {
      return true;
    }
    boolean assignable = TypeConversionUtil.isAssignable(to, from);
    if (parent instanceof PsiExpressionList arguments && arguments.getParent() instanceof PsiCallExpression call) {
      return assignable && sameCall(call, cast);
    }
    if (operand instanceof PsiMethodCallExpression call) {
      var method = call.resolveMethod();
      if (method != null && method.hasTypeParameters() && call.getTypeArguments().length == 0
        && method.getReturnType() instanceof PsiClassType result && result.resolve() instanceof com.intellij.psi.PsiTypeParameter parameter
        && java.util.Arrays.stream(method.getParameterList().getParameters()).anyMatch(argument ->
          PsiDiagnosticTypes.mentions(argument.getType(), parameter))) {
        return false;
      }
    }
    if (to.equals(from)) {
      return true;
    }
    if (parent instanceof com.intellij.psi.PsiArrayAccessExpression && assignable) {
      return true;
    }
    if (parent instanceof com.intellij.psi.PsiPolyadicExpression binary && from instanceof PsiPrimitiveType && to instanceof PsiPrimitiveType) {
      var operands = binary.getOperands();
      return assignable && java.util.Arrays.stream(operands).filter(expression -> expression != anchor)
        .filter(expression -> !(PsiUtil.skipParenthesizedExprDown(expression) instanceof PsiTypeCastExpression))
        .anyMatch(expression -> to.equals(expression.getType())) && to.equals(binary.getType());
    }
    com.intellij.psi.PsiType expected = null;
    if (parent instanceof PsiVariable variable) {
      expected = variable.getType();
    } else if (parent instanceof PsiAssignmentExpression assignment && assignment.getRExpression() == anchor) {
      expected = assignment.getLExpression().getType();
    } else if (parent instanceof PsiReturnStatement) {
      var method = PsiTreeUtil.getParentOfType(parent, PsiMethod.class);
      expected = method == null ? null : method.getReturnType();
    } else if (parent instanceof PsiTypeCastExpression outer) {
      expected = outer.getType();
    }
    if (expected == null || !TypeConversionUtil.isAssignable(expected, from)) {
      return false;
    }
    if (assignable) {
      return true;
    }
    if (operand instanceof PsiMethodCallExpression call) {
      var method = call.resolveMethod();
      return method != null && method.getReturnType() instanceof PsiClassType result && result.resolve() instanceof com.intellij.psi.PsiTypeParameter;
    }
    return false;
  }

  private static boolean resolved(com.intellij.psi.PsiType type) {
    if (type instanceof PsiClassType cls) {
      return cls.resolve() != null && java.util.Arrays.stream(cls.getParameters()).allMatch(PsiCastDiagnostics::resolved);
    }
    if (type instanceof PsiArrayType array) {
      return resolved(array.getComponentType());
    }
    if (type instanceof com.intellij.psi.PsiWildcardType wildcard) {
      return !wildcard.isBounded() || resolved(wildcard.getBound());
    }
    return true;
  }

  static PsiExpression anchor(PsiTypeCastExpression cast) {
    PsiExpression anchor = cast;
    while (anchor.getParent() instanceof PsiParenthesizedExpression parentheses) {
      anchor = parentheses;
    }
    return anchor;
  }

  private static boolean sameCall(PsiCallExpression call, PsiTypeCastExpression cast) {
    var original = call.resolveMethodGenerics();
    if (!original.isValidResult() || original.getElement() == null) {
      return false;
    }
    int start = cast.getTextRange().getStartOffset() - call.getTextRange().getStartOffset();
    int end = cast.getTextRange().getEndOffset() - call.getTextRange().getStartOffset();
    String text = call.getText();
    var replacement = JavaPsiFacade.getElementFactory(call.getProject()).createExpressionFromText(
      text.substring(0, start) + cast.getOperand().getText() + text.substring(end), call);
    if (!(replacement instanceof PsiCallExpression alternative)) {
      return false;
    }
    var result = alternative.resolveMethodGenerics();
    if (!result.isValidResult() || !original.getElement().equals(result.getElement())) {
      return false;
    }
    var method = (PsiMethod) original.getElement();
    if (method.isVarArgs() && cast.getOperand().getType() instanceof PsiArrayType && !cast.getCastType().getType().equals(cast.getOperand().getType())) {
      return false;
    }
    return java.util.Arrays.stream(method.getTypeParameters()).allMatch(parameter -> java.util.Objects.equals(
      original.getSubstitutor().substitute(parameter), result.getSubstitutor().substitute(parameter)));
  }

}
