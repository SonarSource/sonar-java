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

import com.intellij.codeInsight.ExceptionUtil;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDisjunctionType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

final class PsiCatchDiagnostics {
  private PsiCatchDiagnostics() {
  }

  static void collect(PsiTryStatement statement, BiConsumer<PsiElement, String> warning) {
    var block = statement.getTryBlock();
    if (block == null || incompleteExecutable(statement)) {
      return;
    }
    var thrown = new ArrayList<PsiClassType>(ExceptionUtil.getThrownExceptions(block));
    if (statement.getResourceList() != null) {
      thrown.addAll(ExceptionUtil.getThrownExceptions(statement.getResourceList()));
    }
    var previous = new ArrayList<com.intellij.psi.PsiType>();
    for (var section : statement.getCatchSections()) {
      var parameter = section.getParameter();
      if (parameter == null || parameter.getTypeElement() == null) {
        continue;
      }
      var type = parameter.getType();
      List<com.intellij.psi.PsiType> alternatives = type instanceof PsiDisjunctionType union ? union.getDisjunctions() : List.of(type);
      for (var alternative : alternatives) {
        if (!(alternative instanceof PsiClassType cls) || cls.resolve() == null
          || InheritanceUtil.isInheritor(cls, "java.lang.RuntimeException") || InheritanceUtil.isInheritor(cls, "java.lang.Error")
          || cls.equalsToText("java.lang.Exception") || cls.equalsToText("java.lang.Throwable")) {
          continue;
        }
        var relevant = thrown.stream().filter(exception -> alternative.isAssignableFrom(exception) || exception.isAssignableFrom(alternative)).toList();
        if (!relevant.isEmpty() && relevant.stream().allMatch(exception -> previous.stream().anyMatch(caught -> caught.isAssignableFrom(exception)))
          && previous.stream().noneMatch(caught -> caught.isAssignableFrom(alternative))) {
          var element = parameter.getTypeElement();
          if (type instanceof PsiDisjunctionType) {
            element = PsiTreeUtil.getChildrenOfTypeAsList(element, PsiTypeElement.class).stream()
              .filter(candidate -> candidate.getType().equals(alternative)).findFirst().orElse(element);
          }
          warning.accept(element, "Unreachable catch block for " + PsiDiagnosticTypes.name(alternative)
            + ". Only more specific exceptions are thrown and they are handled by previous catch block(s).");
        }
      }
      previous.addAll(alternatives);
    }
  }

  private static boolean incompleteExecutable(PsiTryStatement statement) {
    var method = PsiTreeUtil.getParentOfType(statement, PsiMethod.class);
    var scope = method == null || method.getBody() == null ? statement : method.getBody();
    return PsiTreeUtil.findChildrenOfType(scope, PsiMethodCallExpression.class).stream().anyMatch(call -> call.resolveMethod() == null)
      || PsiTreeUtil.findChildrenOfType(scope, PsiJavaCodeReferenceElement.class).stream()
        .anyMatch(reference -> (reference.getParent() instanceof PsiTypeElement || reference.getParent() instanceof PsiNewExpression) && reference.resolve() == null);
  }
}
