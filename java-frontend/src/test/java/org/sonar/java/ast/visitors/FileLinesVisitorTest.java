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
package org.sonar.java.ast.visitors;

import java.io.File;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.java.JavaFrontend;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JavaVersionImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileLinesVisitorTest {

  private File baseDir;

  @BeforeEach
  void setUp() {
    baseDir = new File("src/test/files/metrics");
  }

  private void checkLines(String filename, FileLinesContext context) {
    InputFile inputFile = TestUtils.inputFile(new File(baseDir, filename));

    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.fileLinesContextFor(Mockito.any(InputFile.class))).thenReturn(context);

    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), null, null, null, null, new FileLinesVisitor(sonarComponents));

    frontend.scan(Collections.singletonList(inputFile), Collections.emptyList(), Collections.emptyList());
  }

  @Test
  void lines_of_code_data() {
    FileLinesContext context = mock(FileLinesContext.class);
    checkLines("LinesOfCode.java", context);

    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 1, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 2, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 3, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 4, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 5, 1);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 6, 1);

    verify(context).save();
  }

  @Test
  void executable_lines_should_be_counted() {
    FileLinesContext context = mock(FileLinesContext.class);
    checkLines("ExecutableLines.java", context);
    int[] expected = new int[] {0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1,
      0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0};
    assertThat(expected).hasSize(56);
    for (int i = 0; i < expected.length; i++) {
      int line = i + 1;
      verify(context).setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, expected[i]);
    }
    verify(context).save();
  }

}
