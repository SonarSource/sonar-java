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
package org.sonar.java.checks.targets;

public class RedundantThrowsDeclarationCheck {

  public void foo1() { // Compliant
  }

  public void foo2() throws Throwable { // Compliant
  }

  public void foo3() throws Error { // Compliant
  }

  public void foo4() throws MyException { // Compliant
  }

  public void foo5() throws RuntimeException { // Non-Compliant
  }

  public void foo6() throws IllegalArgumentException { // Non-Compliant
  }

  public void foo7() throws MyRuntimeException { // Non-Compliant
  }

  public void foo8() throws MyException, Exception { // Non-Compliant
  }

  public void foo9() throws Error, Throwable { // Non-Compliant
  }

  public void foo11() throws MyException, MyException { // Non-Compliant
  }

  public void foo12() throws MyException, MyException, Throwable { // Non-Compliant
  }

  public void foo13() throws MyRuntimeException, MyRuntimeException { // Non-Compliant
  }

  public void foo14() throws MyRuntimeException, Throwable { // Non-Compliant
  }

  public void foo15() throws Exception, Error { // Compliant
  }

  public class MyException extends Exception {
  }

  public class MyRuntimeException extends RuntimeException {
  }

  static interface MyInterface<T> {
     public T plop() throws IllegalStateException; // Non-Compliant
  }

  static class MyClass implements MyInterface<String> {
    public String plop() {
      return "";
    }
  }
}
