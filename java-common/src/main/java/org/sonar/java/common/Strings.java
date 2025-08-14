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
package org.sonar.java.common;

public class Strings {
  public static boolean containsSensitive(String string, String search) {
    return org.apache.commons.lang3.Strings.CS.contains(string, search);
  }

  public static boolean containsInsensitive(String string, String search) {
    return org.apache.commons.lang3.Strings.CI.contains(string, search);
  }

  public static int indexOfInsensitive(String string, String search) {
    return org.apache.commons.lang3.Strings.CI.indexOf(string, search);
  }

  public static boolean startsWithSensitive(String string, String prefix) {
    return org.apache.commons.lang3.Strings.CS.startsWith(string, prefix);
  }

  public static boolean endsWithSensitive(String string, String suffix) {
    return org.apache.commons.lang3.Strings.CS.endsWith(string, suffix);
  }
}
