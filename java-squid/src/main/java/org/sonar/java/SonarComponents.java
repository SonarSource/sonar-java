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
package org.sonar.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Symbolizable;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannersFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

public class SonarComponents implements BatchExtension {

  private final FileLinesContextFactory fileLinesContextFactory;
  private final ResourcePerspectives resourcePerspectives;
  private final JavaFileScannersFactory[] fileScannersFactories;
  private final ProjectClasspath projectClasspath;
  private final Project project;

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, ResourcePerspectives resourcePerspectives, Project project, ProjectClasspath projectClasspath) {
    this(fileLinesContextFactory, resourcePerspectives, project, projectClasspath, null);
  }

  public SonarComponents(FileLinesContextFactory fileLinesContextFactory, ResourcePerspectives resourcePerspectives, Project project,
                         ProjectClasspath projectClasspath,
                         @Nullable JavaFileScannersFactory[] fileScannersFactories) {
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.resourcePerspectives = resourcePerspectives;
    this.project = project;
    this.projectClasspath = projectClasspath;
    this.fileScannersFactories = fileScannersFactories;
  }

  public Iterable<JavaFileScanner> createJavaFileScanners() {
    Iterable<JavaFileScanner> result = ImmutableList.of();
    if (fileScannersFactories != null) {
      for (JavaFileScannersFactory factory : fileScannersFactories) {
        result = Iterables.concat(result, factory.createJavaFileScanners());
      }
    }
    return result;
  }

  public Resource resourceFromIOFile(File file) {
    return org.sonar.api.resources.File.fromIOFile(file, project);
  }

  public FileLinesContext fileLinesContextFor(File file) {
    return fileLinesContextFactory.createFor(resourceFromIOFile(file));
  }

  public Symbolizable symbolizableFor(File file) {
    return resourcePerspectives.as(Symbolizable.class, resourceFromIOFile(file));
  }

  public Highlightable highlightableFor(File file) {
    return resourcePerspectives.as(Highlightable.class, resourceFromIOFile(file));
  }

  public List<File> getProjectClasspath() {
    if (projectClasspath == null) {
      return Lists.newArrayList();
    }
    return projectClasspath.getElements();
  }
}
