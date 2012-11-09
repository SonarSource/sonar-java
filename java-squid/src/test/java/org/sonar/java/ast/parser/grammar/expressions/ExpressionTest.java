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
package org.sonar.java.ast.parser.grammar.expressions;

import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.events.ExtendedStackTrace;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.parser.JavaParser;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class ExpressionTest {

  ExtendedStackTrace extendedStackTrace = new ExtendedStackTrace();
  Parser<JavaGrammar> p = JavaParser.create(extendedStackTrace);
  JavaGrammar g = p.getGrammar();

  @Before
  public void init() {
    p.setRootRule(g.expression);
  }

  /**
   * Our grammar accepts such constructions, whereas should not.
   */
  @Test
  public void error() {
    assertThat(p)
        .matches("a = b + 1 = c + 2");
  }

  @Test
  public void realLife() {
    assertThat(p)
        .matches("b >> 4")
        .matches("b >>= 4")
        .matches("b >>> 4")
        .matches("b >>>= 4");
    // Java 7: diamond
    assertThat(p)
        .matches("new HashMap<>()");
  }

}
