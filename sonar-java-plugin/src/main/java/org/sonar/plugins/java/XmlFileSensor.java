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
package org.sonar.plugins.java;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.CheckList;
import org.sonar.java.xml.XmlAnalyzer;

import java.io.File;

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
    descriptor.name(this.toString());
  }

  @Override
  public void execute(SensorContext context) {
    if (hasXmlFiles()) {
      sonarComponents.registerCheckClasses(CheckList.REPOSITORY_KEY, CheckList.getXmlChecks());
      sonarComponents.setSensorContext(context);
      new XmlAnalyzer(sonarComponents, sonarComponents.checkClasses()).scan(getXmlFiles());
    }
  }

  private boolean hasXmlFiles() {
    return fs.hasFiles(xmlFilePredicate);
  }

  private Iterable<File> getXmlFiles() {
    return fs.files(xmlFilePredicate);
  }

  @Override
  public String toString() {
    return XmlFileSensor.class.getSimpleName();
  }
}
