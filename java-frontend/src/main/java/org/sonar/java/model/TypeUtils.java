/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.plugins.java.api.semantic.Type;

public final class TypeUtils {

  private static final List<String> VALUE_BASED_TYPES = Arrays.asList(
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Double",
    "java.lang.Float",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Short",
    "java.util.Optional",
    "java.util.OptionalDouble",
    "java.util.OptionalInt",
    "java.util.OptionalLong",
    "java.time.chrono.HijrahDate",
    "java.time.chrono.JapaneseDate",
    "java.time.chrono.MinguoDate",
    "java.time.chrono.ThaiBuddhistDate"
  );

  private static final Pattern JAVA_TIME_PACKAGE_PATTERN = Pattern.compile("java\\.time\\.\\w+");
  private static final String JAVA_TIME_CLOCK = "java.time.Clock";

  private TypeUtils() {
    // This utility class should not be instantiated.
  }

  // Check if a type is a known value-based class.
  public static boolean isValueBasedType(Type type) {
    if (type.isUnknown() || type.is(JAVA_TIME_CLOCK)) {
      return false;
    }
    return VALUE_BASED_TYPES.stream().anyMatch(type::is) || JAVA_TIME_PACKAGE_PATTERN.matcher(type.fullyQualifiedName()).matches();
  }

}
