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
package org.sonar.java.it;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.assertj.core.api.Assertions;

/**
 * Loads ruling project configurations from the ruling-projects.json resource file.
 * This allows external tools to discover available projects for ruling tests.
 */
public final class ProjectConfigLoader {

  private static final String CONFIG_RESOURCE = "ruling-projects.json";
  private static final Gson GSON = new Gson();

  private ProjectConfigLoader() {
    // Utility class
  }

  /**
   * Loads all ruling project configurations from the classpath resource.
   *
   * @return unmodifiable list of project configurations, never null
   * @throws IllegalStateException if the resource cannot be found or parsed
   */
  public static List<RulingProject> loadProjects() {
    try (InputStream is = ProjectConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
      Objects.requireNonNull(is, "Resource '" + CONFIG_RESOURCE + "' not found on classpath");
      List<RulingProject> projects = GSON.fromJson(
        new InputStreamReader(is, StandardCharsets.UTF_8),
        new TypeToken<List<RulingProject>>() {}.getType()
      );
      return projects != null ? Collections.unmodifiableList(projects) : Collections.emptyList();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load ruling project configurations", e);
    }
  }

  /**
   * Finds a project configuration by its project name.
   *
   * @param projectName the project name to find
   * @return the project configuration, or null if not found
   */
  public static RulingProject findByProjectName(String projectName) {
    return loadProjects().stream()
      .filter(p -> projectName.equals(p.projectName()))
      .findFirst()
      .orElse(null);
  }

  /**
   * Finds a project configuration by name and asserts it exists.
   *
   * @param projectName the project name to find
   * @return the project configuration, never null
   * @throws AssertionError if the project is not found
   */
  public static RulingProject requireProject(String projectName) {
    RulingProject project = findByProjectName(projectName);
    Assertions.assertThat(project)
      .as("Project '%s' should be defined in ruling-projects.json", projectName)
      .isNotNull();
    return project;
  }
}
