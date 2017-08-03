/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se;

import javax.annotation.Nullable;

import java.text.MessageFormat;

public class EGDataProvider {

  private static final char ESCAPE_CHAR = '?';
  private static final String ESCAPED_COUPLE = escape("{0}") + ":" + escape("{1}");

  public static String escape(String value) {
    return ESCAPE_CHAR + value + ESCAPE_CHAR;
  }

  public static String escapeCouple(Object key, Object value) {
    return MessageFormat.format(ESCAPED_COUPLE, key, value);
  }

  public static String asObject(@Nullable String value) {
    return "{" + valueOrEmpty(value) + "}";
  }

  public static String asList(@Nullable String value) {
    return "[" + valueOrEmpty(value) + "]";
  }

  private static String valueOrEmpty(@Nullable String value) {
    return value != null ? value : "";
  }

}
