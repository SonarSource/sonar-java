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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.java.SonarComponents;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.Nullable;

import java.io.File;

public class MavenFileScannerContextImpl implements MavenFileScannerContext {

  private final MavenProject project;
  private final File file;
  @VisibleForTesting
  public final SourceFile sourceFile;
  private final SonarComponents sonarComponents;

  public MavenFileScannerContextImpl(MavenProject project, SourceFile sourceFile, File file, @Nullable SonarComponents sonarComponents) {
    this.project = project;
    this.sourceFile = sourceFile;
    this.file = file;
    this.sonarComponents = sonarComponents;
  }

  @Override
  public MavenProject getMavenProject() {
    return project;
  }

  @Override
  public void reportIssueOnFile(MavenCheck check, String message) {
    if (sonarComponents != null) {
      sonarComponents.addIssue(file, check, -1, message, null);
    }
  }

  @Override
  public void reportIssue(MavenCheck check, LocatedTree tree, String message) {
    if (sonarComponents != null) {
      sonarComponents.addIssue(file, check, tree.startLocation().line(), message, null);
    }
  }

  @Override
  public void reportIssue(MavenCheck check, int line, String message) {
    if (sonarComponents != null) {
      sonarComponents.addIssue(file, check, line, message, null);
    }
  }
}
