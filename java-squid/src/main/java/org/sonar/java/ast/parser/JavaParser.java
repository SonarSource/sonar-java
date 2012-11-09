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
package org.sonar.java.ast.parser;

import com.google.common.annotations.VisibleForTesting;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.events.ParsingEventListener;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import java.nio.charset.Charset;

public final class JavaParser {

  private JavaParser() {
  }

  @VisibleForTesting
  public static Parser<JavaGrammar> create(ParsingEventListener... parsingEventListeners) {
    return create(new JavaConfiguration(Charset.forName("UTF-8")), parsingEventListeners);
  }

  public static Parser<JavaGrammar> create(JavaConfiguration conf, ParsingEventListener... parsingEventListeners) {
    return new ParserAdapter<JavaGrammar>(conf.getCharset(), new JavaGrammarImpl());
  }

}
