/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

public class NullabilityDataUtils {
  private NullabilityDataUtils() {
    // Utility class
  }

  public static Optional<String> nullabilityAsString(SymbolMetadata.NullabilityData nullabilityData) {
    SymbolMetadata.AnnotationInstance annotation = nullabilityData.annotation();
    if (annotation == null) {
      return Optional.empty();
    }
    String name = getAnnotationName(annotation);
    if (nullabilityData.metaAnnotation()) {
      name += " via meta-annotation";
    }
    String level = levelToString(nullabilityData.level());
    return Optional.of(String.format("@%s%s", name, level));
  }

  private static String getAnnotationName(SymbolMetadata.AnnotationInstance annotation) {
    String name = annotation.symbol().name();
    if (name.equals("Nonnull")) {
      return name + annotationArguments(annotation.values());
    }
    return name;
  }

  private static String annotationArguments(List<SymbolMetadata.AnnotationValue> valuesForAnnotation) {
    return valuesForAnnotation.stream()
      .filter(annotationValue -> "when".equals(annotationValue.name()))
      .map(SymbolMetadata.AnnotationValue::value)
      .filter(Symbol.class::isInstance)
      .map(symbol -> String.format("(when=%s)", ((Symbol) symbol).name()))
      .findFirst().orElse("");
  }

  private static String levelToString(SymbolMetadata.NullabilityLevel level) {
    switch (level) {
      case PACKAGE:
      case CLASS:
        return String.format(" at %s level", level.toString().toLowerCase(Locale.ROOT));
      case METHOD:
      case VARIABLE:
      default:
        return "";
    }
  }
}
