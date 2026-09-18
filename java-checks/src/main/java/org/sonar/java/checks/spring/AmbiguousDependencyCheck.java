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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.springcontext.BeanDefinitionRegistry;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.model.springcontext.ProjectPackageScan;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.model.springcontext.TypeToBeansIndex;
import org.sonar.java.model.springcontext.TypeToDependenciesIndex;
import org.sonar.plugins.java.api.JavaCheck;

/**
 * Not an AST visitor: called directly by {@code SpringContextModelSensor} once the {@link SpringContextModel}
 * has been fully populated by the gatherers, since detecting autowiring ambiguity requires reasoning about every
 * bean of a given type across the whole analyzed scope, not a single file.
 */
@Rule(key = "S9352")
public class AmbiguousDependencyCheck implements JavaCheck, SpringContextCheck {

  private static final String MESSAGE = "Multiple beans match this dependency (%s);"
    + " disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".";

  /**
   * Creates the list of issues using the spring context model.
   * For each injection point, retrieves the beans of the required type that are visible within the
   * consumer's own Spring context (same module, or in a package covered by its {@code @ComponentScan}),
   * then evaluates the candidates under every combination of profiles named by their profile expressions.
   * An issue is raised if at least one combination activates multiple candidates without activating exactly
   * one {@code @Primary} candidate, unless the injection point matches a candidate by name.
   *
   * @param model the Spring context model of the project
   */
  @Override
  public List<SpringContextIssue> execute(SpringContextModel model) {
    BeanDefinitionRegistry registry = model.getBeanDefinitionRegistry();
    TypeToBeansIndex typeToBeansIndex = model.getTypeToBeansIndex();
    TypeToDependenciesIndex typeToDependenciesIndex = model.getTypeToDependenciesIndex();
    ProjectPackageScan projectPackageScan = model.getProjectPackageScan();

    List<SpringContextIssue> issues = new ArrayList<>();
    for (String type : typeToBeansIndex.getKeys()) {
      Map<String, Set<String>> candidatesByModule = new HashMap<>();
      Map<String, Optional<Set<String>>> ambiguousCandidatesByModule = new HashMap<>();
      for (InjectionPoint point : typeToDependenciesIndex.getDependenciesForType(type)) {
        Set<String> candidates = candidatesByModule.computeIfAbsent(point.module(),
          module -> typeToBeansIndex.getNamesForType(type, module, projectPackageScan.getPackagesForModule(module)));
        if (candidates.contains(point.name())) {
          continue;
        }
        ambiguousCandidatesByModule.computeIfAbsent(point.module(), module -> findAmbiguousCandidates(candidates, registry))
          .ifPresent(ambiguousCandidates -> issues.add(new SpringContextIssue(point.location(), message(ambiguousCandidates))));
      }
    }
    return issues;
  }

  private static Optional<Set<String>> findAmbiguousCandidates(Set<String> candidates, BeanDefinitionRegistry registry) {
    List<String> profileNames = candidates.stream()
      .flatMap(candidate -> registry.getByName(candidate).stream())
      .flatMap(bean -> bean.getProfileExpression().profileNames().stream())
      .distinct()
      .sorted()
      .toList();
    Set<String> activeProfiles = new HashSet<>();
    do {
      Set<String> activeCandidates = candidates.stream()
        .filter(candidate -> isActive(registry, candidate, activeProfiles))
        .collect(Collectors.toUnmodifiableSet());
      if (!hasUniqueOrPrimaryCandidate(activeCandidates, registry, activeProfiles)) {
        return Optional.of(activeCandidates);
      }
    } while (activateNextProfileCombination(profileNames, activeProfiles));
    return Optional.empty();
  }

  private static boolean activateNextProfileCombination(List<String> profileNames, Set<String> activeProfiles) {
    for (String profileName : profileNames) {
      if (activeProfiles.remove(profileName)) {
        continue;
      }
      activeProfiles.add(profileName);
      return true;
    }
    return false;
  }

  private static boolean hasUniqueOrPrimaryCandidate(Set<String> candidates, BeanDefinitionRegistry registry, Set<String> activeProfiles) {
    return candidates.size() <= 1 || hasExactlyOnePrimaryCandidate(candidates, registry, activeProfiles);
  }

  private static boolean hasExactlyOnePrimaryCandidate(Set<String> candidates, BeanDefinitionRegistry registry, Set<String> activeProfiles) {
    return candidates.stream().filter(candidate -> isPrimary(registry, candidate, activeProfiles)).count() == 1;
  }

  private static boolean isActive(BeanDefinitionRegistry registry, String beanName, Set<String> activeProfiles) {
    return registry.getByName(beanName).stream()
      .anyMatch(bean -> bean.getProfileExpression().isActiveUnder(activeProfiles));
  }

  private static boolean isPrimary(BeanDefinitionRegistry registry, String beanName, Set<String> activeProfiles) {
    return registry.getByName(beanName).stream()
      .anyMatch(bean -> bean.isPrimary() && bean.getProfileExpression().isActiveUnder(activeProfiles));
  }

  private static String message(Set<String> candidates) {
    String sortedCandidates = candidates.stream().sorted().collect(Collectors.joining(", "));
    return String.format(MESSAGE, sortedCandidates);
  }

}
