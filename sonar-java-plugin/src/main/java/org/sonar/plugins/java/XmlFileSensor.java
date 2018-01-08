/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.plugins.java;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.CheckList;
import org.sonar.java.xml.XmlAnalyzer;

public class XmlFileSensor implements Sensor {

  private final FileSystem fs;
  private final FilePredicate xmlFilePredicate;
  private final SonarComponents sonarComponents;

  public XmlFileSensor(SonarComponents sonarComponents, FileSystem fs) {
    this.fs = fs;
    this.xmlFilePredicate = fs.predicates().matchesPathPattern("**/*.xml");
    this.sonarComponents = sonarComponents;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("SonarJavaXmlFileSensor");
  }

  @Override
  public void execute(SensorContext context) {
    if (hasXmlFiles()) {
      Iterable<InputFile> xmlInputFiles = getXmlFiles();
      // make xml files visible in SQ UI, when XML plugin is not installed
      xmlInputFiles.forEach(context::markForPublishing);
      sonarComponents.registerCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getXmlChecks());
      sonarComponents.setSensorContext(context);
      new XmlAnalyzer(sonarComponents, sonarComponents.checkClasses()).scan(toFile(xmlInputFiles));
    }
  }

  private boolean hasXmlFiles() {
    return fs.hasFiles(xmlFilePredicate);
  }

  private Iterable<InputFile> getXmlFiles() {
    return fs.inputFiles(xmlFilePredicate);
  }

  private static Iterable<File> toFile(Iterable<InputFile> inputFiles) {
    return StreamSupport.stream(inputFiles.spliterator(), false).map(InputFile::file).collect(Collectors.toList());
  }

}
