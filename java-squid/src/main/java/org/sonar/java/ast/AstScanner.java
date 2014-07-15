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
import com.sonar.sslr.api.AuditListener;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.ast.AstWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.InputFile;
import org.sonar.java.ProgressReport;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.visitors.VisitorContext;
import org.sonar.squidbridge.CommentAnalyser;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceCodeSearchEngine;
import org.sonar.squidbridge.api.SourceCodeTreeDecorator;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.SquidIndex;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Replacement for {@link com.sonar.sslr.squid.AstScanner}.
 */
public class AstScanner {

  private static final Logger LOG = LoggerFactory.getLogger(AstScanner.class);

  private final SquidIndex index;
  private final List<SquidAstVisitor<LexerlessGrammar>> visitors = Lists.newArrayList();
  private final List<AuditListener> auditListeners = Lists.newArrayList();
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

  public void scan(Collection<InputFile> files) {
    SourceProject project = new SourceProject("Java Project");
    index.index(project);
    project.setSourceCodeIndexer(index);
    VisitorContext context = new VisitorContext(project);
    context.setCommentAnalyser(commentAnalyser);

    simpleScan(files);

    SourceCodeTreeDecorator decorator = new SourceCodeTreeDecorator(project);
    decorator.decorateWith(JavaMetric.values());
    decorator.decorateWith(org.sonar.squidbridge.measures.Metric.values());
  }

  /**
   * Used to do scan of test files.
   */
  public void simpleScan(Collection<InputFile> files) {
    SourceProject project = (SourceProject) index.search("Java Project");
    VisitorContext context = new VisitorContext(project);
    context.setCommentAnalyser(commentAnalyser);

    for (SquidAstVisitor<LexerlessGrammar> visitor : visitors) {
      visitor.setContext(context);
      visitor.init();
    }

    AstWalker astWalker = new AstWalker(visitors);

    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    progressReport.start(files.size() + " source files to be analyzed");
    int count = 0;
    for (InputFile inputFile : files) {
      File file = inputFile.getFile();

      progressReport.message(count + "/" + files.size() + " files analyzed, current is " + file.getAbsolutePath());
      count++;

      context.setFile(file);

      try {
        AstNode ast = parser.parse(file);
        astWalker.walkAndVisit(ast);
      } catch (RecognitionException e) {
        LOG.error("Unable to parse source file : " + file.getAbsolutePath());
        LOG.error(e.getMessage());

        parseErrorWalkAndVisit(e, file);
      } catch (Exception e) {
        throw new AnalysisException(getAnalyisExceptionMessage(file), e);
      }
    }
    progressReport.stop(files.size() + "/" + files.size() + " source files analyzed");

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

      for (AuditListener auditListener : auditListeners) {
        auditListener.processRecognitionException(e);
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
    if (visitor instanceof AuditListener) {
      auditListeners.add((AuditListener) visitor);
    }
    this.visitors.add(visitor);
  }

  public SourceCodeSearchEngine getIndex() {
    return index;
  }

  public void setCommentAnalyser(CommentAnalyser commentAnalyser) {
    this.commentAnalyser = commentAnalyser;
  }

  public void accept(CodeVisitor visitor) {
    if (visitor instanceof SquidAstVisitor) {
      withSquidAstVisitor((SquidAstVisitor<LexerlessGrammar>) visitor);
    }
  }

}
