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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.sonar.java.reporting.AnalyzerMessage;

public class ProjectContextModel implements org.sonar.plugins.java.api.ProjectContextModelReader {

  public final Set<String> springComponents = new HashSet<>();
  public final Set<String> springRepositories = new HashSet<>();
  public final Map<String, Properties> propertiesFiles = new HashMap<>();

  public record Location(AnalyzerMessage analyzerMessage) {}
  public final Map<String, Set<Location>> injections = new HashMap<>();
  public final Map<String, Set<String>> availableImpls = new HashMap<>();

  @Override
  public boolean isSpringComponent(String fullyQualifiedName) {
    return springComponents.contains(fullyQualifiedName);
  }

  /**
   * Returns true if the type with the given fully qualified name is configured as a Spring repository
   * in any flavor of Spring configuration (e.g., using @Repository annotation, or xml config).
   */
  @Override
  public boolean isSpringRepository(String fullyQualifiedName) {
    return springRepositories.contains(fullyQualifiedName);
  }

  @Override
  public Set<String> getPropertiesFilePaths() {
    return Collections.unmodifiableSet(propertiesFiles.keySet());
  }

  @Override
  public Properties getProperties(String filePath) {
    //careful this is modifiable
    return propertiesFiles.get(filePath);
  }

  @Override
  public Map<String, Set<String>> availableImpls() {
    return availableImpls;
  }


}
