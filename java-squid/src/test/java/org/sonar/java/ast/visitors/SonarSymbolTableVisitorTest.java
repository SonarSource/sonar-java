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
package org.sonar.java.ast.visitors;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.SonarComponents;
import org.sonar.java.model.VisitorsBridge;

import java.io.File;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SonarSymbolTableVisitorTest {

  private final SonarComponents sonarComponents = mock(SonarComponents.class);
  private final Symbolizable symbolizable = mock(Symbolizable.class);
  private final Symbolizable.SymbolTableBuilder symboltableBuilder = mock(Symbolizable.SymbolTableBuilder.class);

  private static final String EOL = "\n";
  private List<String> lines;

  @Before
  public void init() {
    when(sonarComponents.symbolizableFor(any(File.class))).thenReturn(symbolizable);
    when(symbolizable.newSymbolTableBuilder()).thenReturn(symboltableBuilder);
  }

  @Test
  public void sonar_symbol_table() throws Exception {
    File file = new File("src/test/files/highlighter/SonarSymTable.java");
    lines = Files.readLines(file, Charsets.UTF_8);
    JavaAstScanner.scanSingleFile(file, new VisitorsBridge(ImmutableList.of(), sonarComponents.getJavaClasspath(), sonarComponents));

    // import List
    verify(symboltableBuilder).newSymbol(offset(1, 18), offset(1, 22));
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(offset(5, 3)));
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(offset(6, 11)));
    // Example class declaration
    verify(symboltableBuilder).newSymbol(offset(4, 7), offset(4, 14));
    verify(symboltableBuilder).newSymbol(offset(4, 15), offset(4, 16));
    // list field
    verify(symboltableBuilder).newSymbol(offset(5, 16), offset(5, 20));
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(offset(7, 10)));
    // Example constructor
    verify(symboltableBuilder).newSymbol(offset(6, 3), offset(6, 10));
    // list local var
    verify(symboltableBuilder).newSymbol(offset(6, 24), offset(6, 28));
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(offset(7, 17)));
    // method
    verify(symboltableBuilder).newSymbol(offset(9, 7), offset(9, 13));
    //label
    verify(symboltableBuilder).newSymbol(offset(10, 5), offset(10, 10));
    //Enum
    verify(symboltableBuilder).newSymbol(offset(13, 8), offset(13, 26));
    verify(symboltableBuilder).newSymbol(offset(14, 5), offset(14, 12));
    //Do not reference constructor of enum as it can leads to failure in analysis as long as SONAR-5894 is not fixed
    //verify(symboltableBuilder).newReference(any(Symbol.class), eq(offset(14, 5)));
    verify(symboltableBuilder).newSymbol(offset(15, 5), offset(15, 23));
    verify(symboltableBuilder).build();
    verifyNoMoreInteractions(symboltableBuilder);
  }

  private int offset(int line, int column) {
    int result = 0;
    for (int i = 0; i < line - 1; i++) {
      result += lines.get(i).length() + EOL.length();
    }
    result += column - 1;
    return result;
  }

}
