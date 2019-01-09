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
package org.sonar.java.resolve.targets.se;

// This classes is placed in compiled code ('org.sonar.java.resolve.targets.se' package) in order to get
// access to bytecode and resolve constants
class MinMaxRangeCheck {

  private static final int UPPER_INT = 20;
  private static final int LOWER_INT = 0;

  private static final float UPPER_FLOAT = 20.0432f;
  private static final double UPPER_DOUBLE = 20.0432;
  private static final double LOWER_DOUBLE = -15.1728;

  private int otherValue = 0;

  public int doRangeCheckNOK1(int num) {
    int result = Math.min(LOWER_INT, num);
    return Math.max(UPPER_INT, result); // Noncompliant {{Change these chained max/min methods invocations, as final results will always be the upper bound.}}
  }

  public int doRangeCheckNOK2(int num) {
    int result = Math.min(num, LOWER_INT);
    return Math.max(result, UPPER_INT); // Noncompliant
  }

  public double doRangeCheckNOK3(double num) {
    double result = Math.min(num, LOWER_DOUBLE);
    return Math.max(result, UPPER_DOUBLE); // Noncompliant
  }

  public int doRangeCheckNOK4(int num) {
    int newUpper = UPPER_INT;
    int newLower = LOWER_INT;
    int result = Math.min(newLower, num); // flow@f1 [[order=1]] {{Returns the lower bound.}}
    return Math.max(result, newUpper); // Noncompliant [[flows=f1]] flow@f1 [[order=2]] {{Returns the upper bound.}}
  }

  public double doRangeCheckNOK5(int num) {
    int result = Math.max(UPPER_INT, num);
    return Math.min(LOWER_INT, result); // Noncompliant {{Change these chained min/max methods invocations, as final results will always be the lower bound.}}
  }

  public int doRangeCheckNOK6(int num) {
    int upper = 1837;
    int lower = -496;
    int result = Math.min(lower, num);
    return Math.max(result, upper); // Noncompliant
  }

  public double doRangeCheckNOK7(double num) {
    double upper = 18.37;
    double lower = -4.96;
    double result = Math.min(lower, num);
    return Math.max(result, upper); // Compliant - FN
  }

  public double doRangeCheckNOK8(float num) {
    float upper = 18.37f;
    float lower = -4.96f;
    float result = Math.min(lower, num);
    return Math.max(result, upper); // Compliant - FN
  }

  public double doRangeCheckNOK9(long num) {
    long upper = 1837L;
    long lower = -496L;
    long result = Math.min(lower, num);
    return Math.max(result, upper); // Noncompliant
  }

  public int doRangeCheckOK1(int num) { // Let's say num = 12
    int result = Math.min(UPPER_INT, num); // result = 12
    return Math.max(LOWER_INT, result); // Compliant; result is still 12
  }

  public int doRangeCheckOK2(int a, int b) {
    int result = Math.min(a, b);
    return Math.max(LOWER_INT, result); // Compliant; result could be LOWER, a or b
  }

  public int doRangeCheckOK3(int a, int b) {
    int result = Math.min(a, b);
    return Math.max(otherValue, result); // Compliant; result could be otherValue, a or b
  }

  public int doRangeCheckOK4(int num) { // using both time same range
    int result = Math.min(UPPER_INT, num);
    return Math.max(UPPER_INT, result);
  }

  public int doRangeCheckOK5(int num) { // using both time same range
    int result = Math.min(LOWER_INT, num);
    return Math.max(LOWER_INT, result);
  }

  public int doRangeCheckOK6(int num) { // do not handle arithmetic
    int upper = 1837 + 14;
    int lower = -496 * 42;
    int result = Math.min(lower, num);
    return Math.max(result, upper);
  }

  // do not remove this constant, it is used for other test
  public static final String DOT_AS_STRING = ".";

  private static final char DOT = '.'; // 46 as int
  private static final char SEMICOLUMN = ';'; // 59 as int

  public int rangeCheckWithOtherTypes(char num) {
    int result = Math.min(num, DOT);
    return Math.max(result, SEMICOLUMN); // Noncompliant
  }

  public void foo() {
    foo();
  }

  void coverage() {
    int iValue = +UPPER_INT;
    double fValue = -UPPER_FLOAT;
    double dValue = -UPPER_DOUBLE;
  }
}
