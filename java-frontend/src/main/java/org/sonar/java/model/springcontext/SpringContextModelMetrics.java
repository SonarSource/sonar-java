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

import org.sonar.java.telemetry.SizeEstimator;

/**
 * Aggregate figures describing how much Spring-specific data a project's {@link SpringContextModel} holds, reported as
 * telemetry at the end of a project's analysis.
 *
 * @param beanCount                 The number of registered bean definitions, counting several definitions sharing a name separately.
 * @param beanNameCount             The number of distinct bean names.
 * @param injectionPointCount       The number of injection points discovered across all required types.
 * @param componentScanPackageCount The number of component-scanned packages, summed over all modules.
 * @param estimatedSizeInBytes      The estimated heap memory retained by the model.
 */
public record SpringContextModelMetrics(
  long beanCount,
  long beanNameCount,
  long injectionPointCount,
  long componentScanPackageCount,
  long estimatedSizeInBytes
) {

  /**
   * Computes all metrics exposed by the given model's components.
   */
  public static SpringContextModelMetrics of(SpringContextModel model) {
    var beanDefinitions = model.getBeanDefinitionRegistry();
    var dependencies = model.getTypeToDependenciesIndex();
    var packageScan = model.getProjectPackageScan();
    return new SpringContextModelMetrics(
      beanDefinitions.beanCount(),
      beanDefinitions.beanNameCount(),
      dependencies.injectionPointCount(),
      packageScan.packageCount(),
      SizeEstimator.estimate(
        beanDefinitions,
        model.getTypeToBeansIndex(),
        dependencies,
        model.getEntityClassToPropertiesIndex(),
        packageScan));
  }
}
