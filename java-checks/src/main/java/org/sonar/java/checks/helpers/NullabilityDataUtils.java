/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
    if ("Nonnull".equals(name)) {
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
      case PACKAGE, CLASS:
        return String.format(" at %s level", level.toString().toLowerCase(Locale.ROOT));
      case METHOD, VARIABLE:
      default:
        return "";
    }
  }

}
