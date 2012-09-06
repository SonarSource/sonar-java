/*
 * Sonar Java
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
package org.sonar.java;

import com.sonar.sslr.api.CommentAnalyser;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.squid.SquidAstVisitor;
import com.sonar.sslr.squid.metrics.*;
import org.sonar.java.ast.AstScanner;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.*;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.indexer.QueryByType;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;

public final class JavaAstScanner {

  private JavaAstScanner() {
  }

  /**
   * Helper method for testing checks without having to deploy them on a Sonar instance.
   */
  public static SourceFile scanSingleFile(File file, SquidAstVisitor<JavaGrammar>... visitors) {
    if (!file.isFile()) {
      throw new IllegalArgumentException("File '" + file + "' not found.");
    }
    org.sonar.java.ast.AstScanner scanner = create(new JavaConfiguration(Charset.forName("UTF-8")), visitors);
    scanner.scan(Collections.singleton(file));
    Collection<SourceCode> sources = scanner.getIndex().search(new QueryByType(SourceFile.class));
    if (sources.size() != 1) {
      throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
    }
    return (SourceFile) sources.iterator().next();
  }

  public static AstScanner create(JavaConfiguration conf, SquidAstVisitor<JavaGrammar>... visitors) {
    final Parser<JavaGrammar> parser = JavaParser.create(conf);

    AstScanner builder = new AstScanner(parser);

    /* Metrics */
    // builder.withMetrics(JavaMetric.values());

    /* Packages */
    builder.withSquidAstVisitor(new PackageVisitor());

    /* Files */
    builder.withSquidAstVisitor(new FileVisitor());

    /* Classes */
    builder.withSquidAstVisitor(new ClassVisitor());

    /* Methods */
    builder.withSquidAstVisitor(new MethodVisitor());
    if (conf.isAnalysePropertyAccessors()) {
      builder.withSquidAstVisitor(new AccessorVisitor());
    }
    builder.withSquidAstVisitor(new PublicApiVisitor());

    /* Comments */
    builder.setCommentAnalyser(
        new CommentAnalyser() {
          @Override
          public boolean isBlank(String line) {
            // Implementation of this method was taken from org.sonar.squid.text.Line#isThereBlankComment()
            // TODO Godin: for some languages we use Character.isLetterOrDigit instead of Character.isWhitespace
            for (int i = 0; i < line.length(); i++) {
              char character = line.charAt(i);
              if (!Character.isWhitespace(character) && character != '*' && character != '/') {
                return false;
              }
            }
            return true;
          }

          @Override
          public String getContents(String comment) {
            return comment.startsWith("//") ? comment.substring(2) : comment.substring(2, comment.length() - 2);
          }
        });

    /* Metrics */

    builder.withSquidAstVisitor(new LinesVisitor<JavaGrammar>(JavaMetric.LINES));
    builder.withSquidAstVisitor(new LinesOfCodeVisitor<JavaGrammar>(JavaMetric.LINES_OF_CODE));
    builder.withSquidAstVisitor(CommentsVisitor.<JavaGrammar> builder().withCommentMetric(JavaMetric.COMMENT_LINES)
        .withBlankCommentMetric(JavaMetric.COMMENT_BLANK_LINES)
        .withNoSonar(true)
        .withIgnoreHeaderComment(conf.getIgnoreHeaderComments())
        .build());
    builder.withSquidAstVisitor(CounterVisitor.<JavaGrammar> builder()
        .setMetricDef(JavaMetric.STATEMENTS)
        .subscribeTo(
            // This is mostly the same elements as for the grammar rule "statement", but "labeledStatement" and "block" were excluded
            parser.getGrammar().localVariableDeclarationStatement,
            parser.getGrammar().assertStatement,
            parser.getGrammar().ifStatement,
            parser.getGrammar().forStatement,
            parser.getGrammar().whileStatement,
            parser.getGrammar().doStatement,
            // TODO Godin: in my opinion, following node should be included, but it is not counted in previous version
            // parser.getGrammar().tryStatement,
            parser.getGrammar().switchStatement,
            parser.getGrammar().synchronizedStatement,
            parser.getGrammar().returnStatement,
            parser.getGrammar().throwStatement,
            parser.getGrammar().breakStatement,
            parser.getGrammar().continueStatement,
            parser.getGrammar().expressionStatement,
            parser.getGrammar().emptyStatement,
            // TODO Godin: in my opinion, following nodes should not be included, but they are counted in previous version
            JavaKeyword.ELSE,
            parser.getGrammar().labeledStatement,
            parser.getGrammar().switchLabel,
            parser.getGrammar().catchClause,
            parser.getGrammar().finally_)
        .build());
    builder.withSquidAstVisitor(ComplexityVisitor.<JavaGrammar> builder()
        .setMetricDef(JavaMetric.COMPLEXITY)
        .subscribeTo(
            // Entry points
            parser.getGrammar().methodBody,
            // Branching nodes
            parser.getGrammar().ifStatement,
            parser.getGrammar().forStatement,
            parser.getGrammar().whileStatement,
            parser.getGrammar().doStatement,
            parser.getGrammar().switchStatement,
            parser.getGrammar().switchLabel,
            parser.getGrammar().returnStatement,
            parser.getGrammar().throwStatement,
            parser.getGrammar().catchClause,
            // Expressions
            JavaPunctuator.QUERY,
            JavaPunctuator.ANDAND,
            JavaPunctuator.OROR)
        .build());

    /* External visitors (typically Check ones) */
    for (SquidAstVisitor<JavaGrammar> visitor : visitors) {
      builder.withSquidAstVisitor(visitor);
    }

    return builder;
  }

}
