/*
 * SonarQube Java
 * Copyright (C) 2013-2019 SonarSource SA
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
package com.sonar.it.java.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.ClassRule;
import org.junit.Test;

import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsDouble;
import static com.sonar.it.java.suite.JavaTestSuite.getMeasureAsInteger;
import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationTest {

  private static final String DUPLICATION_PROJECT_KEY = "org.sonarsource.it.projects:test-duplications";

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Test
  public void duplication_should_be_computed_by_SQ() {
    MavenBuild build = MavenBuild.create(TestUtils.projectPom("test-duplications")).setCleanPackageSonarGoals();

    orchestrator.executeBuild(build);

    assertThat(getMeasureAsDouble(DUPLICATION_PROJECT_KEY, "duplicated_lines_density")).isEqualTo(32.7);
    assertThat(getMeasureAsInteger(DUPLICATION_PROJECT_KEY, "duplicated_lines")).isEqualTo(36);
    assertThat(getMeasureAsInteger(DUPLICATION_PROJECT_KEY, "duplicated_files")).isEqualTo(2);
    assertThat(getMeasureAsInteger(DUPLICATION_PROJECT_KEY, "duplicated_blocks")).isEqualTo(2);
  }

}
