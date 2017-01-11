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
package org.sonar.java.xml.maven;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.SonarComponents;
import org.sonar.java.xml.maven.PomCheckContext.Location;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.LocatedTreeImpl;
import org.sonar.maven.model.XmlLocation;
import org.sonar.maven.model.maven2.MavenProject;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class PomCheckContextImplTest {

  private static String reportedMessage;
  private PomCheckContext context;
  private static final PomCheck CHECK = new PomCheck() {
    @Override
    public void scanFile(PomCheckContext context) {
    }
  };
  private static final int LINE = 42;

  @Before
  public void setup() {
    reportedMessage = null;
    SonarComponents sonarComponents = createSonarComponentsMock();
    XPath xPath = XPathFactory.newInstance().newXPath();
    context = new PomCheckContextImpl(mock(MavenProject.class), mock(Document.class), mock(File.class), xPath, sonarComponents);
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

    // secondary on located tree
    int secondary1Line = 42;
    int secondary1Column = 3;
    int secondary1Size = 5;
    String msg1 = "msg1";
    secondaries.add(new Location(msg1, fakeLocatedTree(secondary1Line, secondary1Column, secondary1Size)));

    // secondary on located tree with unknown column
    int secondary2Line = 43;
    String msg2 = "msg2";
    secondaries.add(new Location(msg2, fakeLocatedTreeWithUnknownColumn(secondary2Line)));

    String msg = "message";
    context.reportIssue(CHECK, LINE, msg, secondaries);

    String expected = "analyzerMessage:" + msg;
    expected += ";" + msg1 + "[" + secondary1Line + ";" + (secondary1Column - 1) + "/" + secondary1Line + ";" + (secondary1Column + secondary1Size - 1) + "]";
    expected += ";" + msg2 + "[" + secondary2Line + ";0/" + secondary2Line + ";0]";

    assertThat(reportedMessage).isEqualTo(expected);
  }

  private static SonarComponents createSonarComponentsMock() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onLine:" + invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(LINE), anyString(), eq(null));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onFile:" + invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(-1), anyString(), eq(null));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        AnalyzerMessage analyzerMessage = (AnalyzerMessage) invocation.getArguments()[0];
        reportedMessage = "analyzerMessage:" + analyzerMessage.getMessage();
        for (AnalyzerMessage secondary : analyzerMessage.flows.stream().map(l -> l.get(0)).collect(Collectors.toList())) {
          TextSpan location = secondary.primaryLocation();
          reportedMessage += ";" + secondary.getMessage() + "[" +
            location.startLine + ";" + location.startCharacter + "/" +
            location.endLine + ";" + location.endCharacter + "]";
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
    tree.setEndLocation(new XmlLocation(line, 0));
    return tree;
  }

  private static LocatedTree fakeLocatedTree(int line, int column, int size) {
    LocatedTreeImpl tree = new LocatedTreeImpl() {
    };
    tree.setStartLocation(new XmlLocation(line, column, 0));
    tree.setEndLocation(new XmlLocation(line, column + size, size));
    return tree;
  }
}
