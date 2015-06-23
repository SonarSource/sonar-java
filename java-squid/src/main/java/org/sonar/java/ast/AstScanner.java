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
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.ast.AstWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.ast.visitors.VisitorContext;
import org.sonar.squidbridge.AstScannerExceptionHandler;
import org.sonar.squidbridge.CommentAnalyser;
import org.sonar.squidbridge.ProgressReport;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.squidbridge.api.SourceCodeSearchEngine;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.SquidIndex;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AstScanner {

  private static final Logger LOG = LoggerFactory.getLogger(AstScanner.class);

  private final SquidIndex index;
  private final List<SquidAstVisitor<LexerlessGrammar>> visitors = Lists.newArrayList();
  private final List<AstScannerExceptionHandler> astScannerExceptionHandlers = Lists.newArrayList();
  private final Parser<LexerlessGrammar> parser;
  private CommentAnalyser commentAnalyser;

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

    for (SquidAstVisitor<LexerlessGrammar> visitor : visitors) {
      visitor.setContext(context);
      visitor.init();
    }

    AstWalker astWalker = new AstWalker(visitors);
    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(Lists.newArrayList(files));
    for (File file : files) {
      context.setFile(file);
      context.addSourceCode(new SourceFile(file.getAbsolutePath(), file.getPath()));
      try {
        AstNode ast = parser.parse(file);
        astWalker.walkAndVisit(ast);
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

    for (SquidAstVisitor<LexerlessGrammar> visitor : visitors) {
      visitor.destroy();
    }
  }

  private void parseErrorWalkAndVisit(RecognitionException e, File file) {
    try {
      // Process the exception
      for (SquidAstVisitor<? extends Grammar> visitor : visitors) {
        visitor.visitFile(null);
      }

      for (AstScannerExceptionHandler astScannerExceptionHandler : astScannerExceptionHandlers) {
        astScannerExceptionHandler.processRecognitionException(e);
      }

      for (SquidAstVisitor<? extends Grammar> visitor : Lists.reverse(visitors)) {
        visitor.leaveFile(null);
      }

    } catch (Exception e2) {
      throw new AnalysisException(getAnalyisExceptionMessage(file), e2);
    }
  }

  private static String getAnalyisExceptionMessage(File file) {
    return "SonarQube is unable to analyze file : '" + file.getAbsolutePath() + "'";
  }

  public void withSquidAstVisitor(SquidAstVisitor<LexerlessGrammar> visitor) {
    if (visitor instanceof AstScannerExceptionHandler) {
      astScannerExceptionHandlers.add((AstScannerExceptionHandler) visitor);
    }
    this.visitors.add(visitor);
  }

  public SourceCodeSearchEngine getIndex() {
    return index;
  }

  public void setCommentAnalyser(CommentAnalyser commentAnalyser) {
    this.commentAnalyser = commentAnalyser;
  }

}
