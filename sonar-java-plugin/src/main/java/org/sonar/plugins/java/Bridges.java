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
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.java.JavaSquid;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.plugins.java.bridges.Bridge;
import org.sonar.plugins.java.bridges.BridgeFactory;
import org.sonar.plugins.java.bridges.ResourceIndex;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourcePackage;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.QueryByMeasure;
import org.sonar.squidbridge.indexer.QueryByType;

import java.util.Collection;
import java.util.List;

public class Bridges {

  private final JavaSquid squid;
  private final Settings settings;

  public Bridges(JavaSquid squid, Settings settings) {
    this.squid = squid;
    this.settings = settings;
  }

  public void save(SensorContext context, Project project, CheckFactory checkFactory, NoSonarFilter noSonarFilter, RulesProfile profile) {
    boolean skipPackageDesignAnalysis = settings.getBoolean(CoreProperties.DESIGN_SKIP_PACKAGE_DESIGN_PROPERTY);
    ResourceIndex resourceIndex = new ResourceIndex(skipPackageDesignAnalysis).loadSquidResources(squid, context, project);
    List<Bridge> bridges = BridgeFactory.create(
        squid.isBytecodeScanned(),
        skipPackageDesignAnalysis,
        context,
        checkFactory,
        resourceIndex,
        squid,
        noSonarFilter,
        profile);
    saveProject(resourceIndex, bridges);
    savePackages(resourceIndex, bridges);
    saveFiles(resourceIndex, bridges);
  }

  private void saveProject(ResourceIndex resourceIndex, List<Bridge> bridges) {
    SourceProject squidProject = (SourceProject) squid.search(new QueryByType(SourceProject.class)).iterator().next();
    Resource sonarResource = resourceIndex.get(squidProject);
    for (Bridge bridge : bridges) {
      bridge.onProject(squidProject, (Project) sonarResource);
    }
  }

  private void savePackages(ResourceIndex resourceIndex, List<Bridge> bridges) {
    Collection<SourceCode> packages = squid.search(new QueryByType(SourcePackage.class));
    for (SourceCode squidPackage : packages) {
      Resource sonarPackage = resourceIndex.get(squidPackage);
      if (sonarPackage != null) {
        for (Bridge bridge : bridges) {
          bridge.onPackage((SourcePackage) squidPackage, sonarPackage);
        }
      }
    }
  }

  private void saveFiles(ResourceIndex resourceIndex, List<Bridge> bridges) {
    Collection<SourceCode> squidFiles = squid.search(
      new QueryByType(SourceFile.class),
      /* excluding test files: */ new QueryByMeasure(JavaMetric.TESTS, QueryByMeasure.Operator.LESS_THAN, 1));
    for (SourceCode squidFile : squidFiles) {
      Resource sonarFile = resourceIndex.get(squidFile);
      if (sonarFile != null) {
        for (Bridge bridge : bridges) {
          bridge.onFile((SourceFile) squidFile, sonarFile);
        }
      }
    }
  }

}
