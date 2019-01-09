/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.java.resolve;

import org.assertj.core.api.Fail;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class JavaSymbolTest {
  private static final JavaSymbol.PackageJavaSymbol P_PACKAGE_JAVA_SYMBOL = new JavaSymbol.PackageJavaSymbol(null, null);

  @Test
  public void kinds() {
    assertThat(JavaSymbol.TYP).isLessThan(JavaSymbol.ERRONEOUS);
    assertThat(JavaSymbol.VAR).isLessThan(JavaSymbol.ERRONEOUS);
    assertThat(JavaSymbol.MTH).isLessThan(JavaSymbol.ERRONEOUS);
    assertThat(JavaSymbol.ERRONEOUS).isLessThan(JavaSymbol.ABSENT);
  }

  @Test
  public void completion_should_use_completer() {
    JavaSymbol symbol = new JavaSymbol(0, 0, null, null);
    JavaSymbol.Completer completer = mock(JavaSymbol.Completer.class);
    symbol.completer = completer;
    symbol.complete();
    verify(completer).complete(symbol);
    assertThat(symbol.completer).isNull();
  }

  @Test
  public void test_PackageSymbol() {
    JavaSymbol owner = mock(JavaSymbol.class);
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("name", owner);

    assertThat(packageSymbol.kind).isEqualTo(JavaSymbol.PCK);
    assertTrue(packageSymbol.isPackageSymbol());
    assertThat(packageSymbol.flags()).isEqualTo(0);
    assertThat(packageSymbol.owner()).isSameAs(owner);

    assertThat(packageSymbol.packge()).isSameAs(packageSymbol);
    assertThat(packageSymbol.outermostClass()).isNull();
    assertThat(packageSymbol.enclosingClass()).isNull();
  }

  @Test
  public void test_TypeSymbol() {
    JavaSymbol.TypeJavaSymbol outermostClass = new JavaSymbol.TypeJavaSymbol(42, "name", P_PACKAGE_JAVA_SYMBOL);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(42, "name", outermostClass);

    assertThat(typeSymbol.kind).isEqualTo(JavaSymbol.TYP);
    assertTrue(typeSymbol.isTypeSymbol());
    assertThat(typeSymbol.flags()).isEqualTo(42);
    assertThat(typeSymbol.owner()).isSameAs(outermostClass);

    assertThat(typeSymbol.packge()).isSameAs(P_PACKAGE_JAVA_SYMBOL);
    assertThat(typeSymbol.outermostClass()).isSameAs(outermostClass);
    assertThat(typeSymbol.enclosingClass()).isSameAs(typeSymbol);
  }

  @Test
  public void access_to_superclass_should_trigger_completion() {
    JavaSymbol.TypeJavaSymbol typeSymbol = spy(new JavaSymbol.TypeJavaSymbol(42, "name", P_PACKAGE_JAVA_SYMBOL));
    typeSymbol.getSuperclass();
    verify(typeSymbol).complete();
  }

  @Test
  public void access_to_interfaces_should_trigger_completion() {
    JavaSymbol.TypeJavaSymbol typeSymbol = spy(new JavaSymbol.TypeJavaSymbol(42, "name", P_PACKAGE_JAVA_SYMBOL));
    typeSymbol.getInterfaces();
    verify(typeSymbol).complete();
  }

  @Test
  public void access_to_members_should_trigger_completion() {
    JavaSymbol.TypeJavaSymbol typeSymbol = spy(new JavaSymbol.TypeJavaSymbol(42, "name", P_PACKAGE_JAVA_SYMBOL));
    typeSymbol.members();
    verify(typeSymbol).complete();
  }

  @Test
  public void test_MethodSymbol() {
    JavaSymbol.TypeJavaSymbol outermostClass = new JavaSymbol.TypeJavaSymbol(42, "name", P_PACKAGE_JAVA_SYMBOL);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(42, "t", outermostClass);
    JavaSymbol.MethodJavaSymbol methodSymbol = new JavaSymbol.MethodJavaSymbol(42, "name", typeSymbol);

    assertThat(methodSymbol.kind).isEqualTo(JavaSymbol.MTH);
    assertTrue(methodSymbol.isMethodSymbol());
    assertThat(methodSymbol.flags()).isEqualTo(42);
    assertThat(methodSymbol.owner()).isSameAs(typeSymbol);
    assertThat(methodSymbol.isConstructor()).isFalse();

    assertThat(methodSymbol.packge()).isSameAs(P_PACKAGE_JAVA_SYMBOL);
    assertThat(methodSymbol.outermostClass()).isSameAs(outermostClass);
    assertThat(methodSymbol.enclosingClass()).isSameAs(typeSymbol);

    assertThat(methodSymbol.toString()).isEqualTo("t#name()");
    assertThat(new JavaSymbol.MethodJavaSymbol(42, "name", Symbols.unknownType.symbol).toString()).isEqualTo("!unknownOwner!#name()");
    assertThat(Symbols.unknownMethodSymbol.toString()).isEqualTo("!unknownOwner!#!unknownMethod!()");

    JavaSymbol.MethodJavaSymbol constructor = new JavaSymbol.MethodJavaSymbol(42, "<init>", typeSymbol);
    assertThat(constructor.kind).isEqualTo(JavaSymbol.MTH);
    assertTrue(constructor.isMethodSymbol());
    assertThat(constructor.flags()).isEqualTo(42);
    assertThat(constructor.owner()).isSameAs(typeSymbol);
    assertThat(constructor.isConstructor()).isTrue();

    assertThat(constructor.packge()).isSameAs(P_PACKAGE_JAVA_SYMBOL);
    assertThat(constructor.outermostClass()).isSameAs(outermostClass);
    assertThat(constructor.enclosingClass()).isSameAs(typeSymbol);

    assertThat(constructor.toString()).isEqualTo("t#<init>()");
  }

  @Test
  public void test_VariableSymbol() {
    JavaSymbol.TypeJavaSymbol outermostClass = new JavaSymbol.TypeJavaSymbol(42, "name", P_PACKAGE_JAVA_SYMBOL);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(42, "t", outermostClass);
    JavaSymbol.MethodJavaSymbol methodSymbol = new JavaSymbol.MethodJavaSymbol(42, "name", typeSymbol);
    JavaSymbol.VariableJavaSymbol variableSymbol = new JavaSymbol.VariableJavaSymbol(42, "name", methodSymbol);

    assertThat(variableSymbol.kind).isEqualTo(JavaSymbol.VAR);
    assertTrue(variableSymbol.isVariableSymbol());
    assertThat(variableSymbol.flags()).isEqualTo(42);
    assertThat(variableSymbol.owner()).isSameAs(methodSymbol);

    assertThat(variableSymbol.packge()).isSameAs(P_PACKAGE_JAVA_SYMBOL);
    assertThat(variableSymbol.outermostClass()).isSameAs(outermostClass);
    assertThat(variableSymbol.enclosingClass()).isSameAs(typeSymbol);
  }

  @Test
  public void test_WildcardSymbol() {
    String name = "? extends String";
    JavaSymbol.WildcardSymbol wildcardSymbol = new JavaSymbol.WildcardSymbol(name);
    assertThat(wildcardSymbol.kind).isEqualTo(JavaSymbol.TYP);
    assertThat(wildcardSymbol.owner()).isSameAs(Symbols.unknownSymbol);
    assertThat(wildcardSymbol.declaration()).isNull();
    assertThat(wildcardSymbol.getSuperclass()).isNull();
    assertThat(wildcardSymbol.getInterfaces()).isEmpty();
    assertThat(wildcardSymbol.getFullyQualifiedName()).isEqualTo(name);
    try {
      wildcardSymbol.getInternalName();
      Fail.fail("should have failed");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  public void test_helper_methods() {
    JavaSymbol.TypeJavaSymbol outermostClass = new JavaSymbol.TypeJavaSymbol(Flags.INTERFACE, "name", P_PACKAGE_JAVA_SYMBOL);
    JavaSymbol.TypeJavaSymbol annotationType = new JavaSymbol.TypeJavaSymbol(Flags.INTERFACE | Flags.ANNOTATION, "myAnnotation", P_PACKAGE_JAVA_SYMBOL);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.INTERFACE, "t", outermostClass);
    JavaSymbol.MethodJavaSymbol methodSymbol = new JavaSymbol.MethodJavaSymbol(Flags.STATIC | Flags.ABSTRACT, "name", typeSymbol);
    JavaSymbol.MethodJavaSymbol defaultMethodSymbol = new JavaSymbol.MethodJavaSymbol(Flags.STATIC | Flags.ABSTRACT | Flags.DEFAULT, "name", typeSymbol);
    JavaSymbol.TypeJavaSymbol enumeration = new JavaSymbol.TypeJavaSymbol(Flags.ENUM, "enumeration", P_PACKAGE_JAVA_SYMBOL);
    assertThat(methodSymbol.isEnum()).isFalse();
    assertThat(methodSymbol.isFinal()).isFalse();
    assertThat(methodSymbol.isAbstract()).isTrue();
    assertThat(methodSymbol.isAnnotation()).isFalse();
    assertThat(methodSymbol.isStatic()).isTrue();
    assertThat(methodSymbol.isPackageVisibility()).isTrue();
    assertThat(methodSymbol.isVolatile()).isFalse();
    assertThat(methodSymbol.isProtected()).isFalse();

    assertThat(annotationType.isAnnotation()).isTrue();
    assertThat(annotationType.isEnum()).isFalse();
    assertThat(annotationType.isInterface()).isTrue();

    assertThat(enumeration.isEnum()).isTrue();
    assertThat(enumeration.isAbstract()).isFalse();
    assertThat(enumeration.isAnnotation()).isFalse();
    assertThat(enumeration.isStatic()).isFalse();
    assertThat(P_PACKAGE_JAVA_SYMBOL.isPackageSymbol()).isTrue();
    assertThat(outermostClass.isPackageSymbol()).isFalse();

    assertThat(methodSymbol.isDefault()).isFalse();
    assertThat(defaultMethodSymbol.isDefault()).isTrue();
  }
}
