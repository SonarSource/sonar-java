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
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.resolve.SemanticModel;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JSymbolTest {

  @Test
  void owner() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C1 { int f; class C2 { } void m(int p) { class C3 { } } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c1.members().get(0);
    ClassTreeImpl c2 = (ClassTreeImpl) c1.members().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c1.members().get(2);
    VariableTreeImpl p = (VariableTreeImpl) m.parameters().get(0);
    ClassTreeImpl c3 = (ClassTreeImpl) m.block().body().get(0);

    assertThat(cu.sema.typeSymbol(c1.typeBinding).owner().isPackageSymbol())
      .as("of top-level class")
      .isTrue();

    assertThat(cu.sema.typeSymbol(c2.typeBinding).owner())
      .as("of nested class")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c3.typeBinding).owner())
      .as("of local class")
      .isSameAs(cu.sema.methodSymbol(m.methodBinding));

    assertThat(cu.sema.methodSymbol(m.methodBinding).owner())
      .as("of method")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(f.variableBinding).owner())
      .as("of field")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(p.variableBinding).owner())
      .as("of method parameter")
      .isSameAs(cu.sema.methodSymbol(m.methodBinding));
  }

  @Test
  void enclosingClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C1 { int f; class C2 { } void m(int p) { class C3 { } } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c1.members().get(0);
    ClassTreeImpl c2 = (ClassTreeImpl) c1.members().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c1.members().get(2);
    VariableTreeImpl p = (VariableTreeImpl) m.parameters().get(0);
    ClassTreeImpl c3 = (ClassTreeImpl) m.block().body().get(0);

    assertThat(cu.sema.typeSymbol(c1.typeBinding).enclosingClass())
      .as("of top-level class")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c2.typeBinding).enclosingClass())
      .as("of nested class")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c3.typeBinding).enclosingClass())
      .as("of local class")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.methodSymbol(m.methodBinding).enclosingClass())
      .as("of method")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(f.variableBinding).enclosingClass())
      .as("of field")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(p.variableBinding).enclosingClass())
      .as("of method parameter")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));
  }

  JavaTree.CompilationUnitTreeImpl test(String source) {
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
