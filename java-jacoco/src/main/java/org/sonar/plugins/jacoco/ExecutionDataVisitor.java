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
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;

import java.util.HashMap;
import java.util.Map;

public class ExecutionDataVisitor implements ISessionInfoVisitor, IExecutionDataVisitor {

  private final Map<String, ExecutionDataStore> sessions = new HashMap<>();

  private ExecutionDataStore executionDataStore;
  private ExecutionDataStore merged = new ExecutionDataStore();

  @Override
  public void visitSessionInfo(SessionInfo info) {
    String sessionId = info.getId();
    executionDataStore = sessions.computeIfAbsent(sessionId, id -> new ExecutionDataStore());
  }

  @Override
  public void visitClassExecution(ExecutionData data) {
    executionDataStore.put(data);
    merged.put(defensiveCopy(data));
  }

  public Map<String, ExecutionDataStore> getSessions() {
    return sessions;
  }

  public ExecutionDataStore getMerged() {
    return merged;
  }

  private static ExecutionData defensiveCopy(ExecutionData data) {
    boolean[] src = data.getProbes();
    boolean[] dest = new boolean[src.length];
    System.arraycopy(src, 0, dest, 0, src.length);
    return new ExecutionData(data.getId(), data.getName(), dest);
  }

}
