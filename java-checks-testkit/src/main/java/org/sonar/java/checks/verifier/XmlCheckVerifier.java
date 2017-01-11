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

import com.google.common.annotations.Beta;
import org.assertj.core.api.Fail;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.xml.XmlCheck;
import org.sonar.java.xml.XmlCheckContextImpl;
import org.sonar.java.xml.XmlCheckUtils;
import org.sonar.java.xml.XmlParser;
import org.sonar.plugins.java.api.JavaCheck;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Beta
public class XmlCheckVerifier extends CheckVerifier {

  private XmlCheckVerifier() {
  }

  @Override
  public String getExpectedIssueTrigger() {
    return ISSUE_MARKER;
  }

  public static void verify(String filename, XmlCheck check) {
    XmlCheckVerifier xmlCheckVerifier = new XmlCheckVerifier();
    scanFile(filename, check, xmlCheckVerifier);
  }

  public static void verifyNoIssue(String filename, XmlCheck check) {
    XmlCheckVerifier xmlCheckVerifier = new XmlCheckVerifier();
    xmlCheckVerifier.expectNoIssues();
    scanFile(filename, check, xmlCheckVerifier);
  }

  public static void verifyIssueOnFile(String filename, String message, XmlCheck check) {
    XmlCheckVerifier xmlCheckVerifier = new XmlCheckVerifier();
    xmlCheckVerifier.setExpectedFileIssue(message);
    scanFile(filename, check, xmlCheckVerifier);
  }

  private static void scanFile(String filename, XmlCheck check, XmlCheckVerifier xmlCheckVerifier) {
    File file = new File(filename);
    Document document = XmlParser.parseXML(file);
    if (document != null) {
      retrieveExpectedIssuesFromFile(file, xmlCheckVerifier);
      FakeXmlCheckContext context = new FakeXmlCheckContext(document, file);
      check.scanFile(context);
      xmlCheckVerifier.checkIssues(context.messages, false);
    } else {
      Fail.fail("The test file can not be parsed");
    }
  }

  /**
   * Read the XML file using javax XMLStreamReader instead of SAX (used by {@link XmlParser}),
   * as SAX does not provide any  way to retrieve lines information of comments events.
   *
   * @param xmlFile
   * @param checkVerifier
   */
  protected static void retrieveExpectedIssuesFromFile(File xmlFile, CheckVerifier checkVerifier) {
    XMLInputFactory factory = XMLInputFactory.newInstance();

    // disable DTD validation, in order to avoid query any external URL
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

    try (FileInputStream is = new FileInputStream(xmlFile)) {
      XMLStreamReader reader = factory.createXMLStreamReader(is);
      while (reader.hasNext()) {
        int line = reader.getLocation().getLineNumber();
        reader.next();
        if (reader.getEventType() == XMLStreamReader.COMMENT) {
          String text = reader.getText().trim();
          checkVerifier.collectExpectedIssues(text, line);
        }
      }
    } catch (XMLStreamException | IOException e) {
      Fail.fail("The test file can not be parsed to retrieve comments", e);
    }
  }

  public static class FakeXmlCheckContext extends XmlCheckContextImpl {

    private final Set<AnalyzerMessage> messages = new HashSet<>();

    public FakeXmlCheckContext(Document document, File file) {
      super(document, file, XPathFactory.newInstance().newXPath(), null);
    }

    public Set<AnalyzerMessage> getMessages() {
      return messages;
    }

    @Override
    public void reportIssueOnFile(JavaCheck check, String message) {
      reportIssue(check, -1, message);
    }

    @Override
    public void reportIssue(JavaCheck check, Node node, String message) {
      reportIssue(check, node, message, new HashSet<XmlDocumentLocation>(), null);
    }

    @Override
    public void reportIssue(JavaCheck check, int line, String message) {
      messages.add(new AnalyzerMessage(check, getFile(), line, message, 0));
    }

    @Override
    public void reportIssue(JavaCheck check, Node node, String message, Iterable<XmlDocumentLocation> secondary) {
      reportIssue(check, node, message, secondary, null);
    }

    @Override
    public void reportIssue(JavaCheck check, Node node, String message, Iterable<XmlDocumentLocation> secondary, @Nullable Integer cost) {
      Integer line = XmlCheckUtils.nodeLine(node);
      if (line != null) {
        messages.add(buildAnalyzerMessage(check, message, line, secondary, cost, getFile()));
      } else {
        Fail.fail("The provided node does not have line attribute '" + XmlParser.START_COLUMN_ATTRIBUTE + "'");
      }
    }
  }
}
