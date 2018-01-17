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
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class NullableAnnotationUtils {

  private NullableAnnotationUtils() {
  }

  public static final Set<String> NULLABLE_ANNOTATIONS = ImmutableSet.of(
    "edu.umd.cs.findbugs.annotations.Nullable",
    "javax.annotation.CheckForNull",
    "javax.annotation.Nullable",
    "org.eclipse.jdt.annotation.Nullable",
    "org.jetbrains.annotations.Nullable");
  public static final Set<String> NONNULL_ANNOTATIONS = ImmutableSet.of(
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
      || (symbol.isMethodSymbol() && usesEclipseNonNullByDefault((Symbol.MethodSymbol) symbol, "RETURN_TYPE"));
  }

  public static boolean isAnnotatedWith(Symbol symbol, String annotation) {
    return symbol.metadata().isAnnotatedWith(annotation);
  }

  private static boolean isAnnotatedWithOneOf(Symbol symbol, Set<String> annotations) {
    return annotations.stream().anyMatch(annotation -> isAnnotatedWith(symbol, annotation));
  }

  public static boolean isGloballyAnnotatedParameterNonNull(MethodTree methodTree) {
    return isGloballyAnnotatedWith(methodTree, "javax.annotation.ParametersAreNonnullByDefault")
      || usesEclipseNonNullByDefault(methodTree.symbol(), "PARAMETER");
  }

  public static boolean isGloballyAnnotatedParameterNullable(MethodTree methodTree) {
    return isGloballyAnnotatedWith(methodTree, "javax.annotation.ParametersAreNullableByDefault");
  }

  public static boolean isGloballyAnnotatedWith(MethodTree methodTree, String annotation) {
    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) methodTree.symbol();
    return isAnnotatedWith(methodSymbol, annotation)
      || isAnnotatedWith(methodSymbol.enclosingClass(), annotation)
      || isAnnotatedWith(methodSymbol.packge(), annotation);
  }

  private static boolean usesEclipseNonNullByDefault(Symbol.MethodSymbol symbol, String target) {
    List<AnnotationValue> parameters = valuesForGlobalAnnotation(symbol, "org.eclipse.jdt.annotation.NonNullByDefault");
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
      return containsDefaultLocation((Tree) annotationValue, target);
    }
    // from binaries
    return containsDefaultLocation((Object[]) annotationValue, target);
  }

  @CheckForNull
  public static List<SymbolMetadata.AnnotationValue> valuesForGlobalAnnotation(Symbol.MethodSymbol method, String annotation) {
    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) method;
    if (isAnnotatedWith(methodSymbol, annotation)) {
      return valuesForAnnotation(methodSymbol, annotation);
    }
    JavaSymbol.TypeJavaSymbol enclosingClassSymbol = methodSymbol.enclosingClass();
    if (isAnnotatedWith(enclosingClassSymbol, annotation)) {
      return valuesForAnnotation(enclosingClassSymbol, annotation);
    }
    JavaSymbol.PackageJavaSymbol packageSymbol = methodSymbol.packge();
    if (isAnnotatedWith(packageSymbol, annotation)) {
      return valuesForAnnotation(packageSymbol, annotation);
    }
    return null;
  }

  @CheckForNull
  private static List<SymbolMetadata.AnnotationValue> valuesForAnnotation(Symbol symbol, String annotation) {
    return symbol.metadata().valuesForAnnotation(annotation);
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
        // other tree which may be used in annotations ?
        throw new IllegalArgumentException("Unexpected tree used to parameterize annotation");
    }
    return target.equals(symbol.name());
  }

  private static boolean containsDefaultLocation(Object[] defaultLocation, String target) {
    return Arrays.stream(defaultLocation).map(Symbol.class::cast).anyMatch(symbol -> target.equals(symbol.name()));
  }

}
