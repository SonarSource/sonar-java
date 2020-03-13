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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

public class GeneratedFile implements InputFile {

  private final Path path;

  private final InputFile source;

  public GeneratedFile(Path path, InputFile source) {
    this.path = path;
    this.source = source;
  }

  public InputFile getSource() {
    return source;
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
