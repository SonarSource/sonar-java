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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.springcontext.BeanDefinitionHolder;
import org.sonar.java.model.springcontext.BeanDefinitionRegistry;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.model.springcontext.ProfileExpression;
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

  private static final String SUGGESTION = "; disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".";

  /**
   * A bean competing for an injection point, with the metadata needed to decide ambiguity resolved once.
   *
   * @param name              The name the bean is registered under.
   * @param profileExpression The condition under which any definition registered under that name is active.
   * @param isPrimary         Whether any definition registered under that name is annotated with {@code @Primary}.
   * @param qualifier         The {@code @Qualifier} value declared on the bean definition, if any.
   */
  private record Candidate(String name, ProfileExpression profileExpression, boolean isPrimary, @Nullable String qualifier) {
  }

  /**
   * Creates the list of issues using the spring context model.
   * Injection points that collect every matching bean ({@code List<T>}, {@code Set<T>}, {@code Collection<T>},
   * {@code T[]}, {@code Map<String, T>}) are skipped: Spring injects all candidates there, so they can never be
   * ambiguous.
   * For each remaining injection point, retrieves the beans of the required type that are visible within the
   * consumer's own Spring context (same module, or in a package covered by its {@code @ComponentScan}),
   * then checks for ambiguity under each {@link #activationScenarios(Collection, ProfileExpression) activation scenario}:
   * a single candidate, or a single one marked {@code @Primary}, is unambiguous. An injection point whose name or
   * {@code @Qualifier} designates one of the candidates is already disambiguated, whichever profile is active.
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
      Map<String, List<Candidate>> candidatesByModule = new HashMap<>();
      for (InjectionPoint point : typeToDependenciesIndex.getDependenciesForType(type)) {
        if (point.multiple()) {
          continue;
        }
        List<Candidate> candidates = candidatesByModule.computeIfAbsent(point.module(),
          module -> resolveCandidates(typeToBeansIndex.getNamesForType(type, module, projectPackageScan.getPackagesForModule(module)), registry));
        if (candidates.size() <= 1 || matchesByNameOrQualifier(candidates, point.name())) {
          continue;
        }
        reportFirstAmbiguousScenario(candidates, point, issues);
      }
    }
    return issues;
  }

  private static List<Candidate> resolveCandidates(Set<String> beanNames, BeanDefinitionRegistry registry) {
    return beanNames.stream().map(beanName -> {
      List<BeanDefinitionHolder> definitions = registry.getByName(beanName);
      ProfileExpression profileExpression = ProfileExpression.or(definitions.stream().map(BeanDefinitionHolder::getProfileExpression).toList());
      boolean isPrimary = definitions.stream().anyMatch(BeanDefinitionHolder::isPrimary);
      String qualifier = definitions.stream().map(BeanDefinitionHolder::getQualifier).filter(Objects::nonNull).findFirst().orElse(null);
      return new Candidate(beanName, profileExpression, isPrimary, qualifier);
    }).toList();
  }

  /**
   * Looks for an activation scenario under which the consumer exists and cannot be satisfied unambiguously.
   *
   * @param candidates     The beans visible to the injection point.
   * @param injectionPoint The injection point for which ambiguities are being discovered.
   * @param issues         The list of issues being recorded for the rule.
   */
  private static void reportFirstAmbiguousScenario(List<Candidate> candidates, InjectionPoint injectionPoint, List<SpringContextIssue> issues) {
    ProfileExpression consumerProfile = injectionPoint.profileExpression();
    for (Set<String> activeProfiles : activationScenarios(candidates, consumerProfile)) {
      if (!consumerProfile.isActiveUnder(activeProfiles)) {
        continue;
      }
      List<Candidate> activeCandidates = candidates.stream()
        .filter(candidate -> candidate.profileExpression().isActiveUnder(activeProfiles))
        .toList();
      if (!hasUniqueOrPrimaryCandidate(activeCandidates)) {
        issues.add(new SpringContextIssue(injectionPoint.location(), message(activeCandidates, activeProfiles)));
        return;
      }
    }
  }

  /**
   * Enumerates the profile activations to reason about: no profile active, then each profile mentioned by the
   * candidates or by the consumer, active on its own. Activating a single profile at a time keeps the reasoning
   * conservative, at the cost of missing ambiguities that only arise when several profiles are combined.
   *
   * @param candidates      The beans visible to the injection point.
   * @param consumerProfile The condition under which the bean declaring the injection point is active.
   * @return The sets of active profile names to consider, the empty one first and the others in lexicographic order.
   */
  private static List<Set<String>> activationScenarios(Collection<Candidate> candidates, ProfileExpression consumerProfile) {
    Set<String> profileNames = new TreeSet<>(consumerProfile.profileNames());
    candidates.forEach(candidate -> profileNames.addAll(candidate.profileExpression().profileNames()));
    List<Set<String>> scenarios = new ArrayList<>();
    scenarios.add(Set.of());
    profileNames.forEach(profileName -> scenarios.add(Set.of(profileName)));
    return scenarios;
  }

  private static boolean matchesByNameOrQualifier(Collection<Candidate> candidates, String dependencyName) {
    return candidates.stream().anyMatch(candidate -> dependencyName.equals(candidate.name()))
      || candidates.stream().filter(candidate -> dependencyName.equals(candidate.qualifier())).count() == 1;
  }

  private static boolean hasUniqueOrPrimaryCandidate(Collection<Candidate> candidates) {
    return candidates.size() <= 1 || candidates.stream().filter(Candidate::isPrimary).count() == 1;
  }

  private static String message(Collection<Candidate> candidates, Set<String> activeProfiles) {
    String sortedCandidates = candidates.stream().map(Candidate::name).sorted().collect(Collectors.joining(", "));
    if (activeProfiles.isEmpty()) {
      return "Multiple beans match this dependency (" + sortedCandidates + ")" + SUGGESTION;
    }
    return "Multiple beans match this dependency (" + sortedCandidates + ") in profile '" + activeProfiles.iterator().next() + "'" + SUGGESTION;
  }

}
