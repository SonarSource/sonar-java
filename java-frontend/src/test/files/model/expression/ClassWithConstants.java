/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.expression;

import static java.lang.Boolean.TRUE;

// used by AssessableExpressionTreeTest
public class ClassWithConstants {

  public static final String CONSTANT1 = "abc";
  public static final String CONSTANT2 = CONSTANT1 + "def";
  public static final int INT_CONSTANT1 = 42;
  public static final long LONG_CONSTANT1 = 99L;
  public static final Object OBJECT_CONSTANT = new Object();
  public static final boolean BOOLEAN_CONSTANT = false;

  public void literals(String param) {
    System.out.println("hello");
    System.out.println(true);
    System.out.println(43);
    System.out.println(+43);
    System.out.println(-43);
    System.out.println(77L);
    System.out.println(+77L);
    System.out.println(-77L);
    System.out.println(param);
    System.out.println(1_000);
    System.out.println(0x99567L);
  }

  public void identifiers(String param) {
    System.out.println(CONSTANT1);
    System.out.println(CONSTANT2);
    System.out.println(INT_CONSTANT1);
    System.out.println(LONG_CONSTANT1);
    System.out.println(TRUE);
    System.out.println(BOOLEAN_CONSTANT);
  }

  public void parentheses(String parentheses) {
    System.out.println(((CONSTANT1)));
    System.out.println(((INT_CONSTANT1)));
  }

  public void memberSelect(String parentheses) {
    System.out.println(ClassWithConstants.CONSTANT1);
    System.out.println(Boolean.TRUE);
    System.out.println(Boolean.FALSE);
  }

  public void plus(String param) {
    System.out.println("hello " + CONSTANT1);
    System.out.println("hello " + param);
    System.out.println(param + "hello ");
    System.out.println("hello" + INT_CONSTANT1);
    System.out.println(INT_CONSTANT1 + "hello");
    System.out.println(INT_CONSTANT1 + 1);
    System.out.println(LONG_CONSTANT1 + 1);
    System.out.println(2 + LONG_CONSTANT1);
    System.out.println(3L + LONG_CONSTANT1);
  }

  public void other() {
    System.out.println(String.valueOf(1));
    System.out.println(OBJECT_CONSTANT);
  }

}
