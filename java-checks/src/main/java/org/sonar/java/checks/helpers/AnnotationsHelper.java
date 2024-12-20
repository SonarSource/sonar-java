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

import org.sonar.plugins.java.api.semantic.SymbolMetadata;

public class AnnotationsHelper {

  private AnnotationsHelper() {
    // Helper class, should not be implemented.
  }

  public static boolean hasUnknownAnnotation(SymbolMetadata symbolMetadata) {
    return symbolMetadata.annotations().stream().anyMatch(annotation -> annotation.symbol().isUnknown());
  }

  /**
   * Returns the `name` part of a `fully.qualified.name`, that is, the part after the last dot.
   */
  public static String annotationTypeIdentifier(String fullyQualified) {
    return fullyQualified.substring(fullyQualified.lastIndexOf('.') + 1);
  }
}
