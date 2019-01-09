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
package org.sonar.java.se.symbolicvalues;

import org.junit.Test;

import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class SymbolicValueTest {

  @Test
  public void exceptional_SV_should_contain_exception_type_in_toString() {
    SymbolicValue.ExceptionalSymbolicValue unknownException = new SymbolicValue.ExceptionalSymbolicValue(null);
    // contains the exception
    assertThat(unknownException.toString()).contains("!unknownException!");

    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol exceptionSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyException", packageSymbol);
    Type exceptionType = new ClassJavaType(exceptionSymbol);
    SymbolicValue.ExceptionalSymbolicValue knownException = new SymbolicValue.ExceptionalSymbolicValue(exceptionType);

    // contains the exception
    assertThat(knownException.toString()).contains("org.foo.bar.MyException!");
  }

  @Test
  public void exceptional_SV_equals() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol exceptionSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyException", packageSymbol);
    Type exceptionType = new ClassJavaType(exceptionSymbol);

    SymbolicValue.ExceptionalSymbolicValue sv = new SymbolicValue.ExceptionalSymbolicValue(exceptionType);

    assertThat(sv).isEqualTo(sv);
    assertThat(sv).isNotEqualTo(null);
    assertThat(sv).isNotEqualTo(new SymbolicValue());

    // different IDs but same exception
    assertThat(sv).isNotEqualTo(new SymbolicValue.ExceptionalSymbolicValue(sv.exceptionType()));
    // same IDs but different exception
    assertThat(sv).isNotEqualTo(new SymbolicValue.ExceptionalSymbolicValue(null));
  }

  @Test
  public void test_computed_from() throws Exception {
    SymbolicValue symbolicValue = new SymbolicValue();
    assertThat(symbolicValue.computedFrom()).isEmpty();

    SymbolicValue.NotSymbolicValue notSymbolicValue = new SymbolicValue.NotSymbolicValue();
    SymbolicValueTestUtil.computedFrom(notSymbolicValue, symbolicValue);
    assertThat(notSymbolicValue.computedFrom()).contains(symbolicValue);

    RelationalSymbolicValue relationalSymbolicValue = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS);
    SymbolicValueTestUtil.computedFrom(relationalSymbolicValue, symbolicValue, notSymbolicValue);
    assertThat(relationalSymbolicValue.computedFrom()).contains(symbolicValue, notSymbolicValue);
  }

  @Test
  public void test_toString() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue.NotSymbolicValue notSV = new SymbolicValue.NotSymbolicValue();
    SymbolicValueTestUtil.computedFrom(notSV, sv1);
    assertThat(notSV).hasToString("!("+sv1.toString()+")");

    SymbolicValue.AndSymbolicValue andSV = new SymbolicValue.AndSymbolicValue();
    SymbolicValueTestUtil.computedFrom(andSV, sv1, sv2);
    assertThat(andSV).hasToString(sv2 + " & " + sv1);

    SymbolicValue.OrSymbolicValue orSV = new SymbolicValue.OrSymbolicValue();
    SymbolicValueTestUtil.computedFrom(orSV, sv1, sv2);
    assertThat(orSV).hasToString(sv2 + " | " + sv1);

    SymbolicValue.XorSymbolicValue xorSV = new SymbolicValue.XorSymbolicValue();
    SymbolicValueTestUtil.computedFrom(xorSV, sv1, sv2);
    assertThat(xorSV).hasToString(sv2 + " ^ " + sv1);
  }

  @Test
  public void caughtException() {
    SymbolicValue.ExceptionalSymbolicValue thrownSV = new SymbolicValue.ExceptionalSymbolicValue(null);
    SymbolicValue.CaughtExceptionSymbolicValue caughtSV = new SymbolicValue.CaughtExceptionSymbolicValue(thrownSV);

    assertThat(caughtSV.exception()).isEqualTo(thrownSV);
  }

  @Test
  public void test_hashCode_equals() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    assertThat(sv1.equals(sv1)).isTrue();
    assertThat(sv1.equals(null)).isFalse();
    assertThat(sv1.equals(new Object())).isFalse();

    SymbolicValue sv2 = new SymbolicValue();
    assertThat(sv1).isNotEqualTo(sv2);
    assertThat(sv1.hashCode()).isNotEqualTo(sv2.hashCode());
  }
}
