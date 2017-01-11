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
package org.sonar.java.bytecode;

import com.google.common.collect.Lists;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.bytecode.loader.SquidClassLoader;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ClassLoaderBuilder {

  private static final Logger LOG = Loggers.get(ClassLoaderBuilder.class);

  private ClassLoaderBuilder() {
    // only static methods
  }

  public static ClassLoader create(Collection<File> bytecodeFilesOrDirectories) {
    List<File> files = Lists.newArrayList();
    for (File file : bytecodeFilesOrDirectories) {
      if (file.isFile() && file.getPath().endsWith(".class")) {
        LOG.info("SonarQube Squid ClassLoader was expecting a JAR file instead of CLASS file : '" + file.getAbsolutePath() + "'");
      } else {
        files.add(file);
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("----- Classpath analyzed by Squid:");
      for (File file : files) {
        LOG.debug(file.getAbsolutePath());
      }
      LOG.debug("-----");
    }

    try {
      return new SquidClassLoader(files);
    } catch (Exception e) {
      throw new IllegalStateException("Can not create ClassLoader", e);
    }
  }

  /**
   * For tests.
   */
  public static ClassLoader create(File bytecodeFileOrDirectory) {
    return create(Arrays.asList(bytecodeFileOrDirectory));
  }

}
