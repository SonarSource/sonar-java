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

import org.sonar.api.scanner.ScannerSide;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Aggregates all Spring context information collected during project scanning.
 *
 * <p>Acts as the top-level model passed to rules that need to reason about the Spring
 * application context. Each field is a specialized index populated during the scan phase:
 * <ul>
 *   <li>{@link BeanDefinitionRegistry} — bean definitions indexed by bean name</li>
 *   <li>{@link ProjectPackageScan} — packages registered for component scanning, per module</li>
 *   <li>{@link TypeToBeanNamesIndex} — bean names indexed by type</li>
 *   <li>{@link EntityClassToPropertiesIndex} — JPA {@code @Entity} class properties</li>
 * </ul>
 */
@ScannerSide
@SonarLintSide
public class SpringContextModel {
  /** Registry of all bean definitions discovered during scanning. */
  private final BeanDefinitionRegistry beanDefinitionRegistry = new BeanDefinitionRegistry();

  /** Packages registered for Spring component scanning, grouped by module. */
  private final ProjectPackageScan projectPackageScan = new ProjectPackageScan();

  /** Index for resolving bean names by their fully-qualified type. */
  private final TypeToBeanNamesIndex typeToBeanNamesIndex = new TypeToBeanNamesIndex();

  /** Index of properties associated with Spring Data / Hibernate {@code @Entity} classes. */
  private final EntityClassToPropertiesIndex entityClassToPropertiesIndex = new EntityClassToPropertiesIndex();

  public BeanDefinitionRegistry getBeanDefinitionRegistry() {
    return beanDefinitionRegistry;
  }

  public ProjectPackageScan getProjectPackageScan() {
    return projectPackageScan;
  }

  public TypeToBeanNamesIndex getTypeToBeanNamesIndex() {
    return typeToBeanNamesIndex;
  }

  public EntityClassToPropertiesIndex getEntityClassToPropertiesIndex() {
    return entityClassToPropertiesIndex;
  }
}
