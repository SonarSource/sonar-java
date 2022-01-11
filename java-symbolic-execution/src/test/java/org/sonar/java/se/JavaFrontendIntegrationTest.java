/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.se;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.check.Rule;
import org.sonar.java.AnalysisException;
import org.sonar.java.CheckFailureException;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.cfg.CFG;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.java.se.utils.SETestUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class JavaFrontendIntegrationTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private SensorContextTester context;
  private SonarComponents sonarComponents;
  private CompilationUnitTreeImpl tree;
  private InputFile inputFile;

  private static final NullPointerException NPE = new NullPointerException("Nobody expect the spanish inquisition!");

  @BeforeEach
  void setup() throws IOException {
    context = SensorContextTester.create(Paths.get(""));
    context.setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(7, 9)));

    sonarComponents = new SonarComponents(
      Mockito.mock(FileLinesContextFactory.class),
      context.fileSystem(),
      Mockito.mock(ClasspathForMain.class),
      Mockito.mock(ClasspathForTest.class),
      Mockito.mock(CheckFactory.class));
    sonarComponents.setSensorContext(context);

    inputFile = SETestUtils.inputFile("src/test/files/se/SimpleClass.java");
    tree = (CompilationUnitTreeImpl) JParserTestUtils.parse(inputFile.contents());
  }

  /**
   * Extracted from org.sonar.java.model.DefaultJavaFileScannerContextWithSensorContextTest
   */
  @Test
  void DefaultJavaFileScannerContext_should_report_se_issue_with_flow() throws Exception {
    // fake issue
    List<JavaFileScannerContext.Location> flow1 = Collections.singletonList(new JavaFileScannerContext.Location("SE flow1", tree));
    List<JavaFileScannerContext.Location> flow2 = Collections.singletonList(new JavaFileScannerContext.Location("SE flow2", tree));
    Set<List<JavaFileScannerContext.Location>> flows = SetUtils.immutableSetOf(flow1, flow2);

    // spy getRuleKey call, to avoid mocking CheckFactory and Checks
    sonarComponents = Mockito.spy(sonarComponents);
    Mockito.when(sonarComponents.getRuleKey(Mockito.any())).thenReturn(Optional.of(RuleKey.of("repository", "rule")));

    JavaFileScannerContext scannerContext = new DefaultJavaFileScannerContext(tree, inputFile, tree.sema, sonarComponents, null, true, false);
    scannerContext.reportIssueWithFlow(new SE0_DoesNothing(), tree, "msg", flows, null);

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);

    Issue issue = issues.iterator().next();
    assertThat(issue.flows()).hasSize(2);
  }

  /**
   * Extracted from org.sonar.java.ast.JavaAstScannerTest
   */
  @Test
  void JavaAstScanner_should_swallow_log_and_report_checks_exceptions_for_symbolic_execution() {
    VisitorsBridge visitorsBridge = visitorsBridgeWithSymbolicExecution(new SE3_ThrowingNPEInit());

    JavaAstScanner scanner = new JavaAstScanner(sonarComponents);
    scanner.setVisitorBridge(visitorsBridge);

    scanner.scan(Collections.singletonList(inputFile));

    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith("Unable to run check class org.sonar.java.se.SymbolicExecutionVisitor");
  }

  /**
   * Extracted from org.sonar.java.model.VisitorsBridgeTest
   */
  @Test
  void VisitorsBridge_rethrows_exception_when_hidden_property_set_to_true_with_SECheck() {
    context.setSettings(new MapSettings().setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, true));

    VisitorsBridge visitorsBridge = visitorsBridgeWithSymbolicExecution(new SE1_ThrowingNPEPreStatement(), new SE2_ThrowingNPEPostStatement());

    try {
      visitorsBridge.visitFile(tree);
      Fail.fail("scanning of file should have raise an exception");
    } catch (AnalysisException e) {
      assertThat(e.getMessage()).contains("Failing check");
      assertThat(e.getCause()).isInstanceOf(CheckFailureException.class);
      assertThat(e.getCause().getCause()).isSameAs(NPE);
    } catch (Exception e) {
      Fail.fail("Should have been an AnalysisException");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith("Unable to run check class org.sonar.java.se.SymbolicExecutionVisitor");
  }

  /**
   * Extracted from org.sonar.java.model.VisitorsBridgeTest
   */
  @Test
  void VisitorsBridge_rethrows_swallow_exception_when_hidden_property_set_to_false_with_SECheck() {
    context.setSettings(new MapSettings().setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, false));

    VisitorsBridge visitorsBridge = visitorsBridgeWithSymbolicExecution(new SE1_ThrowingNPEPreStatement(), new SE2_ThrowingNPEPostStatement());

    try {
      visitorsBridge.visitFile(tree);
    } catch (Exception e) {
      Fail.fail("Exception should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).startsWith("Unable to run check class org.sonar.java.se.SymbolicExecutionVisitor");
  }

  private VisitorsBridge visitorsBridgeWithSymbolicExecution(SECheck... seChecks) {
    List<SECheck> seChecksList = Arrays.asList(seChecks);

    List<JavaCheck> visitors = new ArrayList<>();
    visitors.add(new SymbolicExecutionVisitor(seChecksList));
    visitors.addAll(seChecksList);

    VisitorsBridge visitorsBridge = new VisitorsBridge(visitors, Collections.emptyList(), sonarComponents);
    visitorsBridge.setCurrentFile(inputFile);

    return visitorsBridge;
  }

  @Rule(key = "SE0")
  private static class SE0_DoesNothing extends SECheck {
  }

  @Rule(key = "SE1")
  private static class SE1_ThrowingNPEPreStatement extends SECheck {
    @Override
    public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
      throw NPE;
    }
  }

  @Rule(key = "SE2")
  private static class SE2_ThrowingNPEPostStatement extends SECheck {
    @Override
    public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
      throw NPE;
    }
  }

  @Rule(key = "SE3")
  private static class SE3_ThrowingNPEInit extends SECheck {
    @Override
    public void init(MethodTree methodTree, CFG cfg) {
      throw NPE;
    }
  }
}
