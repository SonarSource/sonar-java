/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.mockito.InOrder;

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
    listener = new JUnitListener(jacoco);
    final JacocoController jacocoController = mock(JacocoController.class);
    JacocoController.singleton = jacocoController;
 }

  @After
  public void clean() {
    JacocoController.singleton = null;
 }

  @Test
  public void should_have_public_no_arg_constructor() throws Exception {
    JUnitListener.class.getConstructor();
  }

  @Test
  public void lazyController() throws Exception {
    final JUnitListener listener = new JUnitListener();
    Assert.assertNull(listener.jacoco);
    Assert.assertSame(JacocoController.getInstance(), listener.getJacocoController());
    Assert.assertSame(JacocoController.getInstance(), listener.jacoco);

    // Check the instance is stored
    Assert.assertSame(JacocoController.getInstance(), listener.getJacocoController());
  }

  @Test
  public void test_success() {
    execute(Success.class);
    String testName = getClass().getCanonicalName() + "$Success test";
    InOrder orderedExecution = inOrder(jacoco);
    orderedExecution.verify(jacoco).onTestStart(testName);
    orderedExecution.verify(jacoco).onTestFinish();
  }

  @Test
  public void test_failure() {
    execute(Failure.class);
    String testName = getClass().getCanonicalName() + "$Failure test";
    InOrder orderedExecution = inOrder(jacoco);
    orderedExecution.verify(jacoco).onTestStart(testName);
    orderedExecution.verify(jacoco).onTestFinish();
  }

  private void execute(Class<?> cls) {
    JUnitCore junit = new JUnitCore();
    junit.addListener(listener);
    junit.run(cls);
  }

}
