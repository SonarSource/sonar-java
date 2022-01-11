/*
 * Copyright (C) 2012-2022 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
 */
package org.sonar.samples.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/**
 * Declare rule metadata in server repository of rules.
 * That allows to list the rules in the page "Rules".
 */
public class MyJavaRulesDefinition implements RulesDefinition {

  // don't change that because the path is hard coded in CheckVerifier
  private static final String RESOURCE_BASE_PATH = "org/sonar/l10n/java/rules/java";

  public static final String REPOSITORY_KEY = "mycompany-java";

  // Add the rule keys of the rules which need to be considered as template-rules
  private static final Set<String> RULE_TEMPLATES_KEY = Collections.emptySet();

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, "java").setName("MyCompany Custom Repository");

    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_BASE_PATH);

    ruleMetadataLoader.addRulesByAnnotatedClass(repository, new ArrayList<>(RulesList.getChecks()));

    setTemplates(repository);

    repository.done();
  }

  private static void setTemplates(NewRepository repository) {
    RULE_TEMPLATES_KEY.stream()
      .map(repository::rule)
      .filter(Objects::nonNull)
      .forEach(rule -> rule.setTemplate(true));
  }
}
