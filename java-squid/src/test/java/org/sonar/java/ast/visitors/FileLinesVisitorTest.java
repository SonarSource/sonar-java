/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.ast.visitors;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.AstScanner;

import java.io.File;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileLinesVisitorTest {

  @Test
  public void lines_of_code_data() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    FileLinesContext context = mock(FileLinesContext.class);
    when(sonarComponents.fileLinesContextFor(Mockito.any(File.class))).thenReturn(context);

    AstScanner scanner = JavaAstScanner.create(new JavaConfiguration(Charsets.UTF_8), new FileLinesVisitor(sonarComponents, Charsets.UTF_8));
    File baseDir = new File("src/test/files/metrics");
    List<InputFile> inputFiles = InputFileUtils.create(baseDir, ImmutableList.of(new File("src/test/files/metrics/LinesOfCode.java")));
    scanner.scan(inputFiles);

    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 1, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 2, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 3, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 4, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 5, 1);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 6, 1);

    verify(context).save();
  }

  @Test
  public void comment_lines_data() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    FileLinesContext context = mock(FileLinesContext.class);
    when(sonarComponents.fileLinesContextFor(Mockito.any(File.class))).thenReturn(context);

    AstScanner scanner = JavaAstScanner.create(new JavaConfiguration(Charsets.UTF_8), new FileLinesVisitor(sonarComponents, Charsets.UTF_8));
    File baseDir = new File("src/test/files/metrics");
    List<InputFile> inputFiles = InputFileUtils.create(baseDir, ImmutableList.of(new File("src/test/files/metrics/Comments.java")));
    scanner.scan(inputFiles);

    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 1, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 2, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 3, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 4, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 5, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 6, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 7, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 8, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 9, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 10, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 11, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 12, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 13, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 14, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 15, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 16, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 17, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 18, 0);

    verify(context).save();
  }

}
