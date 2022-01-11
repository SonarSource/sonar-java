/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.util.List;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

public final class SpringUtils {

  public static final String SPRING_SCOPE_ANNOTATION = "org.springframework.context.annotation.Scope";

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
        String stringValue = null;
        if (value instanceof String) {
          stringValue = (String) value;
        }
        if (stringValue != null && !"singleton".equals(stringValue)) {
          return false;
        }
      }
    }
    return true;
  }

}
