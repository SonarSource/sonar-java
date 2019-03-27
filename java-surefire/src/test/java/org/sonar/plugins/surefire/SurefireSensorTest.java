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
package org.sonar.plugins.surefire;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.surefire.api.SurefireUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SurefireSensorTest {

  private ResourcePerspectives perspectives;
  private JavaResourceLocator javaResourceLocator;
  private SurefireSensor surefireSensor;
  private DefaultFileSystem fs;
  private PathResolver pathResolver = new PathResolver();

  @Before
  public void before() {
    fs = new DefaultFileSystem(new File("src/test/resources"));
    DefaultInputFile javaFile = new TestInputFileBuilder("", "src/org/foo/java").setLanguage("java").build();
    fs.add(javaFile);
    perspectives = mock(ResourcePerspectives.class);

    javaResourceLocator = mock(JavaResourceLocator.class);
    when(javaResourceLocator.findResourceByClassName(anyString())).thenAnswer(invocation -> resource((String) invocation.getArguments()[0]));

    surefireSensor = new SurefireSensor(new SurefireJavaParser(perspectives, javaResourceLocator), new MapSettings().asConfig(), fs, pathResolver);
  }

  private DefaultInputFile resource(String key) {
    return new TestInputFileBuilder("", key).setType(InputFile.Type.TEST).build();
  }

  @Test
  public void should_execute_if_filesystem_contains_java_files() {
    surefireSensor = new SurefireSensor(new SurefireJavaParser(perspectives, javaResourceLocator), new MapSettings().asConfig(), fs, pathResolver);
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    surefireSensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly("java");
  }

  @Test
  public void shouldNotFailIfReportsNotFound() {
    MapSettings settings = new MapSettings();
    settings.setProperty(SurefireUtils.SUREFIRE_REPORT_PATHS_PROPERTY, "unknown");

    SurefireSensor surefireSensor = new SurefireSensor(mock(SurefireJavaParser.class), settings.asConfig(), fs, pathResolver);
    surefireSensor.execute(mock(SensorContext.class));
  }

  @Test
  public void shouldHandleTestSuiteDetails() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.core.ExtensionsFinderTest"))
      .add(resource("org.sonar.core.ExtensionsFinderTest2"))
      .add(resource("org.sonar.core.ExtensionsFinderTest3"));

    collect(context, "/org/sonar/plugins/surefire/SurefireSensorTest/shouldHandleTestSuiteDetails/");

    assertThat(context.measures(":org.sonar.core.ExtensionsFinderTest")).hasSize(5);
    assertThat(context.measures(":org.sonar.core.ExtensionsFinderTest2")).hasSize(5);
    assertThat(context.measures(":org.sonar.core.ExtensionsFinderTest3")).hasSize(5);

    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TESTS).value()).isEqualTo(4);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(111L);

    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(0);

    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.TESTS).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.TEST_FAILURES).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.TEST_ERRORS).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest2", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(0);

    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.TESTS).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(16);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.TEST_FAILURES).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.TEST_ERRORS).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest3", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1);
  }

  @Test
  public void shouldSaveErrorsAndFailuresInXML() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.core.ExtensionsFinderTest"))
      .add(resource("org.sonar.core.ExtensionsFinderTest2"))
      .add(resource("org.sonar.core.ExtensionsFinderTest3"));

    collect(context, "/org/sonar/plugins/surefire/SurefireSensorTest/shouldSaveErrorsAndFailuresInXML/");

    // 1 classes, 5 measures by class
    assertThat(context.measures(":org.sonar.core.ExtensionsFinderTest")).hasSize(5);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.core.ExtensionsFinderTest", CoreMetrics.TESTS).value()).isEqualTo(7);
  }

  @Test
  public void shouldManageClassesWithDefaultPackage() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("NoPackagesTest"));

    collect(context, "/org/sonar/plugins/surefire/SurefireSensorTest/shouldManageClassesWithDefaultPackage/");

    assertThat(context.measure(":NoPackagesTest", CoreMetrics.TESTS).value()).isEqualTo(2);
  }

  @Test
  public void successRatioIsZeroWhenAllTestsFail() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.Foo"));

    collect(context, "/org/sonar/plugins/surefire/SurefireSensorTest/successRatioIsZeroWhenAllTestsFail/");

    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TESTS).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
  }

  @Test
  public void measuresShouldNotIncludeSkippedTests() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.Foo"));

    collect(context, "/org/sonar/plugins/surefire/SurefireSensorTest/measuresShouldNotIncludeSkippedTests/");

    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TESTS).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_ERRORS).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1);
  }

  @Test
  public void noSuccessRatioIfNoTests() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.Foo"));

    collect(context, "/org/sonar/plugins/surefire/SurefireSensorTest/noSuccessRatioIfNoTests/");

    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TESTS).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_ERRORS).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(2);
  }

  @Test
  public void ignoreSuiteAsInnerClass() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.apache.shindig.protocol.TestHandler"));

    collect(context, "/org/sonar/plugins/surefire/SurefireSensorTest/ignoreSuiteAsInnerClass/");

    // ignore TestHandler$Input.xml
    assertThat(context.measure(":org.apache.shindig.protocol.TestHandler", CoreMetrics.TESTS).value()).isEqualTo(0);
    assertThat(context.measure(":org.apache.shindig.protocol.TestHandler", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(1);
  }

  @Test
  public void should_support_reportNameSuffix() throws URISyntaxException {
    SensorContextTester context = SensorContextTester.create(new File(""));
    context.fileSystem()
      .add(resource("org.sonar.Foo"));

    collect(context, "/org/sonar/plugins/surefire/SurefireSensorTest/should_support_reportNameSuffix/");

    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TESTS).value()).isEqualTo(4);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_FAILURES).value()).isEqualTo(2);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.TEST_ERRORS).value()).isEqualTo(0);
    assertThat(context.measure(":org.sonar.Foo", CoreMetrics.SKIPPED_TESTS).value()).isEqualTo(2);
  }

  private void collect(SensorContextTester context, String path) throws URISyntaxException {
    File file = new File(getClass().getResource(path).toURI());
    surefireSensor.collect(context, Collections.singletonList(file));
  }
}
