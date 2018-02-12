/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.java.checks.CheckList;
import org.sonar.squidbridge.annotations.RuleTemplate;

/**
 * Definition of rules.
 */
public class JavaRulesDefinition implements RulesDefinition {

  private final boolean isDebugEnabled;

  /**
   * 'Configuration' does exists yet in SonarLint context, consequently, in standalone mode, this constructor will be used.
   * See {@link https://jira.sonarsource.com/browse/SLCORE-159}
   */
  public JavaRulesDefinition() {
    this.isDebugEnabled = false;
  }

  public JavaRulesDefinition(Configuration settings) {
    this.isDebugEnabled = settings.getBoolean(Java.DEBUG_RULE_KEY).orElse(false);
  }

  private static final String RESOURCE_BASE_PATH = "/org/sonar/l10n/java/rules/squid";
  private final Gson gson = new Gson();

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(CheckList.REPOSITORY_KEY, Java.KEY)
      .setName("SonarAnalyzer");
    List<Class> checks = getChecks();
    new RulesDefinitionAnnotationLoader().load(repository, Iterables.toArray(checks, Class.class));
    JavaSonarWayProfile.Profile profile = JavaSonarWayProfile.readProfile();
    for (Class ruleClass : checks) {
      newRule(ruleClass, repository, profile);
    }
    repository.done();
  }

  private List<Class> getChecks() {
    ImmutableList.Builder<Class> checksBuilder = ImmutableList.<Class>builder().addAll(CheckList.getChecks());
    if (isDebugEnabled) {
      checksBuilder.addAll(CheckList.getDebugChecks());
    }
    return checksBuilder.build();
  }

  @VisibleForTesting
  protected void newRule(Class<?> ruleClass, NewRepository repository, JavaSonarWayProfile.Profile profile) {

    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.check.Rule.class);
    if (ruleAnnotation == null) {
      throw new IllegalArgumentException("No Rule annotation was found on " + ruleClass);
    }
    String ruleKey = ruleAnnotation.key();
    if (StringUtils.isEmpty(ruleKey)) {
      throw new IllegalArgumentException("No key is defined in Rule annotation of " + ruleClass);
    }
    NewRule rule = repository.rule(ruleKey);
    if (rule == null) {
      throw new IllegalStateException("No rule was created for " + ruleClass + " in " + repository.key());
    }
    String metadataKey = ruleMetadata(ruleClass, rule);
    // 'setActivatedByDefault' is used by SonarLint standalone, to define which rules will be active
    rule.setActivatedByDefault(profile.ruleKeys.contains(ruleKey) || profile.ruleKeys.contains(metadataKey));
    rule.setTemplate(AnnotationUtils.getAnnotation(ruleClass, RuleTemplate.class) != null);
  }

  private String ruleMetadata(Class<?> ruleClass, NewRule rule) {
    String metadataKey = rule.key();
    org.sonar.java.RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.java.RspecKey.class);
    if (rspecKeyAnnotation != null) {
      metadataKey = rspecKeyAnnotation.value();
      rule.setInternalKey(metadataKey);
    }
    addHtmlDescription(rule, metadataKey);
    addMetadata(rule, metadataKey);
    return metadataKey;
  }

  private void addMetadata(NewRule rule, String metadataKey) {
    URL resource = JavaRulesDefinition.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.json");
    if (resource != null) {
      RuleMetadata metadata = gson.fromJson(readResource(resource), RuleMetadata.class);
      rule.setSeverity(metadata.defaultSeverity.toUpperCase(Locale.US));
      rule.setName(metadata.title);
      rule.addTags(metadata.tags);
      rule.setType(RuleType.valueOf(metadata.type));
      rule.setStatus(RuleStatus.valueOf(metadata.status.toUpperCase(Locale.US)));
      if(metadata.remediation != null) {
        rule.setDebtRemediationFunction(metadata.remediation.remediationFunction(rule.debtRemediationFunctions()));
        rule.setGapDescription(metadata.remediation.linearDesc);
      }
    }
  }

  private static void addHtmlDescription(NewRule rule, String metadataKey) {
    URL resource = JavaRulesDefinition.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.html");
    if (resource != null) {
      rule.setHtmlDescription(readResource(resource));
    }
  }

  private static String readResource(URL resource) {
    try {
      return Resources.toString(resource, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read: " + resource, e);
    }
  }

  private static class RuleMetadata {
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
      if(func.startsWith("Constant")) {
        return drf.constantPerIssue(constantCost.replace("mn", "min"));
      }
      if("Linear".equals(func)) {
        return drf.linear(linearFactor.replace("mn", "min"));
      }
      return drf.linearWithOffset(linearFactor.replace("mn", "min"), linearOffset.replace("mn", "min"));
    }
  }

}
