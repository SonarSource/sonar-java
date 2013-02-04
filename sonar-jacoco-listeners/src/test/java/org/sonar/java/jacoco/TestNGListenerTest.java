/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
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
package org.sonar.java.jacoco;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.testng.TestNG;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class TestNGListenerTest {

  public static class Success {
    @org.testng.annotations.Test
    public void test() {
    }
  }

  public static class Failure {
    @org.testng.annotations.Test
    public void test() {
      org.testng.Assert.fail();
    }
  }

  private JacocoController jacoco;
  private TestNGListener listener;

  @Before
  public void setUp() {
    jacoco = mock(JacocoController.class);
    listener = new TestNGListener(jacoco);
  }

  @Test
  public void test_success() {
    execute(Success.class);
    String testName = getClass().getCanonicalName() + "$Success test";
    InOrder orderedExecution = inOrder(jacoco);
    orderedExecution.verify(jacoco).onTestStart(testName);
    orderedExecution.verify(jacoco).onTestFinish(testName);
  }

  @Test
  public void test_failure() {
    execute(Failure.class);
    String testName = getClass().getCanonicalName() + "$Failure test";
    InOrder orderedExecution = inOrder(jacoco);
    orderedExecution.verify(jacoco).onTestStart(testName);
    orderedExecution.verify(jacoco).onTestFinish(testName);
  }

  private void execute(Class cls) {
    TestNG testNg = new TestNG(false);
    testNg.addListener(listener);
    testNg.setTestClasses(new Class[] {cls});
    testNg.run();
  }

}
