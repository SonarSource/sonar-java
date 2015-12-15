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
package org.sonar.plugins.java;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.resources.Project;
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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JavaSquidSensorTest {

  private final DefaultFileSystem fileSystem = new DefaultFileSystem(null);
  private JavaSquidSensor sensor;

  @Before
  public void setUp() {
    sensor = new JavaSquidSensor(new JavaClasspath(mock(Project.class),
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
  public void test_issues_creation() throws Exception {
    Settings settings = new Settings();
    DefaultFileSystem fs = new DefaultFileSystem(new File("src/test/java/"));
    File file = new File("src/test/java/org/sonar/plugins/java/JavaSquidSensorTest.java");
    fs.add(new DefaultInputFile(file.getPath()).setFile(file).setLanguage("java"));
    Project project = mock(Project.class);
    JavaClasspath javaClasspath = new JavaClasspath(project, settings, fs);

    SonarComponents sonarComponents = createSonarComponentsMock(fs);
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(fs, javaClasspath, mock(SuppressWarningsFilter.class));
    JavaSquidSensor jss = new JavaSquidSensor(javaClasspath, sonarComponents, fs, javaResourceLocator, settings, mock(NoSonarFilter.class));
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(InputPath.class))).thenReturn(org.sonar.api.resources.File.create("src/test/java/org/sonar/plugins/java/JavaSquidSensorTest.java"));

    jss.analyse(project, context);

    String message = "Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.";
    verify(sonarComponents, times(3)).addIssue(eq(file.getAbsoluteFile()), any(JavaCheck.class), any(Integer.class), eq(message), isNull(Double.class));

    settings.setProperty(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY, true);
    jss.analyse(project, context);

    settings.setProperty(Java.SOURCE_VERSION, "wrongFormat");
    jss.analyse(project, context);

    settings.setProperty(Java.SOURCE_VERSION, "1.7");
    jss.analyse(project, context);
  }

  private static SonarComponents createSonarComponentsMock(DefaultFileSystem fs) {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    BadMethodName_S00100_Check check = new BadMethodName_S00100_Check();
    when(sonarComponents.checkClasses()).thenReturn(new CodeVisitor[]{check});

    Symbolizable symbolizable = mock(Symbolizable.class);
    when(sonarComponents.symbolizableFor(any(File.class))).thenReturn(symbolizable);
    when(symbolizable.newSymbolTableBuilder()).thenReturn(mock(Symbolizable.SymbolTableBuilder.class));
    when(sonarComponents.fileLinesContextFor(any(File.class))).thenReturn(mock(FileLinesContext.class));

    Highlightable highlightable = mock(Highlightable.class);
    when(highlightable.newHighlighting()).thenReturn(mock(Highlightable.HighlightingBuilder.class));
    when(sonarComponents.highlightableFor(any(File.class))).thenReturn(highlightable);

    when(sonarComponents.getFileSystem()).thenReturn(fs);

    Checks<JavaCheck> checks = mock(Checks.class);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("squid", RuleAnnotationUtils.getRuleKey(BadMethodName_S00100_Check.class)));
    when(sonarComponents.checks()).thenReturn(Lists.<Checks<JavaCheck>>newArrayList(checks));

    return sonarComponents;
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("JavaSquidSensor");
  }

}
