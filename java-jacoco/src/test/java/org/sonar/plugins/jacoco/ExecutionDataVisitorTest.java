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

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.SessionInfo;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionDataVisitorTest {

  @Test
  public void test() {
    ExecutionDataVisitor visitor = new ExecutionDataVisitor();

    visitor.visitSessionInfo(new SessionInfo("foo", 1L, 1L));
    visitor.visitClassExecution(new ExecutionData(1, "", new boolean[] {true, false, false}));

    visitor.visitSessionInfo(new SessionInfo("bar", 2L, 2L));
    visitor.visitClassExecution(new ExecutionData(1, "", new boolean[] {false, true, false}));

    visitor.visitSessionInfo(new SessionInfo("foo", 3L, 3L));
    visitor.visitClassExecution(new ExecutionData(1, "", new boolean[] {false, false, true}));

    assertThat(visitor.getSessions()).hasSize(2);
    assertThat(visitor.getSessions().get("foo").getContents()).hasSize(1);
    assertThat(visitor.getSessions().get("foo").get(1).getProbes()).isEqualTo(new boolean[] {true, false, true});
    assertThat(visitor.getSessions().get("bar").getContents()).hasSize(1);
    assertThat(visitor.getSessions().get("bar").get(1).getProbes()).isEqualTo(new boolean[] {false, true, false});
    assertThat(visitor.getMerged().get(1).getProbes()).isEqualTo(new boolean[] {true, true, true});
  }

}
