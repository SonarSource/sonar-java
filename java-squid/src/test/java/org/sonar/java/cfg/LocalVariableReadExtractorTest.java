/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.cfg;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class LocalVariableReadExtractorTest {

  public static final ActionParser<Tree> PARSER = JavaParser.createParser(Charsets.UTF_8);


  private static MethodTree buildMethodTree(String methodCode) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse("class A { int field; " + methodCode + " }");
    SemanticModel.createFor(cut, Collections.<File>emptyList());
    return (MethodTree) ((ClassTree) cut.types().get(0)).members().get(1);
  }

  @Test
  public void should_extract_local_vars_read() {
    MethodTree methodTree = buildMethodTree("void foo(boolean a) { new Object() { void foo() { System.out.println(a);} };  }");
    StatementTree statementTree = methodTree.block().body().get(0);
    LocalVariableReadExtractor extractor = new LocalVariableReadExtractor(methodTree.symbol());
    statementTree.accept(extractor);
    assertThat(extractor.usedVariables()).hasSize(1);
  }

  @Test
  public void should_not_extract_local_vars_written() throws Exception {
    MethodTree methodTree = buildMethodTree("void foo(boolean a) { new Object() { void foo() { new A().field = 0; a = false;} };  }");
    StatementTree statementTree = methodTree.block().body().get(0);
    LocalVariableReadExtractor extractor = new LocalVariableReadExtractor(methodTree.symbol());
    statementTree.accept(extractor);
    assertThat(extractor.usedVariables()).isEmpty();
    methodTree = buildMethodTree("void foo(boolean a) { new Object() { void foo() { a = !a;} };  }");
    statementTree = methodTree.block().body().get(0);
    extractor = new LocalVariableReadExtractor(methodTree.symbol());
    statementTree.accept(extractor);
    assertThat(extractor.usedVariables()).hasSize(1);
  }

}