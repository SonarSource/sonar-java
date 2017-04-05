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

import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;

import java.io.IOException;

class JacocoController {

  private static final String ERROR = "Unable to access JaCoCo Agent - make sure that you use JaCoCo and version not lower than 0.6.2.";

  private final IAgent agent;

  private boolean testStarted;

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

  public synchronized void onTestStart(String name) {
    if (testStarted) {
      throw new JacocoControllerError(
          "Looks like several tests executed in parallel in the same JVM, thus coverage per test can't be recorded correctly.");
    }
    // Dump coverage between tests
    start(name);
    testStarted = true;
  }

  /**
   * Dump the current session, whatever it's name since has been fixed at the start.
   */
  public synchronized void onTestFinish() {
    // Dump coverage for test
    start("");
    testStarted = false;
  }

  /**
   * Dump the current session keeping its name, then update the session name with a new session
   * identifier.
   * 
   * @param sessionId
   *          The new session identifier used for the fresh session.
   */
  private void start(final String sessionId) {
    try {
      // Dump the current session with its actual name
      agent.dump(true);
    } catch (IOException e) {
      throw new JacocoControllerError(e);
    }
    // For this fresh (empty) session, set the given name
    agent.setSessionId(sessionId);
  }

  public static class JacocoControllerError extends Error {

    /**
     * SID
     */
    private static final long serialVersionUID = 1L;

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
