/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test {@link DefaultJavaFileScannerContext} with {@link SensorContextTester} in {@link SonarComponents#setSensorContext(SensorContext)}
 *
 */
@ExtendWith(MockitoExtension.class)
class DefaultJavaFileScannerContextWithSensorContextTest {

  @Mock private FileLinesContextFactory fileLinesContextFactory;
  @Mock private ClasspathForMain javaClasspath;
  @Mock private ClasspathForTest javaTestClasspath;
  @Mock private CheckFactory checkFactory;
  @Mock private Server server;

  private SensorContextTester sensorContext;
  private DefaultJavaFileScannerContext scannerContext;
  private Tree tree;
  private JavaCheck check = new JavaCheck() {
  };
  
  @BeforeEach
  void setup() throws IOException {
    sensorContext = SensorContextTester.create(Paths.get(""));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, sensorContext.fileSystem(), javaClasspath, javaTestClasspath, checkFactory, sensorContext.activeRules());
    sonarComponents.setSensorContext(sensorContext);

    // spy getRuleKey call, to avoid mocking CheckFactory and Checks
    sonarComponents = spy(sonarComponents);
    when(sonarComponents.getRuleKey(any())).thenReturn(Optional.of(RuleKey.of("repository", "rule")));

    InputFile inputFile = TestUtils.inputFile("src/test/files/api/JavaFileScannerContext.java");
    CompilationUnitTree cut = JParserTestUtils.parse(inputFile.contents());
    tree = cut.types().get(0);
    scannerContext = new DefaultJavaFileScannerContext(cut, inputFile, null, sonarComponents, null, true, false);
  }

  @Test
  void test_report_issue_with_secondary_locations() throws Exception {
    List<JavaFileScannerContext.Location> secondary = Arrays.asList(
      new JavaFileScannerContext.Location("+1", tree),
      new JavaFileScannerContext.Location("+1", tree)
    );
    scannerContext.reportIssue(check, tree, "msg", secondary, null);
    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.flows()).hasSize(2);
  }

  @Test
  void test_report_issue_with_flow() throws Exception {
    List<JavaFileScannerContext.Location> flow1 = Collections.singletonList(new JavaFileScannerContext.Location("flow1", tree));
    List<JavaFileScannerContext.Location> flow2 = Collections.singletonList(new JavaFileScannerContext.Location("flow2", tree));
    Set<List<JavaFileScannerContext.Location>> flows = SetUtils.immutableSetOf(flow1, flow2);
    scannerContext.reportIssueWithFlow(check, tree, "msg", flows, null);
    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.flows()).hasSize(2);
  }

}
