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

import org.jacoco.agent.rt.IAgent;
import org.jacoco.agent.rt.RT;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

// TODO Handling for the case when agent was not attached to JVM (ClassNotFoundException, etc)
public class JacocoController {

  private final IAgent agent;

  public JacocoController() {
    this(RT.getAgent());
  }

  JacocoController(IAgent agent) {
    this.agent = agent;
  }

  public void onTestStart(String name) {
    System.out.println("Test " + name + " started");

    // TODO naming convention for sessions
    agent.setSessionId(name);
    agent.reset();
  }

  public void onTestFinish(String name) {
    System.out.println("Test " + name + " finished");

    try {
      // TODO location should be configurable
      dump("target/jacoco.exec", true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void dump(String destfile, boolean reset) throws Exception {
    byte[] dump = agent.getExecutionData(false);
    new File(destfile).getParentFile().mkdirs();
    // TODO lock file for concurrent access
    OutputStream output = new BufferedOutputStream(new FileOutputStream(destfile, true));
    output.write(dump);
    output.close();
  }

}
