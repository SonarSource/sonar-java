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
package org.sonar.java.model.expression;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import java.io.File;

public class MethodInvocationTreeImplTest {

  private final ActionParser p = JavaParser.createParser(Charsets.UTF_8);

  @Test
  public void symbol_should_be_set() {
    CompilationUnitTree cut = createTree("class A { void foo(){} void bar(){foo();} }");
    ClassTree classTree = (ClassTree) cut.types().get(0);
    Symbol.MethodSymbol declaration = ((MethodTree) classTree.members().get(0)).symbol();
    StatementTree statementTree = ((MethodTree) classTree.members().get(1)).block().body().get(0);
    MethodInvocationTree mit = (MethodInvocationTree) ((ExpressionStatementTree)statementTree).expression();
    Assertions.assertThat(mit.symbol()).isSameAs(declaration);
    Assertions.assertThat(mit.arguments()).isNotNull();
    Assertions.assertThat(mit.arguments().openParenToken()).isNotNull();
    Assertions.assertThat(mit.arguments().closeParenToken()).isNotNull();
  }

  private CompilationUnitTree createTree(String code) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse(code);
    SemanticModel.createFor(compilationUnitTree, Lists.<File>newArrayList());
    return compilationUnitTree;
  }
}