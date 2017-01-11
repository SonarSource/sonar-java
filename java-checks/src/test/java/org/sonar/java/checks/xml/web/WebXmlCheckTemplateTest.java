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
package org.sonar.java.checks.xml.web;

import org.junit.Test;
import org.sonar.java.checks.verifier.XmlCheckVerifier;
import org.sonar.java.xml.XmlCheck;
import org.sonar.java.xml.XmlCheckContext;

public class WebXmlCheckTemplateTest {

  private static final XmlCheck CHECK = new WebXmlFakeCheck();

  @Test
  public void scan_file_if_web_xml() throws Exception {
    XmlCheckVerifier.verifyIssueOnFile("src/test/files/checks/xml/web/WebXmlCheck/web.xml", "expected", CHECK);
  }

  @Test
  public void do_not_scan_file_if_not_web_xml() throws Exception {
    XmlCheckVerifier.verifyNoIssue("src/test/files/checks/xml/web/WebXmlCheck/beans.xml", CHECK);
  }

  private static class WebXmlFakeCheck extends WebXmlCheckTemplate {

    @Override
    public void scanWebXml(XmlCheckContext context) {
      reportIssueOnFile("expected");
    }

    @Override
    public void precompileXPathExpressions(XmlCheckContext context) {
    }
  }
}
