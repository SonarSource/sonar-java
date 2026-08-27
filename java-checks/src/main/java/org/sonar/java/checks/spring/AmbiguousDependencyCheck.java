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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
        if (isResolved(candidates, dependency.getValue(), registry)) {
          continue;
        }
        // A @Fallback candidate is only a real contender when it is the sole remaining one; otherwise it is
        // ignored by Spring, so the effective candidates are whichever bean(s) are not marked @Fallback.
        Set<String> effectiveCandidates = excludeFallbackCandidates(candidates, registry);
        if (effectiveCandidates.size() > 1) {
          ambiguousDependencies.add(new AmbiguousDependency(bean.getLocation(), message(requiredType, effectiveCandidates)));
        }
      }
    }
    return ambiguousDependencies;
  }

  private static boolean isResolved(Set<String> candidates, Set<String> injectionPointNames, BeanDefinitionRegistry registry) {
    // injectionPointNames merges every injection point of this type declared on the bean: it is only resolved
    // if EVERY one of them names a candidate (by bean name or by a @Qualifier declared on that candidate bean
    // itself), otherwise at least one injection point remains ambiguous.
    return candidates.size() <= 1
      || injectionPointNames.stream().allMatch(name -> matchesCandidate(name, candidates, registry))
      || candidates.stream().anyMatch(candidate -> isPrimary(registry, candidate));
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

  private static Set<String> excludeFallbackCandidates(Set<String> candidates, BeanDefinitionRegistry registry) {
    Set<String> nonFallbackCandidates = candidates.stream()
      .filter(candidate -> !isFallback(registry, candidate))
      .collect(Collectors.toUnmodifiableSet());
    return nonFallbackCandidates.isEmpty() ? candidates : nonFallbackCandidates;
  }

  private static boolean isPrimary(BeanDefinitionRegistry registry, String beanName) {
    return registry.getByName(beanName).stream().anyMatch(BeanDefinitionHolder::isPrimary);
  }

  private static boolean isFallback(BeanDefinitionRegistry registry, String beanName) {
    return registry.getByName(beanName).stream().anyMatch(BeanDefinitionHolder::isFallback);
  }

  private static String message(String requiredType, Set<String> candidates) {
    String sortedCandidates = candidates.stream().sorted().collect(Collectors.joining(", "));
    return String.format(MESSAGE, requiredType, sortedCandidates);
  }

}
