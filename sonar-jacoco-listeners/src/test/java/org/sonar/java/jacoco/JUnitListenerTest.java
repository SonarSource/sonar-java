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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.mockito.InOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class JUnitListenerTest {

  public static class Success {
    @org.junit.Test
    public void test() {
    }
  }

  public static class Failure {
    @org.junit.Test
    public void test() {
      org.junit.Assert.fail();
    }
  }

  private JacocoController jacoco;
  private JUnitListener listener;

  @Before
  public void setUp() {
    jacoco = mock(JacocoController.class);
    JacocoController.singleton = jacoco;
    listener = new JUnitListener();
    listener.jacoco = jacoco;
  }

  @Test
  public void should_have_public_no_arg_constructor() throws Exception {
    JUnitListener.class.getConstructor();
  }

  @Test
  public void lazy_initialization_of_controller() throws Exception {
    JUnitListener jUnitListener = new JUnitListener();
    assertNull(jUnitListener.jacoco);
    assertEquals(jacoco, jUnitListener.getJacocoController());
    jUnitListener.jacoco = jacoco;
    assertEquals(jUnitListener.jacoco, jUnitListener.getJacocoController());
  }

  @Test
  public void test_success() {
    execute(Success.class);
    String testName = getClass().getCanonicalName() + "$Success test";
    InOrder orderedExecution = inOrder(jacoco);
    orderedExecution.verify(jacoco).onTestStart();
    orderedExecution.verify(jacoco).onTestFinish(testName);
  }

  @Test
  public void test_failure() {
    execute(Failure.class);
    String testName = getClass().getCanonicalName() + "$Failure test";
    InOrder orderedExecution = inOrder(jacoco);
    orderedExecution.verify(jacoco).onTestStart();
    orderedExecution.verify(jacoco).onTestFinish(testName);
  }

  private void execute(Class cls) {
    JUnitCore junit = new JUnitCore();
    junit.addListener(listener);
    junit.run(cls);
  }

}
