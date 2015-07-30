/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.declaration;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class ClassTreeImplTest {
  private final ActionParser p = JavaParser.createParser(Charsets.UTF_8);

  private CompilationUnitTree createTree(String code) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse(code);
    SemanticModel.createFor(compilationUnitTree, Lists.<File>newArrayList());
    return compilationUnitTree;
  }

  @Test
  public void getLine() {
    CompilationUnitTree tree = createTree("class A {\n" +
        "A a = new A() {};" +
        "\n}");
    ClassTree classTree = (ClassTree) tree.types().get(0);
    assertThat(((JavaTree) classTree).getLine()).isEqualTo(1);
    //get line of anonymous class
    NewClassTree newClassTree = (NewClassTree) ((VariableTree) classTree.members().get(0)).initializer();
    assertThat(((JavaTree)newClassTree.classBody()).getLine()).isEqualTo(2);
  }
}