/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
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
    exception.expectMessage("You are trying to merge two different JaCoCo binary formats. Please use only one version of JaCoCo.");
    merge("jacoco-0.7.5.exec", "jacoco-it-0.7.4.exec");
  }

  @Test
  public void merge_different_format_should_fail_() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("You are trying to merge two different JaCoCo binary formats. Please use only one version of JaCoCo.");
    merge("jacoco-0.7.4.exec", "jacoco-it-0.7.5.exec");
  }

  @Test
  public void merge_same_format_should_not_fail() throws Exception {
    merge("jacoco-0.7.5.exec", "jacoco-it-0.7.5.exec");
  }

  private void merge(String file1, String file2) {
    File current = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/" + file1);
    File previous = TestUtils.getResource("/org/sonar/plugins/jacoco/JaCoCo_incompatible_merge/" + file2);
    JaCoCoReportMerger.mergeReports(new File(testFolder.getRoot(), "dummy"), current, previous);
  }
}