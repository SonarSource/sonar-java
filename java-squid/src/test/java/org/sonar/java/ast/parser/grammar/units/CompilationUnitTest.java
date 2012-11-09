/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.ast.parser.grammar.units;

import com.google.common.base.Joiner;
import com.sonar.sslr.impl.Parser;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.parser.JavaParser;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class CompilationUnitTest {

  Parser<JavaGrammar> p = JavaParser.create();
  JavaGrammar g = p.getGrammar();

  @Before
  public void init() {
    p.setRootRule(g.compilationUnit);
  }

  @Test
  public void ok() {
    g.packageDeclaration.mock();
    g.importDeclaration.mock();
    g.typeDeclaration.mock();

    assertThat(p)
        .matches("packageDeclaration importDeclaration importDeclaration typeDeclaration typeDeclaration")
        .matches("packageDeclaration importDeclaration typeDeclaration")
        .matches("packageDeclaration importDeclaration")
        .matches("packageDeclaration")
        .matches("");
  }

  @Test
  public void realLife() {
    assertThat(p)
        .matches(lines(
            "package org.example;",
            "",
            "public class HelloWorld {",
            "  public static void main(String[] args) {",
            "    System.out.println(\"Hello World!\");",
            "  }",
            "}"));
  }

  private static String lines(String... lines) {
    return Joiner.on("\n").join(lines);
  }

}
