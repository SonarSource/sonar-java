/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.samples.java;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.samples.java.utils.StringUtils;

/**
 * Declare rule metadata in server repository of rules.
 * That allows to list the rules in the page "Rules".
 */
public class MyJavaRulesDefinition implements RulesDefinition {

  // don't change that because the path is hard coded in CheckVerifier
  private static final String RESOURCE_BASE_PATH = "/org/sonar/l10n/java/rules/squid";

  public static final String REPOSITORY_KEY = "mycompany-java";

  private static final Gson GSON = new Gson();

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, "java").setName("MyCompany Custom Repository");

    for (Class<? extends JavaCheck> check : RulesList.getChecks()) {
      new RulesDefinitionAnnotationLoader().load(repository, check);
      newRule(check, repository);
    }
    repository.done();
  }

  // only visible for testing purpose, should be private
  protected static void newRule(Class<? extends JavaCheck> ruleClass, NewRepository repository) {

    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.check.Rule.class);
    checkCondition(ruleAnnotation != null, () -> "No Rule annotation was found on " + ruleClass);

    String ruleKey = ruleAnnotation.key();
    checkCondition(StringUtils.isNotEmpty(ruleKey), () -> "No key is defined in Rule annotation of " + ruleClass);

    NewRule rule = repository.rule(ruleKey);
    checkCondition(rule != null, () -> "No rule was created for " + ruleClass + " in " + repository.key());

    ruleMetadata(rule);

    // for template rules, we just need a way to identify them from the other normal ones, for instance with an annotation
    rule.setTemplate(AnnotationUtils.getAnnotation(ruleClass, org.sonar.samples.java.annotations.RuleTemplate.class) != null);
  }

  private static void checkCondition(boolean condition, Supplier<String> messageSupplier) {
    if (!condition) {
      throw new IllegalArgumentException(messageSupplier.get());
    }
  }

  private static void ruleMetadata(NewRule rule) {
    String metadataKey = rule.key();
    addHtmlDescription(rule, metadataKey);
    addMetadata(rule, metadataKey);
  }

  private static void addMetadata(NewRule rule, String metadataKey) {
    URL resource = MyJavaRulesDefinition.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.json");
    if (resource != null) {
      RuleMetatada metatada = GSON.fromJson(readResource(resource), RuleMetatada.class);
      rule.setSeverity(metatada.defaultSeverity.toUpperCase(Locale.US));
      rule.setName(metatada.title);
      rule.addTags(metatada.tags);
      rule.setType(RuleType.valueOf(metatada.type));
      rule.setStatus(RuleStatus.valueOf(metatada.status.toUpperCase(Locale.US)));
      if (metatada.remediation != null) {
        rule.setDebtRemediationFunction(metatada.remediation.remediationFunction(rule.debtRemediationFunctions()));
        rule.setGapDescription(metatada.remediation.linearDesc);
      }
    }
  }

  private static void addHtmlDescription(NewRule rule, String metadataKey) {
    URL resource = MyJavaRulesDefinition.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.html");
    if (resource != null) {
      rule.setHtmlDescription(readResource(resource));
    }
  }

  // only visible for testing purpose, should be private
  protected static String readResource(URL resource) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read: " + resource, e);
    }
  }

  private static class RuleMetatada {
    String title;
    String status;
    @Nullable
    Remediation remediation;

    String type;
    String[] tags;
    String defaultSeverity;
  }

  private static class Remediation {
    String func;
    String constantCost;
    String linearDesc;
    String linearOffset;
    String linearFactor;

    public DebtRemediationFunction remediationFunction(DebtRemediationFunctions drf) {
      if (func.startsWith("Constant")) {
        return drf.constantPerIssue(constantCost.replace("mn", "min"));
      }
      if ("Linear".equals(func)) {
        return drf.linear(linearFactor.replace("mn", "min"));
      }
      return drf.linearWithOffset(linearFactor.replace("mn", "min"), linearOffset.replace("mn", "min"));
    }
  }

}
