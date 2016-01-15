/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.IOException;

import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;

class JacocoController {

  private static final String ERROR = "Unable to access JaCoCo Agent - make sure that you use JaCoCo and version not lower than 0.6.2.";

  private IAgent agent;

  private boolean testStarted;

  private static JacocoController singleton;

  public static synchronized JacocoController getInstance() {
    if (singleton == null) {
      singleton = new JacocoController();
    }
    return singleton;
  }

  private JacocoController() {
    super();
  }

  JacocoController(IAgent agent) {
    this.agent = agent;
  }


  private IAgent getAgent() {
    if (agent == null) {
      try {
        agent = RT.getAgent();
      } catch (final Exception e) {
        throw new JacocoControllerError(ERROR, e);
      }
    }
    return agent;
  }

  public synchronized void onTestStart(String name) {
    if (testStarted) {
      throw new JacocoControllerError("Looks like several tests executed in parallel in the same JVM, thus coverage per test can't be recorded correctly.");
    }
    // Dump coverage between tests
    dump("");
    testStarted = true;
  }

  public synchronized void onTestFinish(String name) {
    // Dump coverage for test
    dump(name);
    testStarted = false;
  }

  private void dump(String sessionId) {
    getAgent().setSessionId(sessionId);
    try {
      getAgent().dump(true);
    } catch (IOException e) {
      throw new JacocoControllerError(e);
    }
  }

  public static class JacocoControllerError extends Error {
    public JacocoControllerError(String message) {
      super(message);
    }

    public JacocoControllerError(String message, Throwable cause) {
      super(message, cause);
    }

    public JacocoControllerError(Throwable cause) {
      super(cause);
    }
  }

}
