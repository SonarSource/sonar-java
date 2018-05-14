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
package org.sonar.java.checks.helpers;

// used by ConstantUtilsTest
public class ClassWithConstants {

  public static final String CONSTANT1 = "abc";
  public static final String CONSTANT2 = CONSTANT1 + "def";
  public static final int INT_CONSTANT1 = 42;

  public void literals(String param) {
    System.out.println("hello");
    System.out.println(true);
    System.out.println(param);
  }

  public void identifiers(String param) {
    System.out.println(CONSTANT1);
    System.out.println(CONSTANT2);
    System.out.println(INT_CONSTANT1);
  }

  public void parentheses(String parentheses) {
    System.out.println(((CONSTANT1)));
  }

  public void member_select(String parentheses) {
    System.out.println(ClassWithConstants.CONSTANT1);
  }

  public void concatenation(String param) {
    System.out.println("hello " + CONSTANT1);
    System.out.println("hello " + param);
    System.out.println(param + "hello ");
  }

}
