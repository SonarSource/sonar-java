/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.maven.helpers;

import com.google.common.collect.Lists;
import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.maven.MavenParser;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.Dependency;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.maven.model.maven2.MavenProject.Dependencies;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenDependencyCollectorTest {

  private static final String CHECK_KEY = "test";

  @Test
  public void no_dependency() throws Exception {
    assertThat(MavenDependencyCollector.forMavenProject(parse("noDependency")).getDependencies()).isEmpty();
  }

  @Test
  public void should_retrieve_dependencies_from_dependencies() throws Exception {
    assertThat(MavenDependencyCollector.forMavenProject(parse("dependencies")).getDependencies()).hasSize(1);
  }

  @Test
  public void should_retrieve_dependencies_from_dependency_management() throws Exception {
    assertThat(MavenDependencyCollector.forMavenProject(parse("dependencyManagement")).getDependencies()).hasSize(1);
  }

  @Test
  public void should_retrieve_dependencies_from_profile() throws Exception {
    assertThat(MavenDependencyCollector.forMavenProject(parse("profile")).getDependencies()).hasSize(1);
  }

  @Test
  public void should_retrieve_dependencies_from_plugin() throws Exception {
    assertThat(MavenDependencyCollector.forMavenProject(parse("plugin")).getDependencies()).hasSize(1);
  }

  @Test
  public void should_retrieve_dependencies_from_every_possible_position() throws Exception {
    assertThat(MavenDependencyCollector.forMavenProject(parse("allPositions")).getDependencies()).hasSize(8);
  }

  @Test
  public void should_use_name_matcher_to_filter() throws Exception {
    MavenProject mockProject = mockProjectWithDependencies(
      createDependency("a.b.c", "d", "1.0-SNAPSHOT"),
      createDependency("a.b.c", "e"),
      createDependency("a.b.c", "f", ""),
      createDependency("log", "log", "4.5.6"));

    MavenDependencyCollector collector = MavenDependencyCollector.forMavenProject(mockProject);
    assertThat(collector.getDependencies()).hasSize(4);

    List<MavenDependencyNameMatcher> matchers = Collections.<MavenDependencyNameMatcher>emptyList();
    assertThat(collector.withName(matchers).getDependencies()).hasSize(4);

    matchers = MavenDependencyNameMatcher.fromString("*:e", CHECK_KEY);
    assertThat(collector.withName(matchers).getDependencies()).hasSize(1);

    matchers = MavenDependencyNameMatcher.fromString("*:e,log:*", CHECK_KEY);
    assertThat(collector.withName(matchers).getDependencies()).hasSize(2);

    matchers = MavenDependencyNameMatcher.fromString("a.b.c:e", CHECK_KEY);
    assertThat(collector.withName(matchers).getDependencies()).hasSize(1);
  }

  @Test
  public void should_use_version_matcher_to_filter() throws Exception {
    MavenProject mockProject = mockProjectWithDependencies(
      createDependency("a.b.c", "d", "1.0-SNAPSHOT"),
      createDependency("a.b.c", "e"),
      createDependency("a.b.c", "f", ""),
      createDependency("a.b.c", "g", "4.5"),
      createDependency("log", "log", "4.5.6"));

    List<MavenDependencyNameMatcher> nameMatchers = MavenDependencyNameMatcher.fromString("*:*", CHECK_KEY);
    MavenDependencyCollector collector = MavenDependencyCollector.forMavenProject(mockProject).withName(nameMatchers);

    MavenDependencyVersionMatcher versionMatcher = MavenDependencyVersionMatcher.fromString("", CHECK_KEY);
    assertThat(collector.withVersion(versionMatcher).getDependencies()).hasSize(5);

    versionMatcher = MavenDependencyVersionMatcher.fromString("4.5.*", CHECK_KEY);
    assertThat(collector.withVersion(versionMatcher).getDependencies()).hasSize(2);
  }

  private static MavenProject parse(String testCase) {
    File file = new File("src/test/files/checks/maven/dependencyCollector/" + testCase + "-pom.xml");
    MavenProject project = MavenParser.parseXML(file);
    if (project == null) {
      Fail.fail("unable to parse test case: " + file.getAbsolutePath());
    }
    return project;
  }

  private static Dependency createDependency(String groupId, String artifactId) {
    return createDependency(groupId, artifactId, null);
  }

  private static Dependency createDependency(String groupId, String artifactId, @Nullable String version) {
    Dependency dependency = new Dependency();
    dependency.setArtifactId(new LocatedAttribute(artifactId));
    dependency.setGroupId(new LocatedAttribute(groupId));
    if (version != null) {
      dependency.setVersion(new LocatedAttribute(version));
    }
    return dependency;
  }

  private static MavenProject mockProjectWithDependencies(Dependency... dependencies) {
    Dependencies mpDependencies = mock(Dependencies.class);
    when(mpDependencies.getDependencies()).thenReturn(Lists.newArrayList(dependencies));
    MavenProject mavenProject = mock(MavenProject.class);
    when(mavenProject.getDependencies()).thenReturn(mpDependencies);
    return mavenProject;
  }

}
