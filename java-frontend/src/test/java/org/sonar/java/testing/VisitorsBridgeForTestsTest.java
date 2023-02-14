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
package org.sonar.java.testing;

import java.io.File;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.caching.CacheContextImpl;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VisitorsBridgeForTestsTest {

  @Test
  void test_semantic_disabled() {
    SensorContextTester context = SensorContextTester.create(new File("")).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    SonarComponents sonarComponents = new SonarComponents(null, context.fileSystem(), null, null, null);
    sonarComponents.setSensorContext(context);

    Tree parse = JParserTestUtils.parse("class A{}");
    VisitorsBridgeForTests visitorsBridgeForTests = new VisitorsBridgeForTests(Collections.singletonList(new DummyVisitor()), sonarComponents, new JavaVersionImpl());
    visitorsBridgeForTests.setCurrentFile(TestUtils.emptyInputFile("dummy.java"));
    visitorsBridgeForTests.visitFile(parse, false);
    assertThat(visitorsBridgeForTests.lastCreatedTestContext().getSemanticModel()).isNull();

    parse = JParserTestUtils.parse("class A{}");
    visitorsBridgeForTests = new VisitorsBridgeForTests(new DummyVisitor(), sonarComponents);
    visitorsBridgeForTests.setCurrentFile(TestUtils.emptyInputFile("dummy.java"));
    visitorsBridgeForTests.visitFile(parse, false);
    assertThat(visitorsBridgeForTests.lastCreatedTestContext().getSemanticModel()).isNotNull();
  }

  @Test
  void test_report_with_analysis_message() {
    SensorContextTester context = SensorContextTester.create(new File("")).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    SonarComponents sonarComponents = new SonarComponents(null, context.fileSystem(), null, null, null);
    sonarComponents.setSensorContext(context);

    Tree parse = JParserTestUtils.parse("class A{}");
    DummyVisitor javaCheck = new DummyVisitor();
    VisitorsBridgeForTests visitorsBridgeForTests = new VisitorsBridgeForTests(Collections.singletonList(javaCheck), sonarComponents, new JavaVersionImpl());
    visitorsBridgeForTests.setCurrentFile(TestUtils.emptyInputFile("dummy.java"));
    visitorsBridgeForTests.visitFile(parse, false);
    JavaFileScannerContextForTests lastContext = visitorsBridgeForTests.lastCreatedTestContext();
    assertThat(lastContext.getIssues()).isEmpty();

    AnalyzerMessage message = lastContext.createAnalyzerMessage(javaCheck, parse, "test");
    lastContext.addIssue(-1, javaCheck, "test");
    lastContext.addIssue(-1, javaCheck, "test", 15);
    lastContext.addIssueOnFile(javaCheck, "test");
    lastContext.addIssueOnProject(javaCheck, "test");
    lastContext.reportIssue(message);
    assertThat(message.getMessage()).isEqualTo("test");
    assertThat(lastContext.getIssues()).hasSize(5);
  }

  @Test
  void create_InputFileScannerContext_also_sets_testContext_field() {
    SensorContextTester context = SensorContextTester.create(new File("")).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    SonarComponents sonarComponents = new SonarComponents(null, context.fileSystem(), null, null, null);
    sonarComponents.setSensorContext(context);
    DummyVisitor javaCheck = new DummyVisitor();
    VisitorsBridgeForTests visitorsBridgeForTests = new VisitorsBridgeForTests(Collections.singletonList(javaCheck), sonarComponents, new JavaVersionImpl());
    var inputFile = mock(InputFile.class);

    var expectedTestContext =
      visitorsBridgeForTests.createScannerContext(sonarComponents, inputFile, new JavaVersionImpl(), false, CacheContextImpl.of(context));

    assertThat(visitorsBridgeForTests.lastCreatedTestContext()).isSameAs(expectedTestContext);
  }

  @Test
  void lastCreatedModuleContext_returns_last_created_module_context() {
    var bridge = new VisitorsBridgeForTests(Collections.emptyList(), mock(SonarComponents.class), new JavaVersionImpl());
    var firstModuleContext = bridge.createScannerContext(null, new JavaVersionImpl(), false, null);
    var secondAndExpectedModuleContext = bridge.createScannerContext(null, new JavaVersionImpl(), false, null);
    var firstTestContext = bridge.createScannerContext((CompilationUnitTree) null, null, null, false);
    var secondTestContext = bridge.createScannerContext(null, (InputFile) null, new JavaVersionImpl(), false, null);

    assertThat(bridge.lastCreatedModuleContext())
      .isSameAs(secondAndExpectedModuleContext)
      .isNotSameAs(firstModuleContext)
      .isNotSameAs(firstTestContext)
      .isNotSameAs(secondTestContext);
  }

  private static class DummyVisitor implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
    }
  }
}
