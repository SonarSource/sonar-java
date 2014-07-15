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
package org.sonar.plugins.java.bridges;

import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.resources.Resource;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceMethod;

import java.util.HashSet;
import java.util.Set;

public class NoSonarFilterLoader extends Bridge {

  private NoSonarFilter noSonarFilter;

  protected NoSonarFilterLoader(NoSonarFilter noSonarFilter) {
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void onFile(SourceFile squidFile, Resource sonarFile) {
    Set<Integer> ignoredLines = new HashSet<Integer>(squidFile.getNoSonarTagLines());
    visitSuppressWarnings(squidFile, ignoredLines);
    noSonarFilter.addResource(sonarFile, ignoredLines);
  }

  private static void visitSuppressWarnings(SourceCode sourceCode, Set<Integer> ignoredLines) {
    if ((sourceCode instanceof SourceClass && ((SourceClass) sourceCode).isSuppressWarnings())
      || (sourceCode instanceof SourceMethod && ((SourceMethod) sourceCode).isSuppressWarnings())) {
      visitLines(sourceCode, ignoredLines);
    }

    if (sourceCode.hasChildren()) {
      for (SourceCode child : sourceCode.getChildren()) {
        visitSuppressWarnings(child, ignoredLines);
      }
    }
  }

  private static void visitLines(SourceCode sourceCode, Set<Integer> ignoredLines) {
    for (int line = sourceCode.getStartAtLine(); line <= sourceCode.getEndAtLine(); line++) {
      ignoredLines.add(line);
    }
  }

}
