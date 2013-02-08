/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.checkstyle;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import static org.fest.assertions.Assertions.assertThat;

public class CheckstyleConfigurationTest {

  @Test
  public void writeConfigurationToWorkingDir() throws IOException {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "writeConfigurationToWorkingDir/pom.xml");

    CheckstyleProfileExporter exporter = new FakeExporter();
    CheckstyleConfiguration configuration = new CheckstyleConfiguration(null, exporter, null, project.getFileSystem());
    File xmlFile = configuration.getXMLDefinitionFile();

    assertThat(xmlFile.exists()).isTrue();
    assertThat(FileUtils.readFileToString(xmlFile)).isEqualTo("<conf/>");
  }

  public class FakeExporter extends CheckstyleProfileExporter {
    @Override
    public void exportProfile(RulesProfile profile, Writer writer) {
      try {
        writer.write("<conf/>");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
