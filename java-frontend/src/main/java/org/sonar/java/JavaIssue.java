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
package org.sonar.java;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public final class JavaIssue {
  private final NewIssue newIssue;

  public JavaIssue(NewIssue newIssue) {
    this.newIssue = newIssue;
  }

  public static JavaIssue create(SensorContext context, RuleKey ruleKey, @Nullable Double effortToFix) {
    NewIssue newIssue = context.newIssue()
      .forRule(ruleKey)
      .gap(effortToFix);
    return new JavaIssue(newIssue);
  }

  public JavaIssue setPrimaryLocationOnComponent(InputComponent fileOrProject, String message) {
    newIssue.at(
      newIssue.newLocation()
        .on(fileOrProject)
        .message(message));
    return this;
  }

  public JavaIssue setPrimaryLocation(InputFile file, String message, int startLine, int startLineOffset, int endLine, int endLineOffset) {
    NewIssueLocation newIssueLocation;
    if (startLineOffset == -1) {
      newIssueLocation = newIssue.newLocation()
        .on(file)
        .at(file.selectLine(startLine))
        .message(message);
    } else {
      newIssueLocation = newIssue.newLocation()
        .on(file)
        .at(file.newRange(startLine, startLineOffset, endLine, endLineOffset))
        .message(message);
    }
    newIssue.at(newIssueLocation);
    return this;
  }

  public JavaIssue addSecondaryLocation(InputFile file, int startLine, int startLineOffset, int endLine, int endLineOffset, String message) {
    newIssue.addLocation(
      newIssue.newLocation()
        .on(file)
        .at(file.newRange(startLine, startLineOffset, endLine, endLineOffset))
        .message(message));
    return this;
  }

  public JavaIssue addFlow(InputFile file, List<List<AnalyzerMessage>> flows) {
    for (List<AnalyzerMessage> flow : flows) {
      newIssue.addFlow(flow.stream()
        .map(am -> newIssue.newLocation()
          .on(file)
          .at(range(file, am.primaryLocation()))
          .message(am.getMessage()))
        .collect(Collectors.toList()));
    }
    return this;
  }

  private static TextRange range(InputFile file, AnalyzerMessage.TextSpan textSpan) {
    return file.newRange(textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter);
  }



  public void save() {
    newIssue.save();
  }

}
