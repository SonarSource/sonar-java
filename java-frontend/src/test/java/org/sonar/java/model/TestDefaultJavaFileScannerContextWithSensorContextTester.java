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
package org.sonar.java.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaTestClasspath;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test {@link DefaultJavaFileScannerContext} with {@link SensorContextTester} in {@link SonarComponents#setSensorContext(SensorContext)}
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TestDefaultJavaFileScannerContextWithSensorContextTester {

  private static final File JAVA_FILE = new File("src/test/files/api/JavaFileScannerContext.java");

  @Mock private FileLinesContextFactory fileLinesContextFactory;
  @Mock private JavaClasspath javaClasspath;
  @Mock private JavaTestClasspath javaTestClasspath;
  @Mock private CheckFactory checkFactory;

  private SensorContextTester sensorContext;
  private DefaultJavaFileScannerContext scannerContext;
  private Tree tree;
  private JavaCheck check = new JavaCheck() {
  };

  private SECheck seCheck = new SECheck() {
  };

  @Before
  public void setup() throws IOException {
    sensorContext = SensorContextTester.create(Paths.get(""));
    sensorContext.fileSystem().add(
      new DefaultInputFile("myProjectKey", JAVA_FILE.getPath())
        .setLanguage("java")
        .initMetadata(new String(Files.readAllBytes(JAVA_FILE.toPath()), StandardCharsets.UTF_8))
    );
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, sensorContext.fileSystem(), javaClasspath, javaTestClasspath, checkFactory);
    sonarComponents.setSensorContext(sensorContext);

    // spy getRuleKey call, to avoid mocking CheckFactory and Checks
    sonarComponents = spy(sonarComponents);
    when(sonarComponents.getRuleKey(any())).thenReturn(RuleKey.of("repository", "rule"));

    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser(StandardCharsets.UTF_8).parse(JAVA_FILE);
    tree = cut.types().get(0);
    scannerContext = new DefaultJavaFileScannerContext(cut, JAVA_FILE, null, sonarComponents, null, true);
  }

  @Test
  public void test_report_issue_with_secondary_locations() throws Exception {
    ImmutableList<JavaFileScannerContext.Location> secondary = ImmutableList.of(
      new JavaFileScannerContext.Location("+1", tree),
      new JavaFileScannerContext.Location("+1", tree)
    );
    scannerContext.reportIssue(check, tree, "msg", secondary, null);
    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.flows()).hasSize(2);
  }

  @Test
  public void test_report_issue_with_flow() throws Exception {
    ImmutableList<JavaFileScannerContext.Location> flow1 = ImmutableList.of(new JavaFileScannerContext.Location("flow1", tree));
    ImmutableList<JavaFileScannerContext.Location> flow2 = ImmutableList.of(new JavaFileScannerContext.Location("flow2", tree));
    ImmutableSet<List<JavaFileScannerContext.Location>> flows = ImmutableSet.of(flow1, flow2);
    scannerContext.reportIssueWithFlow(check, tree, "msg", flows, null);
    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.flows()).hasSize(2);
  }

  @Test
  public void test_report_se_issue_with_flow() throws Exception {
    ImmutableList<JavaFileScannerContext.Location> flow1 = ImmutableList.of(new JavaFileScannerContext.Location("SE flow1", tree));
    ImmutableList<JavaFileScannerContext.Location> flow2 = ImmutableList.of(new JavaFileScannerContext.Location("SE flow2", tree));
    ImmutableSet<List<JavaFileScannerContext.Location>> flows = ImmutableSet.of(flow1, flow2);

    scannerContext.reportIssueWithFlow(seCheck, tree, "msg", flows, null);
    Issue issue = sensorContext.allIssues().iterator().next();

    //issue is raised with two flows, but only one is expected because SONARJAVA-2109
    assertThat(issue.flows()).hasSize(1);
  }
}
