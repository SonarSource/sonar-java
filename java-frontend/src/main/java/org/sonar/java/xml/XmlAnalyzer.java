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
package org.sonar.java.xml;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.SonarComponents;
import org.sonar.java.xml.maven.PomCheck;
import org.sonar.java.xml.maven.PomCheckContext;
import org.sonar.java.xml.maven.PomCheckContextImpl;
import org.sonar.java.xml.maven.PomParser;
import org.sonar.maven.model.maven2.MavenProject;
import org.sonar.squidbridge.ProgressReport;
import org.sonar.squidbridge.api.CodeVisitor;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class XmlAnalyzer {

  private static final Logger LOG = Loggers.get(XmlAnalyzer.class);
  private final SonarComponents sonarComponents;
  private final List<XmlCheck> xmlChecks;
  private final List<PomCheck> pomChecks;
  private final XPath xPath;

  public XmlAnalyzer(SonarComponents sonarComponents, CodeVisitor... visitors) {
    List<XmlCheck> xmlChecksBuilder = new ArrayList<>();
    List<PomCheck> pomChecksBuilder = new ArrayList<>();
    for (CodeVisitor visitor : visitors) {
      if (visitor instanceof XmlCheck) {
        xmlChecksBuilder.add((XmlCheck) visitor);
      } else if (visitor instanceof PomCheck) {
        pomChecksBuilder.add((PomCheck) visitor);
      }
    }
    this.xmlChecks = Collections.unmodifiableList(xmlChecksBuilder);
    this.pomChecks = Collections.unmodifiableList(pomChecksBuilder);
    this.sonarComponents = sonarComponents;
    this.xPath = XPathFactory.newInstance().newXPath();
  }

  public void scan(Iterable<File> files) {
    boolean hasChecks = !xmlChecks.isEmpty() || !pomChecks.isEmpty();
    List<File> fileForProgressReport = new ArrayList<>();
    files.forEach(fileForProgressReport::add);
    if (hasChecks && fileForProgressReport.isEmpty()) {
      LOG.warn("No 'xml' file have been indexed.");
      return;
    }

    ProgressReport progressReport = new ProgressReport("Report about progress of Xml analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(fileForProgressReport);

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
    Document document = XmlParser.parseXML(file);
    if (document != null) {
      simpleScanAsXmlFile(file, document);
      if ("pom.xml".equals(file.getName())) {
        simpleScanAsPomFile(file, document);
      }
    }
  }

  private void simpleScanAsXmlFile(File file, Document document) {
    XmlCheckContext context = new XmlCheckContextImpl(document, file, xPath, sonarComponents);
    for (XmlCheck check : xmlChecks) {
      check.scanFile(context);
    }
  }

  private void simpleScanAsPomFile(File file, Document document) {
    MavenProject project = PomParser.parseXML(file);
    if (project != null) {
      PomCheckContext context = new PomCheckContextImpl(project, document, file, xPath, sonarComponents);
      for (PomCheck check : pomChecks) {
        check.scanFile(context);
      }
    }
  }
}
