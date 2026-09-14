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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.Duration;
import org.sonar.check.Rule;
import org.sonar.java.reporting.AnalyzerMessage;

final class RuleMetadataValidator {

  private static final String RULE_METADATA_DIRECTORY = "/org/sonar/l10n/java/rules/java/";
  private static final Gson GSON = new Gson();
  private static final Map<String, Optional<String>> CACHE = new ConcurrentHashMap<>();

  private RuleMetadataValidator() {
  }

  static void validate(AnalyzerMessage issue) {
    Rule ruleAnnotation = AnnotationUtils.getAnnotation(issue.getCheck().getClass(), Rule.class);
    if (ruleAnnotation == null) {
      return;
    }

    String function = CACHE.computeIfAbsent(ruleAnnotation.key(), key -> Optional.ofNullable(load(key))).orElse(null);
    if ("Constant/Issue".equals(function) && issue.getCost() != null) {
      throw new AssertionError(String.format(
        "Rule '%s' uses 'Constant/Issue' remediation but reports effort-to-fix %s. Constant remediation cannot use an issue gap.",
        ruleAnnotation.key(), issue.getCost()));
    }
    if ("None".equals(function) && issue.getCost() != null) {
      throw new AssertionError("Rule '" + ruleAnnotation.key() + "' reports effort-to-fix " + issue.getCost()
        + " but has no remediation function. The issue gap would be ignored.");
    }
  }

  @Nullable
  private static String load(String ruleKey) {
    URL resource = RuleMetadataValidator.class.getResource(RULE_METADATA_DIRECTORY + ruleKey + ".json");
    if (resource == null) {
      resource = RuleMetadataValidator.class.getResource(RULE_METADATA_DIRECTORY + ruleKey + "_java.json");
    }
    if (resource == null) {
      return null;
    }

    try (InputStreamReader reader = new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)) {
      return readFunction(reader, ruleKey);
    } catch (IOException | JsonParseException e) {
      throw new AssertionError("Failed to read rule metadata for '" + ruleKey + "' from " + resource, e);
    }
  }

  static String readFunction(Reader reader, String ruleKey) {
    JsonElement json = GSON.fromJson(reader, JsonElement.class);
    if (json == null || !json.isJsonObject()) {
      throw invalidMetadata(ruleKey, "expected a JSON object");
    }
    JsonObject metadata = json.getAsJsonObject();
    if (!metadata.has("remediation")) {
      return "None";
    }
    if (!metadata.get("remediation").isJsonObject()) {
      throw invalidMetadata(ruleKey, "remediation must be an object");
    }
    JsonObject remediation = metadata.getAsJsonObject("remediation");
    String function = stringProperty(remediation, "func", ruleKey);
    Set<String> costs = switch (function) {
      case "Constant/Issue" -> Set.of("constantCost");
      case "Linear" -> Set.of("linearFactor");
      case "Linear with offset" -> Set.of("linearFactor", "linearOffset");
      default -> throw invalidMetadata(ruleKey, "unknown remediation function '" + function + "'");
    };
    for (String cost : costs) {
      String duration = stringProperty(remediation, cost, ruleKey);
      if (!duration.matches("[ ]*[0-9]+[ ]*(min|h|d)")) {
        throw invalidMetadata(ruleKey, "invalid duration in '" + cost + "'");
      }
      try {
        Duration.decode(duration, 24);
      } catch (IllegalArgumentException e) {
        throw invalidMetadata(ruleKey, "invalid duration in '" + cost + "': " + e.getMessage());
      }
    }
    if (remediation.has("linearDesc")) {
      stringProperty(remediation, "linearDesc", ruleKey);
    }
    for (String property : remediation.keySet()) {
      if (!costs.contains(property) && !"func".equals(property)
        && !("linearDesc".equals(property) && !"Constant/Issue".equals(function))) {
        throw invalidMetadata(ruleKey, "unexpected remediation property '" + property + "' for '" + function + "'");
      }
    }
    return function;
  }

  private static String stringProperty(JsonObject object, String property, String ruleKey) {
    var value = object.get(property);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw invalidMetadata(ruleKey, "expected a string for '" + property + "'");
    }
    return value.getAsString();
  }

  private static AssertionError invalidMetadata(String ruleKey, String reason) {
    return new AssertionError("Invalid remediation metadata for rule '" + ruleKey + "': " + reason);
  }
}
