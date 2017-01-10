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
package org.sonar.java;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.PathUtils;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MeasurerTest {

  private static final int NB_OF_METRICS = 10;
  private SensorContextTester context;
  private JavaSquid squid;
  private File baseDir;
  private DefaultFileSystem fs;

  @Before
  public void setUp() throws Exception {
    baseDir = new File("src/test/files/metrics");
    context = SensorContextTester.create(baseDir);
    fs = context.fileSystem();
  }

  @Test
  public void verify_methods_metric() {
    checkMetric("Methods.java", "functions", 7);
  }

  @Test
  public void verify_class_metric() {
    checkMetric("Classes.java", "classes", 8);
  }

  @Test
  public void verify_complexity_metric() {
    checkMetric("Complexity.java", "complexity", 16);
  }

  @Test
  public void verify_complexity_in_classes() {
    checkMetric("Complexity.java", "complexity_in_classes", 16);
  }

  @Test
  public void verify_function_metric() {
    checkMetric("Complexity.java", "functions", 8);
  }

  @Test
  public void verify_comments_metric() {
    checkMetric("Comments.java", "comment_lines", 3);
  }

  @Test
  public void verify_statements_metric() {
    checkMetric("Statements.java", "statements", 18);
  }

  @Test
  public void verify_ncloc_metric() {
    checkMetric("LinesOfCode.java", "ncloc", 2);
    checkMetric("CommentedOutFile.java", "ncloc", 0);
    checkMetric("EmptyFile.java", "ncloc", 0);
  }

  /**
   * Utility method to quickly get metric out of a file.
   */
  private void checkMetric(String filename, String metric, Number expectedValue) {
    String relativePath = PathUtils.sanitize(new File(baseDir, filename).getPath());
    DefaultInputFile inputFile = new DefaultInputFile(context.module().key(), relativePath);
    inputFile.setModuleBaseDir(fs.baseDirPath());
    fs.add(inputFile);
    Measurer measurer = new Measurer(fs, context, mock(NoSonarFilter.class));
    JavaConfiguration conf = new JavaConfiguration(StandardCharsets.UTF_8);
    squid = new JavaSquid(conf, null, measurer, null, null, new CodeVisitor[0]);
    squid.scan(Lists.newArrayList(new File(baseDir, filename)), Collections.emptyList());
    assertThat(context.measures("projectKey:"+relativePath)).hasSize(NB_OF_METRICS);
    assertThat(context.measure("projectKey:"+relativePath, metric).value()).isEqualTo(expectedValue);
  }

}
