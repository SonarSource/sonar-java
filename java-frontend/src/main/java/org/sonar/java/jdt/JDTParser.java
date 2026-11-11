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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisException;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.ExecutionTimeReport;
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
  private static final Set<String> JRE_JARS = new HashSet<>(Arrays.asList("rt.jar", "jrt-fs.jar", "android.jar"));

  final JavaVersion javaVersion;
  final List<File> classpath;
  final boolean shouldIgnoreUnnamedModuleForSplitPackage;

  public static JParser create(JParser.Mode mode, JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
    if (shouldIgnoreUnnamedModuleForSplitPackage) {
      LOG.info("The Java analyzer will ignore the unnamed module for split packages.");
    }
    return switch (mode) {
      case BATCH -> new Batch(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
      case FILE_BY_FILE -> new FileByFile(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
    };
  }

  @VisibleForTesting
  static boolean shouldEnablePreviewFlag(JavaVersion currentVersion) {
    return currentVersion.arePreviewFeaturesEnabled();
  }

  private JDTParser(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
    this.javaVersion = javaVersion;
    this.classpath = classpath;
    this.shouldIgnoreUnnamedModuleForSplitPackage = shouldIgnoreUnnamedModuleForSplitPackage;
  }

  protected ASTParser jdtAstParser() {
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
    JDTProblem.compilerOptions()
      .forEach(option -> options.put(option, "warning"));

    astParser.setCompilerOptions(options);

    boolean includeRunningVMBootclasspath = classpath.stream()
      .noneMatch(f -> JRE_JARS.contains(f.getName()));

    astParser.setEnvironment(classpath.stream()
      .map(File::getAbsolutePath)
      .toArray(String[]::new), new String[]{}, new String[]{}, includeRunningVMBootclasspath);

    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);

    return astParser;
  }

  @VisibleForTesting
  static class Batch extends JDTParser {

    Batch(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      super(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
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
        ASTParser astParser = jdtAstParser();
        astParser.createASTs(sourceFilePaths.toArray(new String[0]), encodings.toArray(new String[0]), new String[0], new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit ast) {
            PerformanceMeasure.Duration convertDuration = PerformanceMeasure.start("Convert");
            InputFile inputFile = inputs.get(new File(sourceFilePath));
            executionTimeReport.start(inputFile);
            JParseResult result;
            try {
              result = JParseResult.success(JDTAstConverter.convert(javaVersion.effectiveJavaVersionAsString(), inputFile.filename(), inputFile.contents(), ast));
            } catch (Exception e) {
              result = JParseResult.failure(e);
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
      FileByFile fileByFile = new FileByFile(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
      fileByFile.parse(inputFiles, isCanceled, analysisProgress, action);
    }
  }

  private static class FileByFile extends JDTParser {

    private FileByFile(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      super(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
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
      try {
        astNode = (CompilationUnit) parser.createAST(null);
      } catch (Exception e) {
        LOG.error("ECJ: Unable to parse file", e);
        throw new RecognitionException(-1, "ECJ: Unable to parse file.", e);
      }

      return JDTAstConverter.convert(version, unitName, source, astNode);
    }
  }

}
