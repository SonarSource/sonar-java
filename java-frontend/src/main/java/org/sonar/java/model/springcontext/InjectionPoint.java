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

/**
 * A single point in the source where a dependency is injected, as registered in {@link TypeToDependenciesIndex}.
 *
 * @param name     the dependency name, either the field/parameter name at the injection point or the value of
 *                 the {@code @Qualifier} annotation if present
 * @param location the source location of the injection point
 */
public record InjectionPoint(String name, BeanLocation location) {
}
