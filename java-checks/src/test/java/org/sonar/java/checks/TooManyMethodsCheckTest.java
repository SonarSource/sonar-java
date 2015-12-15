/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks;

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class TooManyMethodsCheckTest {

  @Test
  public void test() {
    TooManyMethodsCheck check = new TooManyMethodsCheck();
    check.maximumMethodThreshold = 4;
    JavaCheckVerifier.verify("src/test/files/checks/TooManyMethodsCheck.java", check);
  }

  @Test
  public void only_public() {
    TooManyMethodsCheck check = new TooManyMethodsCheck();
    check.maximumMethodThreshold = 4;
    check.countNonPublic = false;
    JavaCheckVerifier.verify("src/test/files/checks/TooManyMethodsCheckOnlyPublic.java", check);
  }

}

class A { // Noncompliant {{Class "A" has 5  methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
  void method5() {}
}

enum B {
  A;
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
}

interface C {
  void method1();
  public void method2();
  void method3();
  public void method4();
}

@interface D {
  String method1();
  public String method2();
  String method3();
  public String method4();
}
