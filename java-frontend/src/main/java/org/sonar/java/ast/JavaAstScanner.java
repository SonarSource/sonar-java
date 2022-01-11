/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.io.File;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.AnalysisException;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaVersion;

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

  public List<File> getClasspath() {
    return visitor.getClasspath();
  }

  public void scan(Iterable<? extends InputFile> inputFiles) {
    List<InputFile> filesNames = filterModuleInfo(inputFiles).collect(Collectors.toList());
    try {
      JParserConfig.Mode.FILE_BY_FILE
        .create(JParserConfig.effectiveJavaVersion(visitor.getJavaVersion()), visitor.getClasspath())
        .parse(filesNames,
          this::analysisCancelled,
          (i, r) -> simpleScan(i, r, JavaAstScanner::cleanUpAst));
    } finally {
      endOfAnalysis();
    }
  }

  public <T extends InputFile> Stream<T> filterModuleInfo(Iterable<T> inputFiles) {
    JavaVersion javaVersion = visitor.getJavaVersion();
    return StreamSupport.stream(inputFiles.spliterator(), false)
      .filter(file -> {
        if (("module-info.java".equals(file.filename())) && !javaVersion.isNotSet() && javaVersion.asInt() <= 8) {
          // When the java version is not set, we use the maximum version supported, able to parse module info.
          logMisconfiguredVersion("module-info.java", javaVersion);
          return false;
        }
        return true;
      });
  }

  public void endOfAnalysis() {
    visitor.endOfAnalysis();
    logUndefinedTypes();
  }

  private void logUndefinedTypes() {
    if (sonarComponents != null) {
      sonarComponents.logUndefinedTypes();
    }
  }

  private boolean analysisCancelled() {
    return sonarComponents != null && sonarComponents.analysisCancelled();
  }

  public void simpleScan(InputFile inputFile, JParserConfig.Result result, Consumer<JavaTree.CompilationUnitTreeImpl> cleanUp) {
    visitor.setCurrentFile(inputFile);
    try {
      JavaTree.CompilationUnitTreeImpl ast = result.get();
      visitor.visitFile(ast);
      collectUndefinedTypes(ast.sema.undefinedTypes());
      cleanUp.accept(ast);
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

  private static void cleanUpAst(JavaTree.CompilationUnitTreeImpl ast) {
    // release environment used for semantic resolution
    ast.sema.cleanupEnvironment();
  }

  private void collectUndefinedTypes(Set<JProblem> undefinedTypes) {
    if (sonarComponents != null) {
      sonarComponents.collectUndefinedTypes(undefinedTypes);
    }
  }

  void logMisconfiguredVersion(String inputFile, JavaVersion javaVersion) {
    if (!reportedMisconfiguredVersion) {
      LOG.warn(String.format(LOG_WARN_MISCONFIGURED_JAVA_VERSION, inputFile, JavaVersion.SOURCE_VERSION, javaVersion.asInt()));
      reportedMisconfiguredVersion = true;
    }
  }

  private void interruptIfFailFast(Exception e, InputFile inputFile) {
    if (shouldFailAnalysis()) {
      throw new AnalysisException(getAnalysisExceptionMessage(inputFile), e);
    }
  }

  public boolean shouldFailAnalysis() {
    return sonarComponents != null && sonarComponents.shouldFailAnalysisOnException();
  }

  public void checkInterrupted(Exception e) {
    Throwable cause = ExceptionUtils.getRootCause(e);
    if (cause instanceof InterruptedException
      || cause instanceof InterruptedIOException
      || cause instanceof CancellationException
      || analysisCancelled()) {
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
