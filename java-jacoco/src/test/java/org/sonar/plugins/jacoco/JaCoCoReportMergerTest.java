/*
 * SonarQube Java
 * Copyright (C) 2010-2019 SonarSource SA
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
package org.sonar.plugins.jacoco;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.jacoco.core.data.ExecutionDataStore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.java.AnalysisException;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static org.assertj.core.api.Assertions.assertThat;

public class JaCoCoReportMergerTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void private_constructor() throws Exception {
    assertThat(isFinal(JaCoCoReportMerger.class.getModifiers())).isTrue();
    Constructor constructor = JaCoCoReportMerger.class.getDeclaredConstructor();
    assertThat(isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void merge_different_format_should_fail() {
    exception.expect(AnalysisException.class);
    exception.expectMessage("You are not using the latest JaCoCo binary format version, please consider upgrading to latest JaCoCo version.");
    merge("jacoco-0.7.5.exec", "jacoco-it-0.7.4.exec");
  }

  @Test
  public void merge_different_format_should_fail_() {
    exception.expect(AnalysisException.class);
    exception.expectMessage("You are not using the latest JaCoCo binary format version, please consider upgrading to latest JaCoCo version.");
    merge("jacoco-0.7.4.exec", "jacoco-it-0.7.5.exec");
  }

  @Test
  public void merge_same_format_should_not_fail() throws Exception {
    merge("jacoco-0.7.5.exec", "jacoco-it-0.7.5.exec");
    File mergedReport = new File(testFolder.getRoot(), "dummy");
    ExecutionDataVisitor edv = new ExecutionDataVisitor();
    new JacocoReportReader(mergedReport).readJacocoReport(edv, edv);
    for (Map.Entry<String, ExecutionDataStore> entry : edv.getSessions().entrySet()) {
      // Verify that each sessions has kept only two elements and that they were not mangled: required for coverage per tests.
      assertThat(entry.getValue().getContents()).hasSize(2);
    }
  }

  @Test
  public void fail_merge() throws Exception {
    exception.expect(AnalysisException.class);
    exception.expectMessage("Unable to write overall coverage report");
    JaCoCoReportMerger.mergeReports(testFolder.getRoot(), new File[0]);

  }

  private void merge(String file1, String file2) {
    File current = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/" + file1);
    File previous = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/" + file2);
    JaCoCoReportMerger.mergeReports(new File(testFolder.getRoot(), "dummy"), current, previous);
  }
}
