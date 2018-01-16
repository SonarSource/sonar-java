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
import java.util.Set;
import org.sonar.plugins.java.api.semantic.Symbol;

public final class AnnotationUtils {

  private AnnotationUtils() {
  }

  private static final Set<String> NULLABLE_ANNOTATIONS = ImmutableSet.of(
    "javax.annotation.CheckForNull",
    "javax.annotation.Nullable",
    "org.jetbrains.annotations.Nullable");
  private static final Set<String> NONNULL_ANNOTATIONS = ImmutableSet.of(
    "javax.annotation.Nonnull",
    "org.jetbrains.annotations.NotNull");

  static boolean isAnnotatedNullable(Symbol symbol) {
    return isAnnotatedWithOneOf(symbol, NULLABLE_ANNOTATIONS);
  }

  static boolean isAnnotatedNonNull(Symbol symbol) {
    return isAnnotatedWithOneOf(symbol, NONNULL_ANNOTATIONS);
  }

  static boolean isAnnotatedWith(Symbol symbol, String annotation) {
    return symbol.metadata().isAnnotatedWith(annotation);
  }

  private static boolean isAnnotatedWithOneOf(Symbol symbol, Set<String> annotations) {
    return annotations.stream().anyMatch(annotation -> isAnnotatedWith(symbol, annotation));
  }

}
