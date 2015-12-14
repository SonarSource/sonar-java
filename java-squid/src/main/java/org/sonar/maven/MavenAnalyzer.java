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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.SonarComponents;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.squidbridge.ProgressReport;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MavenAnalyzer {

  private static final Logger LOG = LoggerFactory.getLogger(MavenAnalyzer.class);
  private final SonarComponents sonarComponents;
  private final List<MavenFileScanner> scanners;

  public MavenAnalyzer(SonarComponents sonarComponents, CodeVisitor... visitors) {
    ImmutableList.Builder<MavenFileScanner> scannersBuilder = ImmutableList.builder();
    for (CodeVisitor visitor : visitors) {
      if (visitor instanceof MavenFileScanner) {
        scannersBuilder.add((MavenFileScanner) visitor);
      }
    }
    this.scanners = scannersBuilder.build();
    this.sonarComponents = sonarComponents;
  }

  public void scan(Iterable<File> files) {
    boolean hasMavenFileScanners = !scanners.isEmpty();
    boolean hasPomFile = !Iterables.isEmpty(files);
    if (hasMavenFileScanners && !hasPomFile) {
      LOG.warn("No 'pom.xml' file have been indexed.");
      return;
    }

    ProgressReport progressReport = new ProgressReport("Report about progress of Maven Pom analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(Lists.newArrayList(files));

    boolean successfulyCompleted = false;
    try {
      for (File file : files) {
        simpleScan(file);
        progressReport.nextFile();
      }
      successfulyCompleted = true;
    } finally {
      if (successfulyCompleted) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
    }

  }

  private void simpleScan(File file) {
    MavenProject project = MavenParser.parseXML(file);
    if (project != null) {
      MavenFileScannerContext scannerContext = new MavenFileScannerContextImpl(project, file, sonarComponents);
      for (MavenFileScanner mavenFileScanner : scanners) {
        mavenFileScanner.scanFile(scannerContext);
      }
    }
  }
}
