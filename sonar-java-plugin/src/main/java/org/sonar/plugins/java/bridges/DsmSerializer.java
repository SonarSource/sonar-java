/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.java.bridges;

import org.sonar.api.design.Dependency;
import org.sonar.api.resources.Resource;
import org.sonar.graph.Dsm;
import org.sonar.graph.DsmCell;

public final class DsmSerializer {

  private Dsm dsm;
  private StringBuilder json;

  private DsmSerializer(Dsm<Resource> dsm) {
    this.dsm = dsm;
    this.json = new StringBuilder();
  }

  private String serialize() {
    json.append('[');
    serializeRows();
    json.append(']');
    return json.toString();
  }

  private void serializeRows() {
    for (int y = 0; y < dsm.getDimension(); y++) {
      if (y > 0) {
        json.append(',');
      }
      serializeRow(y);
    }
  }

  private void serializeRow(int y) {
    Resource sonarResource = (Resource) dsm.getVertex(y);

    json.append("{");
    if (sonarResource != null) {
      json.append("\"i\":");
      json.append(sonarResource.getId());
      json.append(",\"n\":\"");
      json.append(sonarResource.getName());
      json.append("\",\"q\":\"");
      json.append(sonarResource.getQualifier());
      json.append("\",\"v\":[");
      for (int x = 0; x < dsm.getDimension(); x++) {
        if (x > 0) {
          json.append(',');
        }
        serializeCell(y, x);
      }
      json.append("]");
    }
    json.append("}");
  }

  private void serializeCell(int y, int x) {
    DsmCell cell = dsm.getCell(x, y);
    json.append('{');
    if (cell.getEdge() != null && cell.getWeight() > 0) {
      json.append("\"i\":");
      json.append(((Dependency) cell.getEdge()).getId());
      json.append(",\"w\":");
      json.append(cell.getWeight());
    }
    json.append('}');
  }

  public static String serialize(Dsm<Resource> dsm) {
    return new DsmSerializer(dsm).serialize();
  }
}
