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
package org.sonar.java.bytecode;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmClassProvider;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.indexer.SquidIndex;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BytecodeScannerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();


  @Test
  public void rethrow_exception_when_error_during_analysis() throws Exception {
    String className = "com.pack.MyClass";
    JavaResourceLocator javaResourceLocator = null;
    SquidIndex squidIndex = null;
    AsmClassProvider asmProvider = mock(AsmClassProvider.class);
    AsmClass asmClass = mock(AsmClass.class);
    when(asmProvider.getClass(anyString(), any(AsmClassProvider.DETAIL_LEVEL.class))).thenReturn(asmClass);
    BytecodeScanner bytecodeScanner = new BytecodeScanner(squidIndex, javaResourceLocator);
    bytecodeScanner.accept(new Visitor());
    thrown.expectMessage("Unable to analyze .class file com.pack.MyClass");
    bytecodeScanner.scanClasses(Lists.newArrayList(className), asmProvider);

  }

  private static class Visitor extends BytecodeVisitor {
    @Override
    public void visitClass(AsmClass asmClass) {
      throw new NullPointerException();
    }
  }
}