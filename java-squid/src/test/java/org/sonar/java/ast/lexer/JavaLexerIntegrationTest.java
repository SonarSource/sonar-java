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
package org.sonar.java.ast.lexer;

import com.google.common.base.Charsets;
import com.sonar.sslr.impl.Lexer;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class JavaLexerIntegrationTest {

  private static Lexer lexer;

  @BeforeClass
  public static void init() {
    lexer = JavaLexer.create(Charsets.UTF_8);
  }

  private File file = null;

  public JavaLexerIntegrationTest(File file) {
    this.file = file;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() throws Exception {
    Collection<Object[]> parameters = new ArrayList<Object[]>();
    addParametersForPath(parameters, "src/main/java/");
    addParametersForPath(parameters, "src/test/java/");
    addParametersForPath(parameters, "src/test/files/");
    return parameters;
  }

  @Test
  public void lex() throws Exception {
    lexer.lex(file);
  }

  private static void addParametersForPath(Collection<Object[]> parameters, String path) throws Exception {
    Collection<File> files;
    files = FileUtils.listFiles(new File(path), new String[] {"java"}, true);
    for (File file : files) {
      parameters.add(new Object[] {file});
    }
  }

}
