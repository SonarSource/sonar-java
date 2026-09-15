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
package org.sonar.java.model.springcontext;

import org.sonar.java.reporting.AnalyzerMessage;

/**
 * A single point in the source where a dependency is injected, as registered in {@link TypeToDependenciesIndex}.
 *
 * @param name     the dependency name, either the field/parameter name at the injection point or the value of
 *                 the {@code @Qualifier} annotation if present
 * @param location the source location of the injection point
 */
public record InjectionPoint(String name, BeanLocation location) {

  /**
   * An injection point as collected from a single file, holding no reference to that file.
   *
   * <p>This is the form kept in {@link BeanDefinitionHolder.InputFileData} and written to the cache. It becomes
   * an {@link InjectionPoint} once paired with the file it was collected from, which the gatherer knows from the
   * file its beans are stored under.
   *
   * @param name the dependency name, as in {@link InjectionPoint#name()}
   * @param span the text span of the injection point within its own file
   */
  public record InputFileData(String name, AnalyzerMessage.TextSpan span) {
  }
}
