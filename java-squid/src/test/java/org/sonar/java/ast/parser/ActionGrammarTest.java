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
package org.sonar.java.ast.parser;

import com.google.common.base.Charsets;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.java.ast.parser.ActionGrammar.TreeFactory;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

public class ActionGrammarTest {

  @Test
  @Ignore
  public void calculator() throws Exception {
    ActionParser parser = new ActionParser(
      Charsets.UTF_8,
      LexerlessGrammarBuilder.create(),
      ActionGrammar.class,
      new TreeFactory(),
      JavaGrammar.COMPILATION_UNIT);

    System.out.println(parser.parse("foo"));
  }

}
