/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.sonar.sslr.api.typed.ActionParser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class JavaParserIntegrationTest {

  private final ActionParser parser = JavaParser.createParser(Charsets.UTF_8);

  private File file = null;

  public JavaParserIntegrationTest(File file) {
    this.file = file;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() throws URISyntaxException {
    Collection<Object[]> parameters = new ArrayList<Object[]>();
    addParametersForPath(parameters, "src/main/java/");
    addParametersForPath(parameters, "src/test/java/");
    addParametersForPath(parameters, "src/test/files/");
    return parameters;
  }

  @Test
  public void parse() throws IOException, URISyntaxException {
    parser.parse(file);
  }

  protected static void addParametersForPath(Collection<Object[]> parameters, String path) throws URISyntaxException {
    Collection<File> files;
    files = FileUtils.listFiles(new File(path), new String[] {"java"}, true);
    for (File file : files) {
      parameters.add(new Object[] {file});
    }
  }

}
