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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.ExecutionTimeReport;
import org.sonar.java.ProgressMonitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.analyzer.commons.ProgressReport;
import org.sonarsource.performance.measure.PerformanceMeasure;

public abstract class JParserConfig {

  public static final String MAXIMUM_SUPPORTED_JAVA_VERSION = Integer.toString(JavaVersionImpl.MAX_SUPPORTED);

  private static final Logger LOG = Loggers.get(JParserConfig.class);

  private static final String MAXIMUM_ECJ_WARNINGS = "42000";
  private static final Set<String> JRE_JARS = new HashSet<>(Arrays.asList("rt.jar", "jrt-fs.jar", "android.jar"));

  final String javaVersion;
  final List<File> classpath;

  private JParserConfig(String javaVersion, List<File> classpath) {
    this.javaVersion = javaVersion;
    this.classpath = classpath;
  }

  public abstract void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled, BiConsumer<InputFile, Result> action);

  public enum Mode {
    BATCH(Batch::new),
    FILE_BY_FILE(FileByFile::new);

    private final BiFunction<String, List<File>, JParserConfig> supplier;

    Mode(BiFunction<String, List<File>, JParserConfig> supplier) {
      this.supplier = supplier;
    }

    public JParserConfig create(String javaVersion, List<File> classpath) {
      return supplier.apply(javaVersion, classpath);
    }
  }

  public static class Result {
    private final Exception e;
    private final JavaTree.CompilationUnitTreeImpl t;

    private Result(Exception e) {
      this.e = e;
      this.t = null;
    }

    private Result(JavaTree.CompilationUnitTreeImpl t) {
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

  public ASTParser astParser() {
    ASTParser astParser = ASTParser.newParser(AST.JLS_Latest);
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_COMPLIANCE, javaVersion);
    options.put(JavaCore.COMPILER_SOURCE, javaVersion);
    options.put(JavaCore.COMPILER_PB_MAX_PER_UNIT, MAXIMUM_ECJ_WARNINGS);
    if (MAXIMUM_SUPPORTED_JAVA_VERSION.equals(javaVersion)) {
      options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    }
    // enabling all supported compiler warnings
    JProblem.Type.compilerOptions()
      .forEach(option -> options.put(option, "warning"));

    astParser.setCompilerOptions(options);

    boolean includeRunningVMBootclasspath = classpath.stream()
      .noneMatch(f -> JRE_JARS.contains(f.getName()));

    astParser.setEnvironment(classpath.stream()
      .map(File::getAbsolutePath)
      .toArray(String[]::new), new String[] {}, new String[] {}, includeRunningVMBootclasspath);

    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);

    return astParser;
  }

  private static class Batch extends JParserConfig {

    private Batch(String javaVersion, List<File> classpath) {
      super(javaVersion, classpath);
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
        astParser().createASTs(sourceFilePaths.toArray(new String[0]), encodings.toArray(new String[0]), new String[0], new FileASTRequestor() {
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

  private static class FileByFile extends JParserConfig {

    private FileByFile(String javaVersion, List<File> classpath) {
      super(javaVersion, classpath);
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
          try {
            result = new Result(JParser.parse(astParser(), javaVersion, inputFile.filename(), inputFile.contents()));
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

  public static String effectiveJavaVersion(@Nullable JavaVersion javaVersion) {
    if (javaVersion == null || javaVersion.isNotSet()) {
      return JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    }
    return Integer.toString(javaVersion.asInt());
  }

}
