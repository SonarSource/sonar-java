/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.utils.PathUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MeasurerTest {

  private static final int NB_OF_METRICS = 7;
  private static final File BASE_DIR = new File("src/test/files/metrics");
  private SensorContextTester context;

  @BeforeEach
  void setUp() throws Exception {
    context = SensorContextTester.create(BASE_DIR);
  }

  @Test
  void verify_methods_metric() {
    checkMetric("Methods.java", "functions", 7);
  }

  @Test
  void verify_class_metric() {
    checkMetric("Classes.java", "classes", 8);
  }

  @Test
  void verify_complexity_metric() {
    checkMetric("Complexity.java", "complexity", 15);
  }

  @Test
  void verify_cognitive_complexity_metric() {
    checkMetric("CognitiveComplexity.java", "cognitive_complexity", 25);
  }

  @Test
  void verify_function_metric() {
    checkMetric("Complexity.java", "functions", 8);
  }

  @Test
  void verify_comments_metric() {
    checkMetric("Comments.java", "comment_lines", 3);
  }

  @Test
  void verify_statements_metric() {
    checkMetric("Statements.java", "statements", 18);
  }

  @Test
  void verify_ncloc_metric() {
    checkMetric("LinesOfCode.java", "ncloc", 2);
    checkMetric("CommentedOutFile.java", "ncloc", 0);
    checkMetric("EmptyFile.java", "ncloc", 0);
  }

  @Test
  void verify_ncloc_for_test_are_sent_to_sq_8_5() {
    InputFile inputFile = createFile("LinesOfCode.java");
    context.setRuntime(
      SonarRuntimeImpl.forSonarQube(org.sonar.api.utils.Version.create(8,5),
        SonarQubeSide.SCANNER,
        SonarEdition.COMMUNITY));

    scanOnTestSquid(inputFile, true);

    assertThat(context.measures(inputFile.key())).hasSize(1);
    assertThat(context.measure(inputFile.key(), "ncloc").value()).isEqualTo(2);
  }

  @Test
  void verify_ncloc_for_test_are_not_sent_to_sq_7_9() {
    InputFile inputFile = createFile("LinesOfCode.java");
    context.setRuntime(
      SonarRuntimeImpl.forSonarQube(org.sonar.api.utils.Version.create(7,9),
        SonarQubeSide.SCANNER,
        SonarEdition.COMMUNITY));

    scanOnTestSquid(inputFile, true);

    assertThat(context.measures(inputFile.key())).isEmpty();
  }

  @Test
  void verify_ncloc_for_test_are_not_sent_to_sonarlint() {
    InputFile inputFile = createFile("LinesOfCode.java");
    context.setRuntime(SonarRuntimeImpl.forSonarLint(org.sonar.api.utils.Version.create(7,9)));

    scanOnTestSquid(inputFile, true);

    assertThat(context.measures(inputFile.key())).isEmpty();
  }

  /**
   * Utility method to quickly get metric out of a file.
   */
  private void checkMetric(String filename, String metric, Number expectedValue) {
    InputFile inputFile = createFile(filename);

    scanOnTestSquid(inputFile, false);

    assertThat(context.measures(inputFile.key())).hasSize(NB_OF_METRICS);
    assertThat(context.measure(inputFile.key(), metric).value()).isEqualTo(expectedValue);
  }

  private void scanOnTestSquid(InputFile inputFile, boolean asTest) {
    context.fileSystem().add(inputFile);

    Measurer measurer = new Measurer(context, mock(NoSonarFilter.class));
    JavaSquid squid = new JavaSquid(new JavaVersionImpl(), null, measurer, null, null, new JavaCheck[0]);

    if (asTest) {
      squid.scan(Collections.emptyList(), Collections.singletonList(inputFile), Collections.emptyList());
    } else {
      squid.scan(Collections.singletonList(inputFile), Collections.emptyList(), Collections.emptyList());
    }
  }

  private InputFile createFile(String filename) {
    String relativePath = PathUtils.sanitize(new File(BASE_DIR, filename).getPath());
    return TestUtils.inputFile(relativePath);
  }

}
