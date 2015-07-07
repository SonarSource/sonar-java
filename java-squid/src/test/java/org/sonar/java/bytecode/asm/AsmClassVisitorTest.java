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

public class AsmClassVisitorTest {

  private static AsmClassProvider asmClassProvider = new AsmClassProviderImpl(ClassLoaderBuilder.create(new File("src/test/files/bytecode/bin/")));

  @Test
  public void testVisit() {
    AsmClass asmClass = asmClassProvider.getClass("java/lang/String");
    assertThat(asmClass.getInternalName()).isEqualTo("java/lang/String");
    assertThat(asmClass.isAbstract()).isFalse();
    assertThat(asmClass.isInterface()).isFalse();
  }

  @Test
  public void testVisitMethod() {
    AsmClass asmClass = asmClassProvider.getClass("java/lang/String");
    assertThat(asmClass.getMethod("charAt(I)C")).isNotNull();
    assertThat(asmClass.getMethod("charAt(I)C").isPublic()).isTrue();
    assertThat(asmClass.getMethod("charAt(I)C").isDeprecated()).isFalse();
    assertThat(asmClass.getMethod("charAt(I)C").isStatic()).isFalse();
    assertThat(asmClass.getMethod("valueOf(C)Ljava/lang/String;").isStatic()).isTrue();

    asmClass = asmClassProvider.getClass("tags/File");
    AsmMethod getLines = asmClass.getMethod("read(Ljava/util/Collection;Ljava/lang/String;)Ljava/lang/String;");
    assertThat(getLines.getDistinctUsedAsmClasses().contains(new AsmClass("java/util/Collection"))).isTrue();
    assertThat(getLines.getDistinctUsedAsmClasses().contains(new AsmClass("tags/File"))).isTrue();
    assertThat(getLines.getDistinctUsedAsmClasses().contains(new AsmClass("java/lang/String"))).isTrue();
    assertThat(getLines.getDistinctUsedAsmClasses().contains(new AsmClass("java/lang/RuntimeException"))).isTrue();
  }

  @Test
  public void testVisitMehtodAccessFlags() {
    AsmClass fileClass = asmClassProvider.getClass("tags/File");
    assertThat(fileClass.getMethod("read()V").isPublic()).isTrue();
    AsmClass stringClass = asmClassProvider.getClass("java/lang/String");
    assertThat(stringClass.getMethod("toString()Ljava/lang/String;").isDeprecated()).isFalse();
  }

  @Test
  public void testVisitMehtodThrows() {
    AsmClass fileClass = asmClassProvider.getClass("tags/File");
    assertThat(fileClass.getMethod("read()V").getThrows()).hasSize(1);
    assertThat(fileClass.getMethod("read()V").getThrows().get(0).getInternalName()).isEqualTo("tags/TagException");
    AsmClass stringClass = asmClassProvider.getClass("java/lang/String");
    assertThat(stringClass.getMethod("toString()Ljava/lang/String;").getThrows()).isEmpty();
  }

  @Test
  public void testInheritedMethodProperty() {
    AsmClass asmClass = asmClassProvider.getClass("properties/InheritedMethodsProperty");
    assertThat(asmClass.getMethod("equals(Ljava/lang/Object;)Z").isInherited()).isTrue();
    assertThat(asmClass.getMethod("notInheritedMethod()V").isInherited()).isFalse();
    assertThat(asmClass.getMethod("run()V").isInherited()).isTrue();
  }

  @Test
  public void testMethodBodyLoadedProperty() {
    AsmClass asmClass = asmClassProvider.getClass("properties/MethodBodyLoadedProperty");
    assertThat(asmClass.getMethod("doJob()V").isBodyLoaded()).isTrue();
    assertThat(asmClass.getMethod("run()V").isBodyLoaded()).isFalse();
  }

  @Test
  public void testResourceTouchedProperty() {
    AsmClass asmClass = asmClassProvider.getClass("properties/ResourceTouchedProperty");
    assertThat(asmClass.getField("unusedField").isUsed()).isFalse();
    assertThat(asmClass.getField("usedField").isUsed()).isTrue();
    assertThat(asmClass.getMethod("doPrivateJob()V").isUsed()).isTrue();
    assertThat(asmClass.getMethod("run()V").isUsed()).isFalse();
    assertThat(asmClass.isUsed()).isFalse();
    assertThat(asmClassProvider.getClass("java/lang/Runnable").isUsed()).isTrue();
  }

  @Test
  public void testVisitFieldAccessFlags() {
    AsmClass asmClass = asmClassProvider.getClass("java/lang/String");
    assertThat(asmClass.getField("CASE_INSENSITIVE_ORDER")).isNotNull();
    assertThat(asmClass.getField("CASE_INSENSITIVE_ORDER").isStatic()).isTrue();
    assertThat(asmClass.getField("CASE_INSENSITIVE_ORDER").isPublic()).isTrue();
    assertThat(asmClass.getField("CASE_INSENSITIVE_ORDER").isFinal()).isTrue();
    assertThat(asmClass.getField("CASE_INSENSITIVE_ORDER").isDeprecated()).isFalse();
  }

  @Test
  public void testVisitObjectField() {
    AsmClass asmClass = asmClassProvider.getClass("tags/SourceFile");
    AsmEdge pathAsmEdge = asmClass.getField("path").getOutgoingEdges().iterator().next();
    assertThat(pathAsmEdge.getTargetAsmClass().getInternalName()).isEqualTo("java/lang/String");
  }

}
