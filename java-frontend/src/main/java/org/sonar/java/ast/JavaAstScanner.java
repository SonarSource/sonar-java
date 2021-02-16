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
package org.sonar.java.ast;

import com.sonar.sslr.api.RecognitionException;
import java.io.InterruptedIOException;
import java.time.Clock;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.AnalysisException;
import org.sonar.java.ExecutionTimeReport;
import org.sonar.java.PerformanceMeasure;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.analyzer.commons.ProgressReport;

public class JavaAstScanner {
  private static final Logger LOG = Loggers.get(JavaAstScanner.class);

  private static final String LOG_ERROR_STACKOVERFLOW = "A stack overflow error occurred while analyzing file: '%s'";
  private static final String LOG_ERROR_UNABLE_TO_PARSE_FILE = "Unable to parse source file : '%s'";
  private static final String LOG_WARN_MISCONFIGURED_JAVA_VERSION = "Analyzing '%s' file with misconfigured Java version."
    + " Please check that property '%s' is correctly configured (currently set to: %d) or exclude 'module-info.java' files from analysis."
    + " Such files only exist in Java9+ projects.";

  private final SonarComponents sonarComponents;
  private VisitorsBridge visitor;
  private boolean reportedMisconfiguredVersion = false;

  public JavaAstScanner(@Nullable SonarComponents sonarComponents) {
    this.sonarComponents = sonarComponents;
  }

  public void scan(Iterable<? extends InputFile> inputFiles) {
    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(StreamSupport.stream(inputFiles.spliterator(), false).map(InputFile::toString).collect(Collectors.toList()));

    boolean successfullyCompleted = false;
    boolean cancelled = false;
    ExecutionTimeReport executionTimeReport = new ExecutionTimeReport(Clock.systemUTC());
    try {
      for (InputFile inputFile : inputFiles) {
        if (analysisCancelled()) {
          cancelled = true;
          break;
        }
        executionTimeReport.start(inputFile.toString());
        simpleScan(inputFile);
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
      visitor.endOfAnalysis();
      logUndefinedTypes();
    }
  }

  private void logUndefinedTypes() {
    if (sonarComponents != null) {
      sonarComponents.logUndefinedTypes();
    }
  }

  private boolean analysisCancelled() {
    return sonarComponents != null && sonarComponents.analysisCancelled();
  }

  private void simpleScan(InputFile inputFile) {
    visitor.setCurrentFile(inputFile);
    try {
      String version;
      JavaVersion javaVersion = visitor.getJavaVersion();
      if (javaVersion == null || javaVersion.asInt() < 0) {
        version = /* default */ JParser.MAXIMUM_SUPPORTED_JAVA_VERSION;
      } else if ("module-info.java".equals(inputFile.filename()) && javaVersion.asInt() <= 8) {
        logMisconfiguredVersion(inputFile, javaVersion);
        version = /* default */ JParser.MAXIMUM_SUPPORTED_JAVA_VERSION;
      } else {
        version = Integer.toString(javaVersion.asInt());
      }
      PerformanceMeasure.Duration parseDuration = PerformanceMeasure.start("JParser");
      JavaTree.CompilationUnitTreeImpl ast = (JavaTree.CompilationUnitTreeImpl) JParser.parse(
        version,
        inputFile.filename(),
        inputFile.contents(),
        visitor.getClasspath()
      );
      parseDuration.stop();
      visitor.visitFile(ast);
      collectUndefinedTypes(ast.sema.undefinedTypes());
      // release environment used for semantic resolution
      ast.sema.cleanupEnvironment();
    } catch (RecognitionException e) {
      checkInterrupted(e);
      LOG.error(String.format(LOG_ERROR_UNABLE_TO_PARSE_FILE, inputFile));
      LOG.error(e.getMessage());

      parseErrorWalkAndVisit(e, inputFile);
    } catch (AnalysisException e) {
      throw e;
    } catch (Exception e) {
      checkInterrupted(e);
      interruptIfFailFast(e, inputFile);
    } catch (StackOverflowError error) {
      LOG.error(String.format(LOG_ERROR_STACKOVERFLOW, inputFile), error);
      throw error;
    }
  }

  private void collectUndefinedTypes(Set<String> undefinedTypes) {
    if (sonarComponents != null) {
      sonarComponents.collectUndefinedTypes(undefinedTypes);
    }
  }

  void logMisconfiguredVersion(InputFile inputFile, JavaVersion javaVersion) {
    if (!reportedMisconfiguredVersion) {
      LOG.warn(String.format(LOG_WARN_MISCONFIGURED_JAVA_VERSION, inputFile, JavaVersion.SOURCE_VERSION, javaVersion.asInt()));
      reportedMisconfiguredVersion = true;
    }
  }

  private void interruptIfFailFast(Exception e, InputFile inputFile) {
    if (sonarComponents != null && sonarComponents.shouldFailAnalysisOnException()) {
      throw new AnalysisException(getAnalysisExceptionMessage(inputFile), e);
    }
  }

  private static void checkInterrupted(Exception e) {
    Throwable cause = ExceptionUtils.getRootCause(e);
    if (cause instanceof InterruptedException || cause instanceof InterruptedIOException) {
      throw new AnalysisException("Analysis cancelled", e);
    }
  }

  private void parseErrorWalkAndVisit(RecognitionException e, InputFile inputFile) {
    try {
      visitor.processRecognitionException(e, inputFile);
    } catch (Exception e2) {
      throw new AnalysisException(getAnalysisExceptionMessage(inputFile), e2);
    }
  }

  private static String getAnalysisExceptionMessage(InputFile file) {
    return String.format("Unable to analyze file : '%s'", file);
  }

  public void setVisitorBridge(VisitorsBridge visitor) {
    this.visitor = visitor;
  }

  @VisibleForTesting
  public static void scanSingleFileForTests(InputFile file, VisitorsBridge visitorsBridge) {
    scanSingleFileForTests(file, visitorsBridge, new JavaVersionImpl(), null);
  }

  @VisibleForTesting
  public static void scanSingleFileForTests(InputFile inputFile, VisitorsBridge visitorsBridge, JavaVersion javaVersion, @Nullable SonarComponents sonarComponents) {
    JavaAstScanner astScanner = new JavaAstScanner(sonarComponents);
    visitorsBridge.setJavaVersion(javaVersion);
    astScanner.setVisitorBridge(visitorsBridge);
    astScanner.scan(Collections.singleton(inputFile));
  }

}
