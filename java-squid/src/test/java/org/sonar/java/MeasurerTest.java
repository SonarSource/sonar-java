/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.Measure;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MeasurerTest {

  private static final int NB_OF_METRICS = 14;
  private SensorContext context;
  private JavaSquid squid;
  private File baseDir;
  private DefaultFileSystem fs;

  @Before
  public void setUp() throws Exception {
    baseDir = new File("src/test/files/metrics");
    fs = new DefaultFileSystem(baseDir);
  }

  @Test
  public void verify_lines_metric() {
    checkMetric("Lines.java", "lines", 7.0);
    checkMetric("CommentedOutFile.java", "lines", 2.0);
    checkMetric("EmptyFile.java", "lines", 1.0);
  }

  @Test
  public void verify_methods_metric() {
    checkMetric("Methods.java", "functions", 7.0);
  }

  @Test
  public void verify_public_api_metric() {
    checkMetric("Comments.java", "public_api", 2.0);
  }

  @Test
  public void verify_public_api_density_metric() {
    checkMetric("Comments.java", "public_documented_api_density", 100.0);
  }

  @Test
  public void verify_public_undocumented_api() {
    checkMetric("Comments.java", "public_undocumented_api", 0.0);
  }

  @Test
  public void verify_class_metric() {
    checkMetric("Classes.java", "classes", 8.0);
  }

  @Test
  public void verify_accessors_metric() {
    checkMetric("Accessors.java", "accessors", 3.0);
  }

  @Test
  public void verify_complexity_metric() {
    checkMetric("Complexity.java", "complexity", 13.0);
  }

  @Test
  public void verify_function_metric_not_analysing_accessors() {
    checkMetric(false, baseDir, "Complexity.java", "functions", 7.0);
  }

  @Test
  public void verify_accessors_set_to_0_when_not_analysing_accessors() {
    checkMetric(false, baseDir, "Complexity.java", "accessors", 0.0);
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

  @Test
  public void verify_complexity_metric_not_analysing_accessor() {
    checkMetric(false, baseDir, "Complexity.java", "complexity", 15.0);
  }

  private void checkMetric(String filename, String metric, double expectedValue) {
    checkMetric(true, baseDir, filename, metric, expectedValue);
  }

  /**
   * Utility method to quickly get metric out of a file.
   */
  private void checkMetric(boolean separateAccessorsFromMethods, File baseDir, String filename, String metric, double expectedValue) {
    context = mock(SensorContext.class);
    fs.add(new DefaultInputFile(filename));
    Measurer measurer = new Measurer(fs, context, separateAccessorsFromMethods);
    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    conf.setSeparateAccessorsFromMethods(separateAccessorsFromMethods);
    squid = new JavaSquid(conf, null, measurer, null, new CodeVisitor[0]);
    squid.scan(Lists.newArrayList(new File(baseDir, filename)), Collections.<File>emptyList(), Collections.<File>emptyList());
    ArgumentCaptor<Measure> captor = ArgumentCaptor.forClass(Measure.class);
    ArgumentCaptor<InputFile> sonarFilescaptor = ArgumentCaptor.forClass(InputFile.class);
    //-1 for metrics in case we don't analyse Accessors.
    verify(context, times(NB_OF_METRICS)).saveMeasure(sonarFilescaptor.capture(), captor.capture());
    int checkedMetrics = 0;
    for (Measure measure : captor.getAllValues()) {
      if (metric.equals(measure.getMetricKey())) {
        assertThat(measure.getValue()).isEqualTo(expectedValue);
        checkedMetrics++;
      }
    }
    assertThat(checkedMetrics).isEqualTo(1);
  }

}