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
package org.sonar.java.bytecode.se.testdata;

public abstract class BytecodeTestClass {
  static Object fun(boolean a, Object b) {
    if (b == null) {
      return null;
    }
    return "";
  }

  static Object fun2(boolean a) {
    if (a) {
      return null;
    }
    return "";
  }

  static Object int_comparison(int a, int b) {
    if (a < b) {
      if (a < b) {
        return null;
      }
      return "";
    }
    return null;
  }

  static boolean gotoTerminator(Object o) {
    return o == null;
  }

  static void throw_exception() {
    throw new RuntimeException();
  }

  abstract boolean abstractMethod(String s);
  static native boolean nativeMethod(String s);
  final boolean finalMethod(String s) { return true; }
  static boolean staticMethod(String s) { return true; }
  private boolean privateMethod(String s) { return true; }
}
