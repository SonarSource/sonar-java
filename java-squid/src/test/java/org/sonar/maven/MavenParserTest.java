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
package org.sonar.maven;

import org.junit.Test;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.maven.model.maven2.MavenProject.Properties;
import org.sonar.maven.model.maven2.Plugin;
import org.sonar.maven.model.maven2.PluginExecution.Configuration;
import org.w3c.dom.Element;

import javax.annotation.Nullable;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class MavenParserTest {
  private static final File SIMPLE_POM_FILE = new File("src/test/files/maven/simple-project-pom.xml");
  private static final File COMPLEX_ELEMENT_POM_FILE = new File("src/test/files/maven/complex-element-pom.xml");
  private static final File PARSE_ISSUE_POM_FILE = new File("src/test/files/maven/parse-issue-pom.xml");

  @Test
  public void should_parse_simple_pom() throws Exception {
    MavenProject project = MavenParser.parseXML(SIMPLE_POM_FILE);
    assertThat(project).isNotNull();

    assertThat(project.getProperties().getElements()).hasSize(3);
    assertThat(project.getDependencies().getDependencies()).hasSize(4);
    assertThat(project.getBuild().getPlugins().getPlugins()).hasSize(1);
    assertThat(project.getScm()).isNull();
    assertThat(project.getParent()).isNull();
  }

  @Test
  public void should_parse_incompletly_pom_with_complex_elements() throws Exception {
    MavenProject project = MavenParser.parseXML(COMPLEX_ELEMENT_POM_FILE);
    Configuration config = project.getBuild().getPlugins().getPlugins().get(0).getExecutions().getExecutions().get(0).getConfiguration();
    checkPosition(config, 16, 13, 24, 13);
    assertThat(config).isNotNull();

    List<Element> values = config.getElements();
    assertThat(values).hasSize(2);
  }

  @Test
  public void should_fail_parsing() {
    MavenProject project = MavenParser.parseXML(PARSE_ISSUE_POM_FILE);
    assertThat(project).isNull();
  }

  @Test
  public void should_not_parse() {
    MavenProject project = MavenParser.parseXML(new File("."));
    assertThat(project).isNull();
  }

  @Test
  public void should_retrieve_attributes() throws Exception {
    MavenProject project = MavenParser.parseXML(SIMPLE_POM_FILE);

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
    MavenProject project = MavenParser.parseXML(SIMPLE_POM_FILE);

    Properties properties = project.getProperties();
    checkPosition(properties, 19, 3, 24, 3);
    List<Element> values = properties.getElements();
    assertThat(values).hasSize(3);
  }

  @Test
  public void should_retrieve_configurations() throws Exception {
    MavenProject project = MavenParser.parseXML(SIMPLE_POM_FILE);
    List<Plugin> plugins = project.getBuild().getPlugins().getPlugins();
    Configuration configuration = plugins.get(0).getExecutions().getExecutions().get(0).getConfiguration();
    checkPosition(configuration, 76, 13, 78, 13);
    List<Element> values = configuration.getElements();
    assertThat(values).hasSize(1);
  }

  private static void checkAttribute(LocatedAttribute attribute, @Nullable String value, int... coordinates) {
    String attributeValue = attribute.getValue();
    if (value == null) {
      assertThat(attributeValue).isNull();
    } else {
      assertThat(attributeValue.trim()).isEqualTo(value);
    }
    checkPosition(attribute, coordinates);
  }

  private static void checkPosition(LocatedTree tree, int... coordinates) {
    assertThat(tree.startLocation()).overridingErrorMessage("start location should have been correctly parsed").isNotNull();
    assertThat(tree.startLocation().line()).isEqualTo(coordinates[0]);
    assertThat(tree.startLocation().column()).isEqualTo(coordinates[1]);
    assertThat(tree.endLocation()).overridingErrorMessage("end location should have been correctly parsed").isNotNull();
    assertThat(tree.endLocation().line()).isEqualTo(coordinates[2]);
    assertThat(tree.endLocation().column()).isEqualTo(coordinates[3]);
  }
}
