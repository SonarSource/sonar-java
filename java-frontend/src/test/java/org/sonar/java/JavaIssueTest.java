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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.java.TestUtils.computeLineEndOffsets;

public class JavaIssueTest {

  @Test
  public void testIssueCreation() {
    InputFile inputFile = inputFile();
    RuleKey ruleKey = RuleKey.of("squid", "ruleKey");
    SensorContext sensorContext = mock(SensorContext.class);
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue newIssue = new DefaultIssue(storage);
    DefaultIssue newIssueOnFile = new DefaultIssue(storage);
    DefaultIssue newIssueOnLine = new DefaultIssue(storage);
    Mockito.when(sensorContext.newIssue()).thenReturn(newIssue, newIssueOnFile, newIssueOnLine);

    // issue with secondary locations
    JavaIssue javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocation(inputFile, "main message", 1, 2, 1, 6);
    javaIssue.addSecondaryLocation(inputFile, 2, 2, 2, 4, "secondary message 1");
    javaIssue.addSecondaryLocation(inputFile, 3, 1, 3, 5, "secondary message 2");
    javaIssue.save();

    Mockito.verify(storage, Mockito.times(1)).store(newIssue);

    assertThat(newIssue.ruleKey()).isEqualTo(ruleKey);
    assertLocation(newIssue.primaryLocation(), inputFile, "main message", 1, 2, 1, 6);
    assertThat(newIssue.flows()).hasSize(2);
    assertLocation(newIssue.flows().get(0).locations().get(0), inputFile, "secondary message 1", 2, 2, 2, 4);
    assertLocation(newIssue.flows().get(1).locations().get(0), inputFile, "secondary message 2", 3, 1, 3, 5);

    // issue on file
    javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocationOnComponent(inputFile, "file message");
    javaIssue.save();

    Mockito.verify(storage, Mockito.times(1)).store(newIssueOnFile);
    assertThat(newIssueOnFile.ruleKey()).isEqualTo(ruleKey);
    IssueLocation location = newIssueOnFile.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.textRange()).isNull();
    assertThat(location.message()).isEqualTo("file message");

    // issue on entire line
    javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocation(inputFile, "line message", 2, -1, 2, -1);
    javaIssue.save();

    Mockito.verify(storage, Mockito.times(1)).store(newIssueOnLine);
    assertLocation(newIssueOnLine.primaryLocation(), inputFile, "line message", 2, 0, 2, 4);
  }

  @Test
  public void test_addFlow() throws Exception {
    InputFile inputFile = inputFile();
    RuleKey ruleKey = RuleKey.of("squid", "ruleKey");
    SensorContext sensorContext = mock(SensorContext.class);
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue newIssueEmptyFlow = new DefaultIssue(storage);
    DefaultIssue newIssueWithFlow = new DefaultIssue(storage);
    Mockito.when(sensorContext.newIssue()).thenReturn(newIssueEmptyFlow, newIssueWithFlow);

    JavaIssue javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocation(inputFile, "main message", 1, 2, 1, 6);
    javaIssue.addFlow(inputFile, new ArrayList<>());
    javaIssue.save();
    Mockito.verify(storage, Mockito.times(1)).store(newIssueEmptyFlow);
    assertThat(newIssueEmptyFlow.flows()).isEmpty();


    javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocation(inputFile, "main message", 1, 2, 1, 6);
    List<List<AnalyzerMessage>> flows = new ArrayList<>();
    flows.add(
      Lists.newArrayList(
        new AnalyzerMessage(null, inputFile, new AnalyzerMessage.TextSpan(2, 2, 2, 4), "flow message 1", 0)));
    flows.add(
      Lists.newArrayList(
        new AnalyzerMessage(null, inputFile, new AnalyzerMessage.TextSpan(3, 1, 3, 5), "flow message 2", 0)));
    javaIssue.addFlow(inputFile, flows);
    javaIssue.save();
    Mockito.verify(storage, Mockito.times(1)).store(newIssueWithFlow);
    assertThat(newIssueWithFlow.flows()).hasSize(2);
  }

  private static InputFile inputFile() {
    int[] lineStartOffsets = {0, 10, 15};
    int lastValidOffset = 25;
    return new TestInputFileBuilder("module", "relPath")
      .setLines(3)
      .setOriginalLineStartOffsets(lineStartOffsets)
      .setOriginalLineEndOffsets(computeLineEndOffsets(lineStartOffsets, lastValidOffset))
      .setLastValidOffset(lastValidOffset)
      .build();
  }

  private static void assertLocation(IssueLocation location, InputFile file, String message, int startLine, int startOffset, int endLine, int endOffset) {
    assertThat(location.inputComponent()).isEqualTo(file);
    assertThat(location.message()).isEqualTo(message);
    TextRange textRange = location.textRange();
    TextPointer start = textRange.start();
    assertThat(start.line()).isEqualTo(startLine);
    assertThat(start.lineOffset()).isEqualTo(startOffset);
    TextPointer end = textRange.end();
    assertThat(end.line()).isEqualTo(endLine);
    assertThat(end.lineOffset()).isEqualTo(endOffset);
  }
}
