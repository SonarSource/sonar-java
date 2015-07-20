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
package org.sonar.java.bytecode.asm;

import org.junit.Test;
import org.sonar.java.bytecode.ClassLoaderBuilder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class AsmMethodVisitorTest {

  private AsmClassProvider asmClassProvider = new AsmClassProviderImpl(ClassLoaderBuilder.create(new File("src/test/files/bytecode/bin/")));

  @Test
  public void testVisitFieldInsn() {
    AsmClass tagNameClass = asmClassProvider.getClass("tags/TagName");
    AsmField nameField = tagNameClass.getField("name");
    AsmMethod constructorWithString = tagNameClass.getMethod("<init>(Ljava/lang/String;)V");
    assertThat(constructorWithString.getCallsToField()).containsOnly(nameField);
  }

  @Test
  public void testVisitMethodInsn() {
    AsmClass sourceFileClass = asmClassProvider.getClass("tags/SourceFile");
    AsmMethod readMethod = sourceFileClass.getMethod("read()V");
    AsmMethod readSourceFileMethod = sourceFileClass.getMethod("readSourceFile()V");
    assertThat(readSourceFileMethod.getCallsToMethod()).contains(readMethod).hasSize(2);
    assertThat(readSourceFileMethod.getCallsToField()).hasSize(1);
  }

  @Test
  public void testVisitTryCatchBlock() {
    AsmClass sourceFileClass = asmClassProvider.getClass("tags/SourceFile");
    AsmClass tagExceptionClass = asmClassProvider.getClass("tags/TagException");
    AsmMethod readSourceFileMethod = sourceFileClass.getMethod("readSourceFile()V");
    assertThat(readSourceFileMethod.getDistinctUsedAsmClasses()).contains(tagExceptionClass);
  }

  @Test
  public void testVisitTypeInsn() {
    AsmClass sourceFileClass = asmClassProvider.getClass("tags/SourceFile");
    AsmMethod constructor = sourceFileClass.getMethod("<init>()V");
    assertThat(constructor.getDistinctUsedAsmClasses().contains(asmClassProvider.getClass("java/util/ArrayList"))).isNotNull();
  }

  @Test
  public void testEmptyMethodProperty() {
    AsmClass asmClass = asmClassProvider.getClass("properties/EmptyMethodProperty");
    assertThat(asmClass.getMethod("notEmptyMethod()V").isEmpty()).isFalse();
    assertThat(asmClass.getMethod("emptyMethod()V").isEmpty()).isTrue();
    assertThat(asmClass.getMethod("emptyAbstractMethod()V").isEmpty()).isTrue();
  }

}
