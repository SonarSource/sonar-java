/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.typed.ActionParser;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.ProgressReport;
import org.sonar.squidbridge.api.AnalysisException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class JavaAstScanner {
  private static final Logger LOG = Loggers.get(JavaAstScanner.class);

  private final ActionParser<Tree> parser;
  private VisitorsBridge visitor;

  public JavaAstScanner(ActionParser<Tree> parser) {
    this.parser = parser;
  }

  /**
   * Takes parser and index from another instance of {@link JavaAstScanner}
   */
  public JavaAstScanner(JavaAstScanner astScanner) {
    this.parser = astScanner.parser;
  }

  public void scan(Iterable<File> files) {
    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(Lists.newArrayList(files));

    boolean successfullyCompleted = false;
    try {
      for (File file : files) {
        simpleScan(file);
        progressReport.nextFile();
      }
      successfullyCompleted = true;
    } finally {
      if (successfullyCompleted) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
    }
  }

  private void simpleScan(File file) {
    visitor.setCurrentFile(file);
    try {
      Tree ast = parser.parse(file);
      visitor.visitFile(ast);
    } catch (RecognitionException e) {
      checkInterrupted(e);
      LOG.error("Unable to parse source file : " + file.getAbsolutePath());
      LOG.error(e.getMessage());

      parseErrorWalkAndVisit(e, file);
    } catch (Exception e) {
      checkInterrupted(e);
      throw new AnalysisException(getAnalysisExceptionMessage(file), e);
    } catch (StackOverflowError error) {
      LOG.error("A stack overflow error occured while analyzing file: " + file.getAbsolutePath());
      throw error;
    }
  }

  private static void checkInterrupted(Exception e) {
    Throwable cause = Throwables.getRootCause(e);
    if (cause instanceof InterruptedException || cause instanceof InterruptedIOException) {
      throw new AnalysisException("Analysis cancelled", e);
    }
  }

  private void parseErrorWalkAndVisit(RecognitionException e, File file) {
    try {
      // Process the exception
      visitor.processRecognitionException(e, file);
    } catch (Exception e2) {
      throw new AnalysisException(getAnalysisExceptionMessage(file), e2);
    }
  }

  private static String getAnalysisExceptionMessage(File file) {
    return "SonarQube is unable to analyze file : '" + file.getAbsolutePath() + "'";
  }

  public void setVisitorBridge(VisitorsBridge visitor) {
    this.visitor = visitor;
  }

  @VisibleForTesting
  public static void scanSingleFileForTests(File file, VisitorsBridge visitorsBridge) {
    scanSingleFileForTests(file, visitorsBridge, new JavaConfiguration(Charset.forName("UTF-8")));
  }

  @VisibleForTesting
  public static void scanSingleFileForTests(File file, VisitorsBridge visitorsBridge, JavaConfiguration conf) {
    if (!file.isFile()) {
      throw new IllegalArgumentException("File '" + file + "' not found.");
    }
    JavaAstScanner scanner = create(conf, visitorsBridge);

    scanner.scan(Collections.singleton(file));
  }

  private static JavaAstScanner create(JavaConfiguration conf, @Nullable VisitorsBridge visitorsBridge) {
    JavaAstScanner astScanner = new JavaAstScanner(JavaParser.createParser(conf.getCharset()));
    if (visitorsBridge != null) {
      visitorsBridge.setCharset(conf.getCharset());
      visitorsBridge.setJavaVersion(conf.javaVersion());
      astScanner.setVisitorBridge(visitorsBridge);
    }
    return astScanner;
  }

}
