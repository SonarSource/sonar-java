/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class JUtilsTest {

  @Test
  void primitiveWrapperType() {
    JavaTree.CompilationUnitTreeImpl cu = test("");
    Type primitiveType = cu.sema.type(Objects.requireNonNull(cu.sema.resolveType("byte")));
    Type wrapperType = JUtils.primitiveWrapperType(primitiveType);

    assertThat(wrapperType).isNotNull();
    assertThat(JUtils.isPrimitiveWrapper(wrapperType)).isTrue();
    assertThat(wrapperType.fullyQualifiedName()).isEqualTo("java.lang.Byte");
  }

  @Test
  void primitiveType() {
    JavaTree.CompilationUnitTreeImpl cu = test("");
    Type wrapperType = cu.sema.type(Objects.requireNonNull(cu.sema.resolveType("java.lang.Byte")));
    Type primitiveType = JUtils.primitiveType(wrapperType);

    assertThat(primitiveType).isNotNull();
    assertThat(primitiveType.fullyQualifiedName()).isEqualTo("byte");
  }

  @Test
  void isNullType() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) Objects.requireNonNull(method.block()).body().get(0);
    AbstractTypedTree e = (AbstractTypedTree) Objects.requireNonNull(s.expression());

    assertThat(JUtils.isNullType(cu.sema.type(e.typeBinding)))
      .isEqualTo(JUtils.isNullType(e.symbolType()))
      .isTrue();
  }

  @Test
  void isParametrizedMethod() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { <T> void m(T p) { m(42); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTreeImpl s = (ExpressionStatementTreeImpl) Objects.requireNonNull(method.block()).body().get(0);
    MethodInvocationTreeImpl methodInvocation = (MethodInvocationTreeImpl) s.expression();

    assertThat(JUtils.isParametrizedMethod(cu.sema.methodSymbol(method.methodBinding)))
      .isEqualTo(JUtils.isParametrizedMethod(method.symbol()))
      .isEqualTo(method.methodBinding.isGenericMethod())
      .isTrue();
    assertThat(JUtils.isParametrizedMethod(cu.sema.methodSymbol(methodInvocation.methodBinding)))
      .isEqualTo(JUtils.isParametrizedMethod((Symbol.MethodSymbol) methodInvocation.symbol()))
      .isEqualTo(methodInvocation.methodBinding.isParameterizedMethod())
      .isTrue();
  }

  private JavaTree.CompilationUnitTreeImpl test(String source) {
    List<File> classpath = Collections.emptyList();
    JavaTree.CompilationUnitTreeImpl t = (JavaTree.CompilationUnitTreeImpl) JParser.parse("12", "File.java", source, true, classpath);
    SemanticModel.createFor(t, new SquidClassLoader(classpath));
    return t;
  }

}
