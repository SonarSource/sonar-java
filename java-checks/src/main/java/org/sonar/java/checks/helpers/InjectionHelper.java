/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Set;
import org.sonar.plugins.java.api.tree.ClassTree;

public final class InjectionHelper {

  private InjectionHelper() {
    // Utility class
  }

  private static final Set<String> CLASS_PREVENTING_CONSTRUCTOR_INJECTION = Set.of(
    // SONARJAVA-5358: Ignore Android instantiated classes
    "android.app.Activity",
    "android.app.Application",
    "android.app.Fragment",
    "androidx.fragment.app.Fragment",
    "android.app.Service",
    "android.content.BroadcastReceiver",
    "android.content.ContentProvider",
    "android.view.View",
    // SONARJAVA-4753
    "io.micronaut.function.aws.MicronautRequestHandler");

  public static boolean classCannotUseConstructorInjection(ClassTree ct) {
    return CLASS_PREVENTING_CONSTRUCTOR_INJECTION.stream()
      .anyMatch(ignoredType -> ct.symbol().type().isSubtypeOf(ignoredType));
  }
}
