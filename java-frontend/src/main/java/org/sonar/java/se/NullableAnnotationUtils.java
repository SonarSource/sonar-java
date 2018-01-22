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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
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
    return isAnnotatedWithOneOf(symbol, NULLABLE_ANNOTATIONS);
  }

  public static boolean isAnnotatedNonNull(Symbol symbol) {
    return isAnnotatedWithOneOf(symbol, NONNULL_ANNOTATIONS)
      || (symbol.isMethodSymbol() && isGloballyAnnotatedWith((Symbol.MethodSymbol) symbol, ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT, "RETURN_TYPE"));
  }

  public static Optional<String> nonNullAnnotation(Symbol symbol) {
    return NONNULL_ANNOTATIONS.stream()
      .filter(annotation -> isAnnotatedWith(symbol, annotation))
      .findFirst();
  }

  public static boolean isAnnotatedWith(Symbol symbol, String annotation) {
    return symbol.metadata().isAnnotatedWith(annotation);
  }

  private static boolean isAnnotatedWithOneOf(Symbol symbol, Set<String> annotations) {
    return annotations.stream().anyMatch(annotation -> isAnnotatedWith(symbol, annotation));
  }

  public static boolean isGloballyAnnotatedParameterNonNull(Symbol.MethodSymbol method) {
    return parameterNonNullGlobalAnnotation(method) != null;
  }

  @CheckForNull
  public static String parameterNonNullGlobalAnnotation(Symbol.MethodSymbol method) {
    if (isGloballyAnnotatedWith(method, JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT)) {
      return JAVAX_ANNOTATION_PARAMETERS_ARE_NONNULL_BY_DEFAULT;
    }
    if (isGloballyAnnotatedWith(method, ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT, "PARAMETER")) {
      return ORG_ECLIPSE_JDT_ANNOTATION_NON_NULL_BY_DEFAULT;
    }
    return null;
  }

  public static boolean isGloballyAnnotatedParameterNullable(Symbol.MethodSymbol method) {
    return isGloballyAnnotatedWith(method, "javax.annotation.ParametersAreNullableByDefault");
  }

  @VisibleForTesting
  public static boolean isGloballyAnnotatedWith(Symbol.MethodSymbol method, String annotation) {
    return isAnnotatedWith(method, annotation)
      || isAnnotatedWith(method.enclosingClass(), annotation)
      || isAnnotatedWith(((JavaSymbol.MethodJavaSymbol) method).packge(), annotation);
  }

  private static boolean isGloballyAnnotatedWith(Symbol.MethodSymbol symbol, String annotation, String parameter) {
    List<AnnotationValue> parameters = valuesForGlobalAnnotation(symbol, annotation);
    if (parameters == null) {
      return false;
    }
    if (parameters.isEmpty()) {
      // annotation default value include parameters
      return true;
    }
    Object annotationValue = parameters.get(0).value();
    if (annotationValue instanceof Tree) {
      // from sources
      return containsDefaultLocation((Tree) annotationValue, parameter);
    }
    // from binaries
    return containsDefaultLocation((Object[]) annotationValue, parameter);
  }

  @VisibleForTesting
  @CheckForNull
  public static List<SymbolMetadata.AnnotationValue> valuesForGlobalAnnotation(Symbol.MethodSymbol method, String annotation) {
    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) method;
    if (isAnnotatedWith(methodSymbol, annotation)) {
      return methodSymbol.metadata().valuesForAnnotation(annotation);
    }
    JavaSymbol.TypeJavaSymbol enclosingClassSymbol = methodSymbol.enclosingClass();
    if (isAnnotatedWith(enclosingClassSymbol, annotation)) {
      return enclosingClassSymbol.metadata().valuesForAnnotation(annotation);
    }
    JavaSymbol.PackageJavaSymbol packageSymbol = methodSymbol.packge();
    if (isAnnotatedWith(packageSymbol, annotation)) {
      return packageSymbol.metadata().valuesForAnnotation(annotation);
    }
    return null;
  }

  private static boolean containsDefaultLocation(Tree defaultLocation, String target) {
    Symbol symbol;
    switch (defaultLocation.kind()) {
      case IDENTIFIER:
        symbol = ((IdentifierTree) defaultLocation).symbol();
        break;
      case MEMBER_SELECT:
        symbol = ((MemberSelectExpressionTree) defaultLocation).identifier().symbol();
        break;
      case NEW_ARRAY:
        return ((NewArrayTree) defaultLocation).initializers().stream().anyMatch(expr -> containsDefaultLocation(expr, target));
      default:
        throw new IllegalArgumentException("Unexpected tree used to parameterize annotation");
    }
    return target.equals(symbol.name());
  }

  private static boolean containsDefaultLocation(Object[] defaultLocation, String target) {
    return Arrays.stream(defaultLocation).map(Symbol.class::cast).anyMatch(symbol -> target.equals(symbol.name()));
  }

}
