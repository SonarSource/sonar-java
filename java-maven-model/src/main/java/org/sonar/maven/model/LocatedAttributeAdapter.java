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
package org.sonar.maven.model;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.stream.XMLStreamReader;

/**
 * Adapter in charge of converting attributes from XML object, being String, to located attribute,
 * storing information related to the location of the attribute.
 */
public class LocatedAttributeAdapter extends XmlAdapter<String, LocatedAttribute> {

  private final XMLStreamReader reader;

  public LocatedAttributeAdapter(XMLStreamReader reader) {
    this.reader = reader;
  }

  @Override
  public LocatedAttribute unmarshal(String value) throws Exception {
    LocatedAttribute result = new LocatedAttribute(value);
    result.setEndLocation(XmlLocation.getLocation(reader.getLocation()));
    result.setStartLocation(XmlLocation.getStartLocation(value, reader.getLocation()));
    return result;
  }

  @Override
  public String marshal(LocatedAttribute attribute) throws Exception {
    return attribute.getValue();
  }
}
