/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.sonar.sslr.api.RecognitionException;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.AnalysisException;
import org.sonar.java.SonarComponents;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.ProgressReport;

public class JavaAstScanner {
  private static final Logger LOG = Loggers.get(JavaAstScanner.class);

  private final SonarComponents sonarComponents;
  private VisitorsBridge visitor;

  public JavaAstScanner(@Nullable SonarComponents sonarComponents) {
    this.sonarComponents = sonarComponents;
  }

  public void scan(Iterable<InputFile> inputFiles) {
    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(Iterables.transform(inputFiles, InputFile::toString));

    boolean successfullyCompleted = false;
    boolean cancelled = false;
    try {
      for (InputFile inputFile : inputFiles) {
        if (analysisCancelled()) {
          cancelled = true;
          break;
        }
        simpleScan(inputFile);
        progressReport.nextFile();
      }
      successfullyCompleted = !cancelled;
    } finally {
      if (successfullyCompleted) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
      visitor.endOfAnalysis();
    }
  }

  private boolean analysisCancelled() {
    return sonarComponents != null && sonarComponents.analysisCancelled();
  }

  private void simpleScan(InputFile inputFile) {
    visitor.setCurrentFile(inputFile);
    try {
      String fileContent = inputFile.contents();
      final String version;
      if (visitor.getJavaVersion() == null || visitor.getJavaVersion().asInt() < 0) {
        version = /* default */ "12";
      } else {
        version = Integer.toString(visitor.getJavaVersion().asInt());
      }
      Tree ast = JParser.parse(
        version,
        inputFile.filename(),
        fileContent,
        Collections.emptyList()
      );
      visitor.visitFile(ast);
    } catch (RecognitionException e) {
      checkInterrupted(e);
      LOG.error(String.format("Unable to parse source file : '%s'", inputFile));
      LOG.error(e.getMessage());

      parseErrorWalkAndVisit(e, inputFile);
    } catch (Exception e) {
      checkInterrupted(e);
      throw new AnalysisException(getAnalysisExceptionMessage(inputFile), e);
    } catch (StackOverflowError error) {
      LOG.error(String.format("A stack overflow error occurred while analyzing file: '%s'", inputFile), error);
      throw error;
    }
  }

  private static void checkInterrupted(Exception e) {
    Throwable cause = Throwables.getRootCause(e);
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
    return String.format("SonarQube is unable to analyze file : '%s'", file);
  }

  public void setVisitorBridge(VisitorsBridge visitor) {
    this.visitor = visitor;
  }

  @VisibleForTesting
  public static void scanSingleFileForTests(InputFile file, VisitorsBridge visitorsBridge) {
    scanSingleFileForTests(file, visitorsBridge, new JavaVersionImpl());
  }

  @VisibleForTesting
  public static void scanSingleFileForTests(InputFile inputFile, VisitorsBridge visitorsBridge, JavaVersion javaVersion) {
    JavaAstScanner astScanner = new JavaAstScanner(null);
    visitorsBridge.setJavaVersion(javaVersion);
    astScanner.setVisitorBridge(visitorsBridge);
    astScanner.scan(Collections.singleton(inputFile));
  }

}
