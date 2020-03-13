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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

import static java.util.Arrays.asList;

@ScannerSide
public class Jasper {

  private static final Logger LOG = Loggers.get(Jasper.class);

  public List<InputFile> generateFiles(SensorContext context, List<File> javaClasspath) {
    try {
      List<Path> jspFiles = jspFiles(context.fileSystem());
      LOG.debug("Found {} JSP files.", jspFiles.size());
      Path outputDir = outputDir(context);
      Jasper.compileJspFiles(jspFiles, javaClasspath, outputDir);
      try (Stream<Path> fileStream = Files.walk(outputDir)) {
        List<InputFile> generatedFiles = fileStream
          .filter(p -> p.toString().endsWith(".java"))
          .map(path -> new GeneratedFile(path, findSource(path, context.fileSystem())))
          .collect(Collectors.toList());
        LOG.debug("Generated {} Java files.", generatedFiles.size());
        return generatedFiles;
      }
    } catch (Exception e) {
      LOG.warn("Failed to transpile JSP files.", e);
      return Collections.emptyList();
    }
  }

  private static List<Path> jspFiles(FileSystem fs) {
    Iterable<InputFile> inputFiles = fs.inputFiles(fs.predicates().hasLanguage("jsp"));
    return StreamSupport.stream(inputFiles.spliterator(), false)
      .map(InputFile::path)
      .collect(Collectors.toList());
  }

  private static void compileJspFiles(List<Path> jspFiles, List<File> javaClasspath, Path outputDir) {
    if (jspFiles.isEmpty()) {
      return;
    }
    String classpath = javaClasspath.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
    List<String> args = new ArrayList<>(asList("-v", "-failFast",
      "-cache", "false",
      "-javaEncoding", StandardCharsets.UTF_8.toString(),
      "-d", outputDir.toString(),
      "-classpath", classpath));
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

  private static InputFile findSource(Path path, FileSystem fs) {
    String javaFilename = path.getFileName().toString();
    String jspFilename = javaFilename.substring(0, javaFilename.length() - "_jsp.java".length()) + ".jsp";
    // TODO we need to use source map to reliably find JSP file from which java file was generated
    return fs.inputFile(fs.predicates().hasFilename(jspFilename));
  }

  private static Path outputDir(SensorContext sensorContext) {
    return sensorContext.fileSystem().workDir().toPath().resolve("jsp");
  }


}
