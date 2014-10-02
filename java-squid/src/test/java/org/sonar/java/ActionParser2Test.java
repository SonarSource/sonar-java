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
import com.sonar.sslr.api.AstNode;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.java.ast.parser.ActionGrammar;
import org.sonar.java.ast.parser.ActionParser2;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.parser.TreeFactory;
import org.sonar.java.ast.visitors.FileVisitor;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import java.io.File;

public class ActionParser2Test {

  @Test
  @Ignore
  public void test() throws Exception {
    LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

    ActionParser2 parser = new ActionParser2(
      Charsets.UTF_8,
      b,
      ActionGrammar.class,
      new TreeFactory(),
      JavaGrammar.COMPILATION_UNIT,
      false);

    AstNode astNode = parser.parse(new File("/Users/alex/Desktop/sonarsource/java/test/src/main/java/NamedNodeMapImpl.java"));
    System.out.println("Got: " + FileVisitor.getPackageKey(astNode));
  }

}
