/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.se.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.SonarRuntime;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

@ServerSide
@ComputeEngineSide
public class JavaSECheckRegistrar implements CheckRegistrar {

  private static final String RESOURCE_BASE_PATH = "org/sonar/l10n/java/rules/javase";
  static final String SONAR_WAY_PATH = RESOURCE_BASE_PATH + "/Sonar_way_profile.json";
  public static final String REPOSITORY_KEY = "java";

  private static final Set<String> RULE_TEMPLATES_KEY = Set.of("S3546");
  private final SonarRuntime runtime;


  public JavaSECheckRegistrar(SonarRuntime runtime){
    this.runtime = runtime;
  }

  @Override
  public void register(RegistrarContext registrarContext) {
    List<SECheck> checks = new ArrayList<>();
    for(Class<? extends SECheck> check : JavaSECheckList.getChecks()) {
      try {
        checks.add(check.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        throw new IllegalStateException("Could not create instance of " + check, e);
      }
    }
    registrarContext.registerMainSharedCheck(new SymbolicExecutionVisitor(checks), RulesList.getMainRuleKeys());
    registrarContext.registerMainChecks(REPOSITORY_KEY, checks);
  }

  @Override
  public void customRulesDefinition(RulesDefinition.Context context, RulesDefinition.NewRepository javaRepository) {
    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_BASE_PATH, SONAR_WAY_PATH, runtime);

    ruleMetadataLoader.addRulesByAnnotatedClass(javaRepository, new ArrayList<>(JavaSECheckList.getChecks()));

    setTemplates(javaRepository);
  }

  private static void setTemplates(RulesDefinition.NewRepository repository) {
    RULE_TEMPLATES_KEY.stream()
      .map(repository::rule)
      .forEach(rule -> rule.setTemplate(true));
  }
}
