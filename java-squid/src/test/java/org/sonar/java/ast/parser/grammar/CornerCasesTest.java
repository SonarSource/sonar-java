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
package org.sonar.java.ast.parser.grammar;

import com.sonar.sslr.impl.Parser;
import org.junit.Test;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.parser.JavaParser;

import static com.sonar.sslr.test.parser.ParserMatchers.parse;
import static org.junit.Assert.assertThat;

public class CornerCasesTest {

  Parser<JavaGrammar> p = JavaParser.create();
  JavaGrammar g = p.getGrammar();

  @Test
  public void test() {
    p.setRootRule(g.ge);
    assertThat(p, parse(">="));

    p.setRootRule(g.sr);
    assertThat(p, parse(">>"));

    p.setRootRule(g.srequ);
    assertThat(p, parse(">>="));

    p.setRootRule(g.bsr);
    assertThat(p, parse(">>>"));

    p.setRootRule(g.bsrequ);
    assertThat(p, parse(">>>="));
  }

}
