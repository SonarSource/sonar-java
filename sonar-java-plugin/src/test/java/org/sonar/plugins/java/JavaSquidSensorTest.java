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
package org.sonar.plugins.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaTestClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.naming.BadMethodNameCheck;
import org.sonar.java.filters.PostAnalysisIssueFilter;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JavaSquidSensorTest {

  private final DefaultFileSystem fileSystem = new DefaultFileSystem((File) null);
  private JavaSquidSensor sensor;

  @Before
  public void setUp() {
    sensor = new JavaSquidSensor(mock(SonarComponents.class), fileSystem,
      mock(DefaultJavaResourceLocator.class), new MapSettings(), mock(NoSonarFilter.class), new PostAnalysisIssueFilter(fileSystem));
  }

  @Test
  public void test_issues_creation_on_main_file() throws IOException {
    testIssueCreation(InputFile.Type.MAIN, 3);
  }

  @Test
  public void test_issues_creation_on_test_file() throws IOException { // NOSONAR required to test NOSONAR reporting on test files
    testIssueCreation(InputFile.Type.TEST, 0);
  }


  private void testIssueCreation(InputFile.Type onType, int expectedIssues) throws IOException {
    Settings settings = new MapSettings();
    NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
    SensorContextTester context = createContext(onType);
    DefaultFileSystem fs = context.fileSystem();
    SonarComponents sonarComponents = createSonarComponentsMock(context);
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(fs, new JavaClasspath(settings, fs));
    PostAnalysisIssueFilter postAnalysisIssueFilter = new PostAnalysisIssueFilter(fs);
    JavaSquidSensor jss = new JavaSquidSensor(sonarComponents, fs, javaResourceLocator, settings, noSonarFilter, postAnalysisIssueFilter);

    jss.execute(context);
    verify(noSonarFilter, times(1)).noSonarInFile(fs.inputFiles().iterator().next(), Sets.newHashSet(79));
    verify(sonarComponents, times(expectedIssues)).reportIssue(any(AnalyzerMessage.class));

    context = createContext(onType);
    fs = context.fileSystem();
    sonarComponents = createSonarComponentsMock(context);
    javaResourceLocator = new DefaultJavaResourceLocator(fs, new JavaClasspath(settings, fs));
    postAnalysisIssueFilter = new PostAnalysisIssueFilter(fs);
    jss = new JavaSquidSensor(sonarComponents, fs, javaResourceLocator, settings, noSonarFilter, postAnalysisIssueFilter);

    settings.setProperty(Java.SOURCE_VERSION, "wrongFormat");
    jss.execute(context);

    context = createContext(onType);
    fs = context.fileSystem();
    sonarComponents = createSonarComponentsMock(context);
    javaResourceLocator = new DefaultJavaResourceLocator(fs, new JavaClasspath(settings, fs));
    postAnalysisIssueFilter = new PostAnalysisIssueFilter(fs);
    jss = new JavaSquidSensor(sonarComponents, fs, javaResourceLocator, settings, noSonarFilter, postAnalysisIssueFilter);

    settings.setProperty(Java.SOURCE_VERSION, "1.7");
    jss.execute(context);
  }

  private static SensorContextTester createContext(InputFile.Type onType) throws IOException {
    SensorContextTester context = SensorContextTester.create(new File("src/test/java/"));
    DefaultFileSystem fs = context.fileSystem();

    String effectiveKey = "org/sonar/plugins/java/JavaSquidSensorTest.java";
    File file = new File(fs.baseDir(), effectiveKey);
    DefaultInputFile inputFile = new DefaultInputFile("", effectiveKey).setLanguage("java").setType(onType).initMetadata(new String(Files.readAllBytes(file.toPath()), "UTF-8"));
    fs.add(inputFile);
    return context;
  }

  private static SonarComponents createSonarComponentsMock(SensorContextTester contextTester) {

    CheckFactory checkFactory = mock(CheckFactory.class);
    Checks<Object> checks = mock(Checks.class);
    when(checks.addAnnotatedChecks(any(Iterable.class))).thenReturn(checks);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("squid", RuleAnnotationUtils.getRuleKey(BadMethodNameCheck.class)));

    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    when(javaTestClasspath.getElements()).thenReturn(ImmutableList.of());

    JavaClasspath javaClasspath = mock(JavaClasspath.class);
    when(javaClasspath.getElements()).thenReturn(ImmutableList.of());
    when(checkFactory.create(anyString())).thenReturn(checks);

    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
    SonarComponents sonarComponents = spy(new SonarComponents(fileLinesContextFactory, contextTester.fileSystem(), javaClasspath, javaTestClasspath, checkFactory));
    sonarComponents.setSensorContext(contextTester);

    BadMethodNameCheck check = new BadMethodNameCheck();
    when(sonarComponents.checkClasses()).thenReturn(new CodeVisitor[]{check});
    return sonarComponents;
  }

  @Test
  public void test_toString() {
    assertThat(sensor.toString()).isEqualTo("JavaSquidSensor");
  }

}
