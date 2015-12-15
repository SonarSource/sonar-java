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
package org.sonar.java.bytecode;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmClassProvider;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.java.bytecode.visitor.DefaultBytecodeContext;

import java.io.InterruptedIOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BytecodeScannerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private AsmClassProvider asmProvider;
  private AsmClass asmClass;
  private BytecodeScanner bytecodeScanner;

  @Before
  public void setUp() throws Exception {
    asmProvider = mock(AsmClassProvider.class);
    asmClass = mock(AsmClass.class);
    when(asmProvider.getClass(anyString(), any(AsmClassProvider.DETAIL_LEVEL.class))).thenReturn(asmClass);
    bytecodeScanner = new BytecodeScanner(new DefaultBytecodeContext(null));
  }


  @Test
  public void rethrow_exception_when_error_during_analysis() {
    String className = "com.pack.MyClass";
    bytecodeScanner.accept(new CheckThrowingException(new NullPointerException()));
    thrown.expectMessage("Unable to analyze .class file com.pack.MyClass");
    bytecodeScanner.scanClasses(Lists.newArrayList(className), asmProvider);
  }

  @Test
  public void analysis_cancelled_on_InterruptedIOException() {
    String className = "com.pack.MyClass";
    bytecodeScanner.accept(new CheckThrowingException(new RuntimeException("", new InterruptedIOException())));
    thrown.expectMessage("Analysis cancelled");
    bytecodeScanner.scanClasses(Lists.newArrayList(className), asmProvider);
  }

  @Test
  public void analysis_cancelled_on_InterruptedException() {
    String className = "com.pack.MyClass";
    bytecodeScanner.accept(new CheckThrowingException(new RuntimeException("", new InterruptedException())));
    thrown.expectMessage("Analysis cancelled");
    bytecodeScanner.scanClasses(Lists.newArrayList(className), asmProvider);
  }

  private static class CheckThrowingException extends BytecodeVisitor {
    private final RuntimeException e;

    public CheckThrowingException(RuntimeException e) {
      this.e = e;
    }

    @Override
    public void visitClass(AsmClass asmClass) {
      throw e;
    }
  }
}
