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

public class HiddenFieldCheck extends MyBaseClass {

  private int foo;
  public int bar;

  {
    int foo = this.foo; // Non-Compliant
    int ok = 0; // Compliant
    System.out.println(foo + ok);
  }

  public HiddenFieldCheck(int foo) { // Compliant
    this.foo = foo;
  }

  public void setFoo(int foo) { // Compliant
    this.foo = foo;
  }

  public int setNinja(int foo) { // Compliant
    this.foo = foo;
    return 0;
  }

  public int getFoo() {
    return foo;
  }

  public void method1(int foo) { // Non-Compliant
    int base1 = 0; // Compliant
    int base2 = 0; // Compliant
    int unrelated = 0; // Compliant
    System.out.println(base1 + base2 + unrelated);
  }

  @Override
  public void method2() {
    MyOtherBaseClass instance = new MyOtherBaseClass() {

      @Override
      public void foo() {
        int bar = 0; // Non-Compliant
        int otherBase1 = 0; // Compliant - limitation
        System.out.println(bar + otherBase1);
      }

    };

    instance.foo();

  }

  public static class MyInnerClass {

    int bar;
    int myInnerClass1;

    public void foo() {
      int bar = 0; // Non-Compliant
      System.out.println(bar);
    }

    public class MyInnerInnerClass {

      public void foo() {
        int foo = 0; // Non-Compliant
        int myInnerClass1 = 0; // Non-Compliant
        System.out.println(foo + myInnerClass1);
      }

    }

  }

}

class MyBaseClass {

  public int base1;
  private int base2;

  public int getBase2() {
    return base2;
  }

  public void method2() {
    int base1 = 0; // Non-Compliant
    int base2 = 0; // Non-Compliant
    System.out.println(base1 + base2);
  }

}

abstract class MyOtherBaseClass {

  public int otherBase1;

  public abstract void foo();

}

enum MyEnum {
  A, B;

  public void foo() {
    int a = 0;
    System.out.println(a);
  }
}

final class DataUtils {

  public int foo;

  public interface Sortable {

    int size();

    void swap(int foo, int j); // Non-Compliant

    boolean isLess(int i, int j);

  }

}

class Foo {

  int i;

  {
    for (i = 0; i < 42; i++) {
    }

  }
}
