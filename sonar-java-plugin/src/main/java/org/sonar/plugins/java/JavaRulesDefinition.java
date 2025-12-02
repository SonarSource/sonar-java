/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.java.GeneratedCheckList;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKeys;
import org.sonarsource.analyzer.commons.collections.SetUtils;

/**
 * Definition of rules.
 */
public class JavaRulesDefinition implements RulesDefinition {

  private static final String RESOURCE_BASE_PATH = "/org/sonar/l10n/java/rules/java";

  /**
   * Rule templates have to be manually defined
   */
  private static final Set<String> TEMPLATE_RULE_KEY = SetUtils.immutableSetOf(
    "S124",
    "S2253",
    "S3688",
    "S4011");

  private static final Map<String, String> INTERNAL_KEYS = Collections.singletonMap("NoSonar", "S1291");

  private final SonarRuntime sonarRuntime;
  private final CheckRegistrar[] checkRegistrars;

  public JavaRulesDefinition(SonarRuntime sonarRuntime) {
    this(sonarRuntime, new CheckRegistrar[0]);
  }

  public JavaRulesDefinition(SonarRuntime sonarRuntime, CheckRegistrar[] checkRegistrars) {
    this.sonarRuntime = sonarRuntime;
    this.checkRegistrars = checkRegistrars;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(GeneratedCheckList.REPOSITORY_KEY, Java.KEY)
      .setName("SonarAnalyzer");

    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_BASE_PATH, JavaSonarWayProfile.SONAR_WAY_PATH, sonarRuntime);
    ruleMetadataLoader.addRulesByAnnotatedClass(repository, GeneratedCheckList.getChecks());

    TEMPLATE_RULE_KEY.stream()
      .map(repository::rule)
      .forEach(rule -> rule.setTemplate(true));

    INTERNAL_KEYS.forEach((ruleKey, internalKey) -> repository.rule(ruleKey).setInternalKey(internalKey));

    // for all the rules without explicit deprecated key already declared, register them with "squid:key"
    GeneratedCheckList.getChecks().stream()
      .filter(rule -> !deprecatesRules(rule))
      .map(JavaRulesDefinition::ruleKey)
      .map(repository::rule)
      .forEach(rule -> rule.addDeprecatedRuleKey("squid", rule.key()));

    for (CheckRegistrar registrar : checkRegistrars) {
      registrar.customRulesDefinition(context, repository);
    }

    repository.done();
  }

  private static String ruleKey(Class<?> rule) {
    return AnnotationUtils.getAnnotation(rule, org.sonar.check.Rule.class).key();
  }

  @VisibleForTesting
  static boolean deprecatesRules(Class<?> rule) {
    // single annotation
    return AnnotationUtils.getAnnotation(rule, DeprecatedRuleKey.class) != null
      // multiple annotations, for instance java:S4830
      || AnnotationUtils.getAnnotation(rule, DeprecatedRuleKeys.class) != null;
  }
}
