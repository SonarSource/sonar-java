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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.indexer.SquidIndex;

import java.io.File;
import java.util.Collection;

public class DefaultJavaResourceLocator implements JavaResourceLocator {

  private static final Logger LOG = LoggerFactory.getLogger(JavaResourceLocator.class);

  private final Project project;
  private SquidIndex squidIndex;

  public DefaultJavaResourceLocator(Project project) {
    this.project = project;
  }

  public void setSquidIndex(SquidIndex squidIndex) {
    this.squidIndex = Preconditions.checkNotNull(squidIndex);
  }

  private SquidIndex getSquidIndex() {
    Preconditions.checkState(squidIndex != null, "SquidIndex can't be null");
    return squidIndex;
  }

  @Override
  public Resource findResourceByClassName(String className) {
    String name = className.replace('.', '/');
    SourceCode sourceCode = getSquidIndex().search(name);
    if (sourceCode == null) {
      LOG.debug("Class not found in SquidIndex: {}", className);
      return null;
    }
    Preconditions.checkState(sourceCode instanceof SourceClass, "Expected SourceClass, got %s for %s", sourceCode.getClass().getSimpleName(), name);
    String filePath = sourceCode.getParent(SourceFile.class).getName();
    return org.sonar.api.resources.File.fromIOFile(new File(filePath), project);
  }

  @Override
  public Collection<File> classFilesToAnalyze() {
    ImmutableList.Builder<File> result = ImmutableList.builder();
    Collection<SourceCode> sourceClasses = getSquidIndex().search(new QueryByType(SourceClass.class));

    for (SourceCode sourceClass : sourceClasses) {
      String filePath = sourceClass.getKey() + ".class";
      // TODO can be several build directories
      File classFile = new File(project.getFileSystem().getBuildOutputDir(), filePath);
      if (classFile.isFile()) {
        result.add(classFile);
      }
    }
    return result.build();
  }

}
