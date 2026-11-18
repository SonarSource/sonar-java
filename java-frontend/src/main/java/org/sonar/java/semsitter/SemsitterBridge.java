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
package org.sonar.java.semsitter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Client view of the shared the SemSitter analyzer server process.
 *
 * <p>{@link #analyze} scans the module once, {@link #cst} returns the CST of one file with its SemSitter semantics:
 * {@code tree} (nodes with kind {@code k}, field {@code f}, position {@code p} as 1-based lines and 0-based UTF-16
 * columns, text {@code t}, children {@code c}, declared symbol ids {@code decls}, resolved occurrences {@code refs}),
 * {@code symbols} and {@code facts} (graph facts by id). Closing a batch keeps the process, which lives for the whole
 * scanner run.
 */
public final class SemsitterBridge implements AutoCloseable {

  private final AnalyzerSession server;

  private SemsitterBridge(AnalyzerSession server) {
    this.server = server;
  }

  public static SemsitterBridge start(Path executable) {
    return new SemsitterBridge(AnalyzerSession.shared(executable));
  }

  /** Sets the {@code jvm.*} classpath settings the server scans with. */
  public static void moduleSettings(Map<String, List<String>> settings) {
    AnalyzerSession.moduleSettings(settings);
  }

  /** Starts the scan of the module in the background, ahead of the first file. */
  public static void prepare(Path executable) {
    AnalyzerSession.prepareAsync(executable);
  }

  /** Sets the files of the whole analysis, which the server scans exactly; empty means the source roots. */
  public static void scanFiles(List<String> files) {
    AnalyzerSession.scanFiles(files);
  }

  /** Sets the module base directory the server scans. */
  public static void moduleRoot(Path root) {
    AnalyzerSession.moduleRoot(root);
  }

  /**
   * Makes the module ready for the files of this batch.
   * @return the response, whose {@code files} are the paths accepted by {@link #cst}
   */
  public JsonObject analyze(Path root, Collection<Path> files, Collection<String> classpath) throws IOException {
    return analyze(root, files, classpath, Integer.MAX_VALUE);
  }

  /** As {@link #analyze(Path, Collection, Collection)}, reading the sources at the given Java level; {@link Integer#MAX_VALUE} means not set. */
  public JsonObject analyze(Path root, Collection<Path> files, Collection<String> classpath, int javaLevel) throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("root", root.toAbsolutePath().toString());
    JsonArray fileArray = new JsonArray();
    files.forEach(file -> fileArray.add(file.toAbsolutePath().toString()));
    request.add("files", fileArray);
    JsonArray classpathArray = new JsonArray();
    classpath.forEach(classpathArray::add);
    request.add("classpath", classpathArray);
    if (javaLevel != Integer.MAX_VALUE) {
      request.addProperty("javaLevel", javaLevel);
    }
    return server.analyze(request);
  }

  public JsonObject cst(String path) throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("path", path);
    return server.cst(request);
  }

  /**
   * @return declarations (kind, supertypes, members) of the types named by erased binary names, and of their supertypes
   */
  public JsonObject types(Collection<String> names) throws IOException {
    return server.types(new HashSet<>(names)).getAsJsonObject("types");
  }

  /** Whether the package is declared by the project, the classpath or the JDK; null when the server cannot tell. */
  public Boolean hasPackage(String name) throws IOException {
    return server.hasPackage(name);
  }

  /** Single-source parses have no module to scan, so the server does not serve them. */
  public JsonObject parse(String source) throws IOException {
    throw new IOException("single-source parse is not supported by the analyzer server");
  }

  @Override
  public void close() {
    // the process serves the next batch and ends with the scanner
  }
}
