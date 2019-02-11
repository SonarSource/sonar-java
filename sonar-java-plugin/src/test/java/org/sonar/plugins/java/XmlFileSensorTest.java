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
package org.sonar.plugins.java;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlFileSensorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private static final RuleKey XML_RULE_KEY = RuleKey.of("squid", "S3281");
  private SensorContextTester context;

  @Before
  public void setUp() throws Exception {
    context = SensorContextTester.create(temporaryFolder.newFolder());
  }

  @Test
  public void test() throws Exception {
    CheckFactory checkFactory = new CheckFactory(new ActiveRulesBuilder().create(XML_RULE_KEY).activate().build());
    XmlFileSensor sensor = new XmlFileSensor(checkFactory);

    DefaultInputFile xml = (DefaultInputFile) addFileWithIssue("xml");
    sensor.execute(context);
    assertThat(xml.isPublished()).isTrue();

    Collection<Issue> issues = context.allIssues();
    assertThat(issues).hasSize(1);
    Issue issue = issues.iterator().next();

    assertThat(issue.ruleKey()).isEqualTo(XML_RULE_KEY);
    assertThat(issue.primaryLocation().message()).isEqualTo("Move this default interceptor to \"ejb-jar.xml\"");
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(5);
  }

  @Test
  public void testDoNothingIfNoXmlFile() throws Exception {
    CheckFactory checkFactory = new CheckFactory(new ActiveRulesBuilder().create(XML_RULE_KEY).activate().build());
    XmlFileSensor sensor = new XmlFileSensor(checkFactory);

    addFileWithIssue("foo");
    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  public void testDoNothingIfNoXmlRule() throws Exception {
    CheckFactory checkFactory = new CheckFactory(new ActiveRulesBuilder().build());
    XmlFileSensor sensor = new XmlFileSensor(checkFactory);

    DefaultInputFile xml = (DefaultInputFile) addFileWithIssue("xml");
    sensor.execute(context);
    assertThat(xml.isPublished()).isTrue();

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  public void testHandleAnalysisCancellation() throws Exception {
    CheckFactory checkFactory = new CheckFactory(new ActiveRulesBuilder().build());
    XmlFileSensor sensor = new XmlFileSensor(checkFactory);

    context.setCancelled(true);

    DefaultInputFile xml = (DefaultInputFile) addFileWithIssue("xml");
    sensor.execute(context);
    assertThat(xml.isPublished()).isTrue();

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  public void testDoNothingIfParsingError() throws Exception {
    CheckFactory checkFactory = new CheckFactory(new ActiveRulesBuilder().create(XML_RULE_KEY).activate().build());
    XmlFileSensor sensor = new XmlFileSensor(checkFactory);

    DefaultInputFile notXml = TestInputFileBuilder.create("moduleKey", "test.xml")
      .setCharset(StandardCharsets.UTF_8)
      .setContents("<ejb-jar")
      .build();
    context.fileSystem().add(notXml);

    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG).get(0)).isEqualTo("Skipped 'test.xml' due to parsing error");
  }

  @Test
  public void testDoNotFailAnalysisIfUnexpectedIssue() throws Exception {
    CheckFactory checkFactory = new CheckFactory(new ActiveRulesBuilder().create(XML_RULE_KEY).activate().build());
    XmlFileSensor sensor = new XmlFileSensor(checkFactory);

    DefaultInputFile inputFile = TestInputFileBuilder.create("moduleKey", "test.xml")
      .setCharset(StandardCharsets.UTF_8)
      .setContents(
        "<a>\n"
          + "  <b />\n"
          + "</a>\n")
      .build();
    DefaultInputFile mocked = Mockito.spy(inputFile);
    Mockito.when(mocked.contents()).thenThrow(new IllegalStateException("This should have been caught."));
    context.fileSystem().add(mocked);
    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN).get(0)).startsWith("Unable to analyse file 'test.xml'.");
  }

  @Test
  public void testDescriptor() throws Exception {
    XmlFileSensor sensor = new XmlFileSensor(new CheckFactory(new ActiveRulesBuilder().build()));
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);

    assertThat(descriptor.name()).isEqualTo("JavaXmlSensor");
    // todo: do we want to run this sensor only for projects containing JVM languages
    assertThat(descriptor.languages()).isEmpty();
    assertThat(descriptor.isGlobal()).isFalse();
    assertThat(descriptor.configurationPredicate().test(new MapSettings().asConfig())).isFalse();

    sensor = new XmlFileSensor(new CheckFactory(new ActiveRulesBuilder().create(XML_RULE_KEY).activate().build()));
    descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.languages()).isEmpty();
    assertThat(descriptor.isGlobal()).isFalse();
    assertThat(descriptor.configurationPredicate().test(new MapSettings().asConfig())).isTrue();

  }

  @Test
  public void testCheckFailure() throws Exception {
    XmlFileSensor sensor = new XmlFileSensor(new CheckFactory(new ActiveRulesBuilder().build()));
    InputFile inputFile = addFileWithIssue("xml");
    XmlFile xmlFile = XmlFile.create(inputFile);
    sensor.scanFile(context, xmlFile, new TestCheck(), XML_RULE_KEY);

    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).get(0)).isEqualTo("Failed to analyze 'test.xml' with rule squid:S3281");
  }

  private InputFile addFileWithIssue(String extension) {
    DefaultInputFile inputFile = TestInputFileBuilder.create("moduleKey", "test." + extension)
      .setCharset(StandardCharsets.UTF_8)
      .setContents("<ejb-jar>\n" +
        "  <assembly-descriptor>\n" +
        "    <interceptor-binding>\n" +
        "      <ejb-name>*</ejb-name>\n" +
        "      <interceptor-class>com.myco.ImportantInterceptor1</interceptor-class>" +
        "    </interceptor-binding>\n" +
        "  </assembly-descriptor>\n" +
        "</ejb-jar>")
      .setPublish(false)
      .build();
    context.fileSystem().add(inputFile);
    return inputFile;
  }

  private static class TestCheck extends SonarXmlCheck {

    @Override
    public void scanFile(XmlFile file) {
      throw new IllegalStateException("Something wrong happened");
    }
  }

}
