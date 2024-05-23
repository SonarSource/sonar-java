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

import java.util.List;
import org.sonar.api.rule.RuleKey;

public final class RulesList {

  private RulesList() {
  }

  public static List<RuleKey> getMainRuleKeys() {
    return toRuleKeys(List.of(
      "S2095",
      "S2189",
      "S2222",
      "S2259",
      "S2583",
      "S2589",
      "S2637",
      "S2689",
      "S2755",
      "S3065",
      "S3516",
      "S3518",
      "S3655",
      "S3824",
      "S3958",
      "S3959",
      "S4165",
      "S4449",
      "S6373",
      "S6376",
      "S6377"));
  }

  public static List<RuleKey> getSonarWayRuleKeys() {
    return toRuleKeys(List.of(
      "S2095",
      "S2189",
      "S2222",
      "S2259",
      "S2583",
      "S2589",
      "S2637",
      "S2689",
      "S2755",
      "S3065",
      "S3516",
      "S3518",
      "S3655",
      "S3824",
      "S3958",
      "S3959",
      "S4165",
      "S4449",
      "S6373",
      "S6376",
      "S6377"));
  }

  private static List<RuleKey> toRuleKeys(List<String> rules) {
    return rules.stream().map(RulesList::toRuleKey).toList();
  }

  private static RuleKey toRuleKey(String rule) {
    return RuleKey.of(JavaSECheckRegistrar.REPOSITORY_KEY, rule);
  }

}
