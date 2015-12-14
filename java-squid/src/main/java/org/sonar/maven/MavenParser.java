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
package org.sonar.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.maven.model.LocatedAttributeAdapter;
import org.sonar.maven.model.LocatedTreeImpl;
import org.sonar.maven.model.XmlLocation;
import org.sonar.maven.model.maven2.MavenProject;

import javax.annotation.CheckForNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MavenParser {

  private static final Logger LOG = LoggerFactory.getLogger(MavenParser.class);

  private MavenParser() {
  }

  @CheckForNull
  public static MavenProject parseXML(File file) {
    try (FileInputStream is = new FileInputStream(file)) {
      JAXBContext context = JAXBContext.newInstance(org.sonar.maven.model.maven2.ObjectFactory.class);
      XMLInputFactory factory = XMLInputFactory.newInstance();
      XMLStreamReader reader = factory.createXMLStreamReader(is);
      StreamListener streamListener = new StreamListener(reader);
      Unmarshaller unmarshaller = context.createUnmarshaller();
      unmarshaller.setListener(streamListener);

      unmarshaller.setAdapter(new LocatedAttributeAdapter(reader));

      return unmarshaller.unmarshal(reader, MavenProject.class).getValue();
    } catch (JAXBException | XMLStreamException | IOException e) {
      LOG.error("Unable to parse pom file " + file.getPath(), e);
    }
    return null;
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
