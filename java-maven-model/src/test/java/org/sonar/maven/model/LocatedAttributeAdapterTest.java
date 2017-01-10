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

import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Test;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocatedAttributeAdapterTest {
  private LocatedAttributeAdapter adapter;

  @Before
  public void setup() {
    XMLStreamReader reader = mock(XMLStreamReader.class);
    Location fakeXmlLocation = XmlLocationTest.fakeXmlLocation(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    when(reader.getLocation()).thenReturn(fakeXmlLocation);
    adapter = new LocatedAttributeAdapter(reader);
  }

  @Test
  public void unmarshal_operation_should_provide_location() {
    try {
      String value = "HelloWorld";
      LocatedAttribute attribute = adapter.unmarshal(value);
      assertThat(attribute).isNotNull();
      assertThat(attribute.startLocation).isNotNull();
      assertThat(attribute.endLocation).isNotNull();
    } catch (Exception e) {
      Fail.fail("should never happen", e);
    }
  }

  @Test
  public void marshal_operation_should_get_value_back() {
    try {
      String value = "HelloWorld";
      LocatedAttribute attribute = new LocatedAttribute(value);
      assertThat(adapter.marshal(attribute)).isEqualTo(value);
    } catch (Exception e) {
      Fail.fail("should never happen", e);
    }
  }
}
