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
package org.sonar.java.model;

import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.infos.MethodCandidateInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Preserves legacy problem messages and categories for partial hierarchy, conversion, and inference resolution. */
final class PsiResolutionDiagnostics {
  private PsiResolutionDiagnostics() {
  }

  static void collect(PsiJavaFile file, Set<JProblem> problems) {
    file.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitClass(PsiClass declaration) {
        if (Arrays.stream(declaration.getSuperTypes()).map(PsiClassType::resolve).filter(java.util.Objects::nonNull)
          .anyMatch(parent -> incompleteHierarchy(parent, new HashSet<>()))) {
          add(problems, "The hierarchy of the type " + declaration.getName() + " is inconsistent");
        }
        super.visitClass(declaration);
      }

      @Override
      public void visitArrayInitializerExpression(PsiArrayInitializerExpression expression) {
        if (expression.getType() instanceof PsiArrayType array && array.getComponentType() instanceof PsiClassType expected && expected.resolve() == null) {
          for (var initializer : expression.getInitializers()) {
            if (initializer.getType() instanceof PsiClassType actual && actual.resolve() != null) {
              add(problems, "Type mismatch: cannot convert from " + PsiDiagnosticTypes.name(actual) + " to " + PsiDiagnosticTypes.name(expected));
            }
          }
        }
        super.visitArrayInitializerExpression(expression);
      }

      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression call) {
        var resolution = call.resolveMethodGenerics();
        if (resolution instanceof MethodCandidateInfo candidate && candidate.getElement().hasTypeParameters()
          && (candidate.getInferenceErrorMessage() != null || incompleteLambda(call))) {
          var method = candidate.getElement();
          String variables = Arrays.stream(method.getTypeParameters()).map(PsiNamedElement::getName).collect(Collectors.joining(","));
          String parameters = Arrays.stream(method.getParameterList().getParameters()).map(parameter -> PsiDiagnosticTypes.name(parameter.getType()))
            .collect(Collectors.joining(", "));
          add(problems, "Cannot infer type argument(s) for <" + variables + "> " + method.getName() + "(" + parameters + ")");
        }
        super.visitMethodCallExpression(call);
      }
    });
  }

  private static boolean incompleteLambda(PsiMethodCallExpression call) {
    return Arrays.stream(call.getArgumentList().getExpressions()).anyMatch(argument -> argument instanceof PsiLambdaExpression lambda
      && lambda.getBody() instanceof PsiExpression body
      && (body.getType() == null || body.getType() instanceof PsiClassType type && type.resolve() == null));
  }

  private static boolean incompleteHierarchy(PsiClass declaration, Set<PsiClass> seen) {
    if (!seen.add(declaration)) {
      return false;
    }
    return Arrays.stream(declaration.getSuperTypes()).anyMatch(parent -> parent.resolve() == null || incompleteHierarchy(parent.resolve(), seen));
  }

  private static void add(Set<JProblem> problems, String message) {
    problems.add(new JProblem(message, JProblem.Type.PREVIEW_FEATURE_USED));
  }
}
