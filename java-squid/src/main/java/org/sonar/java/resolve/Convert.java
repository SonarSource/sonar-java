/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.resolve;

import org.apache.commons.lang.StringUtils;

public class Convert {

  private Convert() {
  }

  public static String packagePart(String fullName) {
    int lastDot = fullName.lastIndexOf('.');
    return lastDot < 0 ? "" : fullName.substring(0, lastDot);
  }

  public static String shortName(String fullName) {
    return fullName.substring(fullName.lastIndexOf('.') + 1);
  }

  public static String flatName(String bytecodeName) {
    return bytecodeName.replace('/', '.');
  }

  public static String bytecodeName(String fullName) {
    return fullName.replace('.', '/');
  }

  public static String enclosingClassName(String shortName) {
    String normalizedShortName = normalizeShortName(shortName);
    int lastDollar = normalizedShortName.lastIndexOf('$');
    return lastDollar < 0 ? "" : normalizedShortName.substring(0, lastDollar);
  }

  public static String innerClassName(String shortName) {
    return shortName.substring(normalizeShortName(shortName).lastIndexOf('$') +1);
  }

  private static String normalizeShortName(String shortName) {
    return StringUtils.removeEnd(StringUtils.removeEnd(shortName, "$class"), "$");
  }

  public static String fullName(String packagePart, String className) {
    String pck = StringUtils.defaultIfBlank(packagePart, "");
    if(StringUtils.isNotEmpty(pck)) {
      pck += ".";
    }
    return pck + className;
  }
}
