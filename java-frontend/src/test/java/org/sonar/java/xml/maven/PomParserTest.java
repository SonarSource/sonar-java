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
package org.sonar.java.xml.maven;

import org.junit.Test;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.maven2.Dependency;
import org.sonar.maven.model.maven2.DependencyManagement;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.maven.model.maven2.MavenProject.Properties;
import org.sonar.maven.model.maven2.Plugin;
import org.sonar.maven.model.maven2.PluginExecution.Configuration;
import org.w3c.dom.Element;

import javax.annotation.Nullable;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PomParserTest {
  private static final File UNRELATED_XML_FILE = new File("src/test/files/xml/parsing.xml");
  private static final File SIMPLE_POM_FILE = new File("src/test/files/xml/maven/simple-project/pom.xml");
  private static final File COMPLEX_ELEMENT_POM_FILE = new File("src/test/files/xml/maven/complex-element/pom.xml");
  private static final File PARSE_ISSUE_POM_FILE = new File("src/test/files/xml/maven/parse-issue/pom.xml");

  @Test
  public void should_parse_simple_pom() throws Exception {
    MavenProject project = PomParser.parseXML(SIMPLE_POM_FILE);
    assertThat(project).isNotNull();

    assertThat(project.getProperties().getElements()).hasSize(3);
    assertThat(project.getDependencies().getDependencies()).hasSize(4);
    assertThat(project.getBuild().getPlugins().getPlugins()).hasSize(1);
    assertThat(project.getScm()).isNull();
    assertThat(project.getParent()).isNull();
  }

  @Test
  public void should_not_parse_unrelated_xml_files() throws Exception {
    assertThat(PomParser.parseXML(UNRELATED_XML_FILE)).isNull();
  }

  @Test
  public void should_parse_incompletly_pom_with_complex_elements() throws Exception {
    MavenProject project = PomParser.parseXML(COMPLEX_ELEMENT_POM_FILE);
    Configuration config = project.getBuild().getPlugins().getPlugins().get(0).getExecutions().getExecutions().get(0).getConfiguration();
    checkPosition(config, 16, 13, 24, 13);
    assertThat(config).isNotNull();

    List<Element> values = config.getElements();
    assertThat(values).hasSize(2);
  }

  @Test
  public void should_fail_parsing() {
    MavenProject project = PomParser.parseXML(PARSE_ISSUE_POM_FILE);
    assertThat(project).isNull();
  }

  @Test
  public void should_not_parse() {
    MavenProject project = PomParser.parseXML(new File("."));
    assertThat(project).isNull();
  }

  @Test
  public void should_retrieve_attributes() throws Exception {
    MavenProject project = PomParser.parseXML(SIMPLE_POM_FILE);

    checkAttribute(project.getModelVersion(), "4.0.0", 3, 17, 3, 22);
    checkAttribute(project.getGroupId(), "org.sonarsource.java", 5, 12, 5, 32);
    checkAttribute(project.getArtifactId(), "simple-project", 6, 15, 6, 29);
    checkAttribute(project.getPackaging(), "jar", 10, 14, 10, 17);
    checkAttribute(project.getName(), "simple-project", 12, 9, 12, 23);
    checkAttribute(project.getUrl(), "http://maven.apache.org", 13, 8, 13, 31);

    // starting column is unknown
    checkAttribute(project.getVersion(), "1.0-SNAPSHOT", 7, -1, 9, 10);
    // empty property
    checkAttribute(project.getDescription(), "", 14, 16, 14, 16);
  }

  @Test
  public void should_retrieve_properties() throws Exception {
    MavenProject project = PomParser.parseXML(SIMPLE_POM_FILE);

    Properties properties = project.getProperties();
    checkPosition(properties, 19, 1, 24, 1);
    List<Element> values = properties.getElements();
    assertThat(values).hasSize(3);
  }

  @Test
  public void should_retrieve_dependencies_from_dependency_management() {
    MavenProject project = PomParser.parseXML(SIMPLE_POM_FILE);
    DependencyManagement dependencyManagement = project.getDependencyManagement();
    checkPosition(dependencyManagement, 26, 3, 36, 3);
    Dependency dependency = dependencyManagement.getDependencies().getDependencies().get(0);
    checkAttribute(dependency.getGroupId(), "fake", 29, 18, 29, 22);
    checkAttribute(dependency.getArtifactId(), "mock", 30, 21, 30, 25);
    checkAttribute(dependency.getVersion(), "4.0", 31, 18, 31, 21);
    checkAttribute(dependency.getScope(), "system", 32, 16, 32, 22);
    checkAttribute(dependency.getSystemPath(), "hello", 33, 21, 33, 26);
  }

  @Test
  public void should_retrieve_configurations() throws Exception {
    MavenProject project = PomParser.parseXML(SIMPLE_POM_FILE);
    List<Plugin> plugins = project.getBuild().getPlugins().getPlugins();
    Configuration configuration = plugins.get(0).getExecutions().getExecutions().get(0).getConfiguration();
    checkPosition(configuration, 88, 13, 90, 13);
    List<Element> values = configuration.getElements();
    assertThat(values).hasSize(1);
  }

  private static void checkAttribute(LocatedAttribute attribute, @Nullable String value, int startLine, int startColumn, int endLine, int endColumn) {
    String attributeValue = attribute.getValue();
    if (value == null) {
      assertThat(attributeValue).isNull();
    } else {
      assertThat(attributeValue.trim()).isEqualTo(value);
    }
    checkPosition(attribute, startLine, startColumn, endLine, endColumn);
  }

  private static void checkPosition(LocatedTree tree, int startLine, int startColumn, int endLine, int endColumn) {
    assertThat(tree.startLocation()).overridingErrorMessage("start location should have been correctly parsed").isNotNull();
    assertThat(tree.startLocation().line()).isEqualTo(startLine);
    assertThat(tree.startLocation().column()).isEqualTo(startColumn);
    assertThat(tree.endLocation()).overridingErrorMessage("end location should have been correctly parsed").isNotNull();
    assertThat(tree.endLocation().line()).isEqualTo(endLine);
    assertThat(tree.endLocation().column()).isEqualTo(endColumn);
  }
}
