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
package org.sonar.java;

public class JavaCheckMessage extends org.sonar.squidbridge.api.CheckMessage {

  private AnalyzerMessage analyzerMessage;

  public JavaCheckMessage(Object check, String message, Object... messageArguments) {
    super(check, message, messageArguments);
  }

  public AnalyzerMessage getAnalyzerMessage() {
    return analyzerMessage;
  }

  public void setAnalyzerMessage(AnalyzerMessage analyzerMessage) {
    this.analyzerMessage = analyzerMessage;
  }
}
