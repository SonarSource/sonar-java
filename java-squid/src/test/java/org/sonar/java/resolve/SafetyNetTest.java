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
package org.sonar.java.resolve;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.impl.Parser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import java.io.File;

/**
 * TODO in fact it supersedes {@link org.sonar.java.ast.parser.JavaParserIntegrationTest}
 */
public class SafetyNetTest {

  private final String[] dirs = {
      "src/main/java",
      "src/test/java",
      "src/test/files",
      "target/test-projects/struts-core-1.3.9/src",
      "target/test-projects/commons-collections-3.2.1/src"
  };

  @Test
  public void test() {
    Parser<LexerlessGrammar> parser = new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, JavaGrammar.createGrammar());
    for (String dir : dirs) {
      for (File file : FileUtils.listFiles(new File(dir), new String[]{"java"}, true)) {
        try {
          SemanticModel.createFor(new JavaTreeMaker().compilationUnit(parser.parse(file)), Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
        } catch (Exception e) {
          throw new RuntimeException("Unable to process file " + file, e);
        }
      }
    }
  }

}
