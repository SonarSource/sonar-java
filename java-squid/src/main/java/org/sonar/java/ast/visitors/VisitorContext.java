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
package org.sonar.java.ast.visitors;

import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;

import java.io.File;
import java.util.Deque;
import java.util.LinkedList;

public class VisitorContext {

  private final Deque<SourceCode> sourceCodeStack = new LinkedList<>();
  private final SourceProject project;
  private File file;

  public VisitorContext(SourceProject project) {
    if (project == null) {
      throw new IllegalArgumentException("project cannot be null.");
    }
    this.project = project;
    sourceCodeStack.push(project);
  }

  private void addSourceCode(SourceCode child) {
    peekSourceCode().addChild(child);
    sourceCodeStack.push(child);
  }

  public SourceCode peekSourceCode() {
    return sourceCodeStack.peek();
  }

  public void setFile(File file) {
    popTillSourceProject();
    addSourceCode(new SourceFile(file.getAbsolutePath(), file.getPath()));
    this.file = file;
  }

  private void popTillSourceProject() {
    while (!(peekSourceCode().equals(project))) {
      sourceCodeStack.pop();
    }
  }

  public File getFile() {
    return file;
  }
}
