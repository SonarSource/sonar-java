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

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

public final class EcjTestSupport {

  private EcjTestSupport() {
  }

  public static JSema sema(CompilationUnitTree tree) {
    return (JSema) ((JavaTree.CompilationUnitTreeImpl) tree).sema;
  }

  public static ITypeBinding typeBinding(Object tree) {
    return EcjTreeBindings.typeBinding(tree);
  }

  public static IMethodBinding methodBinding(Object tree) {
    return EcjTreeBindings.methodBinding(tree);
  }

  public static IVariableBinding variableBinding(VariableTreeImpl tree) {
    return EcjTreeBindings.variableBinding(tree);
  }

  public static IBinding binding(Object tree) {
    return EcjTreeBindings.binding(tree);
  }

  public static void clearType(JavaTree tree) {
    if (tree instanceof AbstractTypedTree typed) {
      typed.type = SemanticValues.UNKNOWN_TYPE;
    } else if (tree instanceof ClassTreeImpl cls) {
      cls.typeSymbol = SemanticValues.UNKNOWN_TYPE_SYMBOL;
    } else if (tree instanceof TypeParameterTreeImpl parameter) {
      parameter.typeSymbol = SemanticValues.UNKNOWN_SYMBOL;
    } else {
      throw new IllegalArgumentException("No type attachment: " + tree.getClass());
    }
    if (tree instanceof KeywordThis || tree instanceof KeywordSuper) {
      ((IdentifierTreeImpl) tree).symbolSupplier = SemanticValues.UNKNOWN_SYMBOL;
    }
  }

  public static void clearSymbol(JavaTree tree) {
    if (tree instanceof IdentifierTreeImpl identifier) {
      identifier.symbolSupplier = SemanticValues.UNKNOWN_SYMBOL;
    } else if (tree instanceof VariableTreeImpl variable) {
      variable.variableSymbol = SemanticValues.UNKNOWN_SYMBOL;
    } else {
      throw new IllegalArgumentException("No symbol attachment: " + tree.getClass());
    }
  }
}
