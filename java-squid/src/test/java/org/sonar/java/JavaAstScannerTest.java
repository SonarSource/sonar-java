/*
 * Sonar Java
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
package org.sonar.java;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.java.ast.AstScanner;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.indexer.QueryByType;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class JavaAstScannerTest {

  @Test
  public void files() {
    AstScanner scanner = JavaAstScanner.create(new JavaConfiguration(Charsets.UTF_8));
    File baseDir = new File("src/test/files/metrics");
    List<InputFile> inputFiles = InputFileUtils.create(baseDir,
        ImmutableList.of(new File("src/test/files/metrics/Lines.java"), new File("src/test/files/metrics/LinesOfCode.java")));
    scanner.scan(inputFiles);
    SourceProject project = (SourceProject) scanner.getIndex().search(new QueryByType(SourceProject.class)).iterator().next();
    assertThat(project.getInt(JavaMetric.FILES)).isEqualTo(2);
  }

  @Test
  public void lines() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Lines.java"));
    assertThat(file.getInt(JavaMetric.LINES)).isEqualTo(6);
  }

  @Test
  public void lines_of_code() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/LinesOfCode.java"));
    assertThat(file.getInt(JavaMetric.LINES_OF_CODE)).isEqualTo(2);
  }

  @Test
  public void comments() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Comments.java"));
    assertThat(file.getInt(JavaMetric.COMMENT_LINES_WITHOUT_HEADER)).isEqualTo(3);
    assertThat(file.getNoSonarTagLines()).contains(15).hasSize(1);
  }

  @Test
  public void statements() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Statements.java"));
    assertThat(file.getInt(JavaMetric.STATEMENTS)).isEqualTo(20);
  }

  @Test
  public void complexity() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Complexity.java"));
    assertThat(file.getInt(JavaMetric.COMPLEXITY)).isEqualTo(12);
  }

  @Test
  public void methods() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Methods.java"));
    assertThat(file.getInt(JavaMetric.METHODS)).isEqualTo(6);
  }

  @Test
  public void classes() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Classes.java"));
    assertThat(file.getInt(JavaMetric.CLASSES)).isEqualTo(2);
  }

  @Test
  public void packages() {
    AstScanner scanner = JavaAstScanner.create(new JavaConfiguration(Charsets.UTF_8));
    File baseDir = new File("src/test/files/metrics");
    List<InputFile> inputFiles = InputFileUtils.create(baseDir,
        ImmutableList.of(new File("src/test/files/metrics/Packages.java")));
    scanner.scan(inputFiles);
    SourceProject project = (SourceProject) scanner.getIndex().search(new QueryByType(SourceProject.class)).iterator().next();
    assertThat(project.getInt(JavaMetric.PACKAGES)).isEqualTo(1);
  }

}
