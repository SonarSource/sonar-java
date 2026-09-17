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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Index mapping fully-qualified bean type names to the beans of that type discovered during Spring context scanning.
 *
 * <p>Each entry records the bean name alongside its declaring module and package, enabling context-scoped lookups:
 * a consumer in module M can retrieve only the bean names reachable within its own Spring context.
 *
 * <p>Lookup returns an empty set for types with no registered beans.
 */
public class TypeToBeansIndex {

  /**
   * Per-type metadata for a registered bean: its name, the module it was declared in, and its declaring package.
   */
  record BeanEntry(String name, String module, String beanPackage) {
  }

  /** Bean entries indexed by fully-qualified type name. */
  private final Map<String, Set<BeanEntry>> entriesByType = new HashMap<>();

  /**
   * Registers a bean under the given type.
   *
   * @param beanType    fully-qualified class name of the bean's type
   * @param beanName    the bean name to associate with that type
   * @param module      the module key in which the bean is declared
   * @param beanPackage the package of the bean's declaring class
   */
  public void addBeanForType(String beanType, String beanName, String module, String beanPackage) {
    entriesByType.computeIfAbsent(beanType, k -> new HashSet<>()).add(new BeanEntry(beanName, module, beanPackage));
  }

  /**
   * Returns bean names visible to a consumer in {@code consumerModule}, scoped to its Spring context.
   *
   * <p>A bean is considered visible if:
   * <ul>
   *   <li>it is declared in the same module as the consumer, or</li>
   *   <li>its declaring package equals or is a subpackage of one of the packages in {@code scannedPackages}
   *       (covering cross-module {@code @ComponentScan}).</li>
   * </ul>
   *
   * @param beanType        fully-qualified class name of the bean's type
   * @param consumerModule  module key of the consuming bean
   * @param scannedPackages packages declared for component scanning in {@code consumerModule}
   * @return an unmodifiable set of visible bean names, or an empty set if none match
   */
  public Set<String> getNamesForType(String beanType, String consumerModule, Set<String> scannedPackages) {
    return entriesByType.getOrDefault(beanType, Set.of()).stream()
      .filter(entry -> isVisible(entry, consumerModule, scannedPackages))
      .map(BeanEntry::name)
      .collect(Collectors.toUnmodifiableSet());
  }

  public Set<String> getKeys() {
    return Collections.unmodifiableSet(entriesByType.keySet());
  }

  Map<String, Set<BeanEntry>> entriesByType() {
    return entriesByType;
  }

  private static boolean isVisible(BeanEntry entry, String consumerModule, Set<String> scannedPackages) {
    return entry.module().equals(consumerModule)
      || scannedPackages.stream().anyMatch(scanned -> isWithinPackage(entry.beanPackage(), scanned));
  }

  private static boolean isWithinPackage(String beanPackage, String scannedPackage) {
    return beanPackage.equals(scannedPackage) || beanPackage.startsWith(scannedPackage + ".");
  }
}
