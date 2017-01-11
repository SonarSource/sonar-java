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
package org.sonar.java.xml;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.java.xml.maven.PomCheck;
import org.sonar.java.xml.maven.PomCheckContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XmlAnalyzerTest {

  private static final String PARSE_ISSUE_POM = "src/test/files/xml/maven/parse-issue/pom.xml";
  private static final String VALID_POM = "src/test/files/xml/maven/simple-project/pom.xml";
  private static final String INVALID_POM = "src/test/files/xml/maven/fake-pom/pom.xml";

  private static final JavaCheck JAVA_CHECK = new JavaFileScanner() {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      context.addIssue(1, this, "message");
    }
  };

  private static final JavaCheck XML_CHECK = new XmlCheck() {
    @Override
    public void scanFile(XmlCheckContext context) {
      context.reportIssue(this, 1, "message");
    }
  };

  private static final JavaCheck POM_CHECK = new PomCheck() {
    @Override
    public void scanFile(PomCheckContext context) {
      context.reportIssue(this, 1, "message");
    }
  };

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_not_scan_file_with_parsing_issue() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File xmlFile = new File(PARSE_ISSUE_POM);
    fs.add(new DefaultInputFile("", xmlFile.getAbsolutePath()).setLanguage("xml"));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, XML_CHECK, POM_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, XML_CHECK, POM_CHECK);
    analyzer.scan(Lists.newArrayList(xmlFile));

    verify(sonarComponents, never()).addIssue(any(File.class), any(JavaCheck.class), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_not_scan_invalid_pom_file() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File xmlFile = new File(INVALID_POM);
    fs.add(new DefaultInputFile("", xmlFile.getAbsolutePath()).setLanguage("xml"));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, POM_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, POM_CHECK);
    analyzer.scan(Lists.newArrayList(xmlFile));

    verify(sonarComponents, never()).addIssue(any(File.class), any(JavaCheck.class), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_interrupt_analysis_when_InterruptedException_is_thrown() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File pomFile = new File(VALID_POM);
    fs.add(new DefaultInputFile("", pomFile.getAbsolutePath()));
    XmlCheckThrowingException check = new XmlCheckThrowingException(new RuntimeException("Analysis cancelled"));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, check);

    thrown.expectMessage("Analysis cancelled");
    thrown.expect(RuntimeException.class);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, check);
    analyzer.scan(Lists.newArrayList(pomFile));
  }

  @Test
  public void should_scan_xml_file_provided() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File xmlFile = new File(VALID_POM);
    fs.add(new DefaultInputFile("", xmlFile.getAbsolutePath()));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, XML_CHECK, POM_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, XML_CHECK, POM_CHECK);
    analyzer.scan(Lists.newArrayList(xmlFile));

    verify(sonarComponents, times(2)).addIssue(any(File.class), any(JavaCheck.class), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_scan_pom_file_with_xml_check() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File xmlFile = new File(VALID_POM);
    fs.add(new DefaultInputFile("", xmlFile.getAbsolutePath()));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, XML_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, XML_CHECK);
    analyzer.scan(Lists.newArrayList(xmlFile));

    verify(sonarComponents, times(1)).addIssue(any(File.class), any(JavaCheck.class), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_scan_pom_file_with_pom_check() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File xmlFile = new File(VALID_POM);
    fs.add(new DefaultInputFile("", xmlFile.getAbsolutePath()));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, POM_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, POM_CHECK);
    analyzer.scan(Lists.newArrayList(xmlFile));

    verify(sonarComponents, times(1)).addIssue(any(File.class), any(JavaCheck.class), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_scan_xml_file__when_no_check_provided() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File xmlFile = new File(VALID_POM);
    fs.add(new DefaultInputFile("", xmlFile.getAbsolutePath()));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, XML_CHECK, POM_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents);
    analyzer.scan(Lists.newArrayList(xmlFile));

    verify(sonarComponents, never()).addIssue(any(File.class), any(JavaCheck.class), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_not_run_pom_check_when_no_pom_file_provided() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File xmlFile = new File("src/test/files/xml/parsing.xml");
    fs.add(new DefaultInputFile("", xmlFile.getAbsolutePath()).setLanguage("xml"));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, XML_CHECK, POM_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, XML_CHECK, POM_CHECK);
    analyzer.scan(Lists.newArrayList(xmlFile));

    verify(sonarComponents, never()).addIssue(any(File.class), eq(POM_CHECK), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, times(1)).addIssue(any(File.class), eq(XML_CHECK), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_not_run_xml_check_when_no_xml_file_provided() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, XML_CHECK, POM_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, XML_CHECK, POM_CHECK);
    analyzer.scan(Lists.<File>newArrayList());

    verify(sonarComponents, never()).addIssue(any(File.class), eq(POM_CHECK), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).addIssue(any(File.class), eq(XML_CHECK), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_not_run_xml_check_when_no_check_provided() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, JAVA_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents);
    analyzer.scan(Lists.<File>newArrayList());

    verify(sonarComponents, never()).addIssue(any(File.class), eq(POM_CHECK), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).addIssue(any(File.class), eq(XML_CHECK), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_not_scan_when_no_xml_check_provided() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    File pomFile = new File(VALID_POM);
    fs.add(new DefaultInputFile("", pomFile.getAbsolutePath()).setLanguage("xml"));
    SonarComponents sonarComponents = createSonarComponentsMock(fs, JAVA_CHECK);

    XmlAnalyzer analyzer = new XmlAnalyzer(sonarComponents, JAVA_CHECK);
    analyzer.scan(Lists.newArrayList(pomFile));

    verify(sonarComponents, never()).addIssue(any(File.class), any(JavaCheck.class), any(Integer.class), anyString(), isNull(Integer.class));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  private static SonarComponents createSonarComponentsMock(DefaultFileSystem fs, CodeVisitor... codeVisitor) {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.checkClasses()).thenReturn(codeVisitor);

    when(sonarComponents.getFileSystem()).thenReturn(fs);

    Checks<JavaCheck> checks = mock(Checks.class);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(mock(RuleKey.class));
    when(sonarComponents.checks()).thenReturn(Lists.<Checks<JavaCheck>>newArrayList(checks));

    return sonarComponents;
  }

  private static class XmlCheckThrowingException implements XmlCheck {

    private final RuntimeException exception;

    public XmlCheckThrowingException(RuntimeException e) {
      this.exception = e;
    }

    @Override
    public void scanFile(XmlCheckContext context) {
      throw exception;
    }
  }

}
