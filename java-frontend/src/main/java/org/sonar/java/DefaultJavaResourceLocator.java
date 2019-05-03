/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;

public class DefaultJavaResourceLocator implements JavaResourceLocator {

  private static final Logger LOG = Loggers.get(DefaultJavaResourceLocator.class);

  private final JavaClasspath javaClasspath;
  @VisibleForTesting
  Map<String, InputFile> resourcesByClass;

  public DefaultJavaResourceLocator(JavaClasspath javaClasspath) {
    this.javaClasspath = javaClasspath;
    resourcesByClass = new HashMap<>();
  }

  @Override
  public InputFile findResourceByClassName(String className) {
    String name = className.replace('.', '/');
    InputFile inputFile = resourcesByClass.get(name);
    if (inputFile == null) {
      LOG.debug("Class not found in resource cache : {}", className);
    }
    return inputFile;
  }

  private Collection<String> classKeys() {
    return ImmutableSortedSet.<String>naturalOrder().addAll(resourcesByClass.keySet()).build();
  }

  @Override
  public Collection<File> classFilesToAnalyze() {
    ImmutableList.Builder<File> result = ImmutableList.builder();
    for (String key : classKeys()) {
      String filePath = key + ".class";
      for (File binaryDir : javaClasspath.getBinaryDirs()) {
        File classFile = new File(binaryDir, filePath);
        if (classFile.isFile()) {
          result.add(classFile);
          break;
        }
      }
    }
    return result.build();
  }

  @Override
  public Collection<File> classpath() {
    return javaClasspath.getElements();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    InputFile inputFile = context.getInputFile();
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    javaFilesCache.scanFile(context);
    javaFilesCache.getClassNames().forEach(className -> resourcesByClass.put(className, inputFile));
  }
}
