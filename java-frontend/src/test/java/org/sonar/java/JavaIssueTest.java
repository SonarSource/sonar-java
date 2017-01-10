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
package org.sonar.java;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class JavaIssueTest {

  @Test
  public void testIssueCreation() {
    DefaultInputFile file = new DefaultInputFile("module", "relPath");
    file.setLines(3);
    file.setOriginalLineOffsets(new int[]{0, 10, 15});
    file.setLastValidOffset(25);
    RuleKey ruleKey = RuleKey.of("squid", "ruleKey");
    SensorContext sensorContext = mock(SensorContext.class);
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue newIssue = new DefaultIssue(storage);
    DefaultIssue newIssueOnFile = new DefaultIssue(storage);
    DefaultIssue newIssueOnLine = new DefaultIssue(storage);
    Mockito.when(sensorContext.newIssue()).thenReturn(newIssue, newIssueOnFile, newIssueOnLine);

    // issue with secondary locations
    JavaIssue javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocation(file, "main message", 1, 2, 1, 6);
    javaIssue.addSecondaryLocation(file, 2, 2, 2, 4, "secondary message 1");
    javaIssue.addSecondaryLocation(file, 3, 1, 3, 5, "secondary message 2");
    javaIssue.save();

    Mockito.verify(storage, Mockito.times(1)).store(newIssue);

    assertThat(newIssue.ruleKey()).isEqualTo(ruleKey);
    assertLocation(newIssue.primaryLocation(), file, "main message", 1, 2, 1, 6);
    assertThat(newIssue.flows()).hasSize(2);
    assertLocation(newIssue.flows().get(0).locations().get(0), file, "secondary message 1", 2, 2, 2, 4);
    assertLocation(newIssue.flows().get(1).locations().get(0), file, "secondary message 2", 3, 1, 3, 5);

    // issue on file
    javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocationOnFile(file, "file message");
    javaIssue.save();

    Mockito.verify(storage, Mockito.times(1)).store(newIssueOnFile);
    assertThat(newIssueOnFile.ruleKey()).isEqualTo(ruleKey);
    IssueLocation location = newIssueOnFile.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(file);
    assertThat(location.textRange()).isNull();
    assertThat(location.message()).isEqualTo("file message");

    // issue on entire line
    javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocation(file, "line message", 2, -1, 2, -1);
    javaIssue.save();

    Mockito.verify(storage, Mockito.times(1)).store(newIssueOnLine);
    assertLocation(newIssueOnLine.primaryLocation(), file, "line message", 2, 0, 2, 4);
  }

  @Test
  public void test_addFlow() throws Exception {
    DefaultInputFile file = new DefaultInputFile("module", "relPath");
    file.setModuleBaseDir(new java.io.File("").toPath());
    file.setLines(3);
    file.setOriginalLineOffsets(new int[]{0, 10, 15});
    file.setLastValidOffset(25);
    RuleKey ruleKey = RuleKey.of("squid", "ruleKey");
    SensorContext sensorContext = mock(SensorContext.class);
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue newIssueEmptyFlow = new DefaultIssue(storage);
    DefaultIssue newIssueWithFlow = new DefaultIssue(storage);
    Mockito.when(sensorContext.newIssue()).thenReturn(newIssueEmptyFlow, newIssueWithFlow);

    JavaIssue javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocation(file, "main message", 1, 2, 1, 6);
    javaIssue.addFlow(file, new ArrayList<>());
    javaIssue.save();
    Mockito.verify(storage, Mockito.times(1)).store(newIssueEmptyFlow);
    assertThat(newIssueEmptyFlow.flows()).isEmpty();


    javaIssue = JavaIssue.create(sensorContext, ruleKey, null);
    javaIssue.setPrimaryLocation(file, "main message", 1, 2, 1, 6);
    List<List<AnalyzerMessage>> flows = new ArrayList<>();
    flows.add(
      Lists.newArrayList(
        new AnalyzerMessage(null, file.file(), new AnalyzerMessage.TextSpan(2,2,2,4), "flow message 1", 0)));
    flows.add(
      Lists.newArrayList(
        new AnalyzerMessage(null, file.file(), new AnalyzerMessage.TextSpan(3,1,3,5), "flow message 2", 0)));
    javaIssue.addFlow(file, flows);
    javaIssue.save();
    Mockito.verify(storage, Mockito.times(1)).store(newIssueWithFlow);
    assertThat(newIssueWithFlow.flows()).hasSize(2);
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
