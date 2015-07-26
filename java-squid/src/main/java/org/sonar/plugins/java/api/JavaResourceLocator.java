/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.plugins.java.api;

import com.google.common.annotations.Beta;
import org.sonar.api.BatchExtension;
import org.sonar.api.resources.Resource;
import org.sonar.java.bytecode.visitor.ResourceMapping;

import javax.annotation.CheckForNull;

import java.io.File;
import java.util.Collection;

@Beta
public interface JavaResourceLocator extends BatchExtension, JavaFileScanner {

  /**
   * @return null if not found
   */
  @CheckForNull
  Resource findResourceByClassName(String className);

  String findSourceFileKeyByClassName(String className);

  Collection<String> classKeys();

  Collection<File> classFilesToAnalyze();

  Collection<File> classpath();

  Integer getMethodStartLine(String fullyQualifiedMethodName);

  ResourceMapping getResourceMapping();
}
