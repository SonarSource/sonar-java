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
package org.sonar.java.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.SourceMap;
import org.sonar.plugins.java.api.SourceMap.Location;
import org.sonar.plugins.java.api.tree.Tree;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class GeneratedFile implements InputFile {

  private final Path path;
  private String contents = null;
  private String md5 = null;

  @VisibleForTesting
  final List<SmapFile> smapFiles = new ArrayList<>();

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
    smapFiles.add(smap);
  }

  class SourceMapImpl implements SourceMap {

    final Map<Integer, Location> lines = new HashMap<>();

    private SourceMapImpl() {
      for (SmapFile sm : smapFiles) {
        for (SmapFile.LineInfo lineInfo : sm.getLineSection()) {
          sm.getInputFile(lineInfo.lineFileId).ifPresent(inputFile -> processLineInfo(inputFile, lineInfo));
        }
      }
    }

    private void processLineInfo(InputFile inputFile, SmapFile.LineInfo lineInfo) {
      for (int i = 0; i < lineInfo.repeatCount; i++) {
        int inputLine = lineInfo.inputStartLine + i;
        LocationImpl location = new LocationImpl(inputFile, inputLine, inputLine);
        int outputStart = lineInfo.outputStartLine + (i * lineInfo.outputLineIncrement);
        int outputEnd = lineInfo.outputStartLine + ((i + 1) * lineInfo.outputLineIncrement) - 1;
        // when outputLineIncrement == 0, end will be less than start (looks like bug in spec)
        outputEnd = max(outputStart, outputEnd);
        for (int j = outputStart; j <= outputEnd; j++) {
          lines.merge(j, location, LocationImpl::mergeLocations);
        }
      }
    }

    @Override
    public Optional<Location> sourceMapLocationFor(Tree tree) {
      return getLocation(LineUtils.startLine(tree.firstToken()), LineUtils.startLine(tree.lastToken()));
    }

    @VisibleForTesting
    Optional<Location> getLocation(int startLine, int endLine) {
      Location startLoc = lines.get(startLine);
      Location endLoc = lines.get(endLine);
      if (startLoc == null || endLoc == null) {
        return Optional.empty();
      }
      return Optional.of(new LocationImpl(startLoc.file(), startLoc.startLine(), endLoc.endLine()));
    }
  }


  private static final class LocationImpl implements Location {

    private final InputFile inputFile;
    private final int startLine;
    private final int endLine;

    private LocationImpl(InputFile inputFile, int startLine, int endLine) {
      this.inputFile = inputFile;
      this.startLine = startLine;
      this.endLine = endLine;
    }

    @Override
    public InputFile file() {
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
      return new LocationImpl(loc1.file(),
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
    if (contents == null) {
      contents = new String(Files.readAllBytes(path), charset());
    }
    return contents;
  }

  @Override
  public String md5Hash() {
    if (md5 == null) {
      md5 = InputFileUtils.md5Hash(this);
    }
    return md5;
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
