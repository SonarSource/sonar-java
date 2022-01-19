/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package com.sonar.plugins.dbd.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Class required to test SonarWay
 */
public class JavaRules {

  public static Set<String> ruleKeys = new HashSet<>();

  public static Set<String> getDataflowBugDetectionRuleKeys() {
    return ruleKeys;
  }

  public static String getRepositoryKey() {
    return "dbd-repo-key";
  }

  public static Set<String> methodThrowingException() throws Exception {
    throw new RuntimeException("testing");
  }
}
