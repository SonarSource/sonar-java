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
import org.sonar.java.resolve.SemanticModel;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class JMethodSymbolTest {

  @Test
  void test() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C m(C p) throws Exception { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    JMethodSymbol symbol = cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding));
    assertThat(symbol.parameterTypes())
      .containsOnly(cu.sema.type(Objects.requireNonNull(c.typeBinding)));
    assertThat(symbol.returnType())
      .isSameAs(cu.sema.typeSymbol(Objects.requireNonNull(c.typeBinding)));
    assertThat(symbol.thrownTypes())
      .containsOnly(cu.sema.type(Objects.requireNonNull(cu.sema.resolveType("java.lang.Exception"))));
  }

  private JavaTree.CompilationUnitTreeImpl test(String source) {
    List<File> classpath = Collections.emptyList();
    JavaTree.CompilationUnitTreeImpl t = (JavaTree.CompilationUnitTreeImpl) JParser.parse(
      "12",
      "File.java",
      source,
      true,
      classpath
    );
    SemanticModel.createFor(t, new SquidClassLoader(classpath));
    return t;
  }

}
