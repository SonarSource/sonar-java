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

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectConfigLoaderTest {

  @Test
  public void loadProjectsReturnsAllProjects() {
    List<RulingProject> projects = ProjectConfigLoader.loadProjects();
    assertThat(projects).isNotEmpty();
    assertThat(projects).hasSize(12);
  }

  @Test
  public void findByProjectNameReturnsCorrectProject() {
    RulingProject project = ProjectConfigLoader.findByProjectName("guava");
    assertThat(project).isNotNull();
    assertThat(project.projectKey()).isEqualTo("com.google.guava:guava");
    assertThat(project.path()).isEqualTo("guava");
    assertThat(project.buildType()).isEqualTo(RulingProject.BuildType.MAVEN);
  }

  @Test
  public void findByProjectNameReturnsNullForUnknownProject() {
    RulingProject project = ProjectConfigLoader.findByProjectName("non-existent");
    assertThat(project).isNull();
  }

  @Test
  public void mavenExistingProjectsAreMarkedCorrectly() {
    RulingProject project = ProjectConfigLoader.findByProjectName("eclipse-jetty-similar-to-main");
    assertThat(project).isNotNull();
    assertThat(project.isExistingProject()).isTrue();
    assertThat(project.isMavenBuild()).isTrue();
  }

  @Test
  public void sonarScannerProjectIsMarkedCorrectly() {
    RulingProject project = ProjectConfigLoader.findByProjectName("jboss-ejb3-tutorial");
    assertThat(project).isNotNull();
    assertThat(project.buildType()).isEqualTo(RulingProject.BuildType.SONAR_SCANNER);
    assertThat(project.isMavenBuild()).isFalse();
  }
}
