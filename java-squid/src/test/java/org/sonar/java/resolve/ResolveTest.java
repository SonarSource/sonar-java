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
package org.sonar.java.resolve;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResolveTest {

  private ParametrizedTypeCache parametrizedTypeCache = new ParametrizedTypeCache();
  private BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")), parametrizedTypeCache);
  private Resolve resolve = new Resolve(new Symbols(bytecodeCompleter), bytecodeCompleter, parametrizedTypeCache);

  private Resolve.Env env = mock(Resolve.Env.class);

  @Test
  public void access_public_class() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol(null, null);
    JavaSymbol.TypeJavaSymbol targetClassSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "TargetClass", packageSymbol);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();
  }

  @Test
  public void access_protected_class() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol(null, null);
    JavaSymbol.TypeJavaSymbol targetClassSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PROTECTED, "TargetClass", packageSymbol);

    when(env.packge()).thenReturn(packageSymbol);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    when(env.packge()).thenReturn(new JavaSymbol.PackageJavaSymbol("AnotherPackage", null));
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isFalse();
  }

  /**
   * <pre>
   * package p1;
   * class TargetClass {
   * }
   * // accessible
   * package p2;
   * // not accessible
   * </pre>
   */
  @Test
  public void access_package_local_class() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol(null, null);
    JavaSymbol.TypeJavaSymbol targetClassSymbol = new JavaSymbol.TypeJavaSymbol(0, "TargetClass", packageSymbol);

    when(env.packge()).thenReturn(packageSymbol);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    when(env.packge()).thenReturn(new JavaSymbol.PackageJavaSymbol("AnotherPackage", null));
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isFalse();
  }

  /**
   * <pre>
   * public class OutermostClass {
   *   private static class TargetClass {
   *   }
   *   // accessible
   * }
   * class AnotherOutermostClass {
   *   // not accessible
   * }
   * </pre>
   */
  @Test
  public void access_private_class() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol(null, null);
    JavaSymbol.TypeJavaSymbol outermostClassSymbol = new JavaSymbol.TypeJavaSymbol(0, "OutermostClass", packageSymbol);
    JavaSymbol.TypeJavaSymbol targetClassSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PRIVATE, "TargetClass", outermostClassSymbol);

    when(env.enclosingClass()).thenReturn(outermostClassSymbol);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    when(env.enclosingClass()).thenReturn(new JavaSymbol.TypeJavaSymbol(0, "AnotherOutermostClass", packageSymbol));
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isFalse();
  }

  @Test
  public void test_isSubClass() {
    JavaSymbol.TypeJavaSymbol base = new JavaSymbol.TypeJavaSymbol(0, "class", null);

    // same class
    JavaSymbol.TypeJavaSymbol c = base;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // base not extended by class
    c = new JavaSymbol.TypeJavaSymbol(0, "class", null);

    // class extends base
    assertThat(resolve.isSubClass(c, base)).isFalse();
    ((JavaType.ClassJavaType) c.type).supertype = base.type;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class extends superclass
    ((JavaType.ClassJavaType) c.type).supertype = new JavaSymbol.TypeJavaSymbol(0, "superclass", null).type;
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class extends superclass, which extends base
    ((JavaType.ClassJavaType) ((JavaType.ClassJavaType) c.type).supertype).supertype = base.type;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // base - is an interface
    base = new JavaSymbol.TypeJavaSymbol(Flags.INTERFACE, "class", null);
    c = new JavaSymbol.TypeJavaSymbol(0, "class", null);

    // base not implemented by class
    ((JavaType.ClassJavaType) c.type).interfaces = ImmutableList.of();
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class implements base interface
    ((JavaType.ClassJavaType) c.type).interfaces = ImmutableList.of(base.type);
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class implements interface, but not base interface
    JavaSymbol.TypeJavaSymbol i = new JavaSymbol.TypeJavaSymbol(Flags.INTERFACE, "class", null);
    ((JavaType.ClassJavaType) i.type).interfaces = ImmutableList.of();
    ((JavaType.ClassJavaType) c.type).interfaces = ImmutableList.of(i.type);
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class implements interface, which implements base
    ((JavaType.ClassJavaType) c.type).interfaces = ImmutableList.of(base.type);
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class extends superclass
    ((JavaType.ClassJavaType) c.type).interfaces = ImmutableList.of();
    ((JavaType.ClassJavaType) c.type).supertype = new JavaSymbol.TypeJavaSymbol(0, "superclass", null).type;
    ((JavaType.ClassJavaType) ((JavaType.ClassJavaType) c.type).supertype).interfaces = ImmutableList.of();
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class extends superclass, which implements base
    ((JavaType.ClassJavaType) ((JavaType.ClassJavaType) c.type).supertype).interfaces = ImmutableList.of(base.type);
    assertThat(resolve.isSubClass(c, base)).isTrue();
  }

  @Test
  public void test_isInheritedIn() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("package", null);
    JavaSymbol.TypeJavaSymbol clazz = new JavaSymbol.TypeJavaSymbol(0, "class", packageSymbol);

    // public symbol is always inherited
    JavaSymbol symbol = new JavaSymbol(0, Flags.PUBLIC, "name", null);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isTrue();

    // private symbol is inherited only if it's owned by class
    symbol = new JavaSymbol(0, Flags.PRIVATE, "name", null);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isFalse();

    symbol = new JavaSymbol(0, Flags.PRIVATE, "name", clazz);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isTrue();

    // protected symbol is always inherited
    symbol = new JavaSymbol(0, Flags.PROTECTED, "name", null);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isTrue();

    // package local symbol is inherited only if TODO...
    symbol = new JavaSymbol(0, 0, "name", clazz);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isTrue();

    JavaSymbol.PackageJavaSymbol anotherPackageSymbol = new JavaSymbol.PackageJavaSymbol("package", null);
    symbol = new JavaSymbol(0, 0, "name", anotherPackageSymbol);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isFalse();
  }

}
