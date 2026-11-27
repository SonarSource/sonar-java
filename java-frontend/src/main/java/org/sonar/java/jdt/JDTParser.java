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
package org.sonar.java.jdt;

import com.sonar.sslr.api.RecognitionException;
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
import org.sonar.java.FrontendBenchLog;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.analyzer.commons.ProgressReport;
import org.sonarsource.performance.measure.PerformanceMeasure;

public abstract class JDTParser implements JParser {

  private static final Logger LOG = LoggerFactory.getLogger(JDTParser.class);
  private static final String MAXIMUM_ECJ_WARNINGS = "42000";
  private static final String JRT_FS_JAR = "jrt-fs.jar";
  private static final Set<String> JRE_JARS = new HashSet<>(Arrays.asList("rt.jar", JRT_FS_JAR, "android.jar"));

  final JavaVersion javaVersion;
  final List<File> classpath;
  final boolean shouldIgnoreUnnamedModuleForSplitPackage;
  final boolean shouldUseJdkReleaseOption;

  public static JParser create(JParser.Mode mode, JavaVersion javaVersion, List<File> classpath,
                               boolean shouldIgnoreUnnamedModuleForSplitPackage, boolean shouldUseJdkReleaseOption) {
    if (shouldIgnoreUnnamedModuleForSplitPackage) {
      LOG.info("The Java analyzer will ignore the unnamed module for split packages.");
    }
    return switch (mode) {
      case BATCH -> new Batch(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
      case FILE_BY_FILE -> new FileByFile(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
    };
  }

  @VisibleForTesting
  static boolean shouldEnablePreviewFlag(JavaVersion currentVersion) {
    return currentVersion.arePreviewFeaturesEnabled();
  }

  private JDTParser(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage, boolean shouldUseJdkReleaseOption) {
    this.javaVersion = javaVersion;
    this.classpath = classpath;
    this.shouldIgnoreUnnamedModuleForSplitPackage = shouldIgnoreUnnamedModuleForSplitPackage;
    this.shouldUseJdkReleaseOption = shouldUseJdkReleaseOption;
  }

  protected ASTParser jdtAstParser() {
    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    Map<String, String> options = new HashMap<>(JavaCore.getOptions());

    String javaSourceVersion = javaVersion.effectiveJavaVersionAsString();
    JavaCore.setComplianceOptions(javaSourceVersion, options);
    options.put(JavaCore.COMPILER_PB_MAX_PER_UNIT, MAXIMUM_ECJ_WARNINGS);
    if (shouldUseJdkReleaseOption) {
      // This is used by jdt to set the specific --release of the JDK APIs, when applicable
      options.put(SystemReleaseClasspath.OPTION_RELEASE, javaSourceVersion);
    }
    if (shouldIgnoreUnnamedModuleForSplitPackage) {
      options.put(JavaCore.COMPILER_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE, "enabled");
    }
    if (shouldEnablePreviewFlag(javaVersion)) {
      options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    }
    // enabling all supported compiler warnings
    JDTProblem.compilerOptions().forEach(option -> options.put(option, "warning"));

    List<File> environment = classpath;

    // IF the classpath has no JRE libraries, we tell jdt to rely on the running one and also try to set --release, if applicable
    boolean includeRunningVMBootclasspath = environment.stream().noneMatch(f -> JRE_JARS.contains(f.getName()));

    // If we want to target a specific release for the JDK APIs, and there are already modular JDK libraries in the classpath
    if (shouldUseJdkReleaseOption && classpathContainsModularJdk()) {
      // We remove those libraries from the classpath, and tell jdt which JDK home we need to use.
      // A view of that JDK home with the correct API level will be provided by jdt
      environment = removeJdkLibsAndProvideJdtWithJavaHome(options);
    }

    astParser.setCompilerOptions(options);

    astParser.setEnvironment(environment.stream()
      .map(File::getAbsolutePath)
      .toArray(String[]::new), new String[]{}, new String[]{}, includeRunningVMBootclasspath);

    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);

    return astParser;
  }

  /**
   * Removes the runtime image of a modular JDK ({@code <jdk>/lib/jrt-fs.jar}, added to the classpath for
   * {@code sonar.java.jdkHome}), and provides JDT with the JAVA_HOME where it should apply the --release option
   * to only have a specific version of the APIs in the classpath. (SystemReleaseClasspath.resolveTargetRelease)
   *
   * @return the original classpath, without the jrt-fs.jar
   */
  @VisibleForTesting
  List<File> removeJdkLibsAndProvideJdtWithJavaHome(Map<String, String> options) {
    for (File runtimeImage : classpath) {
      Optional<File> jdkHome = jdkHomeOfRuntimeImage(runtimeImage);
      if (jdkHome.isPresent()) {
        options.put(SystemReleaseClasspath.OPTION_JAVA_HOME, jdkHome.get().getAbsolutePath());
        return classpath.stream().filter(f -> !f.equals(runtimeImage)).toList();
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

  private boolean classpathContainsModularJdk() {
    return classpath.stream().anyMatch(f -> JRT_FS_JAR.equals(f.getName()));
  }

  @VisibleForTesting
  static class Batch extends JDTParser {

    Batch(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage, boolean shouldUseJdkReleaseOption) {
      super(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
    }

    @Override
    public void parse(Iterable<InputFile> inputFiles, BooleanSupplier isCanceled, AnalysisProgress analysisProgress, BiConsumer<InputFile, JParseResult> action) {
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
        long setupStart = System.nanoTime();
        ASTParser astParser = jdtAstParser();
        FrontendBenchLog.log("ecj.setup", "cp_entries", classpath.size(), "release", shouldUseJdkReleaseOption, "dur_us", FrontendBenchLog.micros(setupStart));
        long createStart = System.nanoTime();
        FrontendBenchLog.log("ecj.createASTs.start", "files", sourceFilePaths.size());
        var requestor = new FileASTRequestor() {
          long previousAcceptEnd = createStart;
          long firstAcceptUs = -1L;
          int accepted = 0;

          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit ast) {
            long acceptStart = System.nanoTime();
            FrontendBenchLog.resetFile();
            FrontendBenchLog.putFileField("first", accepted == 0);
            FrontendBenchLog.putFileField("ecj_us", (acceptStart - previousAcceptEnd) / 1_000L);
            if (accepted == 0) {
              firstAcceptUs = (acceptStart - createStart) / 1_000L;
            }
            accepted++;
            PerformanceMeasure.Duration convertDuration = PerformanceMeasure.start("Convert");
            InputFile inputFile = inputs.get(new File(sourceFilePath));
            executionTimeReport.start(inputFile);
            JParseResult result;
            long convertStart = System.nanoTime();
            try {
              result = JParseResult.success(JDTAstConverter.convert(javaVersion.effectiveJavaVersionAsString(), inputFile.filename(), inputFile.contents(), ast));
            } catch (Exception e) {
              result = JParseResult.failure(e);
            }
            FrontendBenchLog.putFileField("convert_us", FrontendBenchLog.micros(convertStart));
            convertDuration.stop();
            PerformanceMeasure.Duration analyzeDuration = PerformanceMeasure.start("Analyze");
            action.accept(inputFile, result);

            notYetAnalyzedFiles.remove(inputFile);
            executionTimeReport.end();
            analyzeDuration.stop();
            previousAcceptEnd = System.nanoTime();
          }
        };
        astParser.createASTs(sourceFilePaths.toArray(new String[0]), encodings.toArray(new String[0]), new String[0], requestor, monitor);
        FrontendBenchLog.log("ecj.createASTs.end",
          "files", sourceFilePaths.size(),
          "accepted", requestor.accepted,
          "not_accepted", notYetAnalyzedFiles.size(),
          "first_accept_us", requestor.firstAcceptUs,
          "tail_us", FrontendBenchLog.micros(requestor.previousAcceptEnd),
          "dur_us", FrontendBenchLog.micros(createStart));
        if (!notYetAnalyzedFiles.isEmpty()) {
          String message = String.format("%d/%d files were not analyzed by the batch mode", notYetAnalyzedFiles.size(), sourceFilePaths.size());
          throw new AnalysisException(message);
        }
      } catch (OperationCanceledException e) {
        throw e;
      } catch (RuntimeException e) {
        LOG.warn("Unexpected {}: {}", e.getClass().getSimpleName(), e.getMessage());
        FrontendBenchLog.log("ecj.fallback", "files", notYetAnalyzedFiles.size(), "exception", e.getClass().getSimpleName());
        if (!notYetAnalyzedFiles.isEmpty()) {
          fallbackToFileByFileMode(notYetAnalyzedFiles.stream().toList(), isCanceled, analysisProgress, action);
        }
      } finally {
        batchPerformance.stop();
        // ExecutionTimeReport will not include the parsing time by file when using batch mode.
        executionTimeReport.reportAsBatch();
        monitor.done();
      }
    }

    private void fallbackToFileByFileMode(List<InputFile> inputFiles, BooleanSupplier isCanceled, AnalysisProgress analysisProgress, BiConsumer<InputFile, JParseResult> action) {
      LOG.warn("Fallback to file by file analysis for {} files", inputFiles.size());
      FileByFile fileByFile = new FileByFile(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
      fileByFile.parse(inputFiles, isCanceled, analysisProgress, action);
    }
  }

  private static class FileByFile extends JDTParser {

    private FileByFile(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage,
      boolean shouldUseJdkReleaseOption) {
      super(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
    }

    @Override
    public void parse(Iterable<InputFile> inputFiles, BooleanSupplier isCanceled, AnalysisProgress analysisProgress, BiConsumer<InputFile, JParseResult> action) {
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
          parse(inputFile, javaVersion, action);
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

    private void parse(InputFile inputFile, JavaVersion javaVersion, BiConsumer<InputFile, JParseResult> action) {
      JParseResult result;
      FrontendBenchLog.resetFile();
      PerformanceMeasure.Duration parseDuration = PerformanceMeasure.start("JParser");
      try {
        result = JParseResult.success(parse(javaVersion.effectiveJavaVersionAsString(), inputFile.filename(), inputFile.contents()));
      } catch (Exception e) {
        result = JParseResult.failure(e);
      } finally {
        parseDuration.stop();
      }
      action.accept(inputFile, result);
    }

    private JavaTree.CompilationUnitTreeImpl parse(String version, String unitName, String source) {
      ASTParser parser = jdtAstParser();
      parser.setUnitName(unitName);
      parser.setSource(source.toCharArray());

      CompilationUnit astNode;
      long createAstStart = System.nanoTime();
      try {
        astNode = (CompilationUnit) parser.createAST(null);
      } catch (Exception e) {
        LOG.error("ECJ: Unable to parse file", e);
        throw new RecognitionException(-1, "ECJ: Unable to parse file.", e);
      } finally {
        FrontendBenchLog.putFileField("create_ast_us", FrontendBenchLog.micros(createAstStart));
      }

      long convertStart = System.nanoTime();
      try {
        return JDTAstConverter.convert(version, unitName, source, astNode);
      } finally {
        FrontendBenchLog.putFileField("convert_us", FrontendBenchLog.micros(convertStart));
      }
    }
  }

}
