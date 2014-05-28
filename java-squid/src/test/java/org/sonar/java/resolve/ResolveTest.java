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
package org.sonar.java.resolve;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResolveTest {

  private BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
  private Resolve resolve = new Resolve(new Symbols(bytecodeCompleter), bytecodeCompleter);

  private Resolve.Env env = mock(Resolve.Env.class);

  @Test
  public void access_public_class() {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol(null, null);
    Symbol.TypeSymbol targetClassSymbol = new Symbol.TypeSymbol(Flags.PUBLIC, "TargetClass", packageSymbol);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();
  }

  @Test
  public void access_protected_class() {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol(null, null);
    Symbol.TypeSymbol targetClassSymbol = new Symbol.TypeSymbol(Flags.PROTECTED, "TargetClass", packageSymbol);

    when(env.packge()).thenReturn(packageSymbol);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    when(env.packge()).thenReturn(new Symbol.PackageSymbol("AnotherPackage", null));
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
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol(null, null);
    Symbol.TypeSymbol targetClassSymbol = new Symbol.TypeSymbol(0, "TargetClass", packageSymbol);

    when(env.packge()).thenReturn(packageSymbol);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    when(env.packge()).thenReturn(new Symbol.PackageSymbol("AnotherPackage", null));
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
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol(null, null);
    Symbol.TypeSymbol outermostClassSymbol = new Symbol.TypeSymbol(0, "OutermostClass", packageSymbol);
    Symbol.TypeSymbol targetClassSymbol = new Symbol.TypeSymbol(Flags.PRIVATE, "TargetClass", outermostClassSymbol);

    when(env.enclosingClass()).thenReturn(outermostClassSymbol);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    when(env.enclosingClass()).thenReturn(new Symbol.TypeSymbol(0, "AnotherOutermostClass", packageSymbol));
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isFalse();
  }

  @Test
  public void test_isSubClass() {
    Symbol.TypeSymbol base = new Symbol.TypeSymbol(0, "class", null);

    // same class
    Symbol.TypeSymbol c = base;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // base not extended by class
    c = new Symbol.TypeSymbol(0, "class", null);

    // class extends base
    assertThat(resolve.isSubClass(c, base)).isFalse();
    ((Type.ClassType) c.type).supertype = base.type;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class extends superclass
    ((Type.ClassType) c.type).supertype = new Symbol.TypeSymbol(0, "superclass", null).type;
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class extends superclass, which extends base
    ((Type.ClassType) ((Type.ClassType) c.type).supertype).supertype = base.type;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // base - is an interface
    base = new Symbol.TypeSymbol(Flags.INTERFACE, "class", null);
    c = new Symbol.TypeSymbol(0, "class", null);

    // base not implemented by class
    ((Type.ClassType) c.type).interfaces = ImmutableList.of();
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class implements base interface
    ((Type.ClassType) c.type).interfaces = ImmutableList.of(base.type);
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class implements interface, but not base interface
    Symbol.TypeSymbol i = new Symbol.TypeSymbol(Flags.INTERFACE, "class", null);
    ((Type.ClassType) i.type).interfaces = ImmutableList.of();
    ((Type.ClassType) c.type).interfaces = ImmutableList.of(i.type);
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class implements interface, which implements base
    ((Type.ClassType) c.type).interfaces = ImmutableList.of(base.type);
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class extends superclass
    ((Type.ClassType) c.type).interfaces = ImmutableList.of();
    ((Type.ClassType) c.type).supertype = new Symbol.TypeSymbol(0, "superclass", null).type;
    ((Type.ClassType) ((Type.ClassType) c.type).supertype).interfaces = ImmutableList.of();
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class extends superclass, which implements base
    ((Type.ClassType) ((Type.ClassType) c.type).supertype).interfaces = ImmutableList.of(base.type);
    assertThat(resolve.isSubClass(c, base)).isTrue();
  }

  @Test
  public void test_isInheritedIn() {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("package", null);
    Symbol.TypeSymbol clazz = new Symbol.TypeSymbol(0, "class", packageSymbol);

    // public symbol is always inherited
    Symbol symbol = new Symbol(0, Flags.PUBLIC, "name", null);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isTrue();

    // private symbol is inherited only if it's owned by class
    symbol = new Symbol(0, Flags.PRIVATE, "name", null);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isFalse();

    symbol = new Symbol(0, Flags.PRIVATE, "name", clazz);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isTrue();

    // protected symbol is always inherited
    symbol = new Symbol(0, Flags.PROTECTED, "name", null);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isTrue();

    // package local symbol is inherited only if TODO...
    symbol = new Symbol(0, 0, "name", clazz);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isTrue();

    Symbol.PackageSymbol anotherPackageSymbol = new Symbol.PackageSymbol("package", null);
    symbol = new Symbol(0, 0, "name", anotherPackageSymbol);
    assertThat(resolve.isInheritedIn(symbol, clazz)).isFalse();
  }

}
