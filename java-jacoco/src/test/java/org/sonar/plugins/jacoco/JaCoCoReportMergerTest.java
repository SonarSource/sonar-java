/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
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
package org.sonar.plugins.jacoco;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.test.TestUtils;

import java.io.File;

public class JaCoCoReportMergerTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void merge_different_format_should_fail() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Incompatible execution data");
    File current = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCov0_7_5_incompatible_coverage_per_test/jacoco.exec");
    File previous = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCov0_7_4_incompatible_coverage_per_test/jacoco.exec");
    JaCoCoReportMerger.mergeReports(new File(testFolder.getRoot(), "dummy"), current, previous);
  }

}