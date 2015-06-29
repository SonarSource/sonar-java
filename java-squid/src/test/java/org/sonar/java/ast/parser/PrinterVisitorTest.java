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
package org.sonar.java.ast.parser;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.fest.assertions.Assertions.assertThat;

public class PrinterVisitorTest {

  @Test
  public void testName() throws Exception {
    final ActionParser p = JavaParser.createParser(Charsets.UTF_8);
    CompilationUnitTree cut = (CompilationUnitTree) p.parse("class A { void foo(){}}");
    String expectedOutput = "CompilationUnitTree 1 : [\n" +
        "  ClassTree 1\n" +
        "    ModifiersTree\n" +
        "    TypeParameters\n"+
        "    ListTree : [\n"+
        "    MethodTree 1\n" +
        "      ModifiersTree\n" +
        "      TypeParameters\n"+
        "      PrimitiveTypeTree 1\n" +
        "      ListTree\n"+
        "      BlockTree 1\n" +
        "    ]\n" +
        "  ]\n";
    assertThat(PrinterVisitor.print(cut)).isEqualTo(expectedOutput);
  }


}