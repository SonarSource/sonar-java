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
package org.sonar.java.model;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.SystemReleaseClasspath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisException;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.ExecutionTimeReport;
import org.sonar.java.ProgressMonitor;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.analyzer.commons.ProgressReport;
import org.sonarsource.performance.measure.PerformanceMeasure;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.StreamSupport;

public abstract class JParserConfig {

  public static final JavaVersion MAXIMUM_SUPPORTED_JAVA_VERSION = new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED, true);

  private static final Logger LOG = LoggerFactory.getLogger(JParserConfig.class);

  private static final String MAXIMUM_ECJ_WARNINGS = "42000";
  private static final String JRT_FS_JAR = "jrt-fs.jar";
  private static final Set<String> NON_MODULAR_JRE_JAR = new HashSet<>(Arrays.asList("rt.jar", "android.jar"));
  private static final Set<String> JRE_JARS = new HashSet<>(Arrays.asList("rt.jar", JRT_FS_JAR, "android.jar"));

  final JavaVersion javaVersion;
  final List<File> classpath;
  final boolean shouldIgnoreUnnamedModuleForSplitPackage;
  final boolean shouldUseJdkReleaseOption;

  private JParserConfig(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage,
    boolean shouldUseJdkReleaseOption) {
    this.javaVersion = javaVersion;
    this.classpath = classpath;
    this.shouldIgnoreUnnamedModuleForSplitPackage = shouldIgnoreUnnamedModuleForSplitPackage;
    this.shouldUseJdkReleaseOption = shouldUseJdkReleaseOption;
  }

  public abstract void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
                             AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action);

  public enum Mode {
    BATCH(Batch::new),
    FILE_BY_FILE(FileByFile::new);

    private final ParserConfigConstructor supplier;

    Mode(ParserConfigConstructor supplier) {
      this.supplier = supplier;
    }

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath) {
      return create(javaVersion, classpath, false);
    }

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      return create(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, false);
    }

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage,
      boolean shouldUseJdkReleaseOption) {
      if (shouldIgnoreUnnamedModuleForSplitPackage) {
        LOG.info("The Java analyzer will ignore the unnamed module for split packages.");
      }
      return supplier.apply(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
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
    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    Map<String, String> options = new HashMap<>(JavaCore.getOptions());
    JavaCore.setComplianceOptions(javaVersion.effectiveJavaVersionAsString(), options);
    options.put(JavaCore.COMPILER_PB_MAX_PER_UNIT, MAXIMUM_ECJ_WARNINGS);
    if (shouldIgnoreUnnamedModuleForSplitPackage) {
      options.put(JavaCore.COMPILER_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE, "enabled");
    }
    if (shouldEnablePreviewFlag(javaVersion)) {
      options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    }
    // enabling all supported compiler warnings
    JProblem.compilerOptions()
      .forEach(option -> options.put(option, "warning"));

    List<File> environment = classpath;
    if (shouldUseJdkReleaseOption) {
      options.put(SystemReleaseClasspath.OPTION_RELEASE, javaVersion.effectiveJavaVersionAsString());
      environment = useReleaseApiOfConfiguredJdk(options);
    }

    astParser.setCompilerOptions(options);

    boolean includeRunningVMBootclasspath = environment.stream()
      .noneMatch(f -> JRE_JARS.contains(f.getName()));

    astParser.setEnvironment(environment.stream()
      .map(File::getAbsolutePath)
      .toArray(String[]::new), new String[]{}, new String[]{}, includeRunningVMBootclasspath);

    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);

    return astParser;
  }

  /**
   * When the classpath carries the runtime image of a modular JDK (the {@code lib/jrt-fs.jar} added for
   * {@code sonar.java.jdkHome}), ECJ would see the whole API of that JDK whatever the analyzed Java version is.
   * If the requested release is older than that JDK, the image is removed from the classpath and the JDK is
   * registered in {@code options} as the provider of the release API, which {@link SystemReleaseClasspath} then
   * serves from its {@code ct.sym}.
   *
   * <p>Nothing changes when another JDK-like entry is present ({@code rt.jar}, {@code android.jar}), since it is
   * the actual platform to analyze against, or when the release cannot be served by that JDK.
   *
   * @param options the compiler options, to which the JDK home is added when the release API is used
   * @return the classpath to give to ECJ
   */
  @VisibleForTesting
  List<File> useReleaseApiOfConfiguredJdk(Map<String, String> options) {
    boolean hasOtherPlatformJar = classpath.stream()
      .anyMatch(f -> NON_MODULAR_JRE_JAR.contains(f.getName()));
    if (hasOtherPlatformJar) {
      return classpath;
    }
    for (File runtimeImage : classpath) {
      Optional<File> jdkHome = jdkHomeOfRuntimeImage(runtimeImage);
      if (jdkHome.isPresent()) {
        Map<String, String> candidate = new HashMap<>(options);
        candidate.put(SystemReleaseClasspath.OPTION_JAVA_HOME, jdkHome.get().getAbsolutePath());
        if (SystemReleaseClasspath.isApplicable(candidate)) {
          options.put(SystemReleaseClasspath.OPTION_JAVA_HOME, jdkHome.get().getAbsolutePath());
          return classpath.stream().filter(f -> !f.equals(runtimeImage)).toList();
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("The API of Java {} is not served from the JDK '{}': using the API of that JDK as is.",
            javaVersion.effectiveJavaVersionAsString(), jdkHome.get());
        }
        return classpath;
      }
    }
    return classpath;
  }

  private static Optional<File> jdkHomeOfRuntimeImage(File file) {
    File lib = file.getParentFile();
    if (JRT_FS_JAR.equals(file.getName()) && lib != null && "lib".equals(lib.getName())) {
      return Optional.ofNullable(lib.getParentFile());
    }
    return Optional.empty();
  }

  @VisibleForTesting
  static class Batch extends JParserConfig {

    Batch(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage, boolean shouldUseJdkReleaseOption) {
      super(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
    }

    @Override
    public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
                      AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action) {

      List<String> sourceFilePaths = new ArrayList<>();
      Set<InputFile> notYetAnalyzedFiles = new LinkedHashSet<>();
      List<String> encodings = new ArrayList<>();
      Map<File, InputFile> inputs = new HashMap<>();
      for (InputFile inputFile : inputFiles) {
        String sourceFilePath = inputFile.absolutePath();
        inputs.put(new File(sourceFilePath), inputFile);
        sourceFilePaths.add(sourceFilePath);
        encodings.add(inputFile.charset().name());
        notYetAnalyzedFiles.add(inputFile);
      }

      ExecutionTimeReport executionTimeReport = new ExecutionTimeReport();
      ProgressMonitor monitor = new ProgressMonitor(isCanceled, analysisProgress);
      PerformanceMeasure.Duration batchPerformance = PerformanceMeasure.start("ParseAsBatch");
      try {
        astParser().createASTs(sourceFilePaths.toArray(new String[0]), encodings.toArray(new String[0]), new String[0], new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit ast) {
            PerformanceMeasure.Duration convertDuration = PerformanceMeasure.start("Convert");
            InputFile inputFile = inputs.get(new File(sourceFilePath));
            executionTimeReport.start(inputFile);
            Result result;
            try {
              result = new Result(JParser.convert(javaVersion.effectiveJavaVersionAsString(), inputFile.filename(), inputFile.contents(), ast));
            } catch (Exception e) {
              result = new Result(e);
            }
            convertDuration.stop();
            PerformanceMeasure.Duration analyzeDuration = PerformanceMeasure.start("Analyze");
            action.accept(inputFile, result);

            notYetAnalyzedFiles.remove(inputFile);
            executionTimeReport.end();
            analyzeDuration.stop();
          }
        }, monitor);
        if (!notYetAnalyzedFiles.isEmpty()) {
          String message = String.format("%d/%d files were not analyzed by the batch mode", notYetAnalyzedFiles.size(), sourceFilePaths.size());
          throw new AnalysisException(message);
        }
      } catch (OperationCanceledException e) {
        throw e;
      } catch (RuntimeException e) {
        LOG.warn("Unexpected {}: {}", e.getClass().getSimpleName(), e.getMessage());
        if (!notYetAnalyzedFiles.isEmpty()) {
          fallbackToFileByFileMode(notYetAnalyzedFiles.stream().toList(), isCanceled, action);
        }
      } finally {
        batchPerformance.stop();
        // ExecutionTimeReport will not include the parsing time by file when using batch mode.
        executionTimeReport.reportAsBatch();
        monitor.done();
      }
    }

    private void fallbackToFileByFileMode(List<InputFile> inputFiles, BooleanSupplier isCanceled, BiConsumer<InputFile, Result> action) {
      LOG.warn("Fallback to file by file analysis for {} files", inputFiles.size());
      for (InputFile inputFile : inputFiles) {
        if (isCanceled.getAsBoolean()) {
          break;
        }
        FileByFile.parse(astParser(), inputFile, javaVersion, action);
      }
    }

  }

  private static class FileByFile extends JParserConfig {

    private FileByFile(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage,
      boolean shouldUseJdkReleaseOption) {
      super(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
    }

    @Override
    public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
                      AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action) {
      boolean successfullyCompleted = false;
      boolean cancelled = false;

      ExecutionTimeReport executionTimeReport = new ExecutionTimeReport();
      ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
      List<String> filesNames = StreamSupport.stream(inputFiles.spliterator(), false)
        .map(InputFile::toString)
        .toList();
      progressReport.start(filesNames);
      try {
        for (InputFile inputFile : inputFiles) {
          if (isCanceled.getAsBoolean()) {
            cancelled = true;
            break;
          }
          executionTimeReport.start(inputFile);
          parse(astParser(), inputFile, javaVersion, action);
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

    private static void parse(ASTParser astParser, InputFile inputFile, JavaVersion javaVersion, BiConsumer<InputFile, Result> action) {
      Result result;
      PerformanceMeasure.Duration parseDuration = PerformanceMeasure.start("JParser");
      try {
        result = new Result(JParser.parse(astParser, javaVersion.effectiveJavaVersionAsString(), inputFile.filename(), inputFile.contents()));
      } catch (Exception e) {
        result = new Result(e);
      } finally {
        parseDuration.stop();
      }
      action.accept(inputFile, result);
    }
  }

  @VisibleForTesting
  static boolean shouldEnablePreviewFlag(JavaVersion currentVersion) {
    return currentVersion.arePreviewFeaturesEnabled();
  }

  @FunctionalInterface
  public interface ParserConfigConstructor {
    JParserConfig apply(JavaVersion version, List<File> files, Boolean shouldIgnoreUnnamedModuleForSplitPackage, Boolean shouldUseJdkReleaseOption);
  }

}
