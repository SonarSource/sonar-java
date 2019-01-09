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
import org.jacoco.agent.rt.RT;

import java.io.IOException;

class JacocoController {

  private static final String ERROR = "Unable to access JaCoCo Agent - make sure that you use JaCoCo and version not lower than 0.6.2.";

  private final IAgent agent;

  private boolean testStarted;

  // Visible for testing
  static JacocoController singleton;

  private JacocoController() {
    try {
      this.agent = RT.getAgent();
    } catch (Exception | NoClassDefFoundError e) {
      throw new JacocoControllerError(ERROR, e);
    }
  }

  JacocoController(IAgent agent) {
    this.agent = agent;
  }

  public static synchronized JacocoController getInstance() {
    if (singleton == null) {
      singleton = new JacocoController();
    }
    return singleton;
  }

  public synchronized void onTestStart() {
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
    agent.setSessionId(sessionId);
    try {
      agent.dump(true);
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
