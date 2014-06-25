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

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SymbolTest {

  @Test
  public void kinds() {
    assertThat(Symbol.TYP).isLessThan(Symbol.ERRONEOUS);
    assertThat(Symbol.VAR).isLessThan(Symbol.ERRONEOUS);
    assertThat(Symbol.MTH).isLessThan(Symbol.ERRONEOUS);
    assertThat(Symbol.ERRONEOUS).isLessThan(Symbol.ABSENT);
  }

  @Test
  public void completion_should_use_completer() {
    Symbol symbol = new Symbol(0, 0, null, null);
    Symbol.Completer completer = mock(Symbol.Completer.class);
    symbol.completer = completer;
    symbol.complete();
    verify(completer).complete(symbol);
    assertThat(symbol.completer).isNull();
  }

  @Test
  public void test_PackageSymbol() {
    Symbol owner = mock(Symbol.class);
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("name", owner);

    assertThat(packageSymbol.kind).isEqualTo(Symbol.PCK);
    assertThat(packageSymbol.flags()).isEqualTo(0);
    assertThat(packageSymbol.owner()).isSameAs(owner);

    assertThat(packageSymbol.packge()).isSameAs(packageSymbol);
    assertThat(packageSymbol.outermostClass()).isNull();
    assertThat(packageSymbol.enclosingClass()).isNull();
  }

  @Test
  public void test_TypeSymbol() {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("p", null);
    Symbol.TypeSymbol outermostClass = new Symbol.TypeSymbol(42, "name", packageSymbol);
    Symbol.TypeSymbol typeSymbol = new Symbol.TypeSymbol(42, "name", outermostClass);

    assertThat(typeSymbol.kind).isEqualTo(Symbol.TYP);
    assertThat(typeSymbol.flags()).isEqualTo(42);
    assertThat(typeSymbol.owner()).isSameAs(outermostClass);

    assertThat(typeSymbol.packge()).isSameAs(packageSymbol);
    assertThat(typeSymbol.outermostClass()).isSameAs(outermostClass);
    assertThat(typeSymbol.enclosingClass()).isSameAs(typeSymbol);
  }

  @Test
  public void access_to_superclass_should_trigger_completion() {
    Symbol.TypeSymbol typeSymbol = spy(new Symbol.TypeSymbol(42, "name", null));
    typeSymbol.getSuperclass();
    verify(typeSymbol).complete();
  }

  @Test
  public void access_to_interfaces_should_trigger_completion() {
    Symbol.TypeSymbol typeSymbol = spy(new Symbol.TypeSymbol(42, "name", null));
    typeSymbol.getInterfaces();
    verify(typeSymbol).complete();
  }

  @Test
  public void access_to_members_should_trigger_completion() {
    Symbol.TypeSymbol typeSymbol = spy(new Symbol.TypeSymbol(42, "name", null));
    typeSymbol.members();
    verify(typeSymbol).complete();
  }

  @Test
  public void test_MethodSymbol() {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("p", null);
    Symbol.TypeSymbol outermostClass = new Symbol.TypeSymbol(42, "name", packageSymbol);
    Symbol.TypeSymbol typeSymbol = new Symbol.TypeSymbol(42, "t", outermostClass);
    Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(42, "name", typeSymbol);

    assertThat(methodSymbol.kind).isEqualTo(Symbol.MTH);
    assertThat(methodSymbol.flags()).isEqualTo(42);
    assertThat(methodSymbol.owner()).isSameAs(typeSymbol);

    assertThat(methodSymbol.packge()).isSameAs(packageSymbol);
    assertThat(methodSymbol.outermostClass()).isSameAs(outermostClass);
    assertThat(methodSymbol.enclosingClass()).isSameAs(typeSymbol);
  }

  @Test
  public void test_VariableSymbol() {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("p", null);
    Symbol.TypeSymbol outermostClass = new Symbol.TypeSymbol(42, "name", packageSymbol);
    Symbol.TypeSymbol typeSymbol = new Symbol.TypeSymbol(42, "t", outermostClass);
    Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(42, "name", typeSymbol);
    Symbol.VariableSymbol variableSymbol = new Symbol.VariableSymbol(42, "name", methodSymbol);

    assertThat(variableSymbol.kind).isEqualTo(Symbol.VAR);
    assertThat(variableSymbol.flags()).isEqualTo(42);
    assertThat(variableSymbol.owner()).isSameAs(methodSymbol);

    assertThat(variableSymbol.packge()).isSameAs(packageSymbol);
    assertThat(variableSymbol.outermostClass()).isSameAs(outermostClass);
    assertThat(variableSymbol.enclosingClass()).isSameAs(typeSymbol);
  }

  @Test
  public void test_helper_methods() throws Exception {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("p", null);
    Symbol.TypeSymbol outermostClass = new Symbol.TypeSymbol(Flags.INTERFACE, "name", packageSymbol);
    Symbol.TypeSymbol typeSymbol = new Symbol.TypeSymbol(Flags.INTERFACE, "t", outermostClass);
    Symbol.MethodSymbol methodSymbol = new Symbol.MethodSymbol(Flags.STATIC | Flags.ABSTRACT, "name", typeSymbol);
    Symbol.TypeSymbol enumeration = new Symbol.TypeSymbol(Flags.ENUM, "enumeration", packageSymbol);
    assertThat(methodSymbol.isEnum()).isFalse();
    assertThat(methodSymbol.isAbstract()).isTrue();
    assertThat(methodSymbol.isStatic()).isTrue();
    assertThat(enumeration.isEnum()).isTrue();
    assertThat(enumeration.isAbstract()).isFalse();
    assertThat(enumeration.isStatic()).isFalse();
  }
}
