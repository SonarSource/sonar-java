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

import java.io.File;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultInputFileScannerContextTest {

  protected static final File JAVA_FILE = new File("src/test/files/api/JavaFileScannerContext.java");
  protected static final InputFile JAVA_INPUT_FILE = TestUtils.inputFile(JAVA_FILE);
  protected static final File WORK_DIR = new File("target");
  protected static final File BASE_DIR = new File("");
  protected static final InputComponent PROJECT_BASE_DIR = new DefaultInputDir("", BASE_DIR.getAbsolutePath());
  protected static final int COST = 42;
  protected static final JavaCheck CHECK = new JavaCheck() { };
  protected static final JavaCheck END_OF_ANALYSIS_CHECK = new NoopEndOfAnalysisCheck();
  protected SonarComponents sonarComponents;
  protected CompilationUnitTree compilationUnitTree;
  protected DefaultJavaFileScannerContext context;
  protected AnalyzerMessage reportedMessage;

  private static class NoopEndOfAnalysisCheck implements EndOfAnalysis, JavaCheck {
    @Override
    public void endOfAnalysis(ModuleScannerContext context) {
      // Do nothing
    }
  }

  @BeforeEach
  public void setup() {
    sonarComponents = createSonarComponentsMock();
    compilationUnitTree = JParserTestUtils.parse(JAVA_FILE);
    context = new DefaultJavaFileScannerContext(compilationUnitTree, JAVA_INPUT_FILE, null, sonarComponents, new JavaVersionImpl(), true, false);
    reportedMessage = null;
  }

  @Test
  void getModuleKey() {
    var moduleKey = "some/random/module/key";
    doReturn(moduleKey).when(sonarComponents).getModuleKey();
    var ctx = new DefaultJavaFileScannerContext(null, null, null, sonarComponents, null, false, false);
    assertThat(ctx.getModuleKey()).isEqualTo(moduleKey);
  }

  private SonarComponents createSonarComponentsMock() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doAnswer(invocation -> {
      reportedMessage = (AnalyzerMessage) invocation.getArguments()[0];
      return null;
    }).when(sonarComponents).reportIssue(any(AnalyzerMessage.class));

    doAnswer(invocation -> {
      Integer cost = invocation.getArgument(4);
      reportedMessage = new AnalyzerMessage(invocation.getArgument(1),
        invocation.getArgument(0),
        null,
        invocation.getArgument(3),
        cost != null ? cost : 0);
      return null;
    }).when(sonarComponents).addIssue(any(InputComponent.class), any(JavaCheck.class), anyInt(), anyString(), any());

    when(sonarComponents.fileLines(any(InputFile.class))).thenReturn(Arrays.asList("1st line", "2nd line"));
    when(sonarComponents.inputFileContents(any(InputFile.class))).thenReturn("content");
    when(sonarComponents.projectLevelWorkDir()).thenReturn(WORK_DIR);
    when(sonarComponents.project()).thenReturn(PROJECT_BASE_DIR);

    return sonarComponents;
  }
}
