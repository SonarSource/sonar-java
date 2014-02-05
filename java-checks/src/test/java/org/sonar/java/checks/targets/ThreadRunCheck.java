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

import java.io.Serializable;

class A {
  public static void main(String[] args) {
    Runnable runnable = null;

    Thread myThread = new Thread(runnable);
    myThread.run(); // Noncompliant

    Thread myThread2 = new Thread(runnable);
    myThread2.start(); // Compliant

    run(); // Compliant
    A a = new A();
    a.run(); // Compliant

    B b = new B();
    b.run(); // Noncompliant

    C c = new C();
    c.run(); // Noncompliant

    D d = new D();
    d.run(); // Noncompliant

    E e = new E();
    e.run(); // Compliant

    F f = new F();
    f.run(); // Compliant

    runnable.run(); // Noncompliant
  }

  public static void run() {
  }

  static class B extends Thread {
  }

  static class C implements Runnable {

    @Override
    public void run() {
    }

  }

  static class D extends C {
    @Override
    public void run() {
      C c = new C();
      c.run(); // Noncompliant but false negative
      super.run();
    }
  }

  static class E implements Serializable {

    public void run() {
    }

  }

  static class F extends E {
  }
}
