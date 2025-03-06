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
package org.sonar.java.checks;

public class Constants {

  private Constants() {
  }

  public static final String SPRING_3_2 = "../java-checks-test-sources/spring-3.2";
  public static final String SPRING_3_2_CLASSPATH = SPRING_3_2 + "/target/test-classpath.txt";
  public static final String SPRING_WEB_4_0_TEST_SOURCES = "../java-checks-test-sources/spring-web-4.0";
  public static final String SPRING_WEB_4_0_CLASSPATH = SPRING_WEB_4_0_TEST_SOURCES + "/target/test-classpath.txt";
}
