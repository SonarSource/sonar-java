/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.model;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.plugins.java.api.SourceMap;
import org.sonar.plugins.java.api.SourceMap.Location;
import org.sonar.plugins.java.api.tree.Tree;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class GeneratedFile implements InputFile {

  private final Path path;

  private final List<SmapFile> smapFiles = new ArrayList<>();

  private SourceMap sourceMap;

  public GeneratedFile(Path path) {
    this.path = path;
  }

  public SourceMap sourceMap() {
    if (sourceMap == null) {
      sourceMap = new SourceMapImpl();
    }
    return sourceMap;
  }

  public void addSmap(SmapFile smap) {
    if (!smap.getGeneratedFile().equals(path)) {
      throw new IllegalStateException("Invalid smap " + smap + " for this generated file " + path);
    }
    smapFiles.add(smap);
  }

  class SourceMapImpl implements SourceMap {

    final Map<Integer, Location> lines = new HashMap<>();

    private SourceMapImpl() {
      smapFiles.forEach(sm -> sm.getLineSection().forEach(lineInfo -> {
        for (int i = 0; i < lineInfo.repeatCount; i++) {
          int inputLine = lineInfo.inputStartLine + i;
          Path inputFile = Paths.get(sm.getFileSection().get(lineInfo.lineFileId).sourcePath);
          LocationImpl location = new LocationImpl(inputFile, inputLine, inputLine);
          int outputStart = lineInfo.outputStartLine + (i * lineInfo.outputLineIncrement);
          int outputEnd = lineInfo.outputStartLine + ((i + 1) * lineInfo.outputLineIncrement) - 1;
          // when outputLineIncrement == 0, end will be less than start (looks like bug in spec)
          outputEnd = Math.max(outputStart, outputEnd);
          for (int j = outputStart; j <= outputEnd; j++) {
            lines.merge(j, location, LocationImpl::mergeLocations);
          }
        }
      }));
    }

    @Nullable
    @Override
    public Location sourceMapLocationFor(Tree tree) {
      return getLocation(tree.firstToken().line(), tree.lastToken().line());
    }

    @VisibleForTesting
    @Nullable
    Location getLocation(int startLine, int endLine) {
      Location startLoc = lines.get(startLine);
      if (startLoc == null) {
        return null;
      }
      int inputStartLine = startLoc.startLine();
      Path startFile = startLoc.inputFile();
      Location endLoc = lines.get(endLine);
      if (endLoc == null) {
        return null;
      }
      int inputEndLine = endLoc.endLine();
      Path endFile = endLoc.inputFile();
      if (!startFile.equals(endFile)) {
        return null;
      }
      return new LocationImpl(startFile, inputStartLine, inputEndLine);
    }
  }


  private static final class LocationImpl implements Location {

    private final Path inputFile;
    private final int startLine;
    private final int endLine;

    private LocationImpl(Path inputFile, int startLine, int endLine) {
      this.inputFile = inputFile;
      this.startLine = startLine;
      this.endLine = endLine;
    }

    @Override
    public Path inputFile() {
      return inputFile;
    }

    @Override
    public int startLine() {
      return startLine;
    }

    @Override
    public int endLine() {
      return endLine;
    }

    private static Location mergeLocations(Location loc1, Location loc2) {
      return new LocationImpl(loc1.inputFile(),
        min(loc1.startLine(), loc2.startLine()),
        max(loc1.endLine(), loc2.endLine()));
    }
  }

  @Override
  public String relativePath() {
    return path.toString();
  }

  @Override
  public String absolutePath() {
    return path.toAbsolutePath().toString();
  }

  @Override
  public File file() {
    return path.toFile();
  }

  @Override
  public Path path() {
    return path;
  }

  @Override
  public URI uri() {
    return path.toUri();
  }

  @Override
  public String filename() {
    return path.getFileName().toString();
  }

  @CheckForNull
  @Override
  public String language() {
    return "java";
  }

  @Override
  public Type type() {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream inputStream() throws IOException {
    return Files.newInputStream(path);
  }

  @Override
  public String contents() throws IOException {
    return new String(Files.readAllBytes(path), charset());
  }

  @Override
  public Status status() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lines() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public TextPointer newPointer(int line, int lineOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange newRange(TextPointer start, TextPointer end) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange newRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TextRange selectLine(int line) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Charset charset() {
    return StandardCharsets.UTF_8;
  }

  @Override
  public String key() {
    return absolutePath();
  }

  @Override
  public boolean isFile() {
    return Files.isRegularFile(path);
  }

  @Override
  public String toString() {
    return path.toString();
  }
}
