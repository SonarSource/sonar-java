/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.resolve;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class SafetyNetTest {

  private final String[] dirs = {
    "src/main/java",
    "src/test/java",
    "src/test/files",
    "target/test-projects/struts-core-1.3.9/src",
    "target/test-projects/commons-collections-3.2.1/src",
  };

  @Test
  public void test() {
    ActionParser parser = JavaParser.createParser(StandardCharsets.UTF_8);
    for (String dir : dirs) {
      for (File file : FileUtils.listFiles(new File(dir), new String[] {"java"}, true)) {
        try {
          SemanticModel.createFor((CompilationUnitTree) parser.parse(file), Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
        } catch (Exception e) {
          throw new RuntimeException("Unable to process file " + file, e);
        }
      }
    }
  }

}
