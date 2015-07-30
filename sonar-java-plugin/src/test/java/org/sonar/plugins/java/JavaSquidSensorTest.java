/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.plugins.java;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Symbolizable;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.BadMethodName_S00100_Check;
import org.sonar.java.filters.SuppressWarningsFilter;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaSquidSensorTest {

  private final DefaultFileSystem fileSystem = new DefaultFileSystem(null);
  private JavaSquidSensor sensor;

  @Before
  public void setUp() {
    sensor = new JavaSquidSensor(mock(RulesProfile.class), new JavaClasspath(mock(Project.class),
      new Settings(), new DefaultFileSystem(null)), mock(SonarComponents.class), fileSystem,
      mock(DefaultJavaResourceLocator.class), new Settings(), mock(NoSonarFilter.class));
  }

  @Test
  public void should_execute_on_java_project() {
    Project project = mock(Project.class);
    fileSystem.add(new DefaultInputFile("fake.php").setLanguage("php"));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    fileSystem.add(new DefaultInputFile("fake.java").setLanguage("java"));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void test_analyze_json_output() throws Exception {
    RulesProfile qp = RulesProfile.create("test", Java.KEY);
    Settings settings = new Settings();
    settings.setProperty(JavaPlugin.JSON_OUTPUT_FOLDER, "target");
    DefaultFileSystem fs = new DefaultFileSystem(new File("src/test/java/"));
    File file = new File("src/test/java/org/sonar/plugins/java/JavaSquidSensorTest.java");
    DefaultInputFile defaultInputFile = new DefaultInputFile(file.getPath()).setFile(file).setLanguage("java");
    fs.add(defaultInputFile);
    Project project = mock(Project.class);
    JavaClasspath javaClasspath = new JavaClasspath(project, settings, fs);

    SonarComponents sonarComponents = createSonarComponentsMock();
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(fs, javaClasspath, mock(SuppressWarningsFilter.class));
    JavaSquidSensor jss = new JavaSquidSensor(qp, javaClasspath, sonarComponents, fs, javaResourceLocator, settings, mock(NoSonarFilter.class));
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(InputPath.class))).thenReturn(org.sonar.api.resources.File.create("src/test/java/org/sonar/plugins/java/JavaSquidSensorTest.java"));

    jss.analyse(project, context);

    File outputFile = new File("target/squid-S00100.json");
    if(!outputFile.isFile()) {
      fail("Output json file " + file.getPath() + " was not created");
    }

    List<String> output = FileUtils.readLines(outputFile);
    assertThat(output).containsExactly("{", "'project:src/test/java/org/sonar/plugins/java/JavaSquidSensorTest.java':[", "73,", "83,", "138,", "],", "}");
  }

  private static SonarComponents createSonarComponentsMock() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    BadMethodName_S00100_Check check = new BadMethodName_S00100_Check();
    when(sonarComponents.checkClasses()).thenReturn(new CodeVisitor[] {check});

    Symbolizable symbolizable = mock(Symbolizable.class);
    Symbolizable.SymbolTableBuilder symboltableBuilder = mock(Symbolizable.SymbolTableBuilder.class);
    when(sonarComponents.symbolizableFor(any(File.class))).thenReturn(symbolizable);
    when(symbolizable.newSymbolTableBuilder()).thenReturn(symboltableBuilder);
    when(sonarComponents.fileLinesContextFor(any(File.class))).thenReturn(mock(FileLinesContext.class));

    Highlightable highlightable = mock(Highlightable.class);
    when(highlightable.newHighlighting()).thenReturn(mock(Highlightable.HighlightingBuilder.class));
    when(sonarComponents.highlightableFor(any(File.class))).thenReturn(highlightable);

    Checks<JavaCheck> checks = mock(Checks.class);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("squid", RuleAnnotationUtils.getRuleKey(BadMethodName_S00100_Check.class)));
    when(sonarComponents.checks()).thenReturn(Lists.<Checks<JavaCheck>>newArrayList(checks));


    ResourcePerspectives resourcePerspectives = mock(ResourcePerspectives.class);
    when(sonarComponents.getResourcePerspectives()).thenReturn(resourcePerspectives);
    when(resourcePerspectives.as(any(Issuable.class.getClass()), any(Resource.class))).thenReturn(mock(Issuable.class));
    return sonarComponents;
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("JavaSquidSensor");
  }

}
