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

import com.ctc.wstx.stax.WstxInputFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.sonar.plugins.surefire.data.SurefireStaxHandler;
import org.sonar.plugins.surefire.data.UnitTestIndex;
import org.sonarsource.analyzer.commons.xml.SafetyFactory;

public class StaxParser {

  private SMInputFactory inf;
  private SurefireStaxHandler streamHandler;

  public StaxParser(UnitTestIndex index) {
    this.streamHandler = new SurefireStaxHandler(index);
    XMLInputFactory xmlInputFactory = SafetyFactory.createXMLInputFactory();
    if (xmlInputFactory instanceof WstxInputFactory) {
      WstxInputFactory wstxInputfactory = (WstxInputFactory) xmlInputFactory;
      wstxInputfactory.configureForLowMemUsage();
      wstxInputfactory.getConfig().setUndeclaredEntityResolver((String publicID, String systemID, String baseURI, String namespace) -> namespace);
    }
    this.inf = new SMInputFactory(xmlInputFactory);
  }

  public void parse(File xmlFile) throws XMLStreamException {
    try(FileInputStream input = new FileInputStream(xmlFile)) {
      parse(inf.rootElementCursor(input));
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  private void parse(SMHierarchicCursor rootCursor) throws XMLStreamException {
    try {
      streamHandler.stream(rootCursor);
    } finally {
      rootCursor.getStreamReader().closeCompletely();
    }
  }
}
