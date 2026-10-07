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
package org.sonar.java.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.java.api.JavaVersion;

public final class BytecodeCompiler {

  private static final Logger LOG = LoggerFactory.getLogger(BytecodeCompiler.class);

  public static final Path OUTPUT_DIRECTORY = Path.of("target", "sonar", "bytecode");

  private BytecodeCompiler() {
    // Utils class
  }

  /**
   * Compiles the provided source files using ECJ (Eclipse Compiler for Java).
   * The output bytecode files are written to the default output directory ({@link #OUTPUT_DIRECTORY}).
   *
   * @param sources list of paths to Java source files
   * @param sourceVersion the Java version for source code compatibility
   * @param targetVersion the Java version for target bytecode compatibility
   * @return true if compilation succeeded, false otherwise
   */
  public static boolean compile(List<Path> sources, JavaVersion sourceVersion, JavaVersion targetVersion) {
    return compile(sources, List.of(), OUTPUT_DIRECTORY, sourceVersion, targetVersion);
  }

  /**
   * Compiles sources against dependency JARs and class directories, writing bytecode to the default output directory.
   *
   * @param sources list of paths to Java source files
   * @param classpath dependency JARs and directories containing compiled classes
   * @param sourceVersion the Java version for source code compatibility
   * @param targetVersion the Java version for target bytecode compatibility
   * @return true if compilation succeeded, false otherwise
   */
  public static boolean compile(List<Path> sources, List<Path> classpath, JavaVersion sourceVersion, JavaVersion targetVersion) {
    return compile(sources, classpath, OUTPUT_DIRECTORY, sourceVersion, targetVersion);
  }

  /**
   * Compiles the provided source files using ECJ (Eclipse Compiler for Java).
   * The output bytecode files are written to the specified output directory.
   *
   * @param sources list of paths to Java source files
   * @param outputDirectory path to write compiled bytecode files
   * @param sourceVersion the Java version for source code compatibility
   * @param targetVersion the Java version for target bytecode compatibility
   * @return true if compilation succeeded, false otherwise
   */
  public static boolean compile(List<Path> sources, Path outputDirectory, JavaVersion sourceVersion, JavaVersion targetVersion) {
    return compile(sources, List.of(), outputDirectory, sourceVersion, targetVersion);
  }

  /**
   * Compiles sources against dependency JARs and class directories, writing bytecode to the specified output directory.
   *
   * @param sources list of paths to Java source files
   * @param classpath dependency JARs and directories containing compiled classes
   * @param outputDirectory path to write compiled bytecode files
   * @param sourceVersion the Java version for source code compatibility
   * @param targetVersion the Java version for target bytecode compatibility
   * @return true if compilation succeeded, false otherwise
   */
  public static boolean compile(List<Path> sources, List<Path> classpath, Path outputDirectory, JavaVersion sourceVersion, JavaVersion targetVersion) {
    return compile(sources, classpath, outputDirectory, sourceVersion, targetVersion, Charset.defaultCharset());
  }

  public static boolean compile(List<Path> sources, List<Path> classpath, Path outputDirectory, JavaVersion sourceVersion, JavaVersion targetVersion, Charset charset) {
    if (sources.isEmpty()) {
      return true;
    }

    try {
      Files.createDirectories(outputDirectory);
    } catch (IOException e) {
      LOG.warn("Failed to create output directory: {}", outputDirectory, e);
      return false;
    }

    String[] args = buildCompilerArgs(sources, classpath, outputDirectory, sourceVersion, targetVersion, charset);

    StringWriter outWriter = new StringWriter();
    StringWriter errWriter = new StringWriter();

    boolean result;
    try {
      result = new Main(new PrintWriter(outWriter), new PrintWriter(errWriter), false).compile(args);
    } catch (Exception e) {
      LOG.warn("ECJ batch compiler failed with exception", e);
      return false;
    }

    String output = outWriter.toString();
    String errors = errWriter.toString();

    if (!errors.isEmpty()) {
      if (result) {
        LOG.debug("Compilation warnings:\n{}", errors);
      } else {
        LOG.warn("Compilation errors:\n{}", errors);
      }
    }

    if (!output.isEmpty()) {
      LOG.debug("Compilation output:\n{}", output);
    }

    return result;
  }

  private static String[] buildCompilerArgs(List<Path> sources, List<Path> classpath, Path outputDirectory, JavaVersion sourceVersion, JavaVersion targetVersion, Charset charset) {
    List<String> args = new ArrayList<>();

    args.add("-d");
    args.add(outputDirectory.toAbsolutePath().toString());

    if (!classpath.isEmpty()) {
      args.add("-classpath");
      args.add(classpath.stream().map(path -> path.toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator)));
    }

    args.add("-proc:none");
    args.add("-nowarn");

    args.add("-encoding");
    args.add(charset.name());

    args.add("-source");
    args.add(sourceVersion.effectiveJavaVersionAsString());

    args.add("-target");
    args.add(targetVersion.effectiveJavaVersionAsString());

    if (sourceVersion.arePreviewFeaturesEnabled()) {
      args.add("--enable-preview");
    }

    for (Path source : sources) {
      args.add(source.toAbsolutePath().toString());
    }

    return args.toArray(new String[0]);
  }

}
