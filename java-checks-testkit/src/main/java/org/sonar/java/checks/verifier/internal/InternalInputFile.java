/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class InternalInputFile extends InternalMockedSonarAPI implements InputFile {

  private final File file;
  private final String moduleKey;
  private final String contents;
  private final int numberLines;
  private final InputFile.Type type;
  private final Status status;

  private InternalInputFile(String filename, InputFile.Type type) {
    this.file = new File(filename);
    this.moduleKey = "";
    this.contents = "";
    this.numberLines = -1;
    this.type = type;
    this.status = Status.SAME;
  }

  private InternalInputFile(String moduleKey, File file, Status status) {
    this.file = file;
    this.moduleKey = moduleKey;
    this.contents = readFile(file);
    this.numberLines = contents.split("(\r)?\n|\r").length;
    this.type = InputFile.Type.MAIN;
    this.status = status;
  }

  private static String readFile(File file) {
    try {
      return new String(Files.readAllBytes(file.toPath()), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to read file '%s'", file.getAbsolutePath()));
    }
  }

  public static InputFile inputFile(String moduleKey, File file) {
    return new InternalInputFile(moduleKey, file, Status.SAME);
  }

  public static InputFile inputFile(String moduleKey, File file, Status status) {
    return new InternalInputFile(moduleKey, file, status);
  }

  public static InputFile emptyInputFile(String filename, InputFile.Type type) {
    return new InternalInputFile(filename, type);
  }

  @Override
  public String filename() {
    return file.getName();
  }

  @Override
  public URI uri() {
    return file.toURI();
  }

  @Override
  public boolean isFile() {
    return file.isFile();
  }

  @Override
  public String key() {
    return moduleKey + ":" + filename();
  }

  @Override
  public String absolutePath() {
    return file.getAbsolutePath();
  }

  @Override
  public Charset charset() {
    return UTF_8;
  }

  @Override
  public String contents() throws IOException {
    return contents;
  }

  @Override
  public File file() {
    return file;
  }

  @Override
  public InputStream inputStream() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public boolean isEmpty() {
    return contents.isEmpty();
  }

  @Override
  public String language() {
    return "java";
  }

  @Override
  public int lines() {
    return numberLines;
  }

  @Override
  public TextPointer newPointer(int line, int offset) {
    return new InternalTextPointer(line, offset);
  }

  @Override
  public TextRange newRange(TextPointer start, TextPointer end) {
    return new InternalTextRange(start, end);
  }

  @Override
  public TextRange newRange(int startLine, int startColumn, int endLine, int endColumn) {
    return new InternalTextRange(startLine, startColumn, endLine, endColumn);
  }

  @Override
  public Path path() {
    return file.toPath();
  }

  @Override
  public String relativePath() {
    return file.toPath().toString();
  }

  @Override
  public InputFile.Type type() {
    return type;
  }

  @Override
  public String toString() {
    return path().toString();
  }

  @Override
  public TextRange selectLine(int arg0) {
    throw notSupportedException("selectLine(int)");
  }

  @Override
  public Status status() {
    return status;
  }
}
