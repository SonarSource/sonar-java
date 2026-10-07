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
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.Tree;

public final class Nullability {

  private static final SymbolMetadata.NullabilityData[] NO_ANNOTATION =
    Arrays.stream(SymbolMetadata.NullabilityLevel.values())
      .map(level -> new Data(SymbolMetadata.NullabilityType.NO_ANNOTATION, level, null, null, false))
      .toArray(SymbolMetadata.NullabilityData[]::new);

  private static final SymbolMetadata.NullabilityData[] UNKNOWN =
    Arrays.stream(SymbolMetadata.NullabilityLevel.values())
      .map(level -> new Data(SymbolMetadata.NullabilityType.UNKNOWN, level, null, null, false))
      .toArray(SymbolMetadata.NullabilityData[]::new);

  private Nullability() {
  }

  public static SymbolMetadata.NullabilityData noAnnotationAt(SymbolMetadata.NullabilityLevel level) {
    return NO_ANNOTATION[level.ordinal()];
  }

  public static SymbolMetadata.NullabilityData unknownAt(SymbolMetadata.NullabilityLevel level) {
    return UNKNOWN[level.ordinal()];
  }

  public record Data(SymbolMetadata.NullabilityType type, SymbolMetadata.NullabilityLevel level,
                     @Nullable SymbolMetadata.AnnotationInstance annotation, @Nullable Tree declaration,
                     boolean metaAnnotation) implements SymbolMetadata.NullabilityData {

    @Override
    public boolean isNonNull(SymbolMetadata.NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return test(minLevel, ignoreMetaAnnotation, defaultValue, type -> type == SymbolMetadata.NullabilityType.NON_NULL);
    }

    @Override
    public boolean isNullable(SymbolMetadata.NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return test(minLevel, ignoreMetaAnnotation, defaultValue,
        type -> type == SymbolMetadata.NullabilityType.STRONG_NULLABLE || type == SymbolMetadata.NullabilityType.WEAK_NULLABLE);
    }

    @Override
    public boolean isStrongNullable(SymbolMetadata.NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return test(minLevel, ignoreMetaAnnotation, defaultValue, type -> type == SymbolMetadata.NullabilityType.STRONG_NULLABLE);
    }

    private boolean test(SymbolMetadata.NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue,
                         Predicate<SymbolMetadata.NullabilityType> predicate) {
      if (type == SymbolMetadata.NullabilityType.NO_ANNOTATION) {
        return false;
      }
      if (type == SymbolMetadata.NullabilityType.UNKNOWN || (ignoreMetaAnnotation && metaAnnotation)) {
        return defaultValue;
      }
      return predicate.test(type) && minLevel.ordinal() <= level.ordinal();
    }
  }
}
