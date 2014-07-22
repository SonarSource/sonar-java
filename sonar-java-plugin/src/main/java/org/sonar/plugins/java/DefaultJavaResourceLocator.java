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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.indexer.SquidIndex;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class DefaultJavaResourceLocator implements JavaResourceLocator {

  private static final Logger LOG = LoggerFactory.getLogger(JavaResourceLocator.class);

  private final Project project;
  private final ModuleFileSystem fileSystem;
  private Map<String, Resource> resourcesCache;

  public DefaultJavaResourceLocator(Project project, ModuleFileSystem fileSystem) {
    this.project = project;
    this.fileSystem = fileSystem;
  }

  public void setSquidIndex(SquidIndex squidIndex) {
    this.resourcesCache = Maps.newHashMap();
    for (SourceCode sourceClass : squidIndex.search(new QueryByType(SourceClass.class))) {
      String filePath = sourceClass.getParent(SourceFile.class).getName();
      Resource resource = org.sonar.api.resources.File.fromIOFile(new File(filePath), project);
      resourcesCache.put(sourceClass.getKey(), resource);
    }
  }

  @Override
  public Resource findResourceByClassName(String className) {
    String name = className.replace('.', '/');
    Resource resource = resourcesCache.get(name);
    if (resource == null) {
      LOG.debug("Class not found in SquidIndex: {}", className);
    }
    return resource;
  }

  @Override
  public Collection<File> classFilesToAnalyze() {
    ImmutableList.Builder<File> result = ImmutableList.builder();
    for (String key : resourcesCache.keySet()) {
      String filePath = key + ".class";
      for (File binaryDir : fileSystem.binaryDirs()) {
        File classFile = new File(binaryDir, filePath);
        if (classFile.isFile()) {
          result.add(classFile);
          break;
        }
      }
    }
    return result.build();
  }

}
