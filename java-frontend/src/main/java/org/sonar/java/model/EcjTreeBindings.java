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

import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.pattern.AbstractPatternTree;

final class EcjTreeBindings {

  private EcjTreeBindings() {
  }

  @Nullable
  static ITypeBinding typeBinding(Object tree) {
    return switch (tree) {
      case AbstractTypedTree t -> (ITypeBinding) EcjSemanticAttachment.getBinding(t.type);
      case ClassTreeImpl t -> (ITypeBinding) EcjSemanticAttachment.getBinding(t.typeSymbol);
      case TypeParameterTreeImpl t -> (ITypeBinding) EcjSemanticAttachment.getBinding(t.typeSymbol);
      case AbstractPatternTree t -> (ITypeBinding) EcjSemanticAttachment.getBinding(t.type);
      default -> throw new IllegalArgumentException("No ECJ type attachment: " + tree.getClass());
    };
  }

  @Nullable
  static IMethodBinding methodBinding(Object tree) {
    Supplier<?> attachment = switch (tree) {
      case MethodTreeImpl t -> t.methodSymbol;
      case MethodInvocationTreeImpl t -> t.methodSymbolSupplier;
      case LambdaExpressionTreeImpl t -> t.methodSymbol;
      default -> throw new IllegalArgumentException("No ECJ method attachment: " + tree.getClass());
    };
    return (IMethodBinding) EcjSemanticAttachment.getBinding(attachment);
  }

  @Nullable
  static IVariableBinding variableBinding(VariableTreeImpl tree) {
    return (IVariableBinding) EcjSemanticAttachment.getBinding(tree.variableSymbol);
  }

  @Nullable
  static IBinding binding(Object tree) {
    return switch (tree) {
      case IdentifierTreeImpl t -> EcjSemanticAttachment.getBinding(t.symbolSupplier);
      case JavaTree.ImportTreeImpl t -> EcjSemanticAttachment.getBinding(t.symbolSupplier);
      default -> throw new IllegalArgumentException("No ECJ symbol attachment: " + tree.getClass());
    };
  }

  static void setType(JSema sema, JavaTree tree, @Nullable ITypeBinding binding) {
    switch (tree) {
      case AbstractTypedTree t -> t.type = sema.typeSupplier(binding);
      case ClassTreeImpl t -> t.typeSymbol = binding == null ? SemanticValues.UNKNOWN_TYPE_SYMBOL
        : new EcjSemanticAttachment<>(binding, sema::typeSymbol);
      case TypeParameterTreeImpl t -> t.typeSymbol = sema.symbolSupplier(binding);
      default -> throw new IllegalArgumentException("No type attachment: " + tree.getClass());
    }
    if (tree instanceof KeywordThis t) {
      t.symbolSupplier = binding == null ? SemanticValues.UNKNOWN_SYMBOL
        : new EcjSemanticAttachment<>(binding, b -> sema.typeSymbol(b).thisSymbol);
    } else if (tree instanceof KeywordSuper t) {
      t.symbolSupplier = binding == null ? SemanticValues.UNKNOWN_SYMBOL
        : new EcjSemanticAttachment<>(binding, b -> sema.typeSymbol(b).superSymbol);
    }
  }

  static void setSymbol(JSema sema, JavaTree tree, @Nullable IBinding binding) {
    switch (tree) {
      case MethodTreeImpl t -> t.methodSymbol = sema.methodSupplier((IMethodBinding) binding);
      case MethodInvocationTreeImpl t -> t.methodSymbolSupplier = sema.methodSupplier((IMethodBinding) binding);
      case LambdaExpressionTreeImpl t -> t.methodSymbol = sema.methodSupplier((IMethodBinding) binding);
      case VariableTreeImpl t -> t.variableSymbol = sema.symbolSupplier(binding);
      case IdentifierTreeImpl t -> t.symbolSupplier = sema.symbolSupplier(binding);
      case JavaTree.ImportTreeImpl t -> t.symbolSupplier = binding == null || binding.getKind() == IBinding.PACKAGE
        || binding.getKind() == IBinding.MODULE ? () -> null : sema.symbolSupplier(binding);
      default -> throw new IllegalArgumentException("No symbol attachment: " + tree.getClass());
    }
  }
}
