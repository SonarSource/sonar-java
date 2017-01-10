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

import com.google.common.collect.Lists;
import org.assertj.core.api.Fail;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XmlLocationTest {

  @Test
  public void unknown_column_should_be_minus_one() {
    XmlLocation loc;
    int expectedValue = 1;

    loc = new XmlLocation(expectedValue, expectedValue);
    assertThat(loc.column()).isEqualTo(-1);
    assertThat(loc.line()).isEqualTo(expectedValue);
    assertThat(loc.offset()).isEqualTo(expectedValue);

    loc = new XmlLocation(expectedValue, expectedValue, expectedValue);
    assertThat(loc.column()).isEqualTo(expectedValue);
    assertThat(loc.line()).isEqualTo(expectedValue);
    assertThat(loc.offset()).isEqualTo(expectedValue);
  }

  @Test
  public void should_fail_when_providing_invalid_arguments() {
    int validValue = 1;
    int invalidValue = -1;

    List<int[]> checks = Lists.newArrayList(
      new int[] {validValue, invalidValue},
      new int[] {invalidValue, validValue},
      new int[] {validValue, validValue, invalidValue},
      new int[] {validValue, invalidValue, validValue},
      new int[] {invalidValue, validValue, validValue});

    for (int[] args : checks) {
      try {
        if (args.length == 2) {
          new XmlLocation(args[0], args[1]);
        } else {
          new XmlLocation(args[0], args[1], args[2]);
        }
        Fail.fail("should have failed");
      } catch (Exception e) {
        assertThat(e).isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Test
  public void test_to_string() {
    XmlLocation loc;
    loc = new XmlLocation(1, 2, 3);
    assertThat(loc.toString()).isEqualTo("(1,2)[3]");

    loc = new XmlLocation(1, 2);
    assertThat(loc.toString()).isEqualTo("(1,?)[2]");
  }

  @Test
  public void test_equals() {
    XmlLocation loc1 = new XmlLocation(1, 2, 3);
    Object loc2 = new XmlLocation(1, 2, 3);
    assertThat(loc1.equals(loc2)).isTrue();

    loc2 = new Object();
    assertThat(loc1.equals(loc2)).isFalse();

    loc2 = new XmlLocation(1, 3);
    assertThat(loc1.equals(loc2)).isFalse();

    loc2 = new XmlLocation(3, 2, 3);
    assertThat(loc1.equals(loc2)).isFalse();
  }

  @Test
  public void hashcode_is_relying_on_offset_only() {
    XmlLocation loc1 = new XmlLocation(1, 2, 3);
    XmlLocation loc2 = new XmlLocation(2, 1, 3);
    assertThat(loc1.hashCode()).isEqualTo(3);
    assertThat(loc1.hashCode()).isEqualTo(loc2.hashCode());
  }

  @Test
  public void test_get_location_from_xml_stream_location() {
    int line = 1;
    int column = 2;
    int offset = 3;
    XmlLocation loc = XmlLocation.getLocation(fakeXmlLocation(line, column, offset));
    assertThat(loc).isNotNull();
    assertThat(loc.line()).isEqualTo(line);
    assertThat(loc.column()).isEqualTo(column);
    assertThat(loc.offset()).isEqualTo(offset);
  }

  @Test
  public void start_location_should_be_correctly_calculated() throws Exception {
    String text = "HelloWorld";
    javax.xml.stream.Location endLocation = fakeXmlLocation(5, 20, 50);
    XmlLocation location = XmlLocation.getStartLocation(text, endLocation);
    assertThat(location).isNotNull();
    assertThat(location.line()).isEqualTo(endLocation.getLineNumber());
    assertThat(location.column()).isEqualTo(endLocation.getColumnNumber() - text.length());
    assertThat(location.offset()).isEqualTo(endLocation.getCharacterOffset() - text.length());
  }

  @Test
  public void start_location_should_take_new_lines_into_account() throws Exception {
    String text = "Hello\nWorld";
    javax.xml.stream.Location endLocation = fakeXmlLocation(5, 20, 50);
    XmlLocation location = XmlLocation.getStartLocation(text, endLocation);
    assertThat(location).isNotNull();
    assertThat(location.line()).isEqualTo(endLocation.getLineNumber() - 1);
    // unknown column
    assertThat(location.column()).isEqualTo(-1);
    assertThat(location.offset()).isEqualTo(endLocation.getCharacterOffset() - 11);
  }

  @Test
  public void start_location_should_take_white_characters_into_account() throws Exception {
    String text = "   HelloWorld       ";
    javax.xml.stream.Location endLocation = fakeXmlLocation(5, 35, 50);
    XmlLocation location = XmlLocation.getStartLocation(text, endLocation);
    assertThat(location).isNotNull();
    assertThat(location.line()).isEqualTo(endLocation.getLineNumber());
    assertThat(location.column()).isEqualTo(15);
    assertThat(location.offset()).isEqualTo(30);
  }

  protected static javax.xml.stream.Location fakeXmlLocation(int line, int column, int offset) {
    javax.xml.stream.Location loc = mock(javax.xml.stream.Location.class);
    when(loc.getLineNumber()).thenReturn(line);
    when(loc.getColumnNumber()).thenReturn(column);
    when(loc.getCharacterOffset()).thenReturn(offset);
    return loc;
  }
}
