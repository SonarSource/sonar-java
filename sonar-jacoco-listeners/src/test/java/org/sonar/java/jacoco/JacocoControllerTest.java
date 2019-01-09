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
package org.sonar.java.jacoco;

import org.jacoco.agent.rt.IAgent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.java.jacoco.JacocoController.JacocoControllerError;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JacocoControllerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private IAgent agent;
  private JacocoController jacoco;

  @Before
  public void setUp() {
    agent = mock(IAgent.class);
    jacoco = new JacocoController(agent);
  }

  @Test
  public void test_onStart() throws Exception {
    jacoco.onTestStart();
    InOrder inOrder = Mockito.inOrder(agent);
    inOrder.verify(agent).setSessionId("");
    inOrder.verify(agent).dump(true);
    verifyNoMoreInteractions(agent);
  }

  @Test
  public void test_onFinish() throws Exception {
    when(agent.getExecutionData(false)).thenReturn(new byte[] {});
    jacoco.onTestFinish("test");
    InOrder inOrder = Mockito.inOrder(agent);
    inOrder.verify(agent).setSessionId("test");
    inOrder.verify(agent).dump(true);
    verifyNoMoreInteractions(agent);
  }

  @Test
  public void should_throw_exception_when_dump_failed() throws Exception {
    doThrow(IOException.class).when(agent).dump(anyBoolean());
    thrown.expect(JacocoControllerError.class);
    jacoco.onTestFinish("test");
  }

  @Test
  public void should_throw_exception_when_two_tests_started_in_parallel() {
    jacoco.onTestStart();
    thrown.expect(JacocoControllerError.class);
    thrown.expectMessage("Looks like several tests executed in parallel in the same JVM, thus coverage per test can't be recorded correctly.");
    jacoco.onTestStart();
  }

}
