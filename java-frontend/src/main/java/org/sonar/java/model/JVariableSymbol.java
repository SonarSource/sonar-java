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

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPostfixExpression;
import com.intellij.psi.PsiPrefixExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiResourceVariable;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import java.util.Optional;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.VariableTree;

final class JVariableSymbol extends JSymbol implements Symbol.VariableSymbol {
  private final PsiVariable variable;
  final PsiSubstitutor substitutor;

  JVariableSymbol(JSema sema, PsiVariable variable, PsiSubstitutor substitutor, String key) {
    super(sema, variable, key);
    this.variable = variable;
    this.substitutor = substitutor;
  }

  @Override
  public Type type() {
    return sema.read(() -> {
      var type = sema.type(substitutor.substitute(variable.getType()));
      var original = variable.getContainingFile().getOriginalFile();
      boolean resource = variable instanceof PsiResourceVariable || original != variable.getContainingFile()
        && PsiTreeUtil.getParentOfType(original.findElementAt(variable.getTextRange().getStartOffset()), PsiResourceVariable.class) != null;
      return resource && !type.isUnknown() && !type.isSubtypeOf("java.lang.AutoCloseable") ? Type.UNKNOWN : type;
    });
  }

  @Override
  public Symbol owner() {
    return sema.read(() -> {
      if (variable instanceof PsiField field) {
        var cls = field.getContainingClass();
        return PsiUtil.isArrayClass(cls) ? Symbol.UNKNOWN_SYMBOL
          : sema.type(JavaPsiFacade.getElementFactory(sema.project).createType(cls, substitutor)).symbol();
      }
      return super.owner();
    });
  }

  @Override
  public VariableTree declaration() {
    return (VariableTree) super.declaration();
  }

  @Override
  public TypeSymbol enclosingClass() {
    return variable instanceof PsiField ? owner() instanceof TypeSymbol cls ? cls : TypeSymbol.UNKNOWN_TYPE : super.enclosingClass();
  }

  @Override
  public boolean isLocalVariable() {
    return !isPlaceholder() && owner() instanceof Symbol.MethodSymbol;
  }

  @Override
  public boolean isParameter() {
    return variable instanceof PsiParameter;
  }

  private boolean isPlaceholder() {
    return sema.read(() -> variable instanceof PsiParameter parameter && parameter.getDeclarationScope() instanceof PsiMethod
      && !sema.declarations.containsKey(sema.key(variable)));
  }

  @Override
  public String name() {
    return isPlaceholder() ? sema.read(() -> "arg" + ((PsiMethod) ((PsiParameter) variable).getDeclarationScope()).getParameterList().getParameterIndex((PsiParameter) variable)) : super.name();
  }

  @Override
  public boolean isFinal() {
    return !isPlaceholder() && super.isFinal();
  }

  @Override
  public boolean isEffectivelyFinal() {
    return sema.read(() -> {
      if (isPlaceholder() || isFinal() || variable instanceof PsiField) {
        return false;
      }
      var scope = PsiTreeUtil.getParentOfType(variable, PsiMethod.class, PsiLambdaExpression.class, PsiClassInitializer.class);
      if (scope == null) {
        return false;
      }
      int assignments = 0;
      for (var reference : PsiTreeUtil.findChildrenOfType(scope, PsiReferenceExpression.class)) {
        if (reference.resolve() == variable) {
          var parent = reference.getParent();
          if (parent instanceof PsiAssignmentExpression assignment && assignment.getLExpression() == reference) {
            assignments++;
          } else if (parent instanceof PsiPostfixExpression || parent instanceof PsiPrefixExpression prefix
            && (prefix.getOperationTokenType() == JavaTokenType.PLUSPLUS || prefix.getOperationTokenType() == JavaTokenType.MINUSMINUS)) {
            return false;
          }
        }
      }
      return assignments == 0 || variable.getInitializer() == null && !(variable instanceof PsiParameter) && assignments == 1;
    });
  }

  @Override
  public Optional<Object> constantValue() {
    return sema.read(() -> {
      if (!isStatic() || !isFinal()) {
        return Optional.empty();
      }
      Object value = variable.computeConstantValue();
      if (value instanceof Byte || value instanceof Short) {
        value = ((Number) value).intValue();
      } else if (value instanceof Character character) {
        value = (int) character;
      }
      return Optional.ofNullable(value);
    });
  }
}
