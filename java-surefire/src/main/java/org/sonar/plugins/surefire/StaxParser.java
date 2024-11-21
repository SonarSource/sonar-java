/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.surefire;

import com.ctc.wstx.api.WstxInputProperties;
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
import org.sonarsource.analyzer.commons.xml.SafeStaxParserFactory;

public class StaxParser {

  private SMInputFactory inf;
  private SurefireStaxHandler streamHandler;

  public StaxParser(UnitTestIndex index) {
    this.streamHandler = new SurefireStaxHandler(index);
    XMLInputFactory xmlInputFactory = SafeStaxParserFactory.createXMLInputFactory();
    if (xmlInputFactory instanceof WstxInputFactory wstxInputfactory) {
      wstxInputfactory.configureForLowMemUsage();
      wstxInputfactory.getConfig().setUndeclaredEntityResolver((String publicID, String systemID, String baseURI, String namespace) -> namespace);
      wstxInputfactory.setProperty(WstxInputProperties.P_MAX_ATTRIBUTE_SIZE, Integer.MAX_VALUE);
    }
    this.inf = new SMInputFactory(xmlInputFactory);
  }

  public void parse(File xmlFile) throws XMLStreamException {
    try (FileInputStream input = new FileInputStream(xmlFile)) {
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
