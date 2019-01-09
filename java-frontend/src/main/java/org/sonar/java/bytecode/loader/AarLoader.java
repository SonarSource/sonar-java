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
package org.sonar.java.bytecode.loader;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads resources from 'aar' bundles (binary distribution of Android Library Projects).
 * <strong>Currently, for simplicity and performance, this implementation only looks for classes (.class files)</strong>
 */
public class AarLoader implements Loader {
  private static final String CLASSES_JAR_NAME = "classes.jar";
  private final ZipFile aarFile;
  private Path tempClassesJar;
  private JarLoader classesLoader;

  public AarLoader(@Nullable File file) {
    if (file == null) {
      throw new IllegalArgumentException("file can't be null");
    }

    try {
      aarFile = new ZipFile(file);
      classesLoader = extractClassesJar(file.getName());
    } catch (IOException e) {
      throw new IllegalStateException("Unable to open " + file.getAbsolutePath(), e);
    }
  }

  private JarLoader extractClassesJar(String name) throws IOException {
    ZipEntry classes = aarFile.getEntry(CLASSES_JAR_NAME);
    if (classes == null) {
      return null;
    }
    tempClassesJar = Files.createTempFile(name, ".jar");
    InputStream in = aarFile.getInputStream(classes);

    Files.copy(in, tempClassesJar, StandardCopyOption.REPLACE_EXISTING);
    return new JarLoader(tempClassesJar.toFile());
  }

  /**
   * {@inheritDoc}
   * <strong>Only class resources (bytecode .class files) are found by this method.</strong> 
   */
  @Override
  public URL findResource(String name) {
    if (classesLoader != null) {
      return classesLoader.findResource(name);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * <strong>Only class resources (bytecode .class files) are found by this method.</strong> 
   */
  @Override
  public byte[] loadBytes(String name) {
    if (classesLoader != null) {
      return classesLoader.loadBytes(name);
    }
    return new byte[0];
  }

  @Override
  public void close() {
    try {
      if (classesLoader != null) {
        classesLoader.close();
        deleteDir(tempClassesJar);
      }

      aarFile.close();
    } catch (IOException e) {
      // ignore
    }
  }

  private static void deleteDir(Path directory) throws IOException {
    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

}
