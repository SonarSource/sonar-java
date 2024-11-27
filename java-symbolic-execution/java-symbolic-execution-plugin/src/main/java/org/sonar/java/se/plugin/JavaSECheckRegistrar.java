/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.se.plugin;

import java.util.ArrayList;
import java.util.Set;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

@ServerSide
@ScannerSide
@ComputeEngineSide
public class JavaSECheckRegistrar implements CheckRegistrar {

  private static final String RESOURCE_BASE_PATH = "org/sonar/l10n/java/rules/javase";
  static final String SONAR_WAY_PATH = RESOURCE_BASE_PATH + "/Sonar_way_profile.json";
  public static final String REPOSITORY_KEY = "java";

  private static final Set<String> RULE_TEMPLATES_KEY = Set.of("S3546");
  private final SonarRuntime runtime;

  public JavaSECheckRegistrar(SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void register(RegistrarContext registrarContext) {
    // need the CheckFactory to be provided in order to register checks
  }

  @Override
  public void register(RegistrarContext registrarContext, CheckFactory checkFactory) {
    Checks<JavaCheck> checks = checkFactory.<JavaCheck>create(REPOSITORY_KEY).addAnnotatedChecks(JavaSECheckList.getChecks());

    var seChecks = checks.all().stream()
      .filter(SECheck.class::isInstance)
      .map(c -> (SECheck) c)
      .toList();

    var ruleKeys = seChecks.stream().map(checks::ruleKey).toList();

    registrarContext.registerMainSharedCheck(new SymbolicExecutionVisitor(seChecks), ruleKeys);
    registrarContext.registerMainChecks(checks, seChecks);
  }

  @Override
  public void customRulesDefinition(RulesDefinition.Context context, RulesDefinition.NewRepository javaRepository) {
    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_BASE_PATH, SONAR_WAY_PATH, runtime);

    ruleMetadataLoader.addRulesByAnnotatedClass(javaRepository, new ArrayList<>(JavaSECheckList.getChecks()));

    setTemplates(javaRepository);
  }

  private static void setTemplates(RulesDefinition.NewRepository repository) {
    RULE_TEMPLATES_KEY.forEach(ruleKey -> repository.rule(ruleKey).setTemplate(true));
  }
}
