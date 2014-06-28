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

/**
 * Exception thrown when configuring Findbugs, and there are non classes to analyse.
 * This can happen then there are package-info.java files, that should be analysed by source code analysis tools,
 * but that does not generate any bytocode for Findbugs to analyze.
 */
public class AnalysisNotNeededException extends Exception {

  public AnalysisNotNeededException() {
  }

  public AnalysisNotNeededException(String message) {
    super(message);
  }

  public AnalysisNotNeededException(String message, Throwable cause) {
    super(message, cause);
  }

  public AnalysisNotNeededException(Throwable cause) {
    super(cause);
  }

}
