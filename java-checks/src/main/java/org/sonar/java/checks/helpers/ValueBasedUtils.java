/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.util.Set;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Type.ArrayType;

/**
 * Utility functions related to
 * <a href="http://docs.oracle.com/javase/8/docs/api/java/lang/doc-files/ValueBased.html">value-based classes</a>.
 */
public final class ValueBasedUtils {

  private static final Set<String> KNOWN_VALUE_BASED_CLASSES = SetUtils.immutableSetOf(
    "java.time.chrono.HijrahDate",
    "java.time.chrono.JapaneseDate",
    "java.time.chrono.MinguoDate",
    "java.time.chrono.ThaiBuddhistDate",
    "java.util.Optional",
    "java.util.DoubleOptional",
    "java.util.IntOptional",
    "java.util.LongOptional");

  private ValueBasedUtils() {
    // This class only contains static methods
  }

  public static boolean isValueBased(Type type) {
    String className = getRootElementType(type).fullyQualifiedName();
    return (KNOWN_VALUE_BASED_CLASSES.contains(className) || isInJavaTimePackage(className)) && !"java.time.Clock".equals(className);
  }
  
  private static boolean isInJavaTimePackage(String className) {
    return className.startsWith("java.time");
  }

  private static Type getRootElementType(Type type) {
    return type.isArray() ? getRootElementType(((ArrayType) type).elementType()) : type;
  }
  
}
