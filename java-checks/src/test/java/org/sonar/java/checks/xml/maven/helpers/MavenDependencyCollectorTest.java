/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks.xml.maven.helpers;

import org.assertj.core.api.Fail;
import org.junit.Test;
import org.sonar.java.xml.maven.PomParser;
import org.sonar.maven.model.maven2.Dependency;
import org.sonar.maven.model.maven2.MavenProject;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenDependencyCollectorTest {

  @Test
  public void no_dependency() throws Exception {
    assertThat(new MavenDependencyCollector(parse("noDependency")).allDependencies()).isEmpty();
  }

  @Test
  public void should_retrieve_dependencies_from_dependencies() throws Exception {
    List<Dependency> dep = new MavenDependencyCollector(parse("dependencies")).allDependencies();
    assertThat(dep).hasSize(1);
  }

  @Test
  public void should_retrieve_dependencies_from_dependency_management() throws Exception {
    List<Dependency> dep = new MavenDependencyCollector(parse("dependencyManagement")).allDependencies();
    assertThat(dep).hasSize(1);
  }

  @Test
  public void should_retrieve_dependencies_from_profile() throws Exception {
    List<Dependency> dep = new MavenDependencyCollector(parse("profile")).allDependencies();
    assertThat(dep).hasSize(1);
  }

  @Test
  public void should_retrieve_dependencies_from_plugin() throws Exception {
    List<Dependency> dep = new MavenDependencyCollector(parse("plugin")).allDependencies();
    assertThat(dep).hasSize(1);
  }

  @Test
  public void should_retrieve_dependencies_from_every_possible_position() throws Exception {
    List<Dependency> dep = new MavenDependencyCollector(parse("allPositions")).allDependencies();
    assertThat(dep).hasSize(8);
  }

  private static MavenProject parse(String testCase) {
    File file = new File("src/test/files/checks/xml/maven/dependencyCollector/" + testCase + "-pom.xml");
    MavenProject project = PomParser.parseXML(file);
    if (project == null) {
      Fail.fail("unable to parse test case: " + file.getAbsolutePath());
    }
    return project;
  }

}
