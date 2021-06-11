/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.ExecutionTimeReport;
import org.sonar.java.PerformanceMeasure;
import org.sonar.java.ProgressMonitor;
import org.sonarsource.analyzer.commons.ProgressReport;

public interface JParserConfig {

  static JParserConfig create(String javaVersion, List<File> classpath, Mode mode) {
    if (mode == Mode.BATCH) {
      return new Batch(javaVersion, classpath);
    }
    // default is "File by File"
    return new FileByFile(javaVersion, classpath);
  }

  void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled, BiConsumer<InputFile, Result> action);

  enum Mode {
    BATCH, FILE_BY_FILE
  }

  class Result {
    private final Exception e;
    private final JavaTree.CompilationUnitTreeImpl t;

    Result(Exception e) {
      this.e = e;
      this.t = null;
    }

    Result(JavaTree.CompilationUnitTreeImpl t) {
      this.e = null;
      this.t = t;
    }

    public JavaTree.CompilationUnitTreeImpl get() throws Exception {
      if (e != null) {
        throw e;
      }
      return t;
    }
  }


  class Batch implements JParserConfig {

    private static final Logger LOG = Loggers.get(JParserConfig.Batch.class);
    private final ASTParser astParser;
    private final String javaVersion;

    private Batch(String javaVersion, List<File> classpath) {
      this.javaVersion = javaVersion;
      astParser = JParser.createASTParser(javaVersion, classpath);
    }

    @Override
    public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled, BiConsumer<InputFile, Result> action) {
      LOG.info("Using ECJ batch to parse source files.");

      List<String> sourceFilePaths = new ArrayList<>();
      List<String> encodings = new ArrayList<>();
      Map<File, InputFile> inputs = new HashMap<>();
      for (InputFile inputFile : inputFiles) {
        String sourceFilePath = inputFile.absolutePath();
        inputs.put(new File(sourceFilePath), inputFile);
        sourceFilePaths.add(sourceFilePath);
        encodings.add(inputFile.charset().name());
      }

      PerformanceMeasure.Duration batchPerformance = PerformanceMeasure.start("ParseAsBatch");
      ExecutionTimeReport executionTimeReport = new ExecutionTimeReport();
      ProgressMonitor monitor = new ProgressMonitor(isCanceled);

      try {
        astParser.createASTs(sourceFilePaths.toArray(new String[0]), encodings.toArray(new String[0]), new String[0], new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit ast) {
            PerformanceMeasure.Duration convertDuration = PerformanceMeasure.start("Convert");

            InputFile inputFile = inputs.get(new File(sourceFilePath));
            executionTimeReport.start(inputFile);
            Result result;
            try {
              result = new Result(JParser.convert(javaVersion, inputFile.filename(), inputFile.contents(), ast));
            } catch (Exception e) {
              result = new Result(e);
            }
            convertDuration.stop();
            PerformanceMeasure.Duration analyzeDuration = PerformanceMeasure.start("Analyze");
            action.accept(inputFile, result);

            executionTimeReport.end();
            analyzeDuration.stop();
          }
        }, monitor);
      } finally {
        // ExecutionTimeReport will not include the parsing time by file when using batch mode.
        executionTimeReport.reportAsBatch();
        batchPerformance.stop();
        monitor.done();
      }
    }
  }

  class FileByFile implements JParserConfig {

    private final List<File> classpath;
    private final String javaVersion;

    private FileByFile(String javaVersion, List<File> classpath) {
      this.classpath = classpath;
      this.javaVersion = javaVersion;
    }

    @Override
    public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled, BiConsumer<InputFile, Result> action) {
      boolean successfullyCompleted = false;
      boolean cancelled = false;

      ExecutionTimeReport executionTimeReport = new ExecutionTimeReport();
      ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
      List<String> filesNames = StreamSupport.stream(inputFiles.spliterator(), false)
        .map(InputFile::toString)
        .collect(Collectors.toList());
      progressReport.start(filesNames);
      try {
        for (InputFile inputFile : inputFiles) {
          if (isCanceled.getAsBoolean()) {
            cancelled = true;
            break;
          }
          executionTimeReport.start(inputFile);

          Result result;
          PerformanceMeasure.Duration parseDuration = PerformanceMeasure.start("JParser");
          ASTParser astParser = JParser.createASTParser(javaVersion, classpath);
          try {
            result = new Result(JParser.parse(astParser, javaVersion, inputFile.filename(), inputFile.contents()));
          } catch (Exception e) {
            result = new Result(e);
          } finally {
            parseDuration.stop();
          }

          action.accept(inputFile, result);

          executionTimeReport.end();
          progressReport.nextFile();
        }
        successfullyCompleted = !cancelled;
      } finally {
        if (successfullyCompleted) {
          progressReport.stop();
        } else {
          progressReport.cancel();
        }
        executionTimeReport.report();
      }
    }
  }
}
