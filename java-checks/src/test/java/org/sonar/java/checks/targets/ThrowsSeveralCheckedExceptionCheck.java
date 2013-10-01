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
package org.sonar.java.checks.targets;

import java.io.IOException;
import java.sql.SQLException;

public class ThrowsSeveralCheckedExceptionCheck extends Base {

  public void foo1() { // Compliant
  }

  public void foo2() throws Throwable { // Compliant
  }

  public void foo3() throws Error { // Compliant
  }

  public void foo4() throws MyException { // Compliant
  }

  public void foo5() throws RuntimeException { // Compliant
  }

  public void foo6() throws IllegalArgumentException { // Compliant
  }

  public void foo7() throws MyRuntimeException { // Compliant
  }

  public void foo8() throws IllegalArgumentException, MyException, NullPointerException { // Compliant
  }

  public void foo9() throws IOException, MyException { // Non-Compliant
  }

  public void foo10() throws IOException, IOException, SQLException { // Non-Compliant
  }

  void foo11() throws IOException, IOException, SQLException { // Compliant
  }

  public class MyException extends Exception {
  }

  public class MyRuntimeException extends RuntimeException {
  }

  @Override
  public void overridenMethod() throws IOException, SQLException { // Compliant - overriden methods
  }

}

class Base {

  public void overridenMethod() throws IOException, SQLException { // Non-Compliant
  }

}

class Implements implements I {

  @Override
  public void foo() { // Compliant
  }

  @Override
  public void bar() throws IOException, SQLException { // Compliant - overriden
  }

  public void baz() {
  }

  public void qux() throws IOException, SQLException { // Non-Compliant
  }

}

interface I {
  public void foo();

  public void bar() throws IOException, SQLException; // Non-Compliant

}
