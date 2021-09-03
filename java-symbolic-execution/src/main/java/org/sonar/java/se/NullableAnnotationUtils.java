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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;

public final class NullableAnnotationUtils {

  private NullableAnnotationUtils() {
  }

  private static final String JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT = "javax.annotation.ParametersAreNonnullByDefault";
  private static final String ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT = "org.eclipse.jdt.annotation.NonNullByDefault";
  private static final String ORG_SPRINGFRAMEWORK_LANG_NON_NULL_API = "org.springframework.lang.NonNullApi";
  private static final String ORG_SPRINGFRAMEWORK_LANG_NON_NULL_FIELDS = "org.springframework.lang.NonNullFields";
  private static final String COM_MONGO_DB_LANG_NON_NULL_API = "com.mongodb.lang.NonNullApi";

  /**
   * Nullable annotations can be "strong", when one must check for nullness, or "weak", when it
   * can be null, but it may be fine to not check it.
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
    // For any variable who's type is annotated with @Nullable [...] It is illegal to dereference such a variable for either field or method access.
    "org.eclipse.jdt.annotation.Nullable",
    "org.eclipse.jgit.annotations.Nullable");


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

  private static final Set<String> NULLABLE_ANNOTATIONS = Collections.unmodifiableSet(
    Stream.of(STRONG_NULLABLE_ANNOTATIONS, WEAK_NULLABLE_ANNOTATIONS)
      .flatMap(Set::stream)
      .collect(Collectors.toSet()));

  private static final Set<String> NONNULL_ANNOTATIONS = SetUtils.immutableSetOf(
    "android.annotation.NonNull",
    "android.support.annotation.NonNull",
    "androidx.annotation.NonNull",
    "com.sun.istack.internal.NotNull",
    "com.mongodb.lang.NonNull",
    "edu.umd.cs.findbugs.annotations.NonNull",
    "io.reactivex.annotations.NonNull",
    "io.reactivex.rxjava3.annotations.NonNull",
    "javax.annotation.Nonnull",
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

  public static Optional<AnnotationTree> nullableAnnotation(ModifiersTree modifiers) {
    return modifiers.annotations().stream().filter(NullableAnnotationUtils::isNullableAnnotation).findFirst();
  }

  public static Optional<AnnotationTree> nonNullAnnotation(ModifiersTree modifiers) {
    return modifiers.annotations().stream().filter(NullableAnnotationUtils::isNonNullAnnotation).findFirst();
  }

  private static boolean isNullableAnnotation(AnnotationTree annotation) {
    return isNullableAnnotation(annotation.annotationType().symbolType());
  }

  private static boolean isNonNullAnnotation(AnnotationTree annotation) {
    return isNonNullAnnotation(annotation.symbolType()) && annotation.arguments().isEmpty();
  }

  private static boolean isNonNullAnnotation(Type type) {
    return NONNULL_ANNOTATIONS.contains(type.fullyQualifiedName());
  }

  private static boolean isNullableAnnotation(Type type) {
    return NULLABLE_ANNOTATIONS.contains(type.fullyQualifiedName());
  }

  private static boolean isStrongNullableAnnotation(Type type) {
    return STRONG_NULLABLE_ANNOTATIONS.contains(type.fullyQualifiedName());
  }

  public static boolean isAnnotatedNullable(SymbolMetadata metadata) {
    return isUsingNullable(metadata)
      || collectMetaAnnotations(metadata).stream().map(Symbol::metadata).anyMatch(NullableAnnotationUtils::isUsingNullable);
  }

  public static boolean isAnnotatedNonNull(Symbol symbol) {
    if (isAnnotatedNullable(symbol.metadata())) {
      return false;
    }
    return isUsingNonNull(symbol) || collectMetaAnnotations(symbol.metadata()).stream().anyMatch(NullableAnnotationUtils::isUsingNonNull);
  }

  public static boolean isAnnotatedWithStrongNullness(SymbolMetadata metadata) {
    return metadata.annotations().stream()
      .map(AnnotationInstance::symbol)
      .map(Symbol::type)
      .anyMatch(NullableAnnotationUtils::isStrongNullableAnnotation);
  }

  private static boolean isUsingNullable(SymbolMetadata metadata) {
    return metadata.annotations().stream()
      .anyMatch(annotation -> isNullableAnnotation(annotationType(annotation)) || isNullableThroughNonNull(annotation));
  }

  private static boolean isNullableThroughNonNull(AnnotationInstance annotation) {
    return "javax.annotation.Nonnull".equals(annotationType(annotation).fullyQualifiedName()) &&
      !annotation.values().isEmpty() &&
      (checkAnnotationParameter(annotation.values(), "when", "MAYBE") ||
        checkAnnotationParameter(annotation.values(), "when", "UNKNOWN"));
  }

  private static boolean checkAnnotationParameter(List<SymbolMetadata.AnnotationValue> valuesForAnnotation, String fieldName, String expectedValue) {
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

  private static boolean isUsingNonNull(Symbol symbol) {
    SymbolMetadata metadata = symbol.metadata();
    for (AnnotationInstance annotation : metadata.annotations()) {
      if (isNullableThroughNonNull(annotation)) {
        return false;
      }
      if (isNonNullAnnotation(annotationType(annotation))) {
        return true;
      }
    }
    return nonNullReturnTypeAnnotation(symbol) != null || nonNullFieldAnnotation(symbol) != null;
  }

  private static Type annotationType(AnnotationInstance annotation) {
    return annotation.symbol().type();
  }

  @CheckForNull
  private static String nonNullFieldAnnotation(Symbol symbol) {
    if (symbol.isVariableSymbol() && symbol.owner().isTypeSymbol() && !isUsingNullable(symbol.metadata())) {
      if (valuesForGlobalAnnotation(symbol, ORG_SPRINGFRAMEWORK_LANG_NON_NULL_FIELDS) != null) {
        return ORG_SPRINGFRAMEWORK_LANG_NON_NULL_FIELDS;
      }

      if (isGloballyAnnotatedWithEclipseNonNullByDefault(symbol, "FIELD")) {
        return ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT;
      }
    }
    return null;
  }

  @CheckForNull
  private static String nonNullReturnTypeAnnotation(Symbol symbol) {
    if (symbol.isMethodSymbol() && !isUsingNullable(symbol.metadata())) {
      Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
      if (isGloballyAnnotatedWithEclipseNonNullByDefault(methodSymbol, "RETURN_TYPE")) {
        return ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT;
      }
      return useAnyGlobalAnnotation(methodSymbol, ORG_SPRINGFRAMEWORK_LANG_NON_NULL_API, COM_MONGO_DB_LANG_NON_NULL_API);
    }
    return null;
  }

  @CheckForNull
  public static String nonNullAnnotation(SymbolMetadata metadata) {
    if (isAnnotatedNullable(metadata)) {
      return null;
    }
    return findFirst(NullableAnnotationUtils::isNonNullAnnotation, metadata)
      .map(NullableAnnotationUtils::annotationType)
      .map(Type::fullyQualifiedName)
      .orElse(null);
  }

  @CheckForNull
  public static String nonNullAnnotation(Symbol symbol) {
    String nonNullAnnotation = nonNullAnnotation(symbol.metadata());
    if (nonNullAnnotation != null) {
      return nonNullAnnotation;
    }
    String nonNullReturnAnnotation = nonNullReturnTypeAnnotation(symbol);
    if (nonNullReturnAnnotation != null) {
      return nonNullReturnAnnotation;
    }
    return nonNullFieldAnnotation(symbol);
  }

  private static Optional<AnnotationInstance> findFirst(Predicate<Type> filter, SymbolMetadata metadata) {
    return metadata.annotations().stream()
      .filter(annotation -> filter.test(annotationType(annotation)))
      .findFirst();
  }

  public static boolean isGloballyAnnotatedParameterNullable(Symbol.MethodSymbol method) {
    return valuesForGlobalAnnotation(method, "javax.annotation.ParametersAreNullableByDefault") != null;
  }

  public static boolean isGloballyAnnotatedParameterNonNull(Symbol.MethodSymbol method) {
    return nonNullAnnotationOnParameters(method) != null;
  }

  @CheckForNull
  public static String nonNullAnnotationOnParameters(Symbol.MethodSymbol method) {
    if (isGloballyAnnotatedWithEclipseNonNullByDefault(method, "PARAMETER")) {
      return ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT;
    }
    return useAnyGlobalAnnotation(method,
      JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT,
      ORG_SPRINGFRAMEWORK_LANG_NON_NULL_API,
      COM_MONGO_DB_LANG_NON_NULL_API);
  }

  @CheckForNull
  private static String useAnyGlobalAnnotation(Symbol symbol, String ... annotations) {
    return Stream.of(annotations)
      .filter(annotation -> valuesForGlobalAnnotation(symbol, annotation) != null)
      .findFirst()
      .orElse(null);
  }

  @CheckForNull
  private static List<SymbolMetadata.AnnotationValue> valuesForGlobalAnnotation(Symbol symbol, String annotation) {
    return Stream.of(symbol, symbol.enclosingClass(), JUtils.getPackage(symbol))
      .map(s -> s.metadata().valuesForAnnotation(annotation))
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

  private static boolean isGloballyAnnotatedWithEclipseNonNullByDefault(Symbol symbol, String parameter) {
    List<SymbolMetadata.AnnotationValue> valuesForGlobalAnnotation = valuesForGlobalAnnotation(symbol, ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT);
    if (valuesForGlobalAnnotation == null) {
      return false;
    }
    return valuesForGlobalAnnotation.isEmpty() || checkAnnotationParameter(valuesForGlobalAnnotation, "value", parameter);
  }

  private static ArrayList<Symbol> collectMetaAnnotations(SymbolMetadata metadata) {
    return collectMetaAnnotations(metadata, new HashSet<>());
  }

  private static ArrayList<Symbol> collectMetaAnnotations(SymbolMetadata metadata, Set<Type> knownTypes) {
    List<Symbol> result = new ArrayList<>();
    for (AnnotationInstance annotationInstance : metadata.annotations()) {
      Symbol annotationSymbol = annotationInstance.symbol();
      Type annotationType = annotationSymbol.type();
      if (!knownTypes.contains(annotationType)) {
        knownTypes.add(annotationType);
        result.add(annotationSymbol);
        result.addAll(
          collectMetaAnnotations(annotationSymbol.metadata(), knownTypes)
        );
      }
    }
    return new ArrayList<>(result);
  }

}
