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
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityData;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;

final class JSymbolMetadata {

  private static final NullabilityData[] NO_ANNOTATION_NULLABILITY =
    forEachLevel(level -> new JNullabilityData(NullabilityType.NO_ANNOTATION, level, null, null, false));

  private static final NullabilityData[] UNKNOWN_NULLABILITY =
    forEachLevel(level -> new JNullabilityData(NullabilityType.UNKNOWN, level, null, null, false));

  private JSymbolMetadata() {
  }

  private static NullabilityData[] forEachLevel(Function<NullabilityLevel, NullabilityData> initializer) {
    return Arrays.stream(NullabilityLevel.values()).map(initializer).toArray(NullabilityData[]::new);
  }

  public static NullabilityData noNullabilityAnnotationAt(NullabilityLevel level) {
    return NO_ANNOTATION_NULLABILITY[level.ordinal()];
  }

  public static NullabilityData unknownNullabilityAt(NullabilityLevel level) {
    return UNKNOWN_NULLABILITY[level.ordinal()];
  }

  static final class JNullabilityData implements NullabilityData {

    private final NullabilityType type;
    private final NullabilityLevel level;

    @Nullable
    private final AnnotationInstance annotation;
    @Nullable
    private final AnnotationTree annotationTree;

    private final boolean metaAnnotation;

    public JNullabilityData(NullabilityType type, NullabilityLevel level, @Nullable AnnotationInstance annotation,
      @Nullable AnnotationTree annotationTree, boolean metaAnnotation) {
      this.type = type;
      this.level = level;
      this.annotation = annotation;
      this.annotationTree = annotationTree;
      this.metaAnnotation = metaAnnotation;
    }

    @Override
    public NullabilityType type() {
      return type;
    }

    @Override
    public NullabilityLevel level() {
      return level;
    }

    @Override
    public boolean metaAnnotation() {
      return metaAnnotation;
    }

    @Override
    public boolean isNonNull(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return testNullabilityType(minLevel, ignoreMetaAnnotation, defaultValue, t -> t == NullabilityType.NON_NULL);
    }

    @Override
    public boolean isNullable(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return testNullabilityType(minLevel, ignoreMetaAnnotation, defaultValue,
        t -> t == NullabilityType.STRONG_NULLABLE || t == NullabilityType.WEAK_NULLABLE);
    }

    @Override
    public boolean isStrongNullable(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return testNullabilityType(minLevel, ignoreMetaAnnotation, defaultValue, t -> t == NullabilityType.STRONG_NULLABLE);
    }

    private boolean testNullabilityType(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue, Predicate<NullabilityType> typePredicate) {
      if (type == NullabilityType.NO_ANNOTATION) {
        return false;
      } else if (type == NullabilityType.UNKNOWN || (ignoreMetaAnnotation && metaAnnotation)) {
        return defaultValue;
      } else if (typePredicate.test(type)) {
        return minLevel.ordinal() <= level.ordinal();
      }
      return false;
    }

    @Nullable
    @Override
    public AnnotationInstance annotation() {
      return annotation;
    }

    @Nullable
    @Override
    public Tree declaration() {
      return annotationTree;
    }
  }

}
