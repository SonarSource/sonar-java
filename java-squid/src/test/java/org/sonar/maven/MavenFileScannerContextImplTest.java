/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.maven;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.maven.MavenFileScannerContext.Location;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.LocatedTreeImpl;
import org.sonar.maven.model.XmlLocation;
import org.sonar.maven.model.maven2.MavenProject;

import java.io.File;
import java.util.ArrayList;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MavenFileScannerContextImplTest {

  private static String reportedMessage;
  private MavenFileScannerContext context;
  private SonarComponents sonarComponents;
  private static final MavenCheck CHECK = new MavenCheck() {
  };
  private static final int LINE = 42;

  @Before
  public void setup() {
    reportedMessage = null;
    sonarComponents = createSonarComponentsMock();
    context = new MavenFileScannerContextImpl(mock(MavenProject.class), mock(File.class), sonarComponents);
  }

  @Test
  public void getMavenProject() {
    assertThat(context.getMavenProject()).isNotNull();
  }

  @Test
  public void should_report_issue_on_line() {
    context.reportIssue(CHECK, LINE, "message");
    assertThat(reportedMessage).isEqualTo("onLine:message");
  }

  @Test
  public void should_report_issue_on_tree() {
    context.reportIssue(CHECK, fakeLocatedTreeWithUnknownColumn(LINE), "message");
    assertThat(reportedMessage).isEqualTo("onLine:message");
  }

  @Test
  public void should_report_issue_on_file() {
    context.reportIssueOnFile(CHECK, "message");
    assertThat(reportedMessage).isEqualTo("onFile:message");
  }

  @Test
  public void should_report_issue_on_no_locations() {
    context.reportIssue(CHECK, LINE, "message", new ArrayList<Location>());
    assertThat(reportedMessage).isEqualTo("analyzerMessage:message");
  }

  @Test
  public void should_report_issue_on_lines_of_all_locations() {
    ArrayList<Location> secondaries = new ArrayList<Location>();
    secondaries.add(new Location("msg1", fakeLocatedTree(LINE, 42)));
    // ignore unknown column
    secondaries.add(new Location("msg2", fakeLocatedTreeWithUnknownColumn(LINE)));
    context.reportIssue(CHECK, LINE, "message", secondaries);
    assertThat(reportedMessage).isEqualTo("analyzerMessage:message;msg1;msg2");
  }

  private static SonarComponents createSonarComponentsMock() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onLine:" + (String) invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(LINE), anyString(), eq((Double) null));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onFile:" + (String) invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(-1), anyString(), eq((Double) null));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        AnalyzerMessage analyzerMessage = (AnalyzerMessage) invocation.getArguments()[0];
        reportedMessage = "analyzerMessage:" + analyzerMessage.getMessage();
        for (AnalyzerMessage secondary : analyzerMessage.secondaryLocations) {
          reportedMessage += ";" + secondary.getMessage();
        }
        return null;
      }
    }).when(sonarComponents).reportIssue(any(AnalyzerMessage.class));

    return sonarComponents;
  }

  private static LocatedTree fakeLocatedTreeWithUnknownColumn(int line) {
    LocatedTreeImpl tree = new LocatedTreeImpl() {
    };
    tree.setStartLocation(new XmlLocation(line, 0));
    return tree;
  }

  private static LocatedTree fakeLocatedTree(int line, int column) {
    LocatedTreeImpl tree = new LocatedTreeImpl() {
    };
    tree.setStartLocation(new XmlLocation(line, column, 0));
    return tree;
  }
}
