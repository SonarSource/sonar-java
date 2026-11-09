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

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSuperExpression;
import com.intellij.psi.PsiThisExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiUtil;

final class PsiAssignmentDiagnostics {
  private PsiAssignmentDiagnostics() {
  }

  static boolean hasNoEffect(PsiAssignmentExpression expression) {
    if (expression.getOperationTokenType() != JavaTokenType.EQ) {
      return false;
    }
    var left = PsiUtil.skipParenthesizedExprDown(expression.getLExpression());
    var right = PsiUtil.skipParenthesizedExprDown(expression.getRExpression());
    if (right instanceof PsiAssignmentExpression assignment) {
      right = assignment.getLExpression();
    }
    if (!(left instanceof PsiReferenceExpression lhs) || !(right instanceof PsiReferenceExpression rhs)
      || !(lhs.resolve() instanceof PsiVariable variable) || variable.hasModifierProperty(PsiModifier.VOLATILE) || !variable.equals(rhs.resolve())) {
      return false;
    }
    if (variable instanceof PsiField) {
      return currentInstance(lhs, variable) && currentInstance(rhs, variable);
    }
    return true;
  }

  private static boolean currentInstance(PsiReferenceExpression reference, PsiVariable variable) {
    var qualifier = PsiUtil.skipParenthesizedExprDown(reference.getQualifierExpression());
    return qualifier == null || qualifier instanceof PsiThisExpression current && current.getQualifier() == null
      || qualifier instanceof PsiSuperExpression current && current.getQualifier() == null
      || variable.hasModifierProperty(PsiModifier.STATIC) && qualifier instanceof PsiReferenceExpression name
        && name.resolve() instanceof com.intellij.psi.PsiClass;
  }
}
