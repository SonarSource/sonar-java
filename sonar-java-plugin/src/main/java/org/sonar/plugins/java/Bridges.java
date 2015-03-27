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
package org.sonar.plugins.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.java.JavaSquid;
import org.sonar.java.SonarComponents;
import org.sonar.java.bytecode.visitor.ResourceMapping;
import org.sonar.plugins.java.bridges.ChecksBridge;
import org.sonar.plugins.java.bridges.DesignBridge;
import org.sonar.squidbridge.api.SourceFile;

public class Bridges {

  private static final Logger LOG = LoggerFactory.getLogger(Bridges.class);
  private final JavaSquid squid;
  private final Settings settings;


  public Bridges(JavaSquid squid, Settings settings) {
    this.squid = squid;
    this.settings = settings;
  }

  public void save(SensorContext context, Project project, SonarComponents sonarComponents, ResourceMapping resourceMapping,
    NoSonarFilter noSonarFilter, RulesProfile rulesProfile) {
    boolean skipPackageDesignAnalysis = settings.getBoolean(CoreProperties.DESIGN_SKIP_PACKAGE_DESIGN_PROPERTY);
    //Design
    if (!skipPackageDesignAnalysis && squid.isBytecodeScanned()) {
      DesignBridge designBridge = new DesignBridge(context, squid.getGraph(), resourceMapping, sonarComponents.getResourcePerspectives());
      designBridge.saveDesign(project);
    }
    //Report Issues
    ChecksBridge checksBridge = new ChecksBridge(sonarComponents, rulesProfile);
    reportIssues(resourceMapping, noSonarFilter, checksBridge, project);
  }

  private void reportIssues(ResourceMapping resourceMapping, NoSonarFilter noSonarFilter, ChecksBridge checksBridge, Project project) {
    for (Resource directory : resourceMapping.directories()) {
      checksBridge.reportIssueForPackageInfo((Directory) directory, project);
      for (Resource sonarFile : resourceMapping.files((Directory) directory)) {
        String key = resourceMapping.getFileKeyByResource((org.sonar.api.resources.File) sonarFile);
        //key would be null for test files as they are not in squid index.
        if(key != null) {
          SourceFile squidFile = (SourceFile) squid.search(key);
          if (squidFile != null) {
            noSonarFilter.addResource(sonarFile, squidFile.getNoSonarTagLines());
            checksBridge.reportIssues(squidFile, sonarFile);
          } else {
            LOG.error("Could not report issue on file: " + sonarFile.getKey());
          }
        }
      }
    }
  }

}
