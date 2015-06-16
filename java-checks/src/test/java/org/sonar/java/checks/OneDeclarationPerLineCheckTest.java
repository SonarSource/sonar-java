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
package org.sonar.java.checks;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.plugins.java.api.JavaFileScanner;

public class OneDeclarationPerLineCheckTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/checks/OneDeclarationPerLineCheck.java", new OneDeclarationPerLineCheck());
  }

  @Test
  public void testCornerCaseFilesVarSameLine() throws IOException {
    File f = File.createTempFile("OneDeclarationPerLineCheck-", ".java");
    try {
      FileUtils.write(f, "class C {\nint i;\n}");
      JavaFileScanner check = new OneDeclarationPerLineCheck();
      JavaCheckVerifier.verifyNoIssue(f.getAbsolutePath(), check);
      JavaCheckVerifier.verifyNoIssue(f.getAbsolutePath(), check);
    } finally {
      FileUtils.deleteQuietly(f);
    }
  }
}
