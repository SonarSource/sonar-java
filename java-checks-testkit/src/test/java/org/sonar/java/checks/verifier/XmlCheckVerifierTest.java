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
package org.sonar.java.checks.verifier;

import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.java.checks.verifier.XmlCheckVerifier.FakeXmlCheckContext;
import org.sonar.java.xml.XmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class XmlCheckVerifierTest {

  private static final String XML_WITH_ISSUES = "src/test/files/xml/XmlCheckVerifier.xml";
  private static final String XML_NO_ISSUE = "src/test/files/xml/XmlCheckVerifierNoIssue.xml";
  private static final String XML_PARSING_ISSUE = "src/test/files/xml/XmlCheckVerifierParsingIssue.xml";
  private static XmlCheckContext fakeContext;

  @Test
  public void should_detect_issues() {
    XmlCheckVerifier.verify(XML_WITH_ISSUES, new XmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        fakeContext = context;
        context.reportIssue(this, 2, "Message1");
        context.reportIssue(this, 4, "Message2");
      }
    });
    assertThat(((FakeXmlCheckContext) fakeContext).getMessages()).hasSize(2);
  }

  @Test
  public void should_detect_issues_using_nodes() {
    XmlCheckVerifier.verify(XML_WITH_ISSUES, new XmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        try {
          context.reportIssue(this, context.evaluateXPathExpression("//test2").item(0), "Message1");
          context.reportIssue(this, context.evaluateXPathExpression("//test4").item(0), "Message2");
        } catch (Exception e) {
          Fail.fail();
        }
      }
    });
  }

  @Test
  public void should_failt_when_adding_issues_on_node_from_own_parsing() {
    try {
      XmlCheckVerifier.verify(XML_WITH_ISSUES, new XmlCheck() {
        @Override
        public void scanFile(XmlCheckContext context) {
          try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(context.getFile());
            Node node = ((NodeList) XPathFactory.newInstance().newXPath().compile("//test2").evaluate(doc, XPathConstants.NODESET)).item(0);
            context.reportIssue(this, node, "Message1");
          } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            Fail.fail();
          }
        }
      });
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The provided node does not have line attribute 'start_column'");
    }
  }

  @Test
  public void should_detect_issue_on_file() {
    XmlCheckVerifier.verifyIssueOnFile(XML_NO_ISSUE, "Message", new XmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        context.reportIssueOnFile(this, "Message");
      }
    });
  }

  @Test
  public void should_not_detect_issue() {
    XmlCheckVerifier.verifyNoIssue(XML_NO_ISSUE, new XmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        // do nothing
      }
    });
  }

  @Test(expected = AssertionError.class)
  public void should_fail_when_parsing_issue() {
    XmlCheckVerifier.verifyNoIssue(XML_PARSING_ISSUE, new XmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        // do nothing
      }
    });
  }

}
