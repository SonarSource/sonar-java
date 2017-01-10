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
package org.sonar.plugins.surefire;

import com.ctc.wstx.stax.WstxInputFactory;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.sonar.plugins.surefire.data.SurefireStaxHandler;
import org.sonar.plugins.surefire.data.UnitTestIndex;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class StaxParser {

  private SMInputFactory inf;
  private SurefireStaxHandler streamHandler;

  public StaxParser(UnitTestIndex index) {
    this.streamHandler = new SurefireStaxHandler(index);
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    if (xmlFactory instanceof WstxInputFactory) {
      WstxInputFactory wstxInputfactory = (WstxInputFactory) xmlFactory;
      wstxInputfactory.configureForLowMemUsage();
      wstxInputfactory.getConfig().setUndeclaredEntityResolver((String publicID, String systemID, String baseURI, String namespace) -> namespace);
    }
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
    inf = new SMInputFactory(xmlFactory);
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
