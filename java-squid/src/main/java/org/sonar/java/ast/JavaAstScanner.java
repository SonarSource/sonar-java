/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.VisitorContext;
import org.sonar.java.model.VisitorsBridge;
import com.sonar.sslr.api.typed.ActionParser;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.ProgressReport;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceCodeSearchEngine;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.indexer.SquidIndex;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class JavaAstScanner {

  private static final Logger LOG = LoggerFactory.getLogger(JavaAstScanner.class);

  private final SquidIndex index;
  private final ActionParser<Tree> parser;
  private VisitorsBridge visitor;

  public JavaAstScanner(ActionParser<Tree> parser) {
    this.parser = parser;
    this.index = new SquidIndex();
  }

  /**
   * Takes parser and index from another instance of {@link JavaAstScanner}
   */
  public JavaAstScanner(JavaAstScanner astScanner) {
    this.parser = astScanner.parser;
    this.index = astScanner.index;
  }

  public void scan(Iterable<File> files) {
    SourceProject project = new SourceProject("Java Project");
    index.index(project);
    project.setSourceCodeIndexer(index);

    simpleScan(files);

  }

  /**
   * Used to do scan of test files.
   * @param files
   */
  public void simpleScan(Iterable<File> files) {
    SourceProject project = (SourceProject) index.search("Java Project");
    VisitorContext context = new VisitorContext(project);
    visitor.setContext(context);

    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(Lists.newArrayList(files));
    for (File file : files) {
      context.setFile(file);
      try {
        Tree ast = parser.parse(file);
        visitor.visitFile(ast);
        progressReport.nextFile();
      } catch (RecognitionException e) {
        LOG.error("Unable to parse source file : " + file.getAbsolutePath());
        LOG.error(e.getMessage());

        parseErrorWalkAndVisit(e, file);
      } catch (Exception e) {
        throw new AnalysisException(getAnalyisExceptionMessage(file), e);
      }
    }
    progressReport.stop();
  }

  private void parseErrorWalkAndVisit(RecognitionException e, File file) {
    try {
      // Process the exception
      visitor.visitFile(null);
      visitor.processRecognitionException(e);
    } catch (Exception e2) {
      throw new AnalysisException(getAnalyisExceptionMessage(file), e2);
    }
  }

  private static String getAnalyisExceptionMessage(File file) {
    return "SonarQube is unable to analyze file : '" + file.getAbsolutePath() + "'";
  }

  public void setVisitorBridge(VisitorsBridge visitor) {
    this.visitor = visitor;
  }

  public SourceCodeSearchEngine getIndex() {
    return index;
  }

  /**
   * Helper method for testing checks without having to deploy them on a Sonar instance.
   */
  @VisibleForTesting
  public static SourceFile scanSingleFile(File file, VisitorsBridge visitorsBridge) {
    if (!file.isFile()) {
      throw new IllegalArgumentException("File '" + file + "' not found.");
    }
    JavaAstScanner scanner = create(new JavaConfiguration(Charset.forName("UTF-8")), visitorsBridge);

    scanner.scan(Collections.singleton(file));
    Collection<SourceCode> sources = scanner.getIndex().search(new QueryByType(SourceFile.class));
    if (sources.size() != 1) {
      throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
    }
    return (SourceFile) sources.iterator().next();
  }

  private static JavaAstScanner create(JavaConfiguration conf, @Nullable VisitorsBridge visitorsBridge) {
    JavaAstScanner astScanner = new JavaAstScanner(JavaParser.createParser(conf.getCharset()));
    if(visitorsBridge != null) {
      visitorsBridge.setCharset(conf.getCharset());
      astScanner.setVisitorBridge(visitorsBridge);
    }
    return astScanner;
  }

}
