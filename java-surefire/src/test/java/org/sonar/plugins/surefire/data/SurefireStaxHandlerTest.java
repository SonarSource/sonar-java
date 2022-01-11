/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.plugins.surefire.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.sonar.plugins.surefire.StaxParser;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

class SurefireStaxHandlerTest {

  private UnitTestIndex index;

  @BeforeEach
  public void setUp() {
    index = new UnitTestIndex();
  }

  @Test
  void shouldLoadInnerClasses() throws XMLStreamException {
    parse("innerClasses.xml");

    UnitTestClassReport publicClass = index.get("org.apache.commons.collections.bidimap.AbstractTestBidiMap");
    assertThat(publicClass.getTests()).isEqualTo(2);

    UnitTestClassReport innerClass1 = index.get("org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet");
    assertThat(innerClass1.getTests()).isEqualTo(2);

    UnitTestClassReport innerClass2 = index.get("org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestInverseBidiMap");
    assertThat(innerClass2.getTests()).isEqualTo(3);
    assertThat(innerClass2.getDurationMilliseconds()).isEqualTo(30 + 1L);
    assertThat(innerClass2.getErrors()).isEqualTo(1);
  }

  @Test
  void shouldHaveSkippedTests() throws XMLStreamException {
    parse("skippedTests.xml");
    UnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getTests()).isEqualTo(3);
    assertThat(report.getSkipped()).isEqualTo(1);
  }

  @Test
  void shouldHaveZeroTests() throws XMLStreamException {
    parse("zeroTests.xml");
    assertThat(index.size()).isZero();
  }

  @Test
  void shouldHaveTestOnRootPackage() throws XMLStreamException {
    parse("rootPackage.xml");
    assertThat(index.size()).isEqualTo(1);
    UnitTestClassReport report = index.get("NoPackagesTest");
    assertThat(report.getTests()).isEqualTo(2);
  }

  @Test
  void shouldHaveErrorsAndFailures() throws XMLStreamException {
    parse("errorsAndFailures.xml");
    UnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getErrors()).isEqualTo(1);
    assertThat(report.getFailures()).isEqualTo(1);
    assertThat(report.getResults()).hasSize(2);

    // failure
    UnitTestResult failure = report.getResults().get(0);
    assertThat(failure.getDurationMilliseconds()).isEqualTo(5L);
    assertThat(failure.getStatus()).isEqualTo(UnitTestResult.STATUS_FAILURE);
    assertThat(failure.getName()).isEqualTo("testOne");
    assertThat(failure.getMessage()).startsWith("expected");

    // error
    UnitTestResult error = report.getResults().get(1);
    assertThat(error.getDurationMilliseconds()).isZero();
    assertThat(error.getStatus()).isEqualTo(UnitTestResult.STATUS_ERROR);
    assertThat(error.getName()).isEqualTo("testTwo");
  }

  @Test
  void shouldSupportMultipleSuitesInSameReport() throws XMLStreamException {
    parse("multipleSuites.xml");

    assertThat(index.get("org.sonar.JavaNCSSCollectorTest").getTests()).isEqualTo(11);
    assertThat(index.get("org.sonar.SecondTest").getTests()).isEqualTo(4);
  }

  @Test
  void shouldSupportSkippedTestWithoutTimeAttribute() throws XMLStreamException {
    parse("skippedWithoutTimeAttribute.xml");

    UnitTestClassReport publicClass = index.get("TSuite.A");
    assertThat(publicClass.getSkipped()).isEqualTo(2);
    assertThat(publicClass.getTests()).isEqualTo(4);
  }

  @Test
  void output_of_junit_5_2_test_without_display_name() throws XMLStreamException {
    parse("TEST-#29.xml");
    assertThat(index.get(")").getTests()).isEqualTo(1);
  }


  private void parse(String path) throws XMLStreamException {
    StaxParser parser = new StaxParser(index);
    File xmlFile;
    try {
      xmlFile = new File(getClass().getResource(getClass().getSimpleName() + "/" + path).toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
    parser.parse(xmlFile);
  }
}
