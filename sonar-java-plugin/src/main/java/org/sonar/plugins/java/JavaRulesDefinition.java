/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.java.checks.CheckList;

/**
 * Definition of rules.
 */
public class JavaRulesDefinition implements RulesDefinition {

  private static final String RESOURCE_BASE_PATH = "/org/sonar/l10n/java/rules/squid";
  private static final Gson GSON = new Gson();

  /**
   * Rule templates have to be manually defined
   */
  private static final Set<String> TEMPLATE_RULE_KEY = ImmutableSet.of(
    "S124",
    "S2253",
    "S3417",
    "S3688",
    "S3546",
    "S4011");
  private final boolean isDebugEnabled;
  private final boolean supportsSecurityHotspots;

  /**
   * 'Configuration' does exists yet in SonarLint context, consequently, in standalone mode, this constructor will be used.
   * See {@link https://jira.sonarsource.com/browse/SLCORE-159}
   */
  public JavaRulesDefinition(SonarRuntime sonarRuntime) {
    this.isDebugEnabled = false;
    this.supportsSecurityHotspots = SecurityHotspots.securityHotspotsSupported(sonarRuntime);
  }

  public JavaRulesDefinition(Configuration settings, SonarRuntime sonarRuntime) {
    this.isDebugEnabled = settings.getBoolean(Java.DEBUG_RULE_KEY).orElse(false);
    this.supportsSecurityHotspots = SecurityHotspots.securityHotspotsSupported(sonarRuntime);
  }

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
    String rspecKey = rspecKey(ruleClass, rule);
    RuleMetadata ruleMetadata = readRuleMetadata(rspecKey);
    addMetadata(rule, ruleMetadata);
    String ruleHtmlDescription = readRuleHtmlDescription(rspecKey);
    if (ruleHtmlDescription != null) {
      rule.setHtmlDescription(ruleHtmlDescription);
    }
    // 'setActivatedByDefault' is used by SonarLint standalone, to define which rules will be active
    boolean activatedInProfile = profile.ruleKeys.contains(ruleKey) || profile.ruleKeys.contains(rspecKey);
    boolean isSecurityHotspot = ruleMetadata != null && ruleMetadata.isSecurityHotspot();
    rule.setActivatedByDefault(activatedInProfile && !isSecurityHotspot);
    rule.setTemplate(TEMPLATE_RULE_KEY.contains(ruleKey));
  }

  private static String rspecKey(Class<?> ruleClass, NewRule rule) {
    org.sonar.java.RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.java.RspecKey.class);
    if (rspecKeyAnnotation != null) {
      String rspecKey = rspecKeyAnnotation.value();
      rule.setInternalKey(rspecKey);
      return rspecKey;
    } else {
      return rule.key();
    }
  }

  @Nullable
  static RuleMetadata readRuleMetadata(String metadataKey) {
    URL resource = JavaRulesDefinition.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.json");
    return resource != null ? GSON.fromJson(readResource(resource), RuleMetadata.class) : null;
  }

  private static String readRuleHtmlDescription(String metadataKey) {
    URL resource = JavaRulesDefinition.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.html");
    if (resource != null) {
      return readResource(resource);
    }
    return null;
  }

  private void addMetadata(NewRule rule, @Nullable RuleMetadata metadata) {
    if (metadata == null) {
      return;
    }
    rule.setSeverity(metadata.defaultSeverity.toUpperCase(Locale.US));
    rule.setName(metadata.title);
    rule.addTags(metadata.tags);
    if (metadata.isSecurityHotspot() && !supportsSecurityHotspots) {
      rule.setType(RuleType.VULNERABILITY);
    } else {
      rule.setType(RuleType.valueOf(metadata.type));
    }
    rule.setStatus(RuleStatus.valueOf(metadata.status.toUpperCase(Locale.US)));
    if (metadata.remediation != null) {
      rule.setDebtRemediationFunction(metadata.remediation.remediationFunction(rule.debtRemediationFunctions()));
      rule.setGapDescription(metadata.remediation.linearDesc);
    }
    if (supportsSecurityHotspots) {
      addSecurityStandards(rule, metadata.securityStandards);
    }
  }

  private static void addSecurityStandards(NewRule rule, SecurityStandards securityStandards) {
    for (String s : securityStandards.OWASP) {
      rule.addOwaspTop10(OwaspTop10.valueOf(s));
    }
    rule.addCwe(securityStandards.CWE);
  }

  private static String readResource(URL resource) {
    try {
      return Resources.toString(resource, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read: " + resource, e);
    }
  }

  static class RuleMetadata {
    private static final String SECURITY_HOTSPOT = "SECURITY_HOTSPOT";

    String title;
    String status;
    @Nullable
    Remediation remediation;

    String type;
    String[] tags;
    String defaultSeverity;
    SecurityStandards securityStandards = new SecurityStandards();

    boolean isSecurityHotspot() {
      return SECURITY_HOTSPOT.equals(type);
    }
  }

  static class SecurityStandards {
    int[] CWE = {};
    String[] OWASP = {};
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
