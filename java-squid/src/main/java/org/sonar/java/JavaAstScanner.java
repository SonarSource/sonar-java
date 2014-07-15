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
package org.sonar.java;

import com.sonar.sslr.impl.Parser;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.java.ast.AstScanner;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.visitors.AccessorVisitor;
import org.sonar.java.ast.visitors.AnonymousInnerClassVisitor;
import org.sonar.java.ast.visitors.ClassVisitor;
import org.sonar.java.ast.visitors.CommentLinesVisitor;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.ast.visitors.EndAtLineVisitor;
import org.sonar.java.ast.visitors.FileVisitor;
import org.sonar.java.ast.visitors.LinesOfCodeVisitor;
import org.sonar.java.ast.visitors.LinesVisitor;
import org.sonar.java.ast.visitors.MethodVisitor;
import org.sonar.java.ast.visitors.PackageVisitor;
import org.sonar.java.ast.visitors.PublicApiVisitor;
import org.sonar.squidbridge.CommentAnalyser;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.metrics.CommentsVisitor;
import org.sonar.squidbridge.metrics.CounterVisitor;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

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
  public static SourceFile scanSingleFile(File file, SquidAstVisitor<LexerlessGrammar>... visitors) {
    return scanSingleFile(file, file.getParentFile(), visitors);
  }

  /**
   * Helper method for testing checks without having to deploy them on a Sonar instance.
   */
  public static SourceFile scanSingleFile(File file, File parentFile, SquidAstVisitor<LexerlessGrammar>... visitors) {
    if (!file.isFile()) {
      throw new IllegalArgumentException("File '" + file + "' not found.");
    }
    org.sonar.java.ast.AstScanner scanner = create(new JavaConfiguration(Charset.forName("UTF-8")), visitors);
    InputFile inputFile = InputFileUtils.create(parentFile, file);
    scanner.scan(Collections.singleton(inputFile));
    Collection<SourceCode> sources = scanner.getIndex().search(new QueryByType(SourceFile.class));
    if (sources.size() != 1) {
      throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
    }
    return (SourceFile) sources.iterator().next();
  }

  public static AstScanner create(JavaConfiguration conf, SquidAstVisitor<LexerlessGrammar>... visitors) {
    final Parser<LexerlessGrammar> parser = new ParserAdapter<LexerlessGrammar>(conf.getCharset(), JavaGrammar.createGrammar());

    AstScanner builder = new AstScanner(parser);

    /* Packages */
    builder.withSquidAstVisitor(new PackageVisitor());

    /* Files */
    builder.withSquidAstVisitor(new FileVisitor());

    /* Classes */
    builder.withSquidAstVisitor(new ClassVisitor());
    builder.withSquidAstVisitor(new AnonymousInnerClassVisitor());

    /* Methods */
    builder.withSquidAstVisitor(new MethodVisitor());
    if (conf.isAnalysePropertyAccessors()) {
      builder.withSquidAstVisitor(new AccessorVisitor());
    }
    builder.withSquidAstVisitor(new PublicApiVisitor());

    builder.withSquidAstVisitor(new EndAtLineVisitor());

    /* Comments */
    builder.setCommentAnalyser(
      new CommentAnalyser() {
        @Override
        public boolean isBlank(String line) {
          // Implementation of this method was taken from org.sonar.squidbridge.text.Line#isThereBlankComment()
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

    builder.withSquidAstVisitor(new LinesVisitor(conf.getCharset()));

    builder.withSquidAstVisitor(new LinesOfCodeVisitor());
    builder.withSquidAstVisitor(new CommentLinesVisitor());
    builder.withSquidAstVisitor(CommentsVisitor.<LexerlessGrammar>builder()
      .withNoSonar(true)
      .withIgnoreHeaderComment(true)
      .build());
    builder.withSquidAstVisitor(CounterVisitor.<LexerlessGrammar>builder()
      .setMetricDef(JavaMetric.STATEMENTS)
      .subscribeTo(
        // This is mostly the same elements as for the grammar rule "statement", but "labeledStatement" and "block" were excluded
        JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT,
        JavaGrammar.ASSERT_STATEMENT,
        JavaGrammar.IF_STATEMENT,
        JavaGrammar.FOR_STATEMENT,
        JavaGrammar.WHILE_STATEMENT,
        JavaGrammar.DO_STATEMENT,
        JavaGrammar.TRY_STATEMENT,
        JavaGrammar.SWITCH_STATEMENT,
        JavaGrammar.SYNCHRONIZED_STATEMENT,
        JavaGrammar.RETURN_STATEMENT,
        JavaGrammar.THROW_STATEMENT,
        JavaGrammar.BREAK_STATEMENT,
        JavaGrammar.CONTINUE_STATEMENT,
        JavaGrammar.EXPRESSION_STATEMENT,
        JavaGrammar.EMPTY_STATEMENT)
      .build());

    builder.withSquidAstVisitor(new ComplexityVisitor());

    /* External visitors (typically Check ones) */
    for (SquidAstVisitor<LexerlessGrammar> visitor : visitors) {
      if (visitor instanceof CharsetAwareVisitor) {
        ((CharsetAwareVisitor) visitor).setCharset(conf.getCharset());
      }
      builder.withSquidAstVisitor(visitor);
    }

    return builder;
  }

}
