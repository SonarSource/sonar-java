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
package org.sonar.java.xml;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.squidbridge.api.AnalysisException;

import javax.xml.xpath.XPathExpressionException;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XPathInitializerXmlCheckTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private XmlCheckContext context;

  @Before
  public void setup() {
    context = mock(XmlCheckContext.class);
    File f = mock(File.class);
    when(context.getFile()).thenReturn(f);
    when(f.getAbsolutePath()).thenReturn("'path'");
  }

  @Test()
  public void should_fail_when_initialiation_fail() throws Exception {
    XmlCheck check = new XPathInitializerXmlCheck() {
      @Override
      public void initXPathExpressions(XmlCheckContext context) throws XPathExpressionException {
        throw new XPathExpressionException("");
      }

      @Override
      public void scanFileWithExpressions(XmlCheckContext context) throws XPathExpressionException {
      }
    };
    thrown.expect(AnalysisException.class);
    thrown.expectMessage("Unable perform analysis on file 'path'");

    check.scanFile(context);
  }

  @Test()
  public void should_fail_when_scan_fail() throws Exception {
    XmlCheck check = new XPathInitializerXmlCheck() {
      @Override
      public void initXPathExpressions(XmlCheckContext context) throws XPathExpressionException {
      }

      @Override
      public void scanFileWithExpressions(XmlCheckContext context) throws XPathExpressionException {
        throw new XPathExpressionException("");
      }
    };

    thrown.expect(AnalysisException.class);
    thrown.expectMessage("Unable perform analysis on file 'path'");

    check.scanFile(context);
  }

  @Test
  public void init_should_be_called_before_scan() throws Exception {
    XmlCheck check = new XPathInitializerXmlCheck() {
      private boolean initialized = false;
      private boolean scanned = false;

      @Override
      public void scanFileWithExpressions(XmlCheckContext context) throws XPathExpressionException {
        assertThat(initialized).isTrue();
        scanned = true;
      }

      @Override
      public void initXPathExpressions(XmlCheckContext context) throws XPathExpressionException {
        assertThat(scanned).isFalse();
        initialized = true;
      }
    };
    check.scanFile(context);
  }

  @Test
  public void init_should_be_called_only_once() throws Exception {
    final int numberScan = 5;
    CounterXmlCheck check = new CounterXmlCheck();
    for (int i = 0; i < numberScan; i++) {
      check.scanFile(context);
    }
    assertThat(check.countedInit).isEqualTo(1);
    assertThat(check.countedScan).isEqualTo(numberScan);
  }

  private static class CounterXmlCheck extends XPathInitializerXmlCheck {
    private int countedScan = 0;
    private int countedInit = 0;

    @Override
    public void scanFileWithExpressions(XmlCheckContext context) throws XPathExpressionException {
      countedScan++;
    }

    @Override
    public void initXPathExpressions(XmlCheckContext context) throws XPathExpressionException {
      countedInit++;
    }
  }

}
