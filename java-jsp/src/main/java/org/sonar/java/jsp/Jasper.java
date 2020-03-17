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
package org.sonar.java.jsp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.jasper.JspC;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.AnalysisException;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.SmapFile;

import static java.util.Arrays.asList;

@ScannerSide
public class Jasper {

  private static final Logger LOG = Loggers.get(Jasper.class);

  public Collection<GeneratedFile> generateFiles(SensorContext context, List<File> javaClasspath) {
    List<Path> jspFiles = jspFiles(context.fileSystem());
    LOG.debug("Found {} JSP files.", jspFiles.size());
    if (jspFiles.isEmpty()) {
      return Collections.emptyList();
    }
    Path outputDir = outputDir(context);
    try {
      Jasper.compileJspFiles(jspFiles, javaClasspath, outputDir);
    } catch (Exception e) {
      LOG.warn("Failed to transpile JSP files.", e);
      return Collections.emptyList();
    }
    return findGeneratedFiles(outputDir);
  }

  private static List<Path> jspFiles(FileSystem fs) {
    Iterable<InputFile> inputFiles = fs.inputFiles(fs.predicates().hasLanguage("jsp"));
    return StreamSupport.stream(inputFiles.spliterator(), false)
      .map(InputFile::path)
      .collect(Collectors.toList());
  }

  private static void compileJspFiles(List<Path> jspFiles, List<File> javaClasspath, Path outputDir) {
    String classpath = javaClasspath.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
    List<String> args = new ArrayList<>(asList("-v", "-failFast",
      "-cache", "false",
      "-javaEncoding", StandardCharsets.UTF_8.toString(),
      "-d", outputDir.toString(),
      "-classpath", classpath, "-smap", "-dumpsmap"));
    jspFiles.stream().map(Path::toString).forEach(args::add);
    try {
      LOG.debug("Running JspC with args {} ", args);
      JspC jspc = new JspC();
      jspc.setArgs(args.toArray(new String[0]));
      jspc.execute();
    } catch (Exception e) {
      throw new AnalysisException("Error while transpiling JSP pages.", e);
    }
  }

  private static Collection<GeneratedFile> findGeneratedFiles(Path outputDir) {
    Map<Path, GeneratedFile> generatedFiles = new HashMap<>();
    try (Stream<Path> fileStream = walk(outputDir)) {
      fileStream
        .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".smap"))
        .map(SmapFile::fromPath)
        .forEach(smap -> {
          GeneratedFile generatedFile = generatedFiles.computeIfAbsent(smap.getGeneratedFile(), p -> new GeneratedFile(smap.getGeneratedFile()));
          generatedFile.addSmap(smap);
        });
      LOG.debug("Generated {} Java files.", generatedFiles.size());
      return generatedFiles.values();
    }
  }

  static Path outputDir(SensorContext sensorContext) {
    Path path = sensorContext.fileSystem().workDir().toPath().resolve("jsp");
    try {
      Files.createDirectories(path);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to create output dir for jsp files", ex);
    }
    return path;
  }

  static Stream<Path> walk(Path dir) {
    try {
      return Files.walk(dir);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to walk " + dir, e);
    }
  }
}
