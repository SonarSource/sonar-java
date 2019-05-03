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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ResolveTest {

  private ParametrizedTypeCache parametrizedTypeCache = new ParametrizedTypeCache();
  private BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes"))), parametrizedTypeCache);
  private Resolve resolve = new Resolve(new Symbols(bytecodeCompleter), bytecodeCompleter, parametrizedTypeCache);

  private Resolve.Env env = mock(Resolve.Env.class);

  @Before
  public void setUp() {
    env = new Resolve.Env();
    env.packge = new JavaSymbol.PackageJavaSymbol(null, null);
  }

  @Test
  public void access_public_class() {
    JavaSymbol.TypeJavaSymbol targetClassSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "TargetClass", env.packge);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();
  }

  @Test
  public void access_protected_class() {
    JavaSymbol.TypeJavaSymbol targetClassSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PROTECTED, "TargetClass", env.packge);

    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    env.packge = new JavaSymbol.PackageJavaSymbol("AnotherPackage", null);
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
    JavaSymbol.TypeJavaSymbol targetClassSymbol = new JavaSymbol.TypeJavaSymbol(0, "TargetClass", env.packge);

    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    env.packge = new JavaSymbol.PackageJavaSymbol("AnotherPackage", null);
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
    env.enclosingClass = new JavaSymbol.TypeJavaSymbol(0, "OutermostClass", env.packge);
    JavaSymbol.TypeJavaSymbol targetClassSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PRIVATE, "TargetClass", env.enclosingClass);

    assertThat(resolve.isAccessible(env, targetClassSymbol)).isTrue();

    env.enclosingClass = new JavaSymbol.TypeJavaSymbol(0, "AnotherOutermostClass", env.packge);
    assertThat(resolve.isAccessible(env, targetClassSymbol)).isFalse();
  }

  @Test
  public void test_isSubClass() {
    JavaSymbol.PackageJavaSymbol packageJavaSymbol = new JavaSymbol.PackageJavaSymbol(null, null);
    JavaSymbol.TypeJavaSymbol base = new JavaSymbol.TypeJavaSymbol(0, "class", packageJavaSymbol);

    // same class
    JavaSymbol.TypeJavaSymbol c = base;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // base not extended by class
    c = new JavaSymbol.TypeJavaSymbol(0, "class", packageJavaSymbol);

    // class extends base
    assertThat(resolve.isSubClass(c, base)).isFalse();
    ((ClassJavaType) c.type).supertype = base.type;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class extends superclass
    ((ClassJavaType) c.type).supertype = new JavaSymbol.TypeJavaSymbol(0, "superclass", packageJavaSymbol).type;
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class extends superclass, which extends base
    ((ClassJavaType) ((ClassJavaType) c.type).supertype).supertype = base.type;
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // base - is an interface
    base = new JavaSymbol.TypeJavaSymbol(Flags.INTERFACE, "class", packageJavaSymbol);
    c = new JavaSymbol.TypeJavaSymbol(0, "class", packageJavaSymbol);

    // base not implemented by class
    ((ClassJavaType) c.type).interfaces = Collections.emptyList();
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class implements base interface
    ((ClassJavaType) c.type).interfaces = Collections.singletonList(base.type);
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class implements interface, but not base interface
    JavaSymbol.TypeJavaSymbol i = new JavaSymbol.TypeJavaSymbol(Flags.INTERFACE, "class", packageJavaSymbol);
    ((ClassJavaType) i.type).interfaces = Collections.emptyList();
    ((ClassJavaType) c.type).interfaces = Collections.singletonList(i.type);
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class implements interface, which implements base
    ((ClassJavaType) c.type).interfaces = Collections.singletonList(base.type);
    assertThat(resolve.isSubClass(c, base)).isTrue();

    // class extends superclass
    ((ClassJavaType) c.type).interfaces = Collections.emptyList();
    ((ClassJavaType) c.type).supertype = new JavaSymbol.TypeJavaSymbol(0, "superclass", packageJavaSymbol).type;
    ((ClassJavaType) ((ClassJavaType) c.type).supertype).interfaces = Collections.emptyList();
    assertThat(resolve.isSubClass(c, base)).isFalse();

    // class extends superclass, which implements base
    ((ClassJavaType) ((ClassJavaType) c.type).supertype).interfaces = Collections.singletonList(base.type);
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
