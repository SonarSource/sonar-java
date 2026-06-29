/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.classpath;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.SonarComponents;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide
public abstract class AbstractClasspath {

  private static final char UNIX_SEPARATOR = '/';
  private static final char WINDOWS_SEPARATOR = '\\';
  private static final Logger LOG = LoggerFactory.getLogger(AbstractClasspath.class);
  protected final Configuration settings;
  protected final FileSystem fs;
  private final InputFile.Type fileType;
  protected final String binariesProperty;
  protected final String librariesProperty;
  private static final Path[] STANDARD_CLASSES_DIRS = {Paths.get("target", "classes"), Paths.get("target", "test-classes")};

  protected final List<File> binaries;
  protected List<File> elements;
  protected boolean validateLibraries;
  protected boolean initialized;
  private boolean inAndroidContext = false;
  private final Set<String> classpathWarnings;
  protected final AnalysisWarningsWrapper analysisWarnings;

  protected AbstractClasspath(Configuration settings, FileSystem fs, InputFile.Type fileType, String binariesProperty, String librariesProperty,
    AnalysisWarningsWrapper analysisWarnings) {
    this.settings = settings;
    this.fs = fs;
    this.fileType = fileType;
    this.binariesProperty = binariesProperty;
    this.librariesProperty = librariesProperty;
    this.binaries = new ArrayList<>();
    this.elements = List.of();
    this.analysisWarnings = analysisWarnings;
    classpathWarnings = new LinkedHashSet<>();
    initialized = false;
  }

  protected void init() {
    if (!initialized) {
      initialized = true;
      validateLibraries = hasJavaFiles();
      validatePropertiesPresence(binariesProperty, librariesProperty);
      binaries.addAll(getFilesFromProperty(binariesProperty));
      Set<File> libraries = new LinkedHashSet<>(getJdkJars());
      Set<File> extraLibraries = getFilesFromProperty(librariesProperty);
      libraries.addAll(extraLibraries);
      logResolvedFiles(binariesProperty, binaries);
      logResolvedFiles(librariesProperty, libraries);
      Set<File> all = new LinkedHashSet<>(binaries);
      all.addAll(libraries);
      elements = List.copyOf(all);
    }
  }

  protected void validatePropertiesPresence(String binariesProperty, String librariesProperty) {
    if (settings.getBoolean(SonarComponents.SONAR_AUTOSCAN).orElse(false)) {
      return;
    }
    boolean missingBinary = !settings.hasKey(binariesProperty) && hasMoreThanOneJavaFile();
    boolean missingLibraries = !settings.hasKey(librariesProperty) && hasJavaFiles();
    if (missingBinary && missingLibraries) {
      classpathWarnings.add(String.format("Missing '%s' and '%s' properties. You might end up with less precise analysis results.", binariesProperty, librariesProperty));
    } else if (missingBinary) {
      classpathWarnings.add(String.format("Missing '%s' property. You might end up with less precise analysis results.", binariesProperty));
    } else if (missingLibraries) {
      classpathWarnings.add(String.format("Missing '%s' property. You might end up with less precise analysis results.", librariesProperty));
    }
  }

  protected List<File> getJdkJars() {
    List<File> jdkClassesRoots = settings.get(ClasspathProperties.SONAR_JAVA_JDK_HOME)
      .flatMap(this::existingDirectoryOrLog)
      .map(File::toPath)
      .map(JavaSdkUtil::getJdkClassesRoots)
      .orElse(Collections.emptyList());
    logResolvedFiles(ClasspathProperties.SONAR_JAVA_JDK_HOME, jdkClassesRoots);
    return jdkClassesRoots;
  }

  static void logResolvedFiles(String property, Collection<File> files) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Property '%s' resolved with: %s", property, files.stream()
        .map(File::getAbsolutePath)
        .collect(Collectors.joining(",", "[", "]"))));
    }
  }

  private Optional<File> existingDirectoryOrLog(String path) {
    LOG.debug("Property '{}' set with: {}", ClasspathProperties.SONAR_JAVA_JDK_HOME, path);
    File file = new File(path);
    if (!file.exists() || !file.isDirectory()) {
      classpathWarnings.add(String.format("Invalid value '%s' for '%s' property, defaulting to runtime JDK.", file.getAbsolutePath(), ClasspathProperties.SONAR_JAVA_JDK_HOME));
      return Optional.empty();
    }
    return Optional.of(file);
  }

  public void logClasspathWarnings() {
    for (String warning : classpathWarnings) {
      LOG.warn(warning);
      analysisWarnings.addUnique(warning);
    }
    classpathWarnings.clear();
  }

  protected Set<File> getFilesFromProperty(String property) {
    Set<File> result = new LinkedHashSet<>();
    if (settings.hasKey(property)) {
      List<String> fileNames = Arrays.stream(settings.getStringArray(property))
        .filter(s -> !s.isEmpty()).toList();
      File baseDir = fs.baseDir();
      boolean hasJavaSources = hasJavaSources();
      boolean validateLibs = validateLibraries;
      boolean isLibraryProperty = property.endsWith("libraries");
      for (String pathPattern : fileNames) {
        Set<File> libraryFilesForPattern = getFilesForPattern(baseDir.toPath(), pathPattern, isLibraryProperty);
        if (validateLibraries && libraryFilesForPattern.isEmpty() && hasJavaSources) {
          classpathWarnings.add(String.format("Invalid value for '%s', no files nor directories matching '%s'.", property, pathPattern));
        }
        validateLibraries = validateLibs;
        result.addAll(libraryFilesForPattern);
      }
      if (result.stream().anyMatch(f -> f.getName().endsWith("android.jar"))) {
        inAndroidContext = true;
      }
    }
    return result;
  }

  protected boolean hasJavaSources() {
    return fs.hasFiles(sourcePredicate());
  }

  protected boolean hasMoreThanOneJavaFile() {
    // No need to iterate over the entire collection, checking that there are two elements is enough
    Iterator<InputFile> iterator = fs.inputFiles(sourcePredicate()).iterator();
    if (iterator.hasNext()) {
      iterator.next();
      return iterator.hasNext();
    }
    return false;
  }

  protected boolean hasJavaFiles() {
    return fs.hasFiles(sourcePredicate());
  }

  private FilePredicate sourcePredicate() {
    return fs.predicates().and(fs.predicates().hasLanguage("java"), fs.predicates().hasType(fileType));
  }

  private Set<File> getFilesForPattern(Path baseDir, String pathPattern, boolean libraryProperty) {

    try {
      Path filePath = resolvePath(baseDir, pathPattern);
      File file = filePath.toFile();
      if (file.isFile()) {
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
    LOG.debug("File {} was ignored from java classpath", file.getAbsolutePath());
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
    if (pattern.startsWith("**/")) {
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

  public boolean inAndroidContext() {
    return inAndroidContext;
  }
}
