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
package org.sonar.plugins.findbugs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;
import java.util.List;
import java.util.Set;

public class FindbugsSourceBinaryMatcher {

  private final PathResolver pathResolver = new PathResolver();

  private final List<File> sourceDirs;
  private final List<File> binaryDirs;

  public FindbugsSourceBinaryMatcher(List<File> sourceDirs, List<File> binaryDirs) {
    this.sourceDirs = sourceDirs;
    this.binaryDirs = binaryDirs;
  }

  public List<File> classesToAnalyze(List<File> sourcesToAnalyze) {
    Set<String> allSourcesQualifiedNames = toQualifiedNames(allSources(), sourceDirs);
    Set<String> allSourcesToAnalyzeQualifiedNames = toQualifiedNames(sourcesToAnalyze, sourceDirs);

    ImmutableList.Builder<File> builder = ImmutableList.builder();

    List<File> allClasses = allClasses();
    for (File clazz : allClasses) {
      String correspondingSourceQualifiedName = classToQualifiedSourceName(clazz);

      if (allSourcesToAnalyzeQualifiedNames.contains(correspondingSourceQualifiedName) || !allSourcesQualifiedNames.contains(correspondingSourceQualifiedName)) {
        builder.add(clazz);
      }
    }

    return builder.build();
  }

  private List<File> allSources() {
    ImmutableList.Builder<File> builder = ImmutableList.builder();

    for (File binaryDir : sourceDirs) {
      if (binaryDir.isDirectory()) {
        for (File binaryFile : FileUtils.listFiles(binaryDir, new String[] {"java"}, true)) {
          builder.add(binaryFile);
        }
      }
    }

    return builder.build();
  }

  private List<File> allClasses() {
    ImmutableList.Builder<File> builder = ImmutableList.builder();

    for (File binaryDir : binaryDirs) {
      if (binaryDir.isDirectory()) {
        for (File binaryFile : FileUtils.listFiles(binaryDir, new String[] {"class"}, true)) {
          builder.add(binaryFile);
        }
      }
    }

    return builder.build();
  }

  private Set<String> toQualifiedNames(List<File> files, List<File> relativeFolders) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();

    for (File file : files) {
      builder.add(substringTillLastDotOrDollar(pathResolver.relativePath(relativeFolders, file).path()));
    }

    return builder.build();
  }

  private String classToQualifiedSourceName(File file) {
    return substringTillLastDotOrDollar(pathResolver.relativePath(binaryDirs, file).path());
  }

  private String substringTillLastDotOrDollar(String s) {
    int lastDotIndex = StringUtils.lastIndexOf(s, '.');
    int lastDollarIndex = StringUtils.lastIndexOf(s, '$');

    int trimToIndex;
    if (lastDotIndex != -1 && lastDollarIndex != -1) {
      trimToIndex = Math.min(lastDotIndex, lastDollarIndex);
    } else {
      trimToIndex = Math.max(lastDotIndex, lastDollarIndex);
    }

    return trimToIndex == -1 ? s : s.substring(0, trimToIndex);
  }

}
