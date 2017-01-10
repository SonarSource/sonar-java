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
package org.sonar.java.bytecode.loader;

import com.google.common.collect.Iterators;
import org.apache.commons.lang.ArrayUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Class loader, which is able to load classes from a list of JAR files and directories.
 */
public class SquidClassLoader extends ClassLoader implements Closeable {

  private final List<Loader> loaders;

  /**
   * @param files ordered list of files and directories from which to load classes and resources
   */
  public SquidClassLoader(List<File> files) {
    super(null);
    loaders = new ArrayList<>();
    for (File file : files) {
      if (file.exists()) {
        if (file.isDirectory()) {
          loaders.add(new FileSystemLoader(file));
        } else if (file.getName().endsWith(".jar")) {
          loaders.add(new JarLoader(file));
        } else if (file.getName().endsWith(".aar")) {
          loaders.add(new AarLoader(file));
        }
      }
    }
  }

  @Override
  protected Class findClass(String name) throws ClassNotFoundException {
    String resourceName = name.replace('.', '/') + ".class";
    for (Loader loader : loaders) {
      byte[] classBytes = loader.loadBytes(resourceName);
      if (ArrayUtils.isNotEmpty(classBytes)) {
        // TODO Godin: definePackage ?
        return defineClass(name, classBytes, 0, classBytes.length);
      }
    }
    throw new ClassNotFoundException(name);
  }

  @Override
  public URL findResource(String name) {
    for (Loader loader : loaders) {
      URL url = loader.findResource(name);
      if (url != null) {
        return url;
      }
    }
    return null;
  }

  @Override
  protected Enumeration<URL> findResources(String name) throws IOException {
    List<URL> result = new ArrayList<>();
    for (Loader loader : loaders) {
      URL url = loader.findResource(name);
      if (url != null) {
        result.add(url);
      }
    }
    return Iterators.asEnumeration(result.iterator());
  }

  /**
   * Closes this class loader, so that it can no longer be used to load new classes or resources.
   * Any classes or resources that are already loaded, are still accessible.
   *
   * If class loader is already closed, then invoking this method has no effect.
   */
  @Override
  public void close() {
    for (Loader loader : loaders) {
      loader.close();
    }
  }

}
