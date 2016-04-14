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
package org.sonar.java.filters;

import com.google.common.collect.ImmutableList;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;

public class PostAnalysisIssueFilter implements IssueFilter, JavaFileScanner {

  private final FileSystem fs;
  private SensorContext sensorContext;
  private static final Iterable<JavaIssueFilter> ISSUE_FILTERS = ImmutableList.<JavaIssueFilter>of(
    new EclipseI18NFilter());

  public PostAnalysisIssueFilter(FileSystem fs) {
    this.fs = fs;
  }

  public void setSensorContext(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
  }

  @Override
  public boolean accept(Issue issue, IssueFilterChain chain) {
    for (JavaIssueFilter javaIssueFilter : ISSUE_FILTERS) {
      if (!javaIssueFilter.accept(issue)) {
        return false;
      }
    }
    return chain.accept(issue);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    InputFile inputFile = fs.inputFile(fs.predicates().is(context.getFile()));
    org.sonar.api.resources.File currentResource = (org.sonar.api.resources.File) sensorContext.getResource(inputFile);

    String componentKey = currentResource.getEffectiveKey();
    for (JavaIssueFilter javaIssueFilter : ISSUE_FILTERS) {
      javaIssueFilter.setComponentKey(componentKey);
      javaIssueFilter.scanFile(context);
    }
  }

}
