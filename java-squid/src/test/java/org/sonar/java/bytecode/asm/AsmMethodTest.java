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

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.bytecode.ClassLoaderBuilder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AsmMethodTest {

  private static AsmClass javaBean;
  private final AsmClass stringClass = new AsmClass("java/lang/String");
  private final AsmClass numberClass = new AsmClass("java/lang/Number");

  @BeforeClass
  public static void init() {
    AsmClassProvider asmClassProvider = new AsmClassProviderImpl(ClassLoaderBuilder.create(new File("src/test/files/bytecode/bin/")));
    javaBean = asmClassProvider.getClass("properties/JavaBean");
  }

  @Test
  public void add_and_get_throws() {
    AsmMethod method = new AsmMethod(new AsmClass("java/lang/String"), "toString()Ljava/lang/String;");
    assertThat(method.getThrows()).isEmpty();
    AsmClass class1 = mock(AsmClass.class);
    AsmClass class2 = mock(AsmClass.class);
    AsmClass class3 = mock(AsmClass.class);
    method.addThrowsOfClasses(new AsmClass[] {class1, class2});
    method.addUsesOfClasses(new AsmClass[] {class3});
    assertThat(method.getThrows()).containsExactly(class1, class2);
  }

  @Test
  public void testAsmMethod() {
    AsmMethod method = new AsmMethod(new AsmClass("java/lang/String"), "toString()Ljava/lang/String;");
    assertThat(method.getName()).isEqualTo("toString");
  }

  @Test
  public void testEquals() {
    assertThat(new AsmMethod(stringClass, "firstMethod()V")).isEqualTo(new AsmMethod(stringClass, "firstMethod()V"));
    assertThat(new AsmMethod(stringClass, "firstMethod()V")).isNotEqualTo(new AsmMethod(stringClass, "secondMethod()V"));
    assertThat(new AsmMethod(stringClass, "firstMethod()V")).isNotEqualTo(new AsmMethod(numberClass, "firstMethod()V"));
  }

  @Test
  public void testHashCode() {
    assertThat(new AsmMethod(stringClass, "firstMethod()V").hashCode()).isEqualTo(new AsmMethod(stringClass, "firstMethod()V").hashCode());
    assertThat(new AsmMethod(stringClass, "firstMethod()V").hashCode()).isNotEqualTo(new AsmMethod(stringClass, "secondMethod()V").hashCode());
    assertThat(new AsmMethod(stringClass, "firstMethod()V").hashCode()).isNotEqualTo(new AsmMethod(numberClass, "firstMethod()V").hashCode());
  }

  @Test
  public void testIsAccessor() {
    assertThat(javaBean.getMethod("getName()Ljava/lang/String;").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("getNameIndirect()Ljava/lang/String;").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("getNameOrEmpty()Ljava/lang/String;").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("setName(Ljava/lang/String;)V").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("setFrench(Z)V").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("isFrench()Z").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("anotherMethod()V").isAccessor()).isFalse();
    assertThat(javaBean.getMethod("addFirstName(Ljava/lang/String;)V").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("getNameOrDefault()Ljava/lang/String;").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("accessorWithABunchOfCalls()V").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("accessNameAndDumpStuffSoNotAccessor()V").isAccessor()).isFalse();
    assertThat(javaBean.getMethod("iShouldBeAStaticSetter()V").isAccessor()).isFalse();
    assertThat(javaBean.getMethod("getFirstName()Ljava/lang/String;").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("getFirstNameAndOneArgument(Ljava/lang/String;)Ljava/lang/String;").isAccessor()).isTrue();
    assertThat(javaBean.getMethod("recursiveAbs(I)I").isAccessor()).isFalse();
    assertThat(javaBean.getMethod("recursiveAbsNotAccessor(I)I").isAccessor()).isFalse();
    assertThat(javaBean.getMethod("recursiveAbsSameIncrementA(I)I").isAccessor()).isFalse();
    assertThat(javaBean.getMethod("recursiveAbsDifferentIncrementA(I)I").isAccessor()).isFalse();
  }

  @Test
  public void testGetAccessedField() {
    assertThat(javaBean.getMethod("getName()Ljava/lang/String;").getAccessedField().getName()).isEqualTo("name");
    assertThat(javaBean.getMethod("getNameIndirect()Ljava/lang/String;").getAccessedField().getName()).isEqualTo("name");
    assertThat(javaBean.getMethod("getNameOrEmpty()Ljava/lang/String;").getAccessedField().getName()).isEqualTo("name");
    assertThat(javaBean.getMethod("setName(Ljava/lang/String;)V").getAccessedField().getName()).isEqualTo("name");
    assertThat(javaBean.getMethod("setFrench(Z)V").getAccessedField().getName()).isEqualTo("french");
    assertThat(javaBean.getMethod("isFrench()Z").getAccessedField().getName()).isEqualTo("french");
    assertThat(javaBean.getMethod("anotherMethod()V").getAccessedField()).isNull();
    assertThat(javaBean.getMethod("addFirstName(Ljava/lang/String;)V").getAccessedField().getName()).isEqualTo("firstNames");
    assertThat(javaBean.getMethod("getNameOrDefault()Ljava/lang/String;").getAccessedField().getName()).isEqualTo("name");
    assertThat(javaBean.getMethod("accessorWithABunchOfCalls()V").getAccessedField().getName()).isEqualTo("firstNames");
    assertThat(javaBean.getMethod("iShouldBeAStaticSetter()V").getAccessedField()).isNull();
    assertThat(javaBean.getMethod("getFirstName()Ljava/lang/String;").getAccessedField().getName()).isEqualTo("FirstName");
    assertThat(javaBean.getMethod("getFirstNameAndOneArgument(Ljava/lang/String;)Ljava/lang/String;").getAccessedField().getName()).isEqualTo("FirstName");
    assertThat(javaBean.getMethod("recursiveAbs(I)I").getAccessedField()).isNull();
    assertThat(javaBean.getMethod("recursiveAbsNotAccessor(I)I").getAccessedField()).isNull();
  }

}
