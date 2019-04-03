/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.io.File;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.PathUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MeasurerTest {

  private static final int NB_OF_METRICS = 7;
  private static final File BASE_DIR = new File("src/test/files/metrics");
  private SensorContextTester context;

  @Before
  public void setUp() throws Exception {
    context = SensorContextTester.create(BASE_DIR);
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
    String relativePath = PathUtils.sanitize(new File(BASE_DIR, filename).getPath());
    InputFile inputFile = TestUtils.inputFile(relativePath);
    context.fileSystem().add(inputFile);

    Measurer measurer = new Measurer(context, mock(NoSonarFilter.class));
    JavaSquid squid = new JavaSquid(new JavaVersionImpl(), null, measurer, null, null, new JavaCheck[0]);

    squid.scan(Collections.singletonList(inputFile), Collections.emptyList());

    assertThat(context.measures(inputFile.key())).hasSize(NB_OF_METRICS);
    assertThat(context.measure(inputFile.key(), metric).value()).isEqualTo(expectedValue);
  }

}
