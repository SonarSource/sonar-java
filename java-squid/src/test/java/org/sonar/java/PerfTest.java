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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.Parser;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.java.ast.parser.ActionGrammar;
import org.sonar.java.ast.parser.ActionParser;
import org.sonar.java.ast.parser.ActionParser2;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.parser.SyntaxTreeCreator;
import org.sonar.java.ast.parser.TreeFactory;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParseRunner;
import org.sonar.sslr.parser.ParserAdapter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class PerfTest {

  @Test
  @Ignore
  public void test() throws Exception {
    LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

    ActionParser fullParser1 = new ActionParser(
      Charsets.UTF_8,
      JavaGrammar.createGrammarBuilder(),
      ActionGrammar.class,
      new TreeFactory(),
      JavaGrammar.COMPILATION_UNIT,
      false);

    ActionParser2 fullParser2 = new ActionParser2(
      Charsets.UTF_8,
      b,
      ActionGrammar.class,
      new TreeFactory(),
      JavaGrammar.COMPILATION_UNIT,
      false);

    JavaTreeMaker treeMaker = new JavaTreeMaker();
    ParseRunner rawParser = new ParseRunner(b.build().getRootRule());
    final Parser<LexerlessGrammar> astNodeParser = new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, b.build());

    System.out.println("Trash: " + fullParser1 + fullParser2 + treeMaker + rawParser + astNodeParser);

    Collection<File> files = FileUtils.listFiles(
      new File("/Users/dineshbolkensteyn/Desktop/sonarsource/it-sources/sslr/oracle-jdk-1.6.0.31"),
      new String[] {"java"},
      true);

    Field definitions = LexerlessGrammarBuilder.class.getDeclaredField("definitions");
    definitions.setAccessible(true);
    System.out.println("Rules: " + ((Map) definitions.get(b)).size());

    for (File file : files) {
      AstNode astNode1 = fullParser1.parse(file);
      AstNode astNode2 = fullParser2.parse(file);

      verify(file, astNode1, astNode2);
    }
  }

  public void verify(File file, AstNode astNode1, AstNode astNode2) {
    try {
      Preconditions.checkNotNull(astNode1);
      Preconditions.checkNotNull(astNode2);

      assertThat(astNode2.getName()).isEqualTo(astNode1.getName());
      if (astNode2.getType() != SyntaxTreeCreator.UNDEFINED_TOKEN_TYPE) {
        assertThat(astNode2.getType()).isEqualTo(astNode1.getType());
      }
      assertThat(astNode2.getNumberOfChildren()).isEqualTo(astNode1.getNumberOfChildren());
      assertThat(astNode2.hasToken()).isEqualTo(astNode1.hasToken());
      if (astNode2.hasToken()) {
        assertThat(astNode2.getToken().getLine()).isEqualTo(astNode1.getToken().getLine());
        assertThat(astNode2.getToken().getColumn()).isEqualTo(astNode1.getToken().getColumn());
        assertThat(astNode2.getToken().getOriginalValue()).isEqualTo(astNode1.getToken().getOriginalValue());
        assertThat(astNode2.getToken().getValue()).isEqualTo(astNode1.getToken().getValue());
        assertThat(astNode2.getToken().isGeneratedCode()).isEqualTo(astNode1.getToken().isGeneratedCode());
        assertThat(astNode2.getToken().isCopyBook()).isEqualTo(astNode1.getToken().isCopyBook());
      }
      if (false && astNode1.getFromIndex() != -1) {
        assertThat(astNode2.getFromIndex())
          .as("New: " + load(file, astNode2.getFromIndex()) + "\n"
            + "Old: " + load(file, astNode1.getFromIndex()))
          .isEqualTo(astNode1.getFromIndex());
      }
      if (false && astNode1.getToIndex() != -1) {
        assertThat(astNode2.getToIndex())
          .as("New: " + load(file, astNode2.getToIndex()) + "\n"
            + "Old: " + load(file, astNode1.getToIndex()))
          .isEqualTo(astNode1.getToIndex());
      }
    } catch (Throwable e) {
      throw new RuntimeException("File: " + file.getAbsolutePath()
        + "\nAstNode1: " + astNode1
        + "\nAstNode2: " + astNode2
        + "\nMessage: " + e.getMessage(),
        e);
    }

    for (int i = 0; i < astNode1.getNumberOfChildren(); i++) {
      verify(file, astNode1.getChildren().get(i), astNode2.getChildren().get(i));
    }
  }

  private static String load(File file, int fromIndex) {
    try {
      String after = Files.toString(file, Charsets.UTF_8).substring(fromIndex);
      return after.substring(0, Math.min(after.length(), 10)) + "...";
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

}
