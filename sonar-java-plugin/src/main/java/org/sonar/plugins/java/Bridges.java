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

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.CheckFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.java.JavaSquid;
import org.sonar.java.bytecode.visitor.DSMMapping;
import org.sonar.plugins.java.bridges.ChecksBridge;
import org.sonar.plugins.java.bridges.DesignBridge;
import org.sonar.plugins.java.bridges.ResourceIndex;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;

import java.util.Collection;

public class Bridges {

  private final JavaSquid squid;
  private final Settings settings;

  public Bridges(JavaSquid squid, Settings settings) {
    this.squid = squid;
    this.settings = settings;
  }

  public void save(SensorContext context, Project project, CheckFactory checkFactory, RulesProfile profile, DSMMapping dsmMapping) {
    boolean skipPackageDesignAnalysis = settings.getBoolean(CoreProperties.DESIGN_SKIP_PACKAGE_DESIGN_PROPERTY);
    //Design
    if(!skipPackageDesignAnalysis && squid.isBytecodeScanned()) {
      DesignBridge designBridge = new DesignBridge(context, squid.getGraph(), dsmMapping, checkFactory);
      designBridge.saveDesign(project);
    }
    //Report Issues
    ResourceIndex resourceIndex = new ResourceIndex(skipPackageDesignAnalysis).loadSquidResources(squid, context, project);
    ChecksBridge checksBridge = new ChecksBridge(context, profile, checkFactory);
    reportIssues(resourceIndex, checksBridge);
  }

  private void reportIssues(ResourceIndex resourceIndex, ChecksBridge checksBridge) {
    Collection<SourceCode> squidFiles = squid.search(new QueryByType(SourceFile.class));
    for (SourceCode squidFile : squidFiles) {
      Resource sonarFile = resourceIndex.get(squidFile);
      if (sonarFile != null) {
        checksBridge.reportIssues((SourceFile) squidFile, sonarFile);
      }
    }
  }

}
