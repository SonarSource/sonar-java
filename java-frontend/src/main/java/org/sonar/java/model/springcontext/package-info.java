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

/**
 * Model of the Spring application context built during project scanning.
 *
 * <p>Classes in this package are populated by visitors that traverse the AST and collect
 * Spring-specific metadata. The resulting model is then consumed by rules that reason about
 * the application context — for example, detecting duplicate bean definitions or missing
 * component-scan coverage.
 *
 * <p>The central entry point is {@link org.sonar.java.model.springcontext.SpringContextModel},
 * which aggregates the following indexes:
 * <ul>
 *   <li>{@link org.sonar.java.model.springcontext.BeanDefinitionRegistry} — bean definitions by name</li>
 *   <li>{@link org.sonar.java.model.springcontext.TypeToBeanNamesIndex} — bean names by type</li>
 *   <li>{@link org.sonar.java.model.springcontext.ProjectPackageScan} — component-scan packages by module</li>
 *   <li>{@link org.sonar.java.model.springcontext.EntityClassToPropertiesIndex} — JPA {@code @Entity} properties</li>
 * </ul>
 */
package org.sonar.java.model.springcontext;