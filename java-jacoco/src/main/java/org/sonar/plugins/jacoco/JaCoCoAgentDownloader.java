/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
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
package org.sonar.plugins.jacoco;

import org.apache.commons.io.FileUtils;
import org.jacoco.agent.AgentJar;
import org.jacoco.core.JaCoCo;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;

public class JaCoCoAgentDownloader implements BatchExtension {

  /**
   * Dirty hack, but it allows to extract agent only once during Sonar analyzes for multi-module project.
   */
  private static File agentJarFile;

  public JaCoCoAgentDownloader() {
  }

  public synchronized File getAgentJarFile() {
    if (agentJarFile == null) {
      agentJarFile = extractAgent();
    }
    return agentJarFile;
  }

  private File extractAgent() {
    try {
      File agent = File.createTempFile("jacocoagent", ".jar");
      AgentJar.extractTo(agent);
      // TODO evil method
      FileUtils.forceDeleteOnExit(agent);
      JaCoCoExtensions.LOG.info("JaCoCo agent (version " + JaCoCo.VERSION + ") extracted: {}", agent);
      return agent;
    } catch (IOException e) {
      throw new SonarException(e);
    }
  }
}
