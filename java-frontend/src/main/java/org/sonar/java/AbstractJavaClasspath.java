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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide
public abstract class AbstractJavaClasspath {

  private static final char SEPARATOR = ',';
  private static final char UNIX_SEPARATOR = '/';
  private static final char WINDOWS_SEPARATOR = '\\';
  private static final Logger LOG = Loggers.get(AbstractJavaClasspath.class);
  protected final Configuration settings;
  protected final FileSystem fs;
  private final InputFile.Type fileType;
  private static final Path[] STANDARD_CLASSES_DIRS = {Paths.get("target", "classes"), Paths.get("target", "test-classes")};

  protected List<File> binaries;
  protected List<File> elements;
  protected boolean validateLibraries;
  protected boolean initialized;

  public AbstractJavaClasspath(Configuration settings, FileSystem fs, InputFile.Type fileType) {
    this.settings = settings;
    this.fs = fs;
    this.fileType = fileType;
    initialized = false;
  }

  protected abstract void init();

  protected Set<File> getFilesFromProperty(String property) {
    Set<File> result = new LinkedHashSet<>();
    String fileList = settings.get(property).orElse("");
    if (StringUtils.isNotEmpty(fileList)) {
      Iterable<String> fileNames = Splitter.on(SEPARATOR).omitEmptyStrings().split(fileList);
      File baseDir = fs.baseDir();
      boolean hasJavaSources = hasJavaSources();
      boolean validateLibs = validateLibraries;
      boolean isLibraryProperty = property.endsWith("libraries");
      for (String pathPattern : fileNames) {
        Set<File> libraryFilesForPattern = getFilesForPattern(baseDir.toPath(), pathPattern, isLibraryProperty);
        if (validateLibraries && libraryFilesForPattern.isEmpty() && hasJavaSources) {
          LOG.error("Invalid value for " + property);
          String message = "No files nor directories matching '" + pathPattern + "'";
          throw new IllegalStateException(message);
        }
        validateLibraries = validateLibs;
        result.addAll(libraryFilesForPattern);
      }
    }
    return result;
  }

  protected boolean hasJavaSources() {
    return fs.hasFiles(fs.predicates().and(fs.predicates().hasLanguage("java"), fs.predicates().hasType(fileType)));
  }

  protected boolean hasMoreThanOneJavaFile() {
    return Iterables.size(fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage("java"), fs.predicates().hasType(fileType)))) > 1;
  }

  private Set<File> getFilesForPattern(Path baseDir, String pathPattern, boolean libraryProperty) {

    try {
      Path filePath = resolvePath(baseDir, pathPattern);
      File file = filePath.toFile();
      if(file.isFile()) {
        return getMatchingFile(pathPattern, file);
      }
      if (file.isDirectory()) {
        return getMatchesInDir(filePath, libraryProperty);
      }
    } catch (IOException | InvalidPathException e) {
      // continue
    }

    String dirPath = sanitizeWildcards(pathPattern);
    String fileNamePattern = pathPattern;
    int lastPathSeparator = Math.max(dirPath.lastIndexOf(UNIX_SEPARATOR), dirPath.lastIndexOf(WINDOWS_SEPARATOR));
    if (lastPathSeparator == -1) {
      dirPath = ".";
    } else {
      dirPath = pathPattern.substring(0, lastPathSeparator);
      fileNamePattern = pathPattern.substring(lastPathSeparator + 1);
    }

    Path dir = resolvePath(baseDir, dirPath);
    return getFilesInDir(dir, fileNamePattern, libraryProperty);
  }

  private static Set<File> getFilesInDir(Path dir, String fileNamePattern, boolean libraryProperty) {
    if (!dir.toFile().isDirectory()) {
      return Collections.emptySet();
    }
    try {
      if (libraryProperty) {
        return getMatchingLibraries(fileNamePattern, dir);
      } else {
        return getMatchingDirs(fileNamePattern, dir);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String sanitizeWildcards(String pathPattern) {
    int wildcardIndex = pathPattern.indexOf('*');
    if (wildcardIndex >= 0) {
      return pathPattern.substring(0, wildcardIndex);
    }
    return pathPattern;
  }

  private Set<File> getMatchingFile(String pathPattern, File file) {
    if (pathPattern.endsWith(".jar") || pathPattern.endsWith(".zip") || pathPattern.endsWith(".aar")) {
      return Collections.singleton(file);
    }
    LOG.debug("File " + file.getAbsolutePath() + " was ignored from java classpath");
    validateLibraries = false;
    return Collections.emptySet();
  }

  private static Set<File> getMatchingDirs(String pattern, Path dir) throws IOException {
    if (!StringUtils.isEmpty(pattern)) {
      // find all dirs and subdirs that match the pattern
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher(getGlob(dir, pattern));
      return new DirFinder().find(dir, matcher);
    } else {
      // no pattern, so we just return dir
      return Collections.singleton(dir.toFile());
    }
  }

  private static Set<File> getMatchesInDir(Path dirPath, boolean isLibraryProperty) throws IOException {
    if (isLibraryProperty) {
      for (Path end : STANDARD_CLASSES_DIRS) {
        if (dirPath.endsWith(end)) {
          // don't scan these, as they should only contain .classes with paths starting from the root
          return Collections.singleton(dirPath.toFile());
        }
      }
      Set<File> matches = new LibraryFinder().find(dirPath, p -> true);
      matches.add(dirPath.toFile());
      return matches;
    } else {
      return Collections.singleton(dirPath.toFile());
    }
  }

  private static String separatorsToUnix(final String path) {
    return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
  }

  private static String getGlob(Path dir, String pattern) {
    // globs work with unix separators
    return "glob:" + separatorsToUnix(dir.toString()) + UNIX_SEPARATOR + separatorsToUnix(pattern);
  }

  private static Set<File> getMatchingLibraries(String pattern, Path dir) throws IOException {
    Set<File> matches = new LinkedHashSet<>();
    Set<File> dirs = getMatchingDirs(pattern, dir);

    PathMatcher matcher = FileSystems.getDefault().getPathMatcher(getGlob(dir, pattern));
    for (File d : dirs) {
      matches.addAll(getLibs(d.toPath()));
    }

    matches.addAll(dirs);
    matches.addAll(new LibraryFinder().find(dir, matcher));
    if(pattern.startsWith("**/")) {
      // match jar in the base dir when using wildcard
      matches.addAll(new LibraryFinder().find(dir, FileSystems.getDefault().getPathMatcher(getGlob(dir, pattern.substring(3)))));
    }
    return matches;
  }

  private static List<File> getLibs(Path dir) throws IOException {
    Filter<Path> filter = path -> {
      String name = path.getFileName().toString();
      return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".aar");
    };

    List<File> files = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
      stream.forEach(p -> files.add(p.toFile()));
    }
    return files;
  }

  private abstract static class AbstractFileFinder extends SimpleFileVisitor<Path> {
    protected Set<File> matchedFiles = new LinkedHashSet<>();
    protected PathMatcher matcher;

    Set<File> find(Path dir, PathMatcher matcher) throws IOException {
      this.matcher = matcher;
      Files.walkFileTree(dir, this);
      return matchedFiles;
    }
  }

  private static class DirFinder extends AbstractFileFinder {
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      if (matcher.matches(dir)) {
        matchedFiles.add(dir.toFile());
      }

      return FileVisitResult.CONTINUE;
    }
  }

  private static class LibraryFinder extends AbstractFileFinder {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      String name = file.getFileName().toString();
      if ((name.endsWith(".jar") || name.endsWith(".zip")) && matcher.matches(file)) {
        matchedFiles.add(file.toFile());
      }

      return FileVisitResult.CONTINUE;
    }
  }

  private static Path resolvePath(Path baseDir, String fileName) {
    Path filePath = Paths.get(fileName);
    if (!filePath.isAbsolute()) {
      filePath = baseDir.resolve(fileName);
    }
    return filePath.normalize();
  }

  public List<File> getElements() {
    init();
    return elements;
  }

  public List<File> getBinaryDirs() {
    init();
    return binaries;
  }
}
