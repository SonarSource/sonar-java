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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import static java.lang.Integer.parseInt;

/**
 * SMAP format is described by <a href="https://jcp.org/aboutJava/communityprocess/final/jsr045/index.html">JSR 45</a>
 * JSR 45 defines more generic format supporting translation from multiple languages, here we implement only subset used
 * by Jasper for JSP to Java translation.
 * <p>
 * We expect only single JSP stratum, with single FileSection and LineSection. Moreover only single file is expected in FileSection
 */
public class SmapFile {

  private static final Pattern LINE_INFO = Pattern.compile("(?<inputStartLine>\\d+)" +
    "(?:#(?<lineFileId>\\d+))?" +
    "(?:,(?<repeatCount>\\d+))?:" +
    "(?<outputStartLine>\\d+)" +
    "(?:,(?<outputIncrement>\\d+))?");

  private static final Logger LOG = LoggerFactory.getLogger(SmapFile.class);

  private final Path generatedFile;
  private final Map<Integer, FileInfo> fileSection;
  private final List<LineInfo> lineSection;
  private final Scanner sc;

  public SmapFile(Path rootDir, String smapString, Path uriRoot, FileSystem fileSystem) {
    this.sc = new Scanner(smapString);
    String header = sc.nextLine();
    if (!"SMAP".equals(header)) {
      throw new IllegalStateException("Not a source map");
    }
    String generatedFileName = sc.nextLine();
    generatedFile = rootDir.resolve(generatedFileName);
    String defaultStratum = sc.nextLine();
    if (!"JSP".equals(defaultStratum)) {
      throw new IllegalStateException("Not a JSP source map");
    }
    findSection("*S JSP");
    findSection("*F");
    fileSection = readFileSection(uriRoot, fileSystem);
    findSection("*L");
    lineSection = readLineSection();
  }

  public Path getGeneratedFile() {
    return generatedFile;
  }

  Map<Integer, FileInfo> getFileSection() {
    return fileSection;
  }

  List<LineInfo> getLineSection() {
    return lineSection;
  }

  private List<LineInfo> readLineSection() {
    List<LineInfo> result = new ArrayList<>();
    int lineFileId = 0;
    while (sc.hasNext() && !sc.hasNext("\\*.")) {
      String line = sc.nextLine();
      Matcher matcher = LINE_INFO.matcher(line);
      if (matcher.matches()) {
        int inputStartLine = parseInt(matcher.group("inputStartLine"));
        String lineFileIdGroup = matcher.group("lineFileId");
        if (lineFileIdGroup != null) {
          lineFileId = parseInt(lineFileIdGroup);
        }
        String repeatCountGroup = matcher.group("repeatCount");
        int repeatCount = repeatCountGroup != null ? parseInt(repeatCountGroup) : 1;
        int outputStartLine = parseInt(matcher.group("outputStartLine"));
        String outputIncrementGroup = matcher.group("outputIncrement");
        int outputIncrement = outputIncrementGroup != null ? parseInt(outputIncrementGroup) : 1;
        result.add(new LineInfo(inputStartLine, lineFileId, repeatCount, outputStartLine, outputIncrement));
      } else {
        LOG.warn("Invalid line info {}", line);
      }
    }
    return result;
  }

  private void findSection(String section) {
    while (sc.hasNextLine()) {
      if (section.equals(sc.nextLine())) {
        return;
      }
    }
    throw new IllegalStateException("Section " + section + " not found");
  }

  private Map<Integer, FileInfo> readFileSection(Path uriRoot, FileSystem fileSystem) {
    Map<Integer, FileInfo> result = new HashMap<>();
    while (sc.hasNext() && !sc.hasNext("\\*.")) {
      if (sc.hasNext("\\+")) {
        sc.next();
        int fileId = sc.nextInt();
        String file = sc.next();
        sc.nextLine();
        String path = sc.nextLine();
        InputFile inputFile = findFileWithPath(uriRoot.resolve(path), fileSystem);
        result.put(fileId, new FileInfo(fileId, file, path, inputFile));
      } else {
        int fileId = sc.nextInt();
        String file = sc.next();
        InputFile inputFile = findFileWithPath(uriRoot.resolve(file), fileSystem);
        result.put(fileId, new FileInfo(fileId, file, null, inputFile));
      }
    }
    return result;
  }

  @CheckForNull
  private static InputFile findFileWithPath(Path path, FileSystem fileSystem) {
    FilePredicates predicates = fileSystem.predicates();
    return fileSystem.inputFile(predicates.hasPath(path.toString()));
  }

  @Override
  public String toString() {
    return generatedFile.toString();
  }

  Optional<InputFile> getInputFile(int fileId) {
    return Optional.ofNullable(fileSection.get(fileId))
      .map(info -> info.inputFile);
  }

  static class FileInfo {
    final int fileId;
    final String sourceName;
    final String sourcePath;
    final InputFile inputFile;

    FileInfo(int fileId, String sourceName, @Nullable String sourcePath, @Nullable InputFile inputFile) {
      this.fileId = fileId;
      this.sourceName = sourceName;
      this.sourcePath = sourcePath;
      this.inputFile = inputFile;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FileInfo fileInfo = (FileInfo) o;
      return fileId == fileInfo.fileId &&
        Objects.equals(sourceName, fileInfo.sourceName) &&
        Objects.equals(sourcePath, fileInfo.sourcePath);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fileId, sourceName, sourcePath);
    }

    @Override
    public String toString() {
      return "FileInfo{" +
        "fileId=" + fileId +
        ", sourceName='" + sourceName + '\'' +
        ", sourcePath='" + sourcePath + '\'' +
        '}';
    }
  }

  static class LineInfo {
    final int inputStartLine;
    final int lineFileId;
    final int repeatCount;
    final int outputStartLine;
    final int outputLineIncrement;


    LineInfo(int inputStartLine, int lineFileId, int repeatCount, int outputStartLine, int outputLineIncrement) {
      this.inputStartLine = inputStartLine;
      this.lineFileId = lineFileId;
      this.repeatCount = repeatCount;
      this.outputStartLine = outputStartLine;
      this.outputLineIncrement = outputLineIncrement;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LineInfo lineInfo = (LineInfo) o;
      return inputStartLine == lineInfo.inputStartLine &&
        lineFileId == lineInfo.lineFileId &&
        repeatCount == lineInfo.repeatCount &&
        outputStartLine == lineInfo.outputStartLine &&
        outputLineIncrement == lineInfo.outputLineIncrement;
    }

    @Override
    public int hashCode() {
      return Objects.hash(inputStartLine, lineFileId, repeatCount, outputStartLine, outputLineIncrement);
    }

    @Override
    public String toString() {
      return "LineInfo{" +
        "inputStartLine=" + inputStartLine +
        ", lineFileId=" + lineFileId +
        ", repeatCount=" + repeatCount +
        ", outputStartLine=" + outputStartLine +
        ", outputLineIncrement=" + outputLineIncrement +
        '}';
    }
  }
}
