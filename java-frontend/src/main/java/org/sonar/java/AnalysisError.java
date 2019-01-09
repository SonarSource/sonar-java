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
package org.sonar.java;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AnalysisError {

  public enum Kind {
    PARSE_ERROR,
    SEMANTIC_ERROR,
    CHECK_ERROR,
    SE_ERROR,
  }

  private final String message;
  private final String cause;
  private final String filename;
  private final Kind kind;

  public AnalysisError(Exception exception, String filename, Kind kind) {
    this.message = exception.getMessage() == null ? "" : exception.getMessage();
    StringWriter sw = new StringWriter();
    exception.printStackTrace(new PrintWriter(sw));
    this.cause = sw.toString();
    this.filename = filename;
    this.kind = kind;
  }

  public String getMessage() {
    return message;
  }

  public String getCause() {
    return cause;
  }

  public String getFilename() {
    return filename;
  }

  public Kind getKind() {
    return kind;
  }

  public int serializedSize() {
    return message.length()+cause.length()+filename.length()+ kind.name().length();
  }
}
