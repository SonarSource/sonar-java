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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

public final class SpringUtils {

  public static final String SPRING_BOOT_APP_ANNOTATION = "org.springframework.boot.autoconfigure.SpringBootApplication";
  public static final String CONTROLLER_ANNOTATION = "org.springframework.stereotype.Controller";
  public static final String COMPONENT_ANNOTATION = "org.springframework.stereotype.Component";
  public static final String REPOSITORY_ANNOTATION = "org.springframework.stereotype.Repository";
  public static final String SERVICE_ANNOTATION = "org.springframework.stereotype.Service";
  public static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";
  public static final String VALUE_ANNOTATION = "org.springframework.beans.factory.annotation.Value";
  public static final String TRANSACTIONAL_ANNOTATION = "org.springframework.transaction.annotation.Transactional";
  public static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";
  public static final String SCOPE_ANNOTATION = "org.springframework.context.annotation.Scope";
  public static final String CONFIGURATION_ANNOTATION = "org.springframework.context.annotation.Configuration";
  public static final String ASYNC_ANNOTATION = "org.springframework.scheduling.annotation.Async";
  public static final String DATA_REPOSITORY_ANNOTATION = "org.springframework.data.repository.Repository";
  public static final String REST_CONTROLLER_ANNOTATION = "org.springframework.web.bind.annotation.RestController";

  private SpringUtils() {
    // Utils class
  }

  public static boolean isScopeSingleton(SymbolMetadata clazzMeta) {
    List<SymbolMetadata.AnnotationValue> values = clazzMeta.valuesForAnnotation(SCOPE_ANNOTATION);
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
