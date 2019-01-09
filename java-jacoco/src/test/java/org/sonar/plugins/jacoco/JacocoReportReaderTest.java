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

import com.google.common.collect.Lists;
import java.io.File;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.java.AnalysisException;

public class JacocoReportReaderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private File dummy = new File("DummyFile.dummy");

  @Test
  public void reading_unexisting_file_should_fail() {
    expectedException.expect(AnalysisException.class);
    new JacocoReportReader(dummy);
  }

  @Test
  public void not_existing_class_files_should_not_be_analyzed_for_current() {
    File report = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/jacoco-0.7.5.exec");
    Collection<File> classFile = Lists.newArrayList(dummy);
    new JacocoReportReader(report).analyzeFiles(null, classFile);

  }
  @Test
  public void previous_version_should_fail() {
    File report = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/jacoco-0.7.4.exec");
    Collection<File> classFile = Lists.newArrayList(dummy);
    expectedException.expect(AnalysisException.class);
    new JacocoReportReader(report).analyzeFiles(null, classFile);
  }

  @Test
  public void analyzing_a_deleted_file_should_fail() throws Exception {
    File report = testFolder.newFile("jacoco.exec");
    FileUtils.copyFile(TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/jacoco-0.7.5.exec"), report);
    JacocoReportReader jacocoReportReader = new JacocoReportReader(report);
    expectedException.expect(AnalysisException.class);
    if(!report.delete()) {
      Fail.fail("report was not deleted, unable to complete test.");
    }
    ExecutionDataVisitor edv = new ExecutionDataVisitor();
    jacocoReportReader.readJacocoReport(edv, edv);
  }

  @Test
  public void incorrect_binary_format_should_fail() throws Exception {
    File report = TestUtils.getResource("/Hello.class.toCopy");
    expectedException.expect(AnalysisException.class);
    new JacocoReportReader(report);

  }
}
