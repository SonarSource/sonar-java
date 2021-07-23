/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model;

import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.reporting.JavaIssueBuilderImpl;

public class JavaIssueBuilderForTest extends JavaIssueBuilderImpl {

  private final Set<AnalyzerMessage> issues;

  public JavaIssueBuilderForTest(InputFile inputFile, Set<AnalyzerMessage> issues) {
    super(inputFile, null);
    this.issues = issues;
  }

  @Override
  public void build() {
    // TODO: Add secondaries
    issues.add(new AnalyzerMessage(rule, inputFile, textSpan, message, cost != null ? cost : 0));
  }
}
