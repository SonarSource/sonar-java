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
import java.util.List;
import java.util.Map;
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

  private static final String MESSAGE = "Multiple beans match this dependency (%s);"
    + " disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".";

  /** Creates the list of issues using the spring context model.
   * For each injection point, retrieves the beans of the required type that are visible within the
   * consumer's own Spring context (same module, or in a package covered by its {@code @ComponentScan}),
   * then checks for ambiguity: a single candidate, or a single one marked {@code @Primary}, is unambiguous.
   * Candidates with a profile are then excluded as potentially mutually exclusive, and the same
   * unique/{@code @Primary} check is re-applied; if multiple still remain, an issue is raised for
   * each injection point that does not match one of the <em>full</em> context-scoped candidate set by name —
   * an injection point explicitly qualified towards a profiled bean is already disambiguated.
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
      for (InjectionPoint point : typeToDependenciesIndex.getDependenciesForType(type)) {
        Set<String> candidates = candidatesByModule.computeIfAbsent(point.module(),
          module -> typeToBeansIndex.getNamesForType(type, module, projectPackageScan.getPackagesForModule(module)));
        if (hasUniqueOrPrimaryCandidate(candidates, registry)) {
          continue;
        }
        Set<String> effectiveCandidates = excludeCandidatesWithProfile(candidates, registry);
        if (!hasUniqueOrPrimaryCandidate(effectiveCandidates, registry) && !matchesByNameOrQualifier(candidates, point.name(), registry)) {
          issues.add(new SpringContextIssue(point.location(), message(effectiveCandidates)));
        }
      }
    }
    return issues;
  }

  private static boolean matchesByNameOrQualifier(Set<String> candidates, String dependencyName, BeanDefinitionRegistry registry) {
    return candidates.contains(dependencyName)
      || candidates.stream().filter(candidate -> dependencyName.equals(getBeanQualifier(registry, candidate))).count() == 1;
  }

  private static String getBeanQualifier(BeanDefinitionRegistry registry, String beanName) {
    return registry.getByName(beanName).stream()
      .map(BeanDefinitionHolder::getQualifier)
      .filter(q -> q != null)
      .findFirst()
      .orElse(null);
  }

  private static boolean hasUniqueOrPrimaryCandidate(Set<String> candidates, BeanDefinitionRegistry registry) {
    return candidates.size() <= 1
      || hasExactlyOnePrimaryCandidate(candidates, registry);
  }

  private static boolean hasExactlyOnePrimaryCandidate(Set<String> candidates, BeanDefinitionRegistry registry) {
    return candidates.stream().filter(candidate -> isPrimary(registry, candidate)).count() == 1;
  }

  private static Set<String> excludeCandidatesWithProfile(Set<String> candidates, BeanDefinitionRegistry registry) {
    return candidates.stream().filter(candidate -> !hasProfile(registry, candidate)).collect(Collectors.toUnmodifiableSet());
  }

  private static boolean isPrimary(BeanDefinitionRegistry registry, String beanName) {
    return registry.getByName(beanName).stream().anyMatch(BeanDefinitionHolder::isPrimary);
  }

  private static boolean hasProfile(BeanDefinitionRegistry registry, String beanName) {
    return registry.getByName(beanName).stream().anyMatch(bean -> bean.getProfiles() != null);
  }

  private static String message(Set<String> candidates) {
    String sortedCandidates = candidates.stream().sorted().collect(Collectors.joining(", "));
    return String.format(MESSAGE, sortedCandidates);
  }

}
