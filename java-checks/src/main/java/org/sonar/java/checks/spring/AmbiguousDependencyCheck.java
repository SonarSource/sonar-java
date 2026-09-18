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
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
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

  private static final int MAX_ENUMERATED_PROFILE_NAMES = 12;

  private static final String MESSAGE = "Multiple beans match this dependency (%s);"
    + " disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".";

  /**
   * Creates the list of issues using the spring context model.
   * For each injection point, retrieves the beans of the required type that are visible within the
   * consumer's own Spring context (same module, or in a package covered by its {@code @ComponentScan}),
   * then evaluates the candidates under every combination of profiles named by their profile expressions,
   * provided that the number of distinct profile names stays within a bounded limit.
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
        String module = point.module();
        Set<String> scannedPackages = projectPackageScan.getPackagesForModule(module);
        Set<String> candidates = candidatesByModule.computeIfAbsent(module, key -> typeToBeansIndex.getNamesForType(type, key, scannedPackages));
        if (candidates.contains(point.name())) {
          continue;
        }
        ambiguousCandidatesByModule.computeIfAbsent(module, key -> findAmbiguousCandidates(candidates, registry, key, scannedPackages))
          .ifPresent(ambiguousCandidates -> issues.add(new SpringContextIssue(point.location(), message(ambiguousCandidates))));
      }
    }
    return issues;
  }

  private static Optional<Set<String>> findAmbiguousCandidates(Set<String> candidates, BeanDefinitionRegistry registry,
    String module, Set<String> scannedPackages) {
    Map<String, List<BeanDefinitionHolder>> holdersByCandidate = candidates.stream()
      .collect(Collectors.toUnmodifiableMap(candidate -> candidate, candidate -> visibleHolders(registry, candidate, module, scannedPackages)));
    List<String> profileNames = holdersByCandidate.values().stream()
      .flatMap(List::stream)
      .flatMap(bean -> bean.getProfileExpression().profileNames().stream())
      .distinct()
      .sorted()
      .toList();
    if (profileNames.size() > MAX_ENUMERATED_PROFILE_NAMES) {
      return Optional.empty();
    }
    Set<String> activeProfiles = new HashSet<>();
    do {
      Set<String> activeCandidates = candidates.stream()
        .filter(candidate -> isActive(holdersByCandidate.get(candidate), activeProfiles))
        .collect(Collectors.toUnmodifiableSet());
      if (!hasUniqueOrPrimaryCandidate(activeCandidates, holdersByCandidate, activeProfiles)) {
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

  private static boolean hasUniqueOrPrimaryCandidate(Set<String> candidates, Map<String, List<BeanDefinitionHolder>> holdersByCandidate, Set<String> activeProfiles) {
    return candidates.size() <= 1 || hasExactlyOnePrimaryCandidate(candidates, holdersByCandidate, activeProfiles);
  }

  private static boolean hasExactlyOnePrimaryCandidate(Set<String> candidates, Map<String, List<BeanDefinitionHolder>> holdersByCandidate, Set<String> activeProfiles) {
    return candidates.stream().filter(candidate -> isPrimary(holdersByCandidate.get(candidate), activeProfiles)).count() == 1;
  }

  private static boolean isActive(List<BeanDefinitionHolder> holders, Set<String> activeProfiles) {
    return holders.stream()
      .anyMatch(bean -> bean.getProfileExpression().isActiveUnder(activeProfiles));
  }

  private static boolean isPrimary(List<BeanDefinitionHolder> holders, Set<String> activeProfiles) {
    return holders.stream()
      .anyMatch(bean -> bean.isPrimary() && bean.getProfileExpression().isActiveUnder(activeProfiles));
  }

  private static List<BeanDefinitionHolder> visibleHolders(BeanDefinitionRegistry registry, String beanName, String module, Set<String> scannedPackages) {
    return registry.getByName(beanName).stream()
      .filter(bean -> bean.getModule().equals(module)
        || scannedPackages.stream().anyMatch(scanned -> bean.getBeanPackage().equals(scanned)
        || bean.getBeanPackage().startsWith(scanned + ".")))
      .toList();
  }

  private static String message(Set<String> candidates) {
    String sortedCandidates = candidates.stream().sorted().collect(Collectors.joining(", "));
    return String.format(MESSAGE, sortedCandidates);
  }

}
