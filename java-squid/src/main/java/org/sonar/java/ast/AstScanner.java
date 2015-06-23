/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.ast.visitors.VisitorContext;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.CommentAnalyser;
import org.sonar.squidbridge.ProgressReport;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.squidbridge.api.SourceCodeSearchEngine;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.SquidIndex;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class AstScanner {

  private static final Logger LOG = LoggerFactory.getLogger(AstScanner.class);

  private final SquidIndex index;
  private final Parser<LexerlessGrammar> parser;
  private CommentAnalyser commentAnalyser;
  private VisitorsBridge visitor;

  public AstScanner(Parser<LexerlessGrammar> parser) {
    this.parser = parser;
    this.index = new SquidIndex();
  }

  /**
   * Takes parser and index from another instance of {@link AstScanner}
   */
  public AstScanner(AstScanner astScanner) {
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
    context.setCommentAnalyser(commentAnalyser);
    visitor.setContext(context);

    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(Lists.newArrayList(files));
    for (File file : files) {
      context.setFile(file);
      context.addSourceCode(new SourceFile(file.getAbsolutePath(), file.getPath()));
      try {
        AstNode ast = parser.parse(file);
        visitor.visitFile(ast);
        progressReport.nextFile();
        context.popSourceCode();
      } catch (RecognitionException e) {
        LOG.error("Unable to parse source file : " + file.getAbsolutePath());
        LOG.error(e.getMessage());

        parseErrorWalkAndVisit(e, file);
        context.popSourceCode();
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

  public void setCommentAnalyser(CommentAnalyser commentAnalyser) {
    this.commentAnalyser = commentAnalyser;
  }

}
