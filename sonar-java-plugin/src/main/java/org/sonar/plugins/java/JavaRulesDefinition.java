/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Cardinality;
import org.sonar.java.checks.CheckList;
import org.sonar.squidbridge.rules.ExternalDescriptionLoader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Definition of rules.
 */
public class JavaRulesDefinition implements RulesDefinition {

  private static final String RESOURCE_BASE_PATH = "/org/sonar/l10n/java/rules/squid";
  private final Map<String, RulesProfile> profiles = new HashMap<>();
  private final Gson gson = new Gson();
  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(CheckList.REPOSITORY_KEY, Java.KEY)
      .setName("SonarQube");
    for (Class ruleClass : CheckList.getChecks()) {
      newRule(ruleClass, repository);
    }
    repository.done();
  }

  private void newRule(Class<?> ruleClass, NewRepository repository) {

    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.check.Rule.class);
    if (ruleAnnotation == null) {
      throw new IllegalArgumentException("No Rule annotation was found on " + ruleClass);
    }
    String ruleKey = ruleAnnotation.key();
    if (StringUtils.isEmpty(ruleKey)) {
      throw new IllegalArgumentException("No key is defined in Rule annotation of " + ruleClass);
    }
    NewRule rule = repository.createRule(ruleKey);
    if (rule == null) {
      throw new IllegalStateException("No rule was created for " + ruleClass + " in " + repository);
    }
    if (ruleAnnotation.cardinality() == Cardinality.MULTIPLE) {
      throw new IllegalArgumentException("Cardinality is not supported, use the RuleTemplate annotation instead");
    }
    ruleMetadata(ruleClass, rule);
  }

  private void ruleMetadata(Class<?> ruleClass, NewRule rule) {
    String metadataKey = rule.key();
    org.sonar.java.RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.java.RspecKey.class);
    if (rspecKeyAnnotation != null) {
      metadataKey = rspecKeyAnnotation.value();
      rule.setInternalKey(metadataKey);
    }
    addHtmlDescription(rule, metadataKey);
    addMetadata(rule, metadataKey);

  }

  private void addMetadata(NewRule rule, String metadataKey) {
    URL resource = ExternalDescriptionLoader.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.json");
    if (resource != null) {
      RuleMetatada metatada = gson.fromJson(readResource(resource), RuleMetatada.class);
      rule.setSeverity(metatada.defaultSeverity.toUpperCase());
      rule.setName(metatada.title);
      rule.addTags(metatada.tags);
      rule.setStatus(RuleStatus.valueOf(metatada.status.toUpperCase()));
      rule.setDebtSubCharacteristic(metatada.sqaleSubCharac);
      if(metatada.remediation != null) {
        rule.setDebtRemediationFunction(metatada.remediation.remediationFunction(rule.debtRemediationFunctions()));
        rule.setEffortToFixDescription(metatada.remediation.linearDesc);
      }
      addToProfile(rule, metatada);
    }
  }

  private void addToProfile(NewRule rule, RuleMetatada metatada) {
    for (String profileName : metatada.profiles) {
      RulesProfile rulesProfile = profiles.get(profileName);
      if(rulesProfile == null) {
        rulesProfile = RulesProfile.create(profileName, Java.KEY);
        profiles.put(profileName, rulesProfile);
      }
      rulesProfile.activateRule(org.sonar.api.rules.Rule.create(CheckList.REPOSITORY_KEY, rule.key()), null);
    }
  }

  private static void addHtmlDescription(NewRule rule, String metadataKey) {
    URL resource = ExternalDescriptionLoader.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.html");
    if (resource != null) {
      rule.setHtmlDescription(readResource(resource));
    }
  }

  private static String readResource(URL resource) {
    try {
      return Resources.toString(resource, Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read: " + resource, e);
    }
  }

  public RulesProfile getProfile(String profileName) {
    return profiles.get(profileName);
  }

  private static class RuleMetatada {
    String title;
    String status;
    String[] profiles;
    @Nullable
    Remediation remediation;

    String sqaleSubCharac;
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
