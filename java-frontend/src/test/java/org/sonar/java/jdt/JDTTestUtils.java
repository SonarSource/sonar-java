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
package org.sonar.java.jdt;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

final class JDTTestUtils {

  private JDTTestUtils() {
  }

  static JDTSema sema(CompilationUnitTree tree) {
    return (JDTSema) ((JavaTree.CompilationUnitTreeImpl) tree).sema;
  }

  static ITypeBinding typeBinding(ClassTree tree) {
    return ((JDTType) tree.symbol().type()).typeBinding;
  }

  static IMethodBinding methodBinding(MethodTree tree) {
    return tree.symbol() instanceof JDTMethodSymbol symbol ? symbol.methodBinding() : null;
  }

  static IMethodBinding methodBinding(MethodInvocationTree tree) {
    return tree.methodSymbol() instanceof JDTMethodSymbol symbol ? symbol.methodBinding() : null;
  }

}
