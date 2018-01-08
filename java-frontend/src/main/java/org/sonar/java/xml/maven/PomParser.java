/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.xml.maven;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.maven.model.LocatedAttributeAdapter;
import org.sonar.maven.model.LocatedTreeImpl;
import org.sonar.maven.model.XmlLocation;
import org.sonar.maven.model.maven2.MavenProject;

import javax.annotation.CheckForNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PomParser {

  private static final Logger LOG = Loggers.get(PomParser.class);

  private PomParser() {
  }

  @CheckForNull
  public static MavenProject parseXML(File file) {
    try (FileInputStream is = new FileInputStream(file)) {
      // it is necessary to provide classloader explicitly, otherwise Thread.contextClassLoader will be used,
      // which doesn't include jar of plugin
      JAXBContext context = JAXBContext.newInstance("org.sonar.maven.model.maven2", PomParser.class.getClassLoader());
      XMLInputFactory factory = XMLInputFactory.newInstance();
      enableLocationPropertyForIBM(factory);
      XMLStreamReader reader = factory.createXMLStreamReader(is);
      StreamListener streamListener = new StreamListener(reader);
      Unmarshaller unmarshaller = context.createUnmarshaller();
      unmarshaller.setListener(streamListener);
      unmarshaller.setAdapter(new LocatedAttributeAdapter(reader));
      JAXBElement<MavenProject> unmarshalledObject = unmarshaller.unmarshal(reader, MavenProject.class);
      if (!"project".equalsIgnoreCase(unmarshalledObject.getName().getLocalPart())) {
        return null;
      }
      return unmarshalledObject.getValue();
    } catch (JAXBException | XMLStreamException | IOException e) {
      LOG.error("Unable to parse pom file " + file.getPath(), e);
    }
    return null;
  }

  /**
   * By default, the location of XML element is enabled, except on IBM JVM where it is disabled and has to be manually enabled.
   * The property is a IBM-specific property, not recognized by non-IBM JVMs.
   *
   * See {@link https://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/com.ibm.java.zos.80.doc/user/xml/xlxpj_reference.html}.
   *
   * @param factory
   */
  private static void enableLocationPropertyForIBM(XMLInputFactory factory) {
    if (factory.isPropertySupported("javax.xml.stream.isSupportingLocationCoordinates")) {
      factory.setProperty("javax.xml.stream.isSupportingLocationCoordinates", true);
    }
  }

  private static class StreamListener extends Listener {
    private final XMLStreamReader reader;

    public StreamListener(XMLStreamReader reader) {
      this.reader = reader;
    }

    @Override
    public void beforeUnmarshal(Object target, Object parent) {
      XmlLocation beforeLocation = XmlLocation.getLocation(reader.getLocation());
      ((LocatedTreeImpl) target).setStartLocation(beforeLocation);
    }

    @Override
    public void afterUnmarshal(Object target, Object parent) {
      XmlLocation afterLocation = XmlLocation.getLocation(reader.getLocation());
      ((LocatedTreeImpl) target).setEndLocation(afterLocation);
    }
  }
}
