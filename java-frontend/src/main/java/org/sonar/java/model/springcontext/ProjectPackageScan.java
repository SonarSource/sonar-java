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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the packages registered for Spring component scanning, grouped by module.
 *
 * <p>Corresponds to packages declared via {@code @ComponentScan} (or equivalent) and
 * collected during project analysis. Each module may declare multiple scanned packages.
 */
public class ProjectPackageScan {
  /** Scanned package names indexed by module name. */
  private final Map<String, Set<String>> packagesScannedBySpringPerModule = new HashMap<>();

  /**
   * Registers a package as scanned by Spring for the given module.
   *
   * @param module      the module in which the component scan is configured
   * @param packageName the package name declared for scanning
   */
  public void addPackage(String module, String packageName) {
    packagesScannedBySpringPerModule.computeIfAbsent(module, k -> new HashSet<>()).add(packageName);
  }

  /**
   * Registers all packages in the given collection as scanned by Spring for the given module.
   *
   * @param module       the module in which the component scan is configured
   * @param packageNames the package names to register
   */
  public void addPackages(String module, Collection<String> packageNames) {
    packagesScannedBySpringPerModule.computeIfAbsent(module, k -> new HashSet<>()).addAll(packageNames);
  }

  /**
   * Returns the set of packages scanned by Spring for the given module.
   *
   * @param module the module name
   * @return the scanned package names, or an empty set if none were registered
   */
  public Set<String> getPackagesForModule(String module) {
    return packagesScannedBySpringPerModule.getOrDefault(module, Set.of());
  }

  /**
   * Returns all modules that have at least one registered scanned package.
   *
   * @return the set of module names
   */
  public Set<String> getModules() {
    return packagesScannedBySpringPerModule.keySet();
  }
}