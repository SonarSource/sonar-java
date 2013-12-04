/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.plugins.findbugs;

import edu.umd.cs.findbugs.BugInstance;

public class ReportedBug {

  private final String type;
  private final String message;
  private final String className;
  private final int startLine;

  public ReportedBug(BugInstance bugInstance) {
    this.type = bugInstance.getType();
    this.message = bugInstance.getMessageWithoutPrefix();
    this.className = bugInstance.getPrimarySourceLineAnnotation().getClassName();
    this.startLine = bugInstance.getPrimarySourceLineAnnotation().getStartLine();
  }

  public String getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public String getClassName() {
    return className;
  }

  public int getStartLine() {
    return startLine;
  }

}
