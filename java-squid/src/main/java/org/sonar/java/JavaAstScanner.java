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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.java.ast.AstScanner;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.CommentLinesVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;

import javax.annotation.Nullable;
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
  @VisibleForTesting
  public static SourceFile scanSingleFile(File file, VisitorsBridge visitorsBridge) {
    if (!file.isFile()) {
      throw new IllegalArgumentException("File '" + file + "' not found.");
    }
    org.sonar.java.ast.AstScanner scanner = create(new JavaConfiguration(Charset.forName("UTF-8")), visitorsBridge);

    scanner.scan(Collections.singleton(file));
    Collection<SourceCode> sources = scanner.getIndex().search(new QueryByType(SourceFile.class));
    if (sources.size() != 1) {
      throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
    }
    return (SourceFile) sources.iterator().next();
  }

  public static AstScanner create(JavaConfiguration conf, @Nullable VisitorsBridge visitorsBridge) {

    AstScanner astScanner = new AstScanner(JavaParser.createParser(conf.getCharset()));
    /* Comments */
    astScanner.setCommentAnalyser(new CommentLinesVisitor.JavaCommentAnalyser());
    if(visitorsBridge != null) {
      visitorsBridge.setCharset(conf.getCharset());
      astScanner.setVisitorBridge(visitorsBridge);
    }
    return astScanner;
  }

}
