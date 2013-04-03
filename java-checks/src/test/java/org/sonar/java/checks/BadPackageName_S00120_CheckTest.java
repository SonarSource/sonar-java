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
package org.sonar.java.checks;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.squid.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.ast.AstScanner;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.indexer.QueryByType;

import java.io.File;
import java.util.List;

public class BadPackageName_S00120_CheckTest {

  private BadPackageName_S00120_Check check = new BadPackageName_S00120_Check();

  @Test
  public void test() {
    AstScanner scanner = JavaAstScanner.create(new JavaConfiguration(Charsets.UTF_8), check);
    File baseDir = new File("src/test/files/checks");
    List<InputFile> inputFiles = InputFileUtils.create(baseDir,
        ImmutableList.of(new File("src/test/files/checks/PACKAGE/BadPackageName.java")));
    scanner.scan(inputFiles);
    SourceFile file = (SourceFile) scanner.getIndex().search(new QueryByType(SourceFile.class)).iterator().next();
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(1).withMessage("Rename this package name to match the regular expression '^[a-z]+(\\.[a-z][a-z0-9]*)*$'.")
        .noMore();
  }

  @Test
  public void test2() {
    check.format = "^[a-zA-Z0-9]*$";
    AstScanner scanner = JavaAstScanner.create(new JavaConfiguration(Charsets.UTF_8), check);
    File baseDir = new File("src/test/files/checks");
    List<InputFile> inputFiles = InputFileUtils.create(baseDir,
        ImmutableList.of(new File("src/test/files/checks/PACKAGE/BadPackageName.java")));
    scanner.scan(inputFiles);
    SourceFile file = (SourceFile) scanner.getIndex().search(new QueryByType(SourceFile.class)).iterator().next();
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .noMore();
  }

}
