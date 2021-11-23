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
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityData;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityTarget;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.analyzer.commons.collections.SetUtils;

import static org.sonar.java.model.JSymbolMetadata.noNullabilityAnnotationAt;
import static org.sonar.java.model.JSymbolMetadata.unknownNullabilityAt;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.CLASS;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.VARIABLE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityTarget.FIELD;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityTarget.LOCAL_VARIABLE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityTarget.METHOD;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityTarget.PARAMETER;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.NON_NULL;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.NO_ANNOTATION;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.STRONG_NULLABLE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.UNKNOWN;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.WEAK_NULLABLE;

public class JSymbolMetadataNullabilityHelper {

  private JSymbolMetadataNullabilityHelper() {
    // Utility class
  }

  /**
   * List of "strong" Nullable annotations, when something must be checked for nullness.
   */
  private static final Set<String> STRONG_NULLABLE_ANNOTATIONS = SetUtils.immutableSetOf(
    "javax.annotation.CheckForNull",
    "edu.umd.cs.findbugs.annotations.CheckForNull",
    "org.netbeans.api.annotations.common.CheckForNull",
    // Despite the name, some Nullable annotations are meant to be used as CheckForNull
    // as they are using meta-annotation from javax: @Nonnull(When.MAYBE), same as javax @CheckForNull.
    "org.springframework.lang.Nullable",
    "reactor.util.annotation.Nullable",
    // From the documentation (https://wiki.eclipse.org/JDT_Core/Null_Analysis):
    // For any variable whose type is annotated with @Nullable [...] It is illegal to dereference such a variable for either field or method access.
    "org.eclipse.jdt.annotation.Nullable",
    "org.eclipse.jgit.annotations.Nullable");

  /**
   * List of "weak" annotations, when something can be null, but it may be fine to not check it.
   */
  private static final Set<String> WEAK_NULLABLE_ANNOTATIONS = SetUtils.immutableSetOf(
    "android.annotation.Nullable",
    "android.support.annotation.Nullable",
    "androidx.annotation.Nullable",
    "com.sun.istack.internal.Nullable",
    "com.mongodb.lang.Nullable",
    "edu.umd.cs.findbugs.annotations.Nullable",
    "io.reactivex.annotations.Nullable",
    "io.reactivex.rxjava3.annotations.Nullable",
    "javax.annotation.Nullable",
    "org.checkerframework.checker.nullness.compatqual.NullableDecl",
    "org.checkerframework.checker.nullness.compatqual.NullableType",
    "org.checkerframework.checker.nullness.qual.Nullable",
    "org.jetbrains.annotations.Nullable",
    "org.jmlspecs.annotation.Nullable",
    "org.netbeans.api.annotations.common.NullAllowed",
    "org.netbeans.api.annotations.common.NullUnknown");

  /**
   * Nullable annotations is the combination of weak and strong, when something can be null at one point.
   */
  private static final Set<String> NULLABLE_ANNOTATIONS = Collections.unmodifiableSet(
    Stream.of(STRONG_NULLABLE_ANNOTATIONS, WEAK_NULLABLE_ANNOTATIONS)
      .flatMap(Set::stream)
      .collect(Collectors.toSet()));

  /**
   * List of non-null annotation, when something should never be null.
   */
  private static final Set<String> NONNULL_ANNOTATIONS = SetUtils.immutableSetOf(
    "android.annotation.NonNull",
    "android.support.annotation.NonNull",
    "androidx.annotation.NonNull",
    "com.sun.istack.internal.NotNull",
    "com.mongodb.lang.NonNull",
    "edu.umd.cs.findbugs.annotations.NonNull",
    "io.reactivex.annotations.NonNull",
    "io.reactivex.rxjava3.annotations.NonNull",
    "javax.validation.constraints.NotNull",
    "lombok.NonNull",
    "org.checkerframework.checker.nullness.compatqual.NonNullDecl",
    "org.checkerframework.checker.nullness.compatqual.NonNullType",
    "org.checkerframework.checker.nullness.qual.NonNull",
    "org.eclipse.jdt.annotation.NonNull",
    "org.eclipse.jgit.annotations.NonNull",
    "org.jetbrains.annotations.NotNull",
    "org.jmlspecs.annotation.NonNull",
    "org.netbeans.api.annotations.common.NonNull",
    "org.springframework.lang.NonNull",
    "reactor.util.annotation.NonNull");

  /**
   * Can have different type depending on the argument "when" value:
   * - ALWAYS or no argument = NONNULL
   * - NEVER or MAYBE = STRONG_NULLABLE
   * - UNKNOWN = WEAK_NULLABLE
   */
  private static final String JAVAX_ANNOTATION_NONNULL = "javax.annotation.Nonnull";

  /**
   * Target parameters and return values.
   * Only applicable to package.
   */
  private static final String COM_MONGO_DB_LANG_NON_NULL_API = "com.mongodb.lang.NonNullApi";

  /**
   * Target parameters and return values.
   * Only applicable to package.
   */
  private static final String ORG_SPRINGFRAMEWORK_LANG_NON_NULL_API = "org.springframework.lang.NonNullApi";

  /**
   * Target parameters only.
   */
  private static final String JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT = "javax.annotation.ParametersAreNonnullByDefault";

  /**
   * Target parameters only.
   */
  private static final String JAVAX_ANNOTATION_PARAMETERS_ARE_NULLABLE_BY_DEFAULT = "javax.annotation.ParametersAreNullableByDefault";

  /**
   * Target fields only.
   * Only at package level.
   */
  private static final String ORG_SPRINGFRAMEWORK_LANG_NON_NULL_FIELDS = "org.springframework.lang.NonNullFields";

  /**
   * Can have parameters, setting what should be considered as NonNull.
   * PARAMETER, RETURN_TYPE, FIELD
   */
  private static final String ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT = "org.eclipse.jdt.annotation.NonNullByDefault";

  private static final Set<String> KNOWN_ANNOTATIONS = Stream.of(NULLABLE_ANNOTATIONS, NONNULL_ANNOTATIONS)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());

  private static final Map<ConfigurationKey, TypesForAnnotations> configuration = new HashMap<>();

  static {
    // Low level annotation (directly annotated)
    configureAnnotation(JSymbolMetadataNullabilityHelper::getIfStrongNullable,
      Arrays.asList(PARAMETER, FIELD, LOCAL_VARIABLE), Collections.singletonList(VARIABLE));
    configureAnnotation(JSymbolMetadataNullabilityHelper::getIfStrongNullable,
      Collections.singletonList(METHOD), Collections.singletonList(NullabilityLevel.METHOD));

    configureAnnotation(JSymbolMetadataNullabilityHelper::getIfNullable,
      Arrays.asList(PARAMETER, FIELD, LOCAL_VARIABLE), Collections.singletonList(VARIABLE));
    configureAnnotation(JSymbolMetadataNullabilityHelper::getIfNullable,
      Collections.singletonList(METHOD), Collections.singletonList(NullabilityLevel.METHOD));

    configureAnnotation(JSymbolMetadataNullabilityHelper::getIfNonNull,
      Arrays.asList(PARAMETER, FIELD, LOCAL_VARIABLE), Collections.singletonList(VARIABLE));
    configureAnnotation(JSymbolMetadataNullabilityHelper::getIfNonNull,
      Collections.singletonList(METHOD), Collections.singletonList(NullabilityLevel.METHOD));

    // Low level: javax.NonNull specific case
    configureAnnotation(JSymbolMetadataNullabilityHelper::getTypeFromNonNull,
      Arrays.asList(PARAMETER, FIELD, LOCAL_VARIABLE), Collections.singletonList(VARIABLE));
    configureAnnotation(JSymbolMetadataNullabilityHelper::getTypeFromNonNull,
      Collections.singletonList(METHOD), Collections.singletonList(NullabilityLevel.METHOD));

    // High level annotation
    configureAnnotation(COM_MONGO_DB_LANG_NON_NULL_API, NON_NULL,
      Arrays.asList(METHOD, PARAMETER), Collections.singletonList(PACKAGE));
    configureAnnotation(ORG_SPRINGFRAMEWORK_LANG_NON_NULL_API, NON_NULL,
      Arrays.asList(METHOD, PARAMETER), Collections.singletonList(PACKAGE));

    configureAnnotation(JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT, NON_NULL,
      Collections.singletonList(PARAMETER), Arrays.asList(NullabilityLevel.METHOD, CLASS, PACKAGE));
    configureAnnotation(JAVAX_ANNOTATION_PARAMETERS_ARE_NULLABLE_BY_DEFAULT, WEAK_NULLABLE,
      Collections.singletonList(PARAMETER), Arrays.asList(NullabilityLevel.METHOD, CLASS, PACKAGE));

    configureAnnotation(ORG_SPRINGFRAMEWORK_LANG_NON_NULL_FIELDS, NON_NULL,
      Collections.singletonList(FIELD), Collections.singletonList(PACKAGE));

    // ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT specific case (targeting both high and low level)
    configureAnnotation(annotationInstance -> getIfEclipseNonNullByDefault(annotationInstance, "PARAMETER"),
      Collections.singletonList(PARAMETER), Arrays.asList(VARIABLE, NullabilityLevel.METHOD, CLASS, PACKAGE));
    configureAnnotation(annotationInstance -> getIfEclipseNonNullByDefault(annotationInstance, "FIELD"),
      Collections.singletonList(FIELD), Arrays.asList(VARIABLE, NullabilityLevel.METHOD, CLASS, PACKAGE));
    configureAnnotation(annotationInstance -> getIfEclipseNonNullByDefault(annotationInstance, "RETURN_TYPE"),
      Collections.singletonList(METHOD), Arrays.asList(NullabilityLevel.METHOD, CLASS, PACKAGE));

    // Add all annotations to the set of known annotations
    KNOWN_ANNOTATIONS.add(JAVAX_ANNOTATION_NONNULL);
    KNOWN_ANNOTATIONS.add(COM_MONGO_DB_LANG_NON_NULL_API);
    KNOWN_ANNOTATIONS.add(ORG_SPRINGFRAMEWORK_LANG_NON_NULL_API);
    KNOWN_ANNOTATIONS.add(JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT);
    KNOWN_ANNOTATIONS.add(JAVAX_ANNOTATION_PARAMETERS_ARE_NULLABLE_BY_DEFAULT);
    KNOWN_ANNOTATIONS.add(ORG_SPRINGFRAMEWORK_LANG_NON_NULL_FIELDS);
    KNOWN_ANNOTATIONS.add(ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT);
  }

  private static void configureAnnotation(String name, NullabilityType type, List<NullabilityTarget> targets, List<NullabilityLevel> levels) {
    configureAnnotation(annotation -> annotationType(annotation).fullyQualifiedName().equals(name) ? type : NO_ANNOTATION, targets, levels);
  }

  private static void configureAnnotation(Function<AnnotationInstance, NullabilityType> typeFromAnnotation, List<NullabilityTarget> targets, List<NullabilityLevel> levels) {
    for (NullabilityTarget target : targets) {
      for (NullabilityLevel level : levels) {
        ConfigurationKey key = new ConfigurationKey(target, level);
        configuration.computeIfAbsent(key, k -> new TypesForAnnotations()).add(typeFromAnnotation);
      }
    }
  }

  /**
   * Return the Nullability data given the metadata of the current symbol, a level and a target.
   */
  public static NullabilityData getNullabilityDataAtLevel(SymbolMetadata metadata, NullabilityTarget target, NullabilityLevel level) {
    TypesForAnnotations typeForAnnotations = configuration.get(new ConfigurationKey(target, level));
    if (typeForAnnotations != null) {
      return getNullabilityDataAtLevel(new HashSet<>(), metadata, level, false, typeForAnnotations);
    }
    return noNullabilityAnnotationAt(level);
  }

  private static NullabilityData getNullabilityDataAtLevel(Set<Type> knownTypes, SymbolMetadata metadata,
    NullabilityLevel level, boolean isMetaAnnotated, TypesForAnnotations typeForAnnotations) {
    // Check if the symbol is directly annotated
    NullabilityData directlyAnnotated = getNullabilityData(metadata, level, isMetaAnnotated, typeForAnnotations);
    if (directlyAnnotated.type() != NO_ANNOTATION) {
      return directlyAnnotated;
    }
    for (AnnotationInstance annotationInstance : metadata.annotations()) {
      Symbol annotationSymbol = annotationInstance.symbol();
      Type annotationType = annotationSymbol.type();
      if (knownTypes.add(annotationType) && !KNOWN_ANNOTATIONS.contains(annotationType(annotationInstance).fullyQualifiedName())) {
        // Only do recursion when we face unknown annotations, as we already know the nullability impact and might contain contradicting
        // annotations.
        NullabilityData nullabilityData = getNullabilityDataAtLevel(knownTypes, annotationSymbol.metadata(), level,
          true, typeForAnnotations);
        if (nullabilityData.type() != NO_ANNOTATION) {
          return nullabilityData;
        }
      }
    }
    return noNullabilityAnnotationAt(level);
  }

  private static NullabilityData getNullabilityData(SymbolMetadata metadata,
    NullabilityLevel level,
    boolean isMetaAnnotated,
    TypesForAnnotations typeForAnnotations) {
    NullabilityType nullabilityType = NullabilityType.NO_ANNOTATION;
    AnnotationInstance annotationInstance = null;
    for (AnnotationInstance annotation : metadata.annotations()) {
      NullabilityType typeFromAnnotation = typeForAnnotations.getTypeFromAnnotation(annotation);
      if (typeFromAnnotation.ordinal() > nullabilityType.ordinal()) {
        nullabilityType = typeFromAnnotation;
        annotationInstance = annotation;
      }
    }
    if (nullabilityType == UNKNOWN) {
      return unknownNullabilityAt(level);
    } else if (annotationInstance == null) {
      return noNullabilityAnnotationAt(level);
    }
    return new JSymbolMetadata.JNullabilityData(nullabilityType, level,
      annotationInstance, metadata.findAnnotationTree(annotationInstance), isMetaAnnotated);
  }

  private static NullabilityType getIfStrongNullable(AnnotationInstance annotation) {
    if (isStrongNullableAnnotation(annotationType(annotation))) {
      return STRONG_NULLABLE;
    }
    return NO_ANNOTATION;
  }

  private static boolean isStrongNullableAnnotation(Type type) {
    return STRONG_NULLABLE_ANNOTATIONS.contains(type.fullyQualifiedName());
  }

  private static NullabilityType getIfNullable(AnnotationInstance annotation) {
    if (isNullableAnnotation(annotationType(annotation))) {
      return WEAK_NULLABLE;
    }
    return NO_ANNOTATION;
  }

  private static boolean isNullableAnnotation(Type type) {
    return NULLABLE_ANNOTATIONS.contains(type.fullyQualifiedName());
  }

  private static NullabilityType getIfNonNull(AnnotationInstance annotation) {
    if (isNonNullAnnotation(annotationType(annotation))) {
      return annotation.values().isEmpty() ? NON_NULL : UNKNOWN;
    }
    return NO_ANNOTATION;
  }

  private static boolean isNonNullAnnotation(Type type) {
    return NONNULL_ANNOTATIONS.contains(type.fullyQualifiedName());
  }

  private static NullabilityType getTypeFromNonNull(AnnotationInstance annotation) {
    if (JAVAX_ANNOTATION_NONNULL.equals(annotationType(annotation).fullyQualifiedName())) {
      List<AnnotationValue> values = annotation.values();
      if (values.isEmpty() || checkAnnotationParameter(values, "when", "ALWAYS")) {
        return NON_NULL;
      } else if (checkAnnotationParameter(values, "when", "UNKNOWN")) {
        return WEAK_NULLABLE;
      } else {
        // when=NEVER or when=MAYBE
        return STRONG_NULLABLE;
      }
    }
    return NO_ANNOTATION;
  }

  private static NullabilityType getIfEclipseNonNullByDefault(AnnotationInstance annotation, String expectedValue) {
    if (ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT.equals(annotationType(annotation).fullyQualifiedName())) {
      return (annotation.values().isEmpty() || checkAnnotationParameter(annotation.values(), "value", expectedValue)) ? NON_NULL : NO_ANNOTATION;
    }
    return NO_ANNOTATION;
  }

  private static Type annotationType(AnnotationInstance annotation) {
    return annotation.symbol().type();
  }

  private static boolean checkAnnotationParameter(List<AnnotationValue> valuesForAnnotation, String fieldName, String expectedValue) {
    return valuesForAnnotation.stream()
      .filter(annotationValue -> fieldName.equals(annotationValue.name()))
      .anyMatch(annotationValue -> isExpectedValue(annotationValue.value(), expectedValue));
  }

  private static boolean isExpectedValue(Object annotationValue, String expectedValue) {
    if (annotationValue instanceof Object[]) {
      return containsValue((Object[]) annotationValue, expectedValue);
    }
    return annotationValue instanceof Symbol && expectedValue.equals(((Symbol) annotationValue).name());
  }

  private static boolean containsValue(Object[] annotationValue, String expectedValue) {
    return Arrays.stream(annotationValue).map(Symbol.class::cast).anyMatch(symbol -> expectedValue.equals(symbol.name()));
  }

  private static class ConfigurationKey {
    private final NullabilityTarget target;
    private final NullabilityLevel level;

    ConfigurationKey(NullabilityTarget target, NullabilityLevel level) {
      this.target = target;
      this.level = level;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ConfigurationKey that = (ConfigurationKey) o;

      if (target != that.target) return false;
      return level == that.level;
    }

    @Override
    public int hashCode() {
      int result = target.hashCode();
      result = 31 * result + level.hashCode();
      return result;
    }
  }

  private static class TypesForAnnotations extends ArrayList<Function<AnnotationInstance, NullabilityType>> {

    private NullabilityType getTypeFromAnnotation(AnnotationInstance annotation) {
      if (annotation.symbol().isUnknown()) {
        return NullabilityType.UNKNOWN;
      }
      for (Function<AnnotationInstance, NullabilityType> typeForAnnotation : this) {
        NullabilityType type = typeForAnnotation.apply(annotation);
        if (type != NullabilityType.NO_ANNOTATION) {
          return type;
        }
      }
      return NullabilityType.NO_ANNOTATION;
    }

  }

}
