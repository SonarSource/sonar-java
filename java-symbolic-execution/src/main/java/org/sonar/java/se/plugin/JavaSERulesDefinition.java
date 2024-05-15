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

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/**
 * Declare rule metadata in server repository of rules.
 * That allows to list the rules in the page "Rules".
 */
public class JavaSERulesDefinition {

//  private static final String RESOURCE_BASE_PATH = "org/sonar/l10n/java/rules/javase";
//  static final String SONAR_WAY_PATH = RESOURCE_BASE_PATH + "/Sonar_way_profile.json";
//  public static final String REPOSITORY_KEY = "java";
//  public static final String REPOSITORY_NAME = "SonarAnalyzer";
//
//  private final SonarRuntime runtime;
//
//  private static final Set<String> RULE_TEMPLATES_KEY = Set.of("S3546");
//
//
//  private static void setTemplates(NewRepository repository) {
//    RULE_TEMPLATES_KEY.stream()
//      .map(repository::rule)
//      .filter(Objects::nonNull)
//      .forEach(rule -> rule.setTemplate(true));
//  }

}
