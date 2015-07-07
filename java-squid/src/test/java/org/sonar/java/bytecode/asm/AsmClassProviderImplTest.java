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
import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;

import java.io.File;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class AsmClassProviderImplTest {

  private AsmClassProviderImpl asmClassProviderImpl = new AsmClassProviderImpl();

  @Test
  public void testReadClass() {
    AsmClass asmClass = asmClassProviderImpl.getClass("java/lang/Object");
    assertThat(asmClass).isNotNull();
    assertThat(asmClass.getInternalName()).isEqualTo("java/lang/Object");
    assertThat(asmClassProviderImpl.getClass("bytecode/bin/tags/File")).isNotNull();
  }

  @Test
  public void testCacheMechanism() {
    AsmClass asmClass = asmClassProviderImpl.getClass("java/lang/Object");
    AsmClass asmClass2 = asmClassProviderImpl.getClass("java/lang/Object");
    assertThat(asmClass).isEqualTo(asmClass2);
  }

  @Test
  public void testGetUnknownClass() {
    AsmClass unknownClass = asmClassProviderImpl.getClass("java/lang/UnknownClass");
    assertThat(unknownClass).isNotNull();
    assertThat(unknownClass.getSuperClass()).isNull();
  }

  @Test
  public void testloadSuperClass() {
    AsmClass doubleClass = asmClassProviderImpl.getClass("java/lang/Double");
    assertThat(doubleClass.getSuperClass().getInternalName()).isEqualTo("java/lang/Number");
    assertThat(doubleClass.getSuperClass().getSuperClass().getInternalName()).isEqualTo("java/lang/Object");
    assertThat(doubleClass.getSuperClass().getSuperClass().getSuperClass()).isNull();
  }

  @Test
  public void testloadInterfaces() {
    AsmClass characterClass = asmClassProviderImpl.getClass("java/lang/Character");
    assertThat(characterClass.getInterfaces().size()).isEqualTo(2);
  }

  @Test
  public void getSeveralTimesTheSameClassButWithHigherDetailLevel() {
    AsmClass integerClass = asmClassProviderImpl.getClass("java/lang/Integer", DETAIL_LEVEL.NOTHING);
    assertThat(integerClass.getInternalName()).isEqualTo("java/lang/Integer");
    assertThat(integerClass.getDetailLevel()).isEqualTo(DETAIL_LEVEL.NOTHING);
    assertThat(integerClass.getMethods().size()).isEqualTo(0);
    assertThat(integerClass.getSuperClass()).isNull();

    AsmClass integerClassWithHigherDetailLevel = asmClassProviderImpl.getClass("java/lang/Integer", DETAIL_LEVEL.STRUCTURE_AND_CALLS);
    assertThat(integerClassWithHigherDetailLevel.getDetailLevel()).isEqualTo(DETAIL_LEVEL.STRUCTURE_AND_CALLS);
    assertThat(integerClassWithHigherDetailLevel).isSameAs(integerClass);
    assertThat(integerClass.getSuperClass().getInternalName()).isEqualTo("java/lang/Number");
  }

  @Test
  public void should_not_duplicate_usages_and_throws_after_reload_with_higher_detail_level() {
    AsmClass integerClass = asmClassProviderImpl.getClass("java/lang/Integer", DETAIL_LEVEL.STRUCTURE);
    AsmMethod parseIntMethod = getParseIntMethod(integerClass.getMethods());
    assertThat(parseIntMethod.getThrows()).hasSize(1);

    integerClass = asmClassProviderImpl.getClass("java/lang/Integer", DETAIL_LEVEL.STRUCTURE_AND_CALLS);
    parseIntMethod = getParseIntMethod(integerClass.getMethods());
    assertThat(parseIntMethod.getThrows()).hasSize(1);
  }

  private static AsmMethod getParseIntMethod(Collection<AsmMethod> methods) {
    for (AsmMethod method : methods) {
      if ("parseInt(Ljava/lang/String;)I".equals(method.getKey())) {
        return method;
      }
    }
    throw new IllegalArgumentException();
  }

  @Test
  public void testPersonalClassLoader() {
    asmClassProviderImpl = new AsmClassProviderImpl(ClassLoaderBuilder.create(new File("src/test/files/bytecode/bin/")));
    assertThat(asmClassProviderImpl.getClass("tags/Line", DETAIL_LEVEL.STRUCTURE_AND_CALLS).getDetailLevel()).isEqualTo(DETAIL_LEVEL.STRUCTURE_AND_CALLS);
  }

}
