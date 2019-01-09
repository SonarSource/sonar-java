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
package org.sonar.java.resolve.targets;

import org.sonar.java.resolve.targets.subpackage.MethodSymbolsParentTypes;

public class MethodSymbols {

  class A1<T> implements MethodSymbolsParentTypes.Interface<T> {
    void foo(){};
    public int methodOverriden(int i) { //Overriden
      return 0;
    }

    public void foo(T t) {
    }
  }
  class A2 extends MethodSymbolsParentTypes.A implements MethodSymbolsParentTypes.SuperInterface{
    int method(int j){
      return 1;
    }

    public int bar(String str) {
      return 0;
    }

    public int methodOverriden(int i) {//Overriden
      Object obj = new MethodSymbolsParentTypes.ForAnonymousClass() {
        public int methodUndecidable() { //found as overidden for anonymous classes
          return 0;
        }
      };
      return 0;
    }
  }

  class A3 extends org.sonar.java.resolve.targets.subpackage.MethodSymbolsParentTypes.A {
    //
    public int bar(String str) {
      return super.bar(str);
    }
  }

  class Parent {
    private void method(){}
  }

  class A4 extends Parent {
    void method(){}
  }

  class A5<T extends CharSequence> implements MethodSymbolsParentTypes.Interface<T> {

    @Override
    public void foo(T t) {
    }

    @Override
    public int methodOverriden(int i) {
      return 0;
    }
  }
  interface B1<T> {
    void foo(T t);
  }
  class B2<T extends CharSequence> implements B1<T> {
    public void foo(T t) {}
  }

  interface C1 {
    @Override
    public boolean equals(Object obj);
    @Override
    public String toString();
    void foo();
    void bar();
  }

  interface C2 extends C1 {
    @Override
    void foo();
    @Override
    void bar();
    @Override
    public boolean equals(Object obj);
  }

  static class D {
    static void foo() {}
  }
  static class D2 extends D {
    static void foo() {}
  }
}
