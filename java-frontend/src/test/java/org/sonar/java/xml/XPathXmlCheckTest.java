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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Node;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XPathXmlCheckTest {

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


  @Test
  public void init_should_be_called_before_scan() throws Exception {
    XmlCheck check = new XPathXmlCheck() {
      private boolean initialized = false;
      private boolean scanned = false;

      @Override
      public void scanFileWithXPathExpressions(XmlCheckContext context) {
        assertThat(initialized).isTrue();
        scanned = true;
      }

      @Override
      public void precompileXPathExpressions(XmlCheckContext context) {
        assertThat(scanned).isFalse();
        initialized = true;
      }
    };
    check.scanFile(context);
  }

  @Test
  public void should_report_issues_on_node() throws Exception {
    final Node node = mock(Node.class);
    final String message = "message";
    XmlCheck check = new XPathXmlCheck() {
      @Override
      public void scanFileWithXPathExpressions(XmlCheckContext context) {
      }

      @Override
      public void precompileXPathExpressions(XmlCheckContext context) {
        reportIssue(node, message);
      }
    };
    check.scanFile(context);
    verify(context).reportIssue(eq(check), eq(node), eq(message));
  }

  @Test
  public void should_report_issues_on_File() throws Exception {
    final String message = "message";
    XmlCheck check = new XPathXmlCheck() {
      @Override
      public void scanFileWithXPathExpressions(XmlCheckContext context) {
      }

      @Override
      public void precompileXPathExpressions(XmlCheckContext context) {
        reportIssueOnFile(message);
      }
    };
    check.scanFile(context);
    verify(context).reportIssueOnFile(eq(check), eq(message));
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

  private static class CounterXmlCheck extends XPathXmlCheck {
    private int countedScan = 0;
    private int countedInit = 0;

    @Override
    public void scanFileWithXPathExpressions(XmlCheckContext context) {
      countedScan++;
    }

    @Override
    public void precompileXPathExpressions(XmlCheckContext context) {
      countedInit++;
    }
  }

}
