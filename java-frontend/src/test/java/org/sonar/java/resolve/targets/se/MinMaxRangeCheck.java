/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.resolve.targets.se;

// This classes is placed in compiled code ('org.sonar.java.resolve.targets.se' package) in order to get
// access to bytecode and resolve constants
class MinMaxRangeCheck {

  private static final int UPPER = 20;
  private static final int LOWER = 0;
  private int otherValue = 0;

  public int doRangeCheckNOK1(int num) { // Let's say num = 12
    int result = Math.min(LOWER, num); // result = 0
    return Math.max(UPPER, result); // Noncompliant; result is now 20: even though 12 was in the range
  }

  public int doRangeCheckNOK2(int num) { // Let's say num = 12
    int result = Math.min(num, LOWER); // result = 0
    return Math.max(result, UPPER); // Noncompliant; result is now 20: even though 12 was in the range
  }

  public int doRangeCheckOK1(int num) { // Let's say num = 12
    int result = Math.min(UPPER, num); // result = 12
    return Math.max(LOWER, result); // Compliant; result is still 12
  }

  public int doRangeCheckOK2(int a, int b) {
    int result = Math.min(a, b);
    return Math.max(LOWER, result); // Compliant; result could be LOWER, a or b
  }

  public int doRangeCheckOK3(int a, int b) {
    int result = Math.min(a, b);
    return Math.max(otherValue, result); // Compliant; result could be otherValue, a or b
  }

  public int doRangeCheckNOK4(int num) { // using both time same range
    int result = Math.min(UPPER, num);
    return Math.max(UPPER, result);
  }

  public int doRangeCheckNOK5(int num) { // using both time same range
    int result = Math.min(LOWER, num);
    return Math.max(LOWER, result);
  }

  public void foo() {
    foo();
  }
}
