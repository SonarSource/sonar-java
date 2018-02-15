/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.plugins.java;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.xml.maven.PomElementOrderCheck;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XmlFileSensorTest {

  private XmlFileSensor sensor;

  @Before
  public void setUp() {
    DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
    sensor = new XmlFileSensor(mock(SonarComponents.class), fileSystem);
  }

  @Test
  public void describe() {
    SensorDescriptor sensorDescriptor = mock(SensorDescriptor.class);
    sensor.describe(sensorDescriptor);
    verify(sensorDescriptor).name("SonarJavaXmlFileSensor");
  }

  @Test
  public void test_issues_creation() throws Exception {
    SensorContextTester context = SensorContextTester.create(new File("src/test/files/maven/").getAbsoluteFile());
    DefaultFileSystem fs = context.fileSystem();
    final File file = new File("src/test/files/maven/pom.xml");
    DefaultInputFile inputFile = new TestInputFileBuilder("", "pom.xml").setModuleBaseDir(fs.baseDirPath()).setPublish(false).build();
    fs.add(inputFile);
    SonarComponents sonarComponents = createSonarComponentsMock(fs, file);
    XmlFileSensor sensor = new XmlFileSensor(sonarComponents, fs);

    assertThat(inputFile.isPublished()).isFalse();
    sensor.execute(context);
    assertThat(inputFile.isPublished()).isTrue();

    verify(sonarComponents, times(1)).reportIssue(Mockito.argThat(argument -> file.getAbsolutePath().equals(argument.getFile().getAbsolutePath())));
  }

  @Test
  public void test_no_issues_but_xml_file_still_published() throws Exception {
    SensorContextTester context = SensorContextTester.create(new File("src/test/files/maven2/").getAbsoluteFile());
    DefaultFileSystem fs = context.fileSystem();
    final File file = new File("src/test/files/maven2/pom.xml");
    DefaultInputFile inputFile = new TestInputFileBuilder("", "pom.xml").setModuleBaseDir(fs.baseDirPath()).setPublish(false).build();
    fs.add(inputFile);
    SonarComponents sonarComponents = createSonarComponentsMock(fs, file);
    XmlFileSensor sensor = new XmlFileSensor(sonarComponents, fs);

    assertThat(inputFile.isPublished()).isFalse();
    sensor.execute(context);
    assertThat(inputFile.isPublished()).isTrue();

    verify(sonarComponents, never()).reportIssue(Mockito.argThat(argument -> file.getAbsolutePath().equals(argument.getFile().getAbsolutePath())));
  }

  @Test
  public void not_executed_without_xml_files_in_file_system() throws Exception {
    SensorContextTester context = SensorContextTester.create(new File("src/test/files/maven/"));
    DefaultFileSystem fs = context.fileSystem();
    SonarComponents sonarComponents = createSonarComponentsMock(fs);
    XmlFileSensor sensor = new XmlFileSensor(sonarComponents, fs);

    sensor.execute(context);

    verify(sonarComponents, Mockito.never()).reportIssue(any());
  }

  private static SonarComponents createSonarComponentsMock(DefaultFileSystem fs) throws IOException {
    return createSonarComponentsMock(fs, null);
  }

  private static SonarComponents createSonarComponentsMock(DefaultFileSystem fs, @Nullable File file) throws IOException {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.checkClasses()).thenReturn(new JavaCheck[] {new PomElementOrderCheck()});

    when(sonarComponents.getFileSystem()).thenReturn(fs);
    if (file != null) {
      when(sonarComponents.fileLines(any(File.class))).thenReturn(Files.readLines(file, StandardCharsets.UTF_8));
    }

    Checks<JavaCheck> checks = mock(Checks.class);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("squid", RuleAnnotationUtils.getRuleKey(PomElementOrderCheck.class)));
    when(sonarComponents.checks()).thenReturn(Lists.<Checks<JavaCheck>>newArrayList(checks));

    return sonarComponents;
  }
}
