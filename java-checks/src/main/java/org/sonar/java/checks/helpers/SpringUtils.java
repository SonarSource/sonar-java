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
package org.sonar.java.checks.helpers;

import java.util.List;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

public final class SpringUtils {

  public static final String SPRING_SCOPE_ANNOTATION = "org.springframework.context.annotation.Scope";
  public static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";

  private SpringUtils() {
    // Utils class
  }

  public static boolean isScopeSingleton(SymbolMetadata clazzMeta) {
    List<SymbolMetadata.AnnotationValue> values = clazzMeta.valuesForAnnotation(SPRING_SCOPE_ANNOTATION);
    if (values == null) {
      // Scope is singleton by default
      return true;
    }
    for (SymbolMetadata.AnnotationValue annotationValue : values) {
      if ("value".equals(annotationValue.name()) || "scopeName".equals(annotationValue.name())) {
        Object value = annotationValue.value();
        if (value instanceof String stringValue && !"singleton".equals(stringValue)) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean isAutowired(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith(AUTOWIRED_ANNOTATION);
  }

}
