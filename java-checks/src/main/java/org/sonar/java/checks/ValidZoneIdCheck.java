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
package org.sonar.java.checks;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S8265")
public class ValidZoneIdCheck extends AbstractMethodDetection {

  private static final Set<String> VALID_ZONE_IDS;
  private static final Set<String> SPECIAL_ZONE_IDS = Set.of("UTC", "GMT", "Z", "UT");
  
  private static final Pattern FIXED_OFFSET_PATTERN = Pattern.compile(
    "^[+-]\\d{1,2}(:\\d{2}(:\\d{2})?)?$|^[+-]\\d{4}$"
  );
  
  private static final Pattern ZONE_OFFSET_PATTERN = Pattern.compile(
    "^(UTC|GMT)[+-]\\d{1,2}(:\\d{2})?$"
  );
  
  private static final Map<String, String> COMMON_CORRECTIONS = Map.ofEntries(
    Map.entry("PST", "America/Los_Angeles"),
    Map.entry("PDT", "America/Los_Angeles"),
    Map.entry("EST", "America/New_York"),
    Map.entry("EDT", "America/New_York"),
    Map.entry("CST", "America/Chicago"),
    Map.entry("CDT", "America/Chicago"),
    Map.entry("MST", "America/Denver"),
    Map.entry("MDT", "America/Denver"),
    Map.entry("US/Pacific", "America/Los_Angeles"),
    Map.entry("US/Eastern", "America/New_York"),
    Map.entry("US/Central", "America/Chicago"),
    Map.entry("US/Mountain", "America/Denver"),
    Map.entry("US/Alaska", "America/Anchorage"),
    Map.entry("US/Hawaii", "Pacific/Honolulu"),
    Map.entry("IST", "Asia/Kolkata"),
    Map.entry("JST", "Asia/Tokyo"),
    Map.entry("BST", "Europe/London"),
    Map.entry("CET", "Europe/Paris")
  );

  static {
    VALID_ZONE_IDS = ZoneId.getAvailableZoneIds();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.time.ZoneId")
      .names("of")
      .addParametersMatcher("java.lang.String")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    
    if (arguments.size() != 1) {
      return;
    }
    
    ExpressionTree argument = arguments.get(0);
    Optional<String> constant = argument.asConstant(String.class);
    
    if (!constant.isPresent()) {
      return;
    }
    
    String zoneId = constant.get();
    
    if (!isValidZoneId(zoneId)) {
      String message = createIssueMessage(zoneId);
      reportIssue(argument, message);
    }
  }

  private static boolean isValidZoneId(String zoneId) {
    return VALID_ZONE_IDS.contains(zoneId)
      || SPECIAL_ZONE_IDS.contains(zoneId)
      || FIXED_OFFSET_PATTERN.matcher(zoneId).matches()
      || ZONE_OFFSET_PATTERN.matcher(zoneId).matches();
  }

  private String createIssueMessage(String invalidZoneId) {
    String suggestion = suggestAlternative(invalidZoneId);
    
    if (suggestion != null) {
      return String.format(
        "\"%s\" is not a valid time zone identifier. Did you mean \"%s\"?",
        invalidZoneId, suggestion
      );
    }
    
    return String.format(
      "\"%s\" is not a valid time zone identifier.",
      invalidZoneId
    );
  }

  private String suggestAlternative(String invalidZoneId) {
    if (COMMON_CORRECTIONS.containsKey(invalidZoneId)) {
      return COMMON_CORRECTIONS.get(invalidZoneId);
    }
    
    return VALID_ZONE_IDS.stream()
      .min(Comparator.comparingInt(valid -> levenshteinDistance(invalidZoneId, valid)))
      .filter(closest -> levenshteinDistance(invalidZoneId, closest) <= 3)
      .orElse(null);
  }

  private static int levenshteinDistance(String s1, String s2) {
    int[][] dp = new int[s1.length() + 1][s2.length() + 1];
    
    for (int i = 0; i <= s1.length(); i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= s2.length(); j++) {
      dp[0][j] = j;
    }
    
    for (int i = 1; i <= s1.length(); i++) {
      for (int j = 1; j <= s2.length(); j++) {
        int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
        dp[i][j] = Math.min(
          Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
          dp[i - 1][j - 1] + cost
        );
      }
    }
    
    return dp[s1.length()][s2.length()];
  }
}