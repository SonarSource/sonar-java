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
package org.sonar.java.se;

import com.google.common.collect.Lists;
import org.apache.commons.io.Charsets;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.parser.sslr.ActionParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class ExplodedGraphWalkerTest {

  @Test
  public void test() throws Exception {
    ActionParser parser = JavaParser.createParser(Charsets.UTF_8);
    CompilationUnitTree cut = (CompilationUnitTree) parser.parse("class A  { Object a; void func() { if(a==null) a.toString(); } } ");
    ExplodedGraphWalker graphWalker = new ExplodedGraphWalker(System.out);
    cut.accept(graphWalker);
    assertThat(graphWalker.steps).isEqualTo(8);
  }

  @Test
  public void local_variable() throws Exception {
    ActionParser parser = JavaParser.createParser(Charsets.UTF_8);
    CompilationUnitTree cut = (CompilationUnitTree) parser.parse("class A  { \nvoid func() {\n Object a; a.toString();\n }\n } ");
    SemanticModel.createFor(cut, Lists.<File>newArrayList());
    ExplodedGraphWalker graphWalker = new ExplodedGraphWalker(System.out);
    cut.accept(graphWalker);
    //Only two steps as we sink into the second because of the NPE.
    assertThat(graphWalker.steps).isEqualTo(2);
  }

}