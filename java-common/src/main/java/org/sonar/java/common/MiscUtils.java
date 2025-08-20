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

public class MiscUtils {
  public static String unescapeJava(String string) {
//    return org.apache.commons.lang3.StringEscapeUtils.unescapeJava(string);
    if (string == null) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    int len = string.length();
    for (int i = 0; i < len; i++) {
      char c = string.charAt(i);

      if (c == '\\') {
        i++;
        if (i >= len) {
          sb.append('\\');
          break;
        }
        char nextChar = string.charAt(i);
        switch (nextChar) {
          case 'n':
            sb.append('\n');
            break;
          case 't':
            sb.append('\t');
            break;
          case 'r':
            sb.append('\r');
            break;
          case 'b':
            sb.append('\b');
            break;
          case 'f':
            sb.append('\f');
            break;
          case '\\':
            sb.append('\\');
            break;
          case '\'':
            sb.append('\'');
            break;
          case '"':
            sb.append('"');
            break;
          case 'u':
            if (i + 4 < len) {
              String hex = string.substring(i + 1, i + 5);
              try {
                int unicodeValue = Integer.parseInt(hex, 16);
                sb.append((char) unicodeValue);
                i += 4;
              } catch (NumberFormatException e) {
                // Invalid Unicode sequence, append as is
                sb.append("\\u" + hex);
                i += 4;
              }
            } else {
              sb.append("\\u");
            }
            break;
          default:
            // For any other backslash, append it and the next char
            sb.append('\\').append(nextChar);
            break;
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public static Throwable getRootCause(Throwable throwable) {
//    return org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(exception);
    if (throwable == null) {
      return null;
    }

    Throwable cause = throwable.getCause();
    while (cause != null && cause != throwable) {
      throwable = cause;
      cause = throwable.getCause();
    }
    return throwable;
  }

  // easy to remove
  public static String getStackTrace(Throwable throwable) {
    return org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(throwable);
  }
}
