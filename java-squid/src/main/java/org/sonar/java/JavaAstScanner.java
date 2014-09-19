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
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.CommentLinesVisitor;
import org.sonar.java.ast.visitors.FileVisitor;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.metrics.CommentsVisitor;
import org.sonar.sslr.parser.LexerlessGrammar;

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
    final Parser parser = JavaParser.createParser(conf.getCharset(), conf.getVerifyAssertions());

    AstScanner builder = new AstScanner(parser);

    /* Files */
    builder.withSquidAstVisitor(new FileVisitor());

    /* Comments */
    builder.setCommentAnalyser(new CommentLinesVisitor.JavaCommentAnalyser());

    builder.withSquidAstVisitor(CommentsVisitor.<LexerlessGrammar>builder()
      .withNoSonar(true)
      .withIgnoreHeaderComment(true)
      .build());

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
