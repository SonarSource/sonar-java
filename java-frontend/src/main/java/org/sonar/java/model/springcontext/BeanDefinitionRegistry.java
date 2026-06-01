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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks {@link BeanDefinitionHolder bean definitions} collected during Spring context scanning,
 * indexed by bean name.
 *
 * <p>Multiple definitions registered under the same name are preserved rather than overwritten,
 * allowing callers to detect and report duplicate bean declarations.
 *
 * @see BeanDefinitionHolder
 */
public class BeanDefinitionRegistry {
  /**
   * Maps bean names to their corresponding list of {@link BeanDefinitionHolder} instances.
   * A list is used as the value to capture duplicate bean definitions under the same name,
   * which is an invalid state that should be reported during analysis.
   */
  private final Map<String, List<BeanDefinitionHolder>> beanDefinitions = new HashMap<>();

  public List<BeanDefinitionHolder> getByName(String beanName) {
    return beanDefinitions.getOrDefault(beanName, List.of());
  }

  public void addBeanDefinition(String beanName, BeanDefinitionHolder beanDefinition) {
    beanDefinitions.computeIfAbsent(beanName, k -> new ArrayList<>()).add(beanDefinition);
  }
}
