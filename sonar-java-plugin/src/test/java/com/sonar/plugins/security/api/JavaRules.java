/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package com.sonar.plugins.security.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Class required to test SonarWay
 */
public class JavaRules {

  public static Set<String> ruleKeys = new HashSet<>();

  public static Set<String> getRuleKeys() {
    return ruleKeys;
  }

  public static Set<String> getSecurityRuleKeys() {
    return ruleKeys;
  }

  public static String getRepositoryKey() {
    return "security-repo-key";
  }
}
