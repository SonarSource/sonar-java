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
package org.sonar.java.xml.maven;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.SonarComponents;
import org.sonar.java.xml.XmlCheckContextImpl;
import org.sonar.maven.model.LocatedTree;
import org.sonar.maven.model.XmlLocation;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.plugins.java.api.JavaCheck;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class PomCheckContextImpl extends XmlCheckContextImpl implements PomCheckContext {

  private final MavenProject project;

  public PomCheckContextImpl(MavenProject project, Document document, File file, XPath xPath, SonarComponents sonarComponents) {
    super(document, file, xPath, sonarComponents);
    this.project = project;
  }

  @Override
  public MavenProject getMavenProject() {
    return project;
  }

  @Override
  public void reportIssue(PomCheck check, LocatedTree tree, String message) {
    getSonarComponents().addIssue(getFile(), check, tree.startLocation().line(), message, null);
  }

  @Override
  public void reportIssue(PomCheck check, int line, String message, List<Location> secondary) {
    File file = getFile();
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(check, file, line, message, 0);
    for (Location location : secondary) {
      AnalyzerMessage secondaryLocation = getSecondaryAnalyzerMessage(check, file, location);
      analyzerMessage.flows.add(Collections.singletonList(secondaryLocation));
    }
    getSonarComponents().reportIssue(analyzerMessage);
  }

  @VisibleForTesting
  static AnalyzerMessage getSecondaryAnalyzerMessage(JavaCheck check, File file, Location location) {
    XmlLocation startLocation = location.tree.startLocation();
    XmlLocation endLocation = location.tree.endLocation();
    TextSpan ts = new TextSpan(
      startLocation.line(), offsetFromColumn(startLocation.column()),
      endLocation.line(), offsetFromColumn(endLocation.column()));
    return new AnalyzerMessage(check, file, ts, location.msg, 0);
  }

  private static int offsetFromColumn(int column) {
    // unknown column are marked as -1,
    // first column is equivalent to an offset of 0
    return column > 0 ? (column - 1) : 0;
  }
}
