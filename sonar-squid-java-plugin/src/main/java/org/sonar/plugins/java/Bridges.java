/*
 * Sonar Java
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
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.java.JavaSquid;
import org.sonar.java.api.JavaClass;
import org.sonar.java.api.JavaMethod;
import org.sonar.plugins.java.bridges.Bridge;
import org.sonar.plugins.java.bridges.BridgeFactory;
import org.sonar.plugins.java.bridges.ResourceIndex;
import org.sonar.squid.api.*;
import org.sonar.squid.indexer.QueryByType;

import java.util.Collection;
import java.util.List;

public class Bridges {

  private final JavaSquid squid;

  public Bridges(JavaSquid squid) {
    this.squid = squid;
  }

  public void save(SensorContext context, Project project, CheckFactory checkFactory) {
    boolean skipPackageDesignAnalysis = project.getConfiguration().getBoolean(
        CoreProperties.DESIGN_SKIP_PACKAGE_DESIGN_PROPERTY,
        CoreProperties.DESIGN_SKIP_PACKAGE_DESIGN_DEFAULT_VALUE);

    ResourceIndex resourceIndex = new ResourceIndex().loadSquidResources(squid, context, project);
    List<Bridge> bridges = BridgeFactory.create(
        true /* TODO bytecodeScanned */,
        skipPackageDesignAnalysis,
        context,
        checkFactory,
        resourceIndex,
        squid, null /* TODO noSonarFilter */);
    saveProject(resourceIndex, bridges);
    savePackages(resourceIndex, bridges);
    saveFiles(resourceIndex, bridges);
    saveClasses(resourceIndex, bridges);
    saveMethods(resourceIndex, bridges);
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
      for (Bridge bridge : bridges) {
        bridge.onPackage((SourcePackage) squidPackage, sonarPackage);
      }
    }
  }

  private void saveFiles(ResourceIndex resourceIndex, List<Bridge> bridges) {
    Collection<SourceCode> squidFiles = squid.search(new QueryByType(SourceFile.class));
    for (SourceCode squidFile : squidFiles) {
      Resource sonarFile = resourceIndex.get(squidFile);
      for (Bridge bridge : bridges) {
        bridge.onFile((SourceFile) squidFile, sonarFile);
      }
    }
  }

  private void saveClasses(ResourceIndex resourceIndex, List<Bridge> bridges) {
    Collection<SourceCode> squidClasses = squid.search(new QueryByType(SourceClass.class));
    for (SourceCode squidClass : squidClasses) {
      Resource sonarClass = resourceIndex.get(squidClass);
      // can be null with anonymous classes
      if (sonarClass != null) {
        for (Bridge bridge : bridges) {
          bridge.onClass((SourceClass) squidClass, (JavaClass) sonarClass);
        }
      }
    }
  }

  private void saveMethods(ResourceIndex resourceIndex, List<Bridge> bridges) {
    Collection<SourceCode> squidMethods = squid.search(new QueryByType(SourceMethod.class));
    for (SourceCode squidMethod : squidMethods) {
      JavaMethod sonarMethod = (JavaMethod) resourceIndex.get(squidMethod);
      if (sonarMethod != null) {
        for (Bridge bridge : bridges) {
          bridge.onMethod((SourceMethod) squidMethod, sonarMethod);
        }
      }
    }
  }

}
