/*
 * SonarQube Java
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
package org.sonar.java.ast.visitors;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.SonarComponents;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import java.io.File;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SonarSymbolTableVisitorTest {

  private SonarComponents sonarComponents = mock(SonarComponents.class);
  private Symbolizable symbolizable = mock(Symbolizable.class);
  private Symbolizable.SymbolTableBuilder symboltableBuilder = mock(Symbolizable.SymbolTableBuilder.class);

  @Test
  public void sonar_symbol_table() throws Exception {
    File file = new File("src/test/files/highlighter/SonarSymTable.java");
    when(sonarComponents.symbolizableFor(any(File.class))).thenReturn(symbolizable);
    when(symbolizable.newSymbolTableBuilder()).thenReturn(symboltableBuilder);
    JavaAstScanner.scanSingleFile(file, new VisitorsBridge(Lists.newArrayList(new JavaFileScanner() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
      }
    }), sonarComponents));
    //import List
    verify(symboltableBuilder).newSymbol(17, 21);
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(42));
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(71));
    //Example class declaration
    verify(symboltableBuilder).newSymbol(30, 38);
    //list field
    verify(symboltableBuilder).newSymbol(55, 59);
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(101));
    //Example constructor
    verify(symboltableBuilder).newSymbol(63, 70);
    //list local var
    verify(symboltableBuilder).newSymbol(84, 88);
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(108));
    //method
    verify(symboltableBuilder).newSymbol(124, 130);
    verify(symboltableBuilder).build();
    verifyNoMoreInteractions(symboltableBuilder);
  }

  @Test
  public void sonar_symbol_table_on_demand() throws Exception {
    File file = new File("src/test/files/highlighter/SonarSymTableOnDemand.java");
    when(sonarComponents.symbolizableFor(any(File.class))).thenReturn(symbolizable);
    when(symbolizable.newSymbolTableBuilder()).thenReturn(symboltableBuilder);
    JavaAstScanner.scanSingleFile(file, new VisitorsBridge(Lists.newArrayList(new JavaFileScanner() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
      }
    }), sonarComponents));
    //Example class declaration
    verify(symboltableBuilder).newSymbol(30, 38);
    //list field
    verify(symboltableBuilder).newSymbol(55, 59);
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(101));
    //Example constructor
    verify(symboltableBuilder).newSymbol(63, 70);
    //list local var
    verify(symboltableBuilder).newSymbol(84, 88);
    verify(symboltableBuilder).newReference(any(Symbol.class), eq(108));
    //method
    verify(symboltableBuilder).newSymbol(124, 130);
    verify(symboltableBuilder).build();
    verifyNoMoreInteractions(symboltableBuilder);
  }
}
