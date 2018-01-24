/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class NullableAnnotationUtils {

  private NullableAnnotationUtils() {
  }

  private static final String JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT = "javax.annotation.ParametersAreNonnullByDefault";
  private static final String ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT = "org.eclipse.jdt.annotation.NonNullByDefault";

  private static final Set<String> NULLABLE_ANNOTATIONS = ImmutableSet.of(
    "edu.umd.cs.findbugs.annotations.Nullable",
    "javax.annotation.CheckForNull",
    "javax.annotation.Nullable",
    "org.eclipse.jdt.annotation.Nullable",
    "org.jetbrains.annotations.Nullable");
  private static final Set<String> NONNULL_ANNOTATIONS = ImmutableSet.of(
    "android.support.annotation.NonNull",
    "edu.umd.cs.findbugs.annotations.NonNull",
    "javax.annotation.Nonnull",
    "javax.validation.constraints.NotNull",
    "lombok.NonNull",
    "org.eclipse.jdt.annotation.NonNull",
    "org.jetbrains.annotations.NotNull");

  public static boolean isAnnotatedNullable(Symbol symbol) {
    SymbolMetadata metadata = symbol.metadata();
    return NULLABLE_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }

  public static boolean isAnnotatedNonNull(Symbol symbol) {
    SymbolMetadata metadata = symbol.metadata();
    return NONNULL_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith) || isMethodAnnotatedWithEclipseNonNullReturnType(symbol);
  }

  private static boolean isMethodAnnotatedWithEclipseNonNullReturnType(Symbol symbol) {
    return symbol.isMethodSymbol() && isGloballyAnnotatedWithEclipseNonNullByDefault((Symbol.MethodSymbol) symbol, "RETURN_TYPE");
  }

  @CheckForNull
  public static String nonNullAnnotation(Symbol symbol) {
    SymbolMetadata metadata = symbol.metadata();
    Optional<String> result = NONNULL_ANNOTATIONS.stream().filter(metadata::isAnnotatedWith).findFirst();
    if (result.isPresent()) {
      return result.get();
    }
    if (isMethodAnnotatedWithEclipseNonNullReturnType(symbol)) {
      return ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT;
    }
    return null;
  }

  public static boolean isGloballyAnnotatedParameterNullable(Symbol.MethodSymbol method) {
    return valuesForGlobalAnnotation(method, "javax.annotation.ParametersAreNullableByDefault") != null;
  }

  public static boolean isGloballyAnnotatedParameterNonNull(Symbol.MethodSymbol method) {
    return nonNullAnnotationOnParameters(method) != null;
  }

  @CheckForNull
  public static String nonNullAnnotationOnParameters(Symbol.MethodSymbol method) {
    if (valuesForGlobalAnnotation(method, JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT) != null) {
      return JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT;
    }
    if (isGloballyAnnotatedWithEclipseNonNullByDefault(method, "PARAMETER")) {
      return ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT;
    }
    return null;
  }

  @CheckForNull
  private static List<SymbolMetadata.AnnotationValue> valuesForGlobalAnnotation(Symbol.MethodSymbol method, String annotation) {
    return Arrays.asList(method, method.enclosingClass(), ((JavaSymbol.MethodJavaSymbol) method).packge()).stream()
      .map(symbol -> symbol.metadata().valuesForAnnotation(annotation))
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

  private static boolean isGloballyAnnotatedWithEclipseNonNullByDefault(Symbol.MethodSymbol symbol, String parameter) {
    List<SymbolMetadata.AnnotationValue> annotationValues = valuesForGlobalAnnotation(symbol, ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT);
    if (annotationValues == null) {
      return false;
    }
    if (annotationValues.isEmpty()) {
      // annotation default value include parameters
      return true;
    }
    Object annotationValue = annotationValues.get(0).value();
    if (annotationValue instanceof Tree) {
      // from sources
      return containsEclipseDefaultLocation((Tree) annotationValue, parameter);
    }
    // from binaries
    return containsEclipseDefaultLocation((Object[]) annotationValue, parameter);
  }

  private static boolean containsEclipseDefaultLocation(Tree defaultLocation, String target) {
    Symbol symbol;
    switch (defaultLocation.kind()) {
      case IDENTIFIER:
        symbol = ((IdentifierTree) defaultLocation).symbol();
        break;
      case MEMBER_SELECT:
        symbol = ((MemberSelectExpressionTree) defaultLocation).identifier().symbol();
        break;
      case NEW_ARRAY:
        return ((NewArrayTree) defaultLocation).initializers().stream().anyMatch(expr -> containsEclipseDefaultLocation(expr, target));
      default:
        throw new IllegalArgumentException("Unexpected tree used to parameterize annotation");
    }
    return target.equals(symbol.name());
  }

  private static boolean containsEclipseDefaultLocation(Object[] defaultLocation, String target) {
    return Arrays.stream(defaultLocation).map(Symbol.class::cast).anyMatch(symbol -> target.equals(symbol.name()));
  }
}
