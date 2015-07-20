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

import org.sonar.java.checks.targets.subpackage.ThrowSeveralCheckedExceptionCheckSubpackage;

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

class BaseClass<T> {
  public T meth() throws IOException, SQLException{return null;}
}
class BaseChildClass<T> extends BaseClass<T>{}
class Synth extends BaseChildClass<Integer> {
  @Override
  public Integer meth() throws IOException { return 1;}
}

class J {
  public void method(int a, String b) throws IOException, SQLException{}
  public J method2(int a, String b) throws IOException, SQLException{return null;}
  public String method3(int a, String b) throws IOException, SQLException{return null;}
}
class K extends J {
  public void method(String a, String b) throws IOException, SQLException{}
  public void method(int a, String b) throws IOException, SQLException{}
  public K method2(int a, String b) throws IOException, SQLException{return null;}
  public String method3(int a) throws IOException, SQLException{return null;}
}
interface L {
  void method(String a, String b) throws IOException, SQLException;
}
interface M extends L {}
class O implements M {
  public void method(String a, String b) throws IOException, SQLException {}
}
class P {
  public void method(Q a, String b) throws IOException, SQLException {}
  private void privateMethod(Q a, String b) throws IOException, SQLException {}
  class Q{}
}
class R extends P {
  public void method(Q a, String b) throws IOException, SQLException {}
  public static void foo(Q a, String b) throws IOException, SQLException {
    P p = new P() { //Ignore anonymous classes: false negative SONARJAVA-645
      public void method(Q a, String b) throws IOException, SQLException {}
    };
  }
}
class S<T> {
  public T method(T a) throws IOException, SQLException { return null;}
}
class U extends S<String> {
  public String method(String a) throws IOException, SQLException { return null; } //false positive
}
class V extends P {
  public void privateMethod(Q a, String b) throws IOException, SQLException {} //Non-Compliant
}
class W extends ThrowSeveralCheckedExceptionCheckSubpackage {
  String bar(int j) throws IOException, SQLException {return null;}
  public String foo(int a) throws IOException, SQLException {return null;} //Non-Compliant : different package : does not overrides
}
class Y extends W {
  public String bar(int j) throws IOException, SQLException  {return null;}//Compliant : same package : overrides
  public static void main(String[] args) throws IOException, SQLException {}
  public String plop() throws IOException, ThrowSeveralCheckedExceptionCheckSubpackage.MyCustomException {return null;}
}
