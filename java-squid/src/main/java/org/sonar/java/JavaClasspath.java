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
package org.sonar.java;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.WildcardPattern;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

public class JavaClasspath implements BatchExtension {

  private static final char SEPARATOR = ',';
  private static final Logger LOG = LoggerFactory.getLogger(JavaClasspath.class);

  private List<File> binaries;
  private List<File> elements;
  private boolean validateLibraries;
  private boolean hasJavaSources;

  public JavaClasspath(Project project, Settings settings, FileSystem fileSystem) {
    this(project, settings, fileSystem, null);
  }

  public JavaClasspath(Project project, Settings settings, FileSystem fileSystem, @Nullable MavenProject pom) {
    validateLibraries = project.getModules().isEmpty();
    FilePredicates predicates = fileSystem.predicates();
    //FIXME: call to filesystem before sensor should be removed.
    hasJavaSources = fileSystem.hasFiles(predicates.and(predicates.hasLanguage("java"), predicates.hasType(InputFile.Type.MAIN)));
    binaries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_BINARIES, settings, fileSystem.baseDir());
    List<File> libraries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES, settings, fileSystem.baseDir());
    boolean useDeprecatedProperties = binaries.isEmpty() && libraries.isEmpty();
    if (useDeprecatedProperties) {
      binaries = getFilesFromProperty("sonar.binaries", settings, fileSystem.baseDir());
      libraries = getFilesFromProperty("sonar.libraries", settings, fileSystem.baseDir());
    }
    if (pom != null && libraries.isEmpty()) {
      //check mojo
      elements = getLibrariesFromMaven(pom);
    } else {
      elements = Lists.newArrayList(binaries);
      elements.addAll(libraries);
      if (useDeprecatedProperties && !elements.isEmpty()) {
        LOG.warn("sonar.binaries and sonar.libraries are deprecated since version 2.5 of sonar-java-plugin, please use sonar.java.binaries and sonar.java.libraries instead");
      }
    }
  }

  private List<File> getLibrariesFromMaven(MavenProject pom) {
    try {
      List<File> files = Lists.newArrayList();
      if (pom.getCompileClasspathElements() != null) {
        for (String classPathString : (List<String>) pom.getCompileClasspathElements()) {
          files.add(new File(classPathString));
        }
      }
      if (pom.getBuild().getOutputDirectory() != null) {
        File outputDirectoryFile = new File(pom.getBuild().getOutputDirectory());
        if (outputDirectoryFile.exists()) {
          files.add(outputDirectoryFile);
        }
      }
      return files;
    } catch (DependencyResolutionRequiredException e) {
      throw new SonarException("Fail to create the project classloader", e);
    }
  }


  private List<File> getFilesFromProperty(String property, Settings settings, File baseDir) {
    List<File> result = Lists.newArrayList();
    String fileList = settings.getString(property);
    if (StringUtils.isNotEmpty(fileList)) {
      Iterable<String> fileNames = Splitter.on(SEPARATOR).omitEmptyStrings().split(fileList);
      for (String pathPattern : fileNames) {
        boolean isLibraryProperty = property.endsWith("libraries");
        List<File> libraryFilesForPattern = getFilesForPattern(baseDir, pathPattern, isLibraryProperty);
        if (validateLibraries && libraryFilesForPattern.isEmpty() && hasJavaSources) {
          LOG.error("Invalid value for " + property);
          String message = "No files nor directories matching '" + pathPattern + "'";
          throw new IllegalStateException(message);
        }
        result.addAll(libraryFilesForPattern);
      }
    }
    return result;
  }

  private List<File> getFilesForPattern(File baseDir, String pathPattern, boolean libraryProperty) {
    String dirPath = pathPattern;
    String filePattern;
    int wildcardIndex = pathPattern.indexOf('*');
    if (wildcardIndex >= 0) {
      dirPath = pathPattern.substring(0, wildcardIndex);
    }
    int lastPathSeparator = Math.max(dirPath.lastIndexOf('/'), dirPath.lastIndexOf('\\'));
    if (lastPathSeparator == -1) {
      dirPath = ".";
      filePattern = pathPattern;
    } else {
      dirPath = pathPattern.substring(0, lastPathSeparator);
      filePattern = pathPattern.substring(lastPathSeparator + 1);
    }
    File dir = resolvePath(baseDir, dirPath);
    if (!dir.isDirectory()) {
      return Lists.newArrayList();
    }
    return getMatchingFiles(filePattern, dir, libraryProperty);
  }

  private List<File> getMatchingFiles(String pattern, File dir, boolean libraryProperty) {
    WilcardPatternFileFilter wilcardPatternFileFilter = new WilcardPatternFileFilter(dir, pattern);
    FileFilter fileFilter = wilcardPatternFileFilter;
    List<File> files = Lists.newArrayList();
    if (libraryProperty) {
      if (pattern.endsWith("*")) {
        fileFilter = new AndFileFilter((IOFileFilter) fileFilter,
            new OrFileFilter(Lists.newArrayList(suffixFileFilter(".jar", IOCase.INSENSITIVE), suffixFileFilter(".zip", IOCase.INSENSITIVE))));
      }
      //find jar and zip files
      files.addAll(Lists.newArrayList(FileUtils.listFiles(dir, (IOFileFilter) fileFilter, TrueFileFilter.TRUE)));
    }
    //find directories matching pattern.
    IOFileFilter subdirectories = pattern.isEmpty() ? FalseFileFilter.FALSE : TrueFileFilter.TRUE;
    Collection<File> dirs = FileUtils.listFilesAndDirs(dir, new AndFileFilter(wilcardPatternFileFilter, DirectoryFileFilter.DIRECTORY), subdirectories);
    //remove searching dir from matching as listFilesAndDirs always includes it in the list see https://issues.apache.org/jira/browse/IO-328
    if (!pattern.isEmpty()) {
      dirs.remove(dir);
      //remove subdirectories that were included during search
      Iterator<File> iterator = dirs.iterator();
      while (iterator.hasNext()) {
        File matchingDir = iterator.next();
        if (!wilcardPatternFileFilter.accept(matchingDir)) {
          iterator.remove();
        }
      }
    }

    if (libraryProperty) {
      for (File directory : dirs) {
        files.addAll(getMatchingFiles("**/*.jar", directory, true));
        files.addAll(getMatchingFiles("**/*.zip", directory, true));
      }
    }
    files.addAll(dirs);
    return files;
  }

  private File resolvePath(File baseDir, String fileName) {
    File file = new File(fileName);
    if (!file.isAbsolute()) {
      file = new File(baseDir, fileName);
    }
    return file;
  }

  public List<File> getElements() {
    return elements;
  }

  public List<File> getBinaryDirs() {
    return binaries;
  }

  private static class WilcardPatternFileFilter implements IOFileFilter {
    private File baseDir;
    private WildcardPattern wildcardPattern;

    public WilcardPatternFileFilter(File baseDir, String wildcardPattern) {
      this.baseDir = baseDir;
      this.wildcardPattern = WildcardPattern.create(FilenameUtils.separatorsToSystem(wildcardPattern), File.separator);
    }

    @Override
    public boolean accept(File dir, String name) {
      return accept(new File(dir, name));
    }

    @Override
    public boolean accept(File file) {
      String path = file.getAbsolutePath();
      path = path.substring(baseDir.getAbsolutePath().length() + 1);
      return wildcardPattern.match(path);
    }
  }
}
