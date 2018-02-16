/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.PathUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MeasurerTest {

  private static final int NB_OF_METRICS = 11;
  private SensorContextTester context;
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
    checkMetric("Complexity.java", "complexity", 15);
  }

  @Test
  public void verify_cognitive_complexity_metric() {
    checkMetric("CognitiveComplexity.java", "cognitive_complexity", 25);
  }

  @Test
  public void verify_complexity_in_classes() {
    checkMetric("Complexity.java", "complexity_in_classes", 15);
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
    checkMetric(filename, metric, expectedValue, NB_OF_METRICS);
  }

  private void checkMetric(String filename, String metric, @Nullable Number expectedValue, int numberOfMetrics) {
    String relativePath = PathUtils.sanitize(new File(baseDir, filename).getPath());
    TestInputFileBuilder inputFile = new TestInputFileBuilder(context.module().key(), relativePath);
    inputFile.setModuleBaseDir(fs.baseDirPath());
    fs.add(inputFile.build());
    Measurer measurer = new Measurer(fs, context, mock(NoSonarFilter.class));
    JavaSquid squid = new JavaSquid(new JavaVersionImpl(), null, measurer, null, null, new JavaCheck[0]);
    squid.scan(Lists.newArrayList(new File(baseDir, filename)), Collections.emptyList());
    assertThat(context.measures("projectKey:" + relativePath)).hasSize(numberOfMetrics);
    Measure<Serializable> measure = context.measure("projectKey:" + relativePath, metric);
    if (expectedValue == null) {
      assertThat(measure).isNull();
    } else {
      assertThat(measure.value()).isEqualTo(expectedValue);
    }
  }

}
