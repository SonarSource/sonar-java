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
package org.sonar.java.resolve;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class SemanticModelTest {

  private static final ActionParser<Tree> PARSER = JavaParser.createParser(Charsets.UTF_8);

  @Test
  public void parent_link_should_be_computed() {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse("class A { int field; void foo() {} }");
    SemanticModel semanticModel = SemanticModel.createFor(cut, Collections.<File>emptyList());
    ClassTree classTree = (ClassTree) cut.types().get(0);
    VariableTree field = (VariableTree) classTree.members().get(0);
    MethodTree method = (MethodTree) classTree.members().get(1);

    assertThat(semanticModel.getParent(method)).isSameAs(classTree);
    assertThat(semanticModel.getParent(field)).isSameAs(classTree);
    assertThat(semanticModel.getParent(classTree)).isSameAs(cut);

  }
}