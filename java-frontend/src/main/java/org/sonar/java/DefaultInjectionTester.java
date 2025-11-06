/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.plugins.java.api.internal.InjectionTester;

public class DefaultInjectionTester implements InjectionTester {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultInjectionTester.class);

  private final ProjectDefinition projectDefinition;

  public DefaultInjectionTester(SonarComponents sonarComponents, ProjectDefinition projectDefinition) {
    LOG.error("SONAR COMPONENTS WAS INJECTED.");
    LOG.error("PROJECT DEFINITION WAS INJECTED. Module key: {}", projectDefinition.getKey());

    this.projectDefinition = projectDefinition;
  }

  @Override
  public String moduleKey() {
    return projectDefinition.getKey();
  }
}
