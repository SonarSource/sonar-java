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
package org.sonar.java.checks.verifier;

import com.google.common.collect.Lists;
import org.assertj.core.api.Fail;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.check.Rule;
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

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlCheckVerifierTest {

  private static final String XML_WITH_ISSUES = "src/test/files/xml/XmlCheckVerifier.xml";
  private static final String XML_WITH_SECONDARIES = "src/test/files/xml/XmlCheckVerifierSecondaries.xml";
  private static final String XML_NO_ISSUE = "src/test/files/xml/XmlCheckVerifierNoIssue.xml";
  private static final String XML_PARSING_ISSUE = "src/test/files/xml/XmlCheckVerifierParsingIssue.xml";
  private static XmlCheckContext fakeContext;

  @Rule(key = "ConstantJSON")
  private interface TestXmlCheck extends XmlCheck {}

  @Test
  public void should_detect_issues() {
    XmlCheckVerifier.verify(XML_WITH_ISSUES, new TestXmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        fakeContext = context;
        context.reportIssue(this, 2, "message1");
        context.reportIssue(this, 4, "message2");
      }
    });
    assertThat(((FakeXmlCheckContext) fakeContext).getMessages()).hasSize(2);
  }

  @Test
  public void should_detect_issues_using_nodes() {
    XmlCheckVerifier.verify(XML_WITH_ISSUES, new TestXmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        try {
          context.reportIssue(this, firstNode(context, "//test2"), "message1");
          context.reportIssue(this, firstNode(context, "//test4"), "message2");
        } catch (Exception e) {
          Fail.fail("");
        }
      }
    });
  }

  @Test
  public void should_detect_issues_using_secondaries() {
    XmlCheckVerifier.verify(XML_WITH_SECONDARIES, new TestXmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        try {
          context.reportIssue(
            this,
            firstNode(context, "//test2"),
            "Message1",
            Lists.newArrayList(new XmlCheckContext.XmlDocumentLocation("Message2", firstNode(context, "//test4"))));
        } catch (Exception e) {
          Fail.fail("");
        }
      }
    });
  }

  private static Node firstNode(XmlCheckContext context, String expression) throws XPathExpressionException {
    Node result = null;
    for (Node node : context.evaluateOnDocument(context.compile(expression))) {
      result = node;
      break;
    }
    return result;
  }

  @Test
  public void should_fail_when_adding_issues_on_unknown_node() {
    try {
      XmlCheckVerifier.verify(XML_WITH_ISSUES, new TestXmlCheck() {
        @Override
        public void scanFile(XmlCheckContext context) {
          try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(context.getFile());
            Node node = ((NodeList) XPathFactory.newInstance().newXPath().compile("//test2").evaluate(doc, XPathConstants.NODESET)).item(0);
            context.reportIssue(this, node, "Message1");
          } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            Fail.fail("");
          }
        }
      });
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The provided node does not have line attribute 'start_column'");
    }
  }

  @Test
  public void should_detect_issue_on_file() {
    XmlCheckVerifier.verifyIssueOnFile(XML_NO_ISSUE, "Message", new TestXmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        context.reportIssueOnFile(this, "Message");
      }
    });
  }

  @Test
  public void should_not_detect_issue() {
    XmlCheckVerifier.verifyNoIssue(XML_NO_ISSUE, new TestXmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        // do nothing
      }
    });
  }

  @Test(expected = AssertionError.class)
  public void should_fail_when_parsing_issue() {
    XmlCheckVerifier.verifyNoIssue(XML_PARSING_ISSUE, new TestXmlCheck() {
      @Override
      public void scanFile(XmlCheckContext context) {
        // do nothing
      }
    });
  }

  @Test(expected = AssertionError.class)
  public void should_fail_retrieving_messages_when_parsing_issue() {
    XmlCheckVerifier.retrieveExpectedIssuesFromFile(new File(XML_PARSING_ISSUE), Mockito.mock(CheckVerifier.class));
  }

  @Test(expected = AssertionError.class)
  public void should_fail_retrieving_messages_when_files_does_not_exist() {
    XmlCheckVerifier.retrieveExpectedIssuesFromFile(new File(""), Mockito.mock(CheckVerifier.class));
  }

}
