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
package org.sonar.java.checks.verifier.internal;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.reporting.AnalyzerMessage;

final class RuleMetadataValidator {

  private static final String RULE_METADATA_DIRECTORY = "/org/sonar/l10n/java/rules/java/";

  private RuleMetadataValidator() {
  }

  static void validate(AnalyzerMessage issue) {
    Rule ruleAnnotation = AnnotationUtils.getAnnotation(issue.getCheck().getClass(), Rule.class);
    if (ruleAnnotation == null) {
      return;
    }

    RuleMetadata metadata = load(ruleAnnotation.key());
    if (metadata != null
      && metadata.remediation != null
      && "Constant/Issue".equals(metadata.remediation.func)
      && issue.getCost() != null) {
      throw new AssertionError(String.format(
        "Rule '%s' uses 'Constant/Issue' remediation but reports effort-to-fix %s. Constant remediation cannot use an issue gap.",
        ruleAnnotation.key(), issue.getCost()));
    }
  }

  @Nullable
  private static RuleMetadata load(String ruleKey) {
    URL resource = RuleMetadataValidator.class.getResource(RULE_METADATA_DIRECTORY + ruleKey + ".json");
    if (resource == null) {
      // Keep compatibility with analyzers and custom-rule projects still using language-suffixed metadata files.
      resource = RuleMetadataValidator.class.getResource(RULE_METADATA_DIRECTORY + ruleKey + "_java.json");
    }
    if (resource == null) {
      return null;
    }

    try (InputStreamReader reader = new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)) {
      return new Gson().fromJson(reader, RuleMetadata.class);
    } catch (IOException | JsonParseException e) {
      throw new AssertionError("Failed to read rule metadata for '" + ruleKey + "' from " + resource, e);
    }
  }

  private static class RuleMetadata {
    Remediation remediation;
  }

  private static class Remediation {
    String func;
  }
}
