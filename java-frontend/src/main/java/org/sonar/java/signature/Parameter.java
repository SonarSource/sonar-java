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
package org.sonar.java.signature;

import javax.annotation.Nullable;

public class Parameter {

  private final JvmJavaType jvmJavaType;
  private final String className;
  private final boolean isArray;

  public Parameter(JvmJavaType jvmJavaType, boolean isArray) {
    if (jvmJavaType == JvmJavaType.L) {
      throw new IllegalArgumentException();
    }
    this.jvmJavaType = jvmJavaType;
    this.className = null;
    this.isArray = isArray;
  }

  public Parameter(@Nullable String classCanonicalName, boolean isArray) {
    if (classCanonicalName == null || "".equals(classCanonicalName)) {
      throw new IllegalArgumentException("With an Object JavaType, this is mandatory to specify the canonical name of the class.");
    }
    this.jvmJavaType = JvmJavaType.L;
    this.className = extractClassName(classCanonicalName);
    this.isArray = isArray;
  }

  public Parameter(Parameter other) {
    this.jvmJavaType = other.jvmJavaType;
    this.className = other.className;
    this.isArray = other.isArray;
  }

  public boolean isVoid() {
    return jvmJavaType == JvmJavaType.V;
  }

  public JvmJavaType getJvmJavaType() {
    return jvmJavaType;
  }

  public String getClassName() {
    return className;
  }

  public boolean isArray() {
    return isArray;
  }

  public boolean isOject() {
    return jvmJavaType == JvmJavaType.L;
  }

  private static String extractClassName(String classCanonicalName) {
    int slashIndex = classCanonicalName.lastIndexOf('/');
    int dollarIndex = classCanonicalName.lastIndexOf('$');
    if (slashIndex != -1 || dollarIndex != -1) {
      return classCanonicalName.substring(Math.max(slashIndex, dollarIndex) + 1);
    }
    return classCanonicalName;
  }

}
