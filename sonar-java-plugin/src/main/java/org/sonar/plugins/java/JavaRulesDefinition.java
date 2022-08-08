/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.java;

import java.util.Set;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.checks.CheckList;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;
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
    "S3546",
    "S4011");

  private final SonarRuntime sonarRuntime;

  public JavaRulesDefinition(SonarRuntime sonarRuntime) {
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(CheckList.REPOSITORY_KEY, Java.KEY)
      .setName("SonarAnalyzer");

    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_BASE_PATH, JavaSonarWayProfile.SONAR_WAY_PATH, sonarRuntime);
    ruleMetadataLoader.addRulesByAnnotatedClass(repository, CheckList.getChecks());

    TEMPLATE_RULE_KEY.stream()
      .map(repository::rule)
      .forEach(rule -> rule.setTemplate(true));

    repository.rules().stream().forEach(rule -> rule.addDeprecatedRuleKey("squid", rule.key()));

    repository.done();
  }
}
