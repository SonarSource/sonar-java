/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se.symbolicvalues;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Type;

import static org.fest.assertions.Assertions.assertThat;

public class SymbolicValueTest {

  @Test
  public void exceptional_SV_should_contain_exception_type_in_toString() {
    SymbolicValue.ExceptionalSymbolicValue unknownException = new SymbolicValue.ExceptionalSymbolicValue(42, null);
    // contains the key
    assertThat(unknownException.toString()).contains("42");
    // contains the exception
    assertThat(unknownException.toString()).contains("!unknownException!");

    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol exceptionSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyException", packageSymbol);
    Type exceptionType = new ClassJavaType(exceptionSymbol);
    SymbolicValue.ExceptionalSymbolicValue knownException = new SymbolicValue.ExceptionalSymbolicValue(42, exceptionType);

    // contains the key
    assertThat(knownException.toString()).contains("42");
    // contains the exception
    assertThat(knownException.toString()).contains("org.foo.bar.MyException!");
  }

  @Test
  public void exceptional_SV_equals() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol exceptionSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyException", packageSymbol);
    Type exceptionType = new ClassJavaType(exceptionSymbol);

    SymbolicValue.ExceptionalSymbolicValue sv = new SymbolicValue.ExceptionalSymbolicValue(42, exceptionType);

    assertThat(sv).isEqualTo(sv);
    assertThat(sv).isNotEqualTo(null);
    assertThat(sv).isNotEqualTo(new SymbolicValue(sv.id()));

    // different IDs but same exception
    assertThat(sv).isNotEqualTo(new SymbolicValue.ExceptionalSymbolicValue(sv.id() + 1, sv.exceptionType()));
    // same IDs but different exception
    assertThat(sv).isNotEqualTo(new SymbolicValue.ExceptionalSymbolicValue(sv.id(), null));
  }

  @Test
  public void test_computed_from() throws Exception {
    SymbolicValue symbolicValue = new SymbolicValue(3);
    assertThat(symbolicValue.computedFrom()).isEmpty();

    SymbolicValue.NotSymbolicValue notSymbolicValue = new SymbolicValue.NotSymbolicValue(4);
    notSymbolicValue.computedFrom(ImmutableList.of(symbolicValue));
    assertThat(notSymbolicValue.computedFrom()).contains(symbolicValue);

    RelationalSymbolicValue relationalSymbolicValue = new RelationalSymbolicValue(5, RelationalSymbolicValue.Kind.METHOD_EQUALS);
    relationalSymbolicValue.computedFrom(ImmutableList.of(symbolicValue, notSymbolicValue));
    assertThat(relationalSymbolicValue.computedFrom()).contains(symbolicValue, notSymbolicValue);
  }
}
