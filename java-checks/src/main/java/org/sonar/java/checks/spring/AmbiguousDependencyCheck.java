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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanDefinitionRegistry;
import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.model.springcontext.TypeToBeanNamesIndex;
import org.sonar.java.model.springcontext.TypeToDependenciesIndex;
import org.sonar.java.model.springcontext.TypeToDependenciesIndex.InjectionPoint;
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

  @Override
  public List<SpringContextIssue> execute(SpringContextModel model) {
    BeanDefinitionRegistry registry = model.getBeanDefinitionRegistry();
    TypeToBeanNamesIndex typeToBeanNamesIndex = model.getTypeToBeanNamesIndex();
    TypeToDependenciesIndex typeToDependenciesIndex = model.getTypeToDependenciesIndex();

    List<SpringContextIssue> issues = new ArrayList<>();
    for (String type : typeToBeanNamesIndex.getKeys()) {
      Set<String> candidates = typeToBeanNamesIndex.getNamesForType(type);
      Set<InjectionPoint> injectionPoints = typeToDependenciesIndex.getDependenciesForType(type);
      if (!isResolved(candidates, registry)) {
        // excluding all beans with a configured profile, no matter what the profile is, to avoid FPs
        Set<String> effectiveCandidates = excludeCandidatesWithProfile(candidates, registry);
        if (effectiveCandidates.size() > 1) {
          for (InjectionPoint unresolvedInjectionPoint : computeUnmatchingNames(effectiveCandidates, injectionPoints, registry)) {
            issues.add(new SpringContextIssue(unresolvedInjectionPoint.location(), message(effectiveCandidates)));
          }
        }
      }
    }
    return issues;
  }

  private static boolean isResolved(Set<String> candidates, BeanDefinitionRegistry registry) {
    return candidates.size() <= 1
      || hasExactlyOnePrimaryCandidate(candidates, registry);
  }

  private static Set<InjectionPoint> computeUnmatchingNames(Set<String> candidates, Set<InjectionPoint> injectionPointNames, BeanDefinitionRegistry registry) {
    return injectionPointNames.stream().filter(injectionPoint -> matchesCandidate(injectionPoint.name(), candidates, registry)).collect(Collectors.toSet());
  }

  private static boolean hasExactlyOnePrimaryCandidate(Set<String> candidates, BeanDefinitionRegistry registry) {
    return candidates.stream().filter(candidate -> isPrimary(registry, candidate)).count() == 1;
  }

  private static boolean matchesCandidate(String injectionPointName, Set<String> candidates, BeanDefinitionRegistry registry) {
    return candidates.contains(injectionPointName)
      || candidates.stream().anyMatch(candidate -> injectionPointName.equals(qualifierOf(registry, candidate)));
  }

  @Nullable
  private static String qualifierOf(BeanDefinitionRegistry registry, String beanName) {
    return registry.getByName(beanName).stream()
      .map(BeanDefinitionHolder::getQualifier)
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
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
