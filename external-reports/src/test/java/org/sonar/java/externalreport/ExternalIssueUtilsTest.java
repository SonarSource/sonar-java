/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.externalreport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.externalreport.PmdSensor.RULE_LOADER;

class ExternalIssueUtilsTest {

  private static final String RULE_ID = "ruleId";
  private static final String ENGINE_ID = "engineId";
  private static final String ISSUE_MESSAGE = "my message";
  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources")
    .toAbsolutePath().normalize();

  @Test
  void issue_messages_are_trimed() throws Exception {
    SensorContextTester context = ExternalReportTestUtils.createContext(PROJECT_DIR);
    InputFile f = context.fileSystem().inputFiles(inputFile -> true).iterator().next();
    ExternalIssueUtils.saveIssue(context, RULE_LOADER, f, ENGINE_ID, RULE_ID, "1", ISSUE_MESSAGE);
    ExternalIssueUtils.saveIssue(context, RULE_LOADER, f, ENGINE_ID, RULE_ID, "2", "\n" + ISSUE_MESSAGE + "\n");
    ExternalIssueUtils.saveIssue(context, RULE_LOADER, f, ENGINE_ID, RULE_ID, "3", "\n" + ISSUE_MESSAGE + "    ");
    ExternalIssueUtils.saveIssue(context, RULE_LOADER, f, ENGINE_ID, RULE_ID, "4", "    " + ISSUE_MESSAGE + " \n ");
    ExternalIssueUtils.saveIssue(context, RULE_LOADER, f, ENGINE_ID, RULE_ID, "4", "    " + ISSUE_MESSAGE + "    ");

    Collection<ExternalIssue> issues = context.allExternalIssues();
    assertThat(issues).hasSize(5);
    assertThat(issues.stream().map(issue -> issue.primaryLocation().message())).allMatch(ISSUE_MESSAGE::equals);
  }

}
