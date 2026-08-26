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
package org.sonar.java.checks.spring;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanDefinitionRegistry;
import org.sonar.java.model.springcontext.BeanLocation;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.model.springcontext.TypeToBeanNamesIndex;
import org.sonar.plugins.java.api.JavaCheck;

/**
 * Not an AST visitor: called directly by {@code SpringContextModelSensor} once the {@link SpringContextModel}
 * has been fully populated by the gatherers, since detecting autowiring ambiguity requires reasoning about every
 * bean of a given type across the whole analyzed scope, not a single file.
 */
@Rule(key = "S9352")
public class AmbiguousDependencyCheck implements JavaCheck {

  /**
   * @param location bean whose dependency is ambiguous, used to anchor the reported issue
   * @param message  issue message describing the ambiguity
   */
  public record AmbiguousDependency(BeanLocation location, String message) {
  }

  private static final String MESSAGE = "Multiple beans of type \"%s\" match this dependency (%s);"
    + " disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".";

  public List<AmbiguousDependency> findAmbiguousDependencies(SpringContextModel model) {
    BeanDefinitionRegistry registry = model.getBeanDefinitionRegistry();
    TypeToBeanNamesIndex typeToBeanNamesIndex = model.getTypeToBeanNamesIndex();

    List<AmbiguousDependency> ambiguousDependencies = new ArrayList<>();
    for (BeanDefinitionHolder bean : registry.getAll()) {
      for (Map.Entry<String, Set<String>> dependency : bean.getDependingBeans().entrySet()) {
        String requiredType = dependency.getKey();
        Set<String> candidates = typeToBeanNamesIndex.getNamesForType(requiredType);
        if (isAmbiguous(candidates, dependency.getValue(), registry)) {
          ambiguousDependencies.add(new AmbiguousDependency(bean.getLocation(), message(requiredType, candidates)));
        }
      }
    }
    return ambiguousDependencies;
  }

  private static boolean isAmbiguous(Set<String> candidates, Set<String> injectionPointNames, BeanDefinitionRegistry registry) {
    return candidates.size() > 1
      && candidates.stream().noneMatch(candidate -> isPrimary(registry, candidate))
      && candidates.stream().noneMatch(injectionPointNames::contains);
  }

  private static boolean isPrimary(BeanDefinitionRegistry registry, String beanName) {
    return registry.getByName(beanName).stream().anyMatch(BeanDefinitionHolder::isPrimary);
  }

  private static String message(String requiredType, Set<String> candidates) {
    String sortedCandidates = candidates.stream().sorted().collect(Collectors.joining(", "));
    return String.format(MESSAGE, requiredType, sortedCandidates);
  }

}
