/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.plugins.java;

import com.google.common.base.Throwables;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.Issuable;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class CompIssue {

  private static final Reflection SQ_5_2;

  static {
    SQ_5_2 = Reflection.create();
    if (SQ_5_2 != null) {
      LoggerFactory.getLogger(CompIssue.class).warn(">>> Enabled usage of SonarQube 5.2 BETA APIs <<<");
    }
  }

  private static class Reflection {
    private final Method inputFile_newRange;
    private final Method inputFile_selectLine;

    private final Method newIssueLocation_on;
    private final Method newIssueLocation_at;
    private final Method newIssueLocation_message;

    private final Method issueBuilder_at;
    private final Method issueBuilder_newLocation;
    private final Method issueBuilder_addFlow;

    public static Reflection create() {
      try {
        return new Reflection();
      } catch (ClassNotFoundException | NoSuchMethodException e) {
        return null;
      }
    }

    public Reflection() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> classTextRange = Class.forName("org.sonar.api.batch.fs.TextRange");
      Class<?> classInputComponent = Class.forName("org.sonar.api.batch.fs.InputComponent");
      Class<?> classNewIssueLocation = Class.forName("org.sonar.api.batch.sensor.issue.NewIssueLocation");

      inputFile_selectLine = method(InputFile.class, "selectLine", int.class);
      inputFile_newRange = method(InputFile.class, "newRange", int.class, int.class, int.class, int.class);

      issueBuilder_at = method(Issuable.IssueBuilder.class, "at", classNewIssueLocation);
      issueBuilder_newLocation = method(Issuable.IssueBuilder.class, "newLocation");
      issueBuilder_addFlow = method(Issuable.IssueBuilder.class, "addFlow", Iterable.class);

      newIssueLocation_on = method(classNewIssueLocation, "on", classInputComponent);
      newIssueLocation_at = method(classNewIssueLocation, "at", classTextRange);
      newIssueLocation_message = method(classNewIssueLocation, "message", String.class);
    }

    private static Method method(Class<?> cls, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
      Method method = cls.getMethod(name, parameterTypes);
      method.setAccessible(true);
      return method;
    }
  }

  private final InputFile inputFile;
  private final Issuable issuable;
  private final Issuable.IssueBuilder issueBuilder;
  @Nullable
  private List<Object> secondaryLocations;

  public static CompIssue create(InputFile inputFile, Issuable issuable, RuleKey ruleKey, @Nullable Double effortToFix) {
    Issuable.IssueBuilder issueBuilder = issuable.newIssueBuilder()
      .ruleKey(ruleKey)
      .effortToFix(effortToFix);
    return new CompIssue(issuable, issueBuilder, inputFile);
  }

  public CompIssue(Issuable issuable, Issuable.IssueBuilder issueBuilder, InputFile inputFile) {
    this.issuable = issuable;
    this.issueBuilder = issueBuilder;
    this.inputFile = inputFile;
  }

  public CompIssue setPrimaryLocation(String message, @Nullable Integer line) {
    if (SQ_5_2 == null) {
      issueBuilder.line(line).message(message);
      return this;
    }
    try {
      Object range = line == null ? null : SQ_5_2.inputFile_selectLine.invoke(inputFile, line);
      setPrimaryLocation(message, range);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw Throwables.propagate(e);
    }
    return this;
  }

  public CompIssue setPrimaryLocation(String message, int startLine, int startLineOffset, int endLine, int endLineOffset) {
    if (SQ_5_2 == null) {
      issueBuilder.line(startLine).message(message);
      return this;
    }
    try {
      Object range;
      if (startLineOffset != -1) {
        range = SQ_5_2.inputFile_newRange.invoke(inputFile, startLine, startLineOffset, endLine, endLineOffset);
      } else {
        range = SQ_5_2.inputFile_selectLine.invoke(inputFile, startLine);
      }
      setPrimaryLocation(message, range);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw Throwables.propagate(e);
    }
    return this;
  }

  public CompIssue addSecondaryLocation(int startLine, int startLineOffset, int endLine, int endLineOffset, String message) {
    if (SQ_5_2 == null) {
      return this;
    }
    try {
      Object newLocation = SQ_5_2.issueBuilder_newLocation.invoke(issueBuilder);
      SQ_5_2.newIssueLocation_on.invoke(newLocation, inputFile);
      Object range = SQ_5_2.inputFile_newRange.invoke(inputFile, startLine, startLineOffset, endLine, endLineOffset);
      SQ_5_2.newIssueLocation_at.invoke(newLocation, range);
      SQ_5_2.newIssueLocation_message.invoke(newLocation, message);
      if (secondaryLocations == null) {
        secondaryLocations = new ArrayList<>();
      }
      secondaryLocations.add(newLocation);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw Throwables.propagate(e);
    }
    return this;
  }

  private void setPrimaryLocation(String message, @Nullable Object range) throws InvocationTargetException, IllegalAccessException {
    Object newLocation = SQ_5_2.issueBuilder_newLocation.invoke(issueBuilder);
    SQ_5_2.newIssueLocation_on.invoke(newLocation, inputFile);
    if (range != null) {
      SQ_5_2.newIssueLocation_at.invoke(newLocation, range);
    }
    SQ_5_2.newIssueLocation_message.invoke(newLocation, message);
    SQ_5_2.issueBuilder_at.invoke(issueBuilder, newLocation);
  }

  public void save() {
    if (secondaryLocations != null) {
      try {
        SQ_5_2.issueBuilder_addFlow.invoke(issueBuilder, secondaryLocations);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw Throwables.propagate(e);
      }
    }
    issuable.addIssue(issueBuilder.build());
  }

}
