/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.verifier.internal;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;

final class InternalFileSystem extends InternalMockedSonarAPI implements FileSystem {
  private static final TreeSet<String> LANGUAGES = new TreeSet<>(Collections.singleton("java"));
  private static final File BASE_DIR = new File(".");

  @Override
  public File baseDir() {
    return BASE_DIR;
  }

  @Override
  public Charset encoding() {
    return StandardCharsets.UTF_8;
  }

  @Override
  public Iterable<File> files(FilePredicate arg0) {
    return Collections.emptyList();
  }

  @Override
  public boolean hasFiles(FilePredicate arg0) {
    return false;
  }

  @Override
  public SortedSet<String> languages() {
    return LANGUAGES;
  }

  @Override
  public InputDir inputDir(File arg0) {
    throw notSupportedException("inputDir(File)");
  }

  @Override
  public InputFile inputFile(FilePredicate arg0) {
    throw notSupportedException("inputFile(FilePredicate)");
  }

  @Override
  public Iterable<InputFile> inputFiles(FilePredicate arg0) {
    throw notSupportedException("inputFiles(FilePredicate)");
  }

  @Override
  public FilePredicates predicates() {
    throw notSupportedException("predicates()");
  }

  @Override
  public File resolvePath(String arg0) {
    throw notSupportedException("resolvePath(String)");
  }

  @Override
  public File workDir() {
    throw notSupportedException("workDir()");
  }
}
