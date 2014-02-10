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
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.indexer.SquidIndex;

import java.io.File;

public class DefaultJavaResourceLocator implements JavaResourceLocator {

  private final Project project;
  private SquidIndex squidIndex;

  public DefaultJavaResourceLocator(Project project) {
    this.project = project;
  }

  public void setSquidIndex(SquidIndex squidIndex) {
    this.squidIndex = Preconditions.checkNotNull(squidIndex);
  }

  @Override
  public Resource findResourceByClassName(String name) {
    Preconditions.checkState(squidIndex != null);
    name = name.replace('.', '/');
    SourceCode sourceCode = squidIndex.search(name);
    Preconditions.checkState(sourceCode instanceof SourceClass);
    String filePath = sourceCode.getParent(SourceFile.class).getName();
    return org.sonar.api.resources.File.fromIOFile(new File(filePath), project);
  }

}
