/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageInfoCheckTest {

  private static AnalyzerMessage reportedMessage;

  @Before
  public void setup() {
    reportedMessage = null;
  }

  @Test
  public void test() throws Exception {
    PackageInfoCheck check = new PackageInfoCheck();
    File testFile = new File("src/test/files/checks/packageInfo/HelloWorld.java");
    JavaAstScanner.scanSingleFileForTests(testFile, visitorsBridgeForTests(check, testFile, InputFile.Type.MAIN));
    assertThat(check.directoriesWithoutPackageFile).isEmpty();
    assertThat(reportedMessage).isNull();
  }

  @Test
  public void testNoPackageInfo() throws Exception {
    PackageInfoCheck check = new PackageInfoCheck();
    File testFile1 = new File("src/test/files/checks/packageInfo/nopackageinfo/nopackageinfo.java");
    JavaAstScanner.scanSingleFileForTests(testFile1, visitorsBridgeForTests(check, testFile1, InputFile.Type.MAIN));
    File testFile2 = new File("src/test/files/checks/packageInfo/nopackageinfo/HelloWorld.java");
    JavaAstScanner.scanSingleFileForTests(testFile2, visitorsBridgeForTests(check, testFile2, InputFile.Type.MAIN));
    Set<File> set = check.directoriesWithoutPackageFile;
    assertThat(set).hasSize(1);
    assertThat(set.iterator().next().getName()).isEqualTo("nopackageinfo");
    assertThat(reportedMessage).isNotNull();
    assertThat(reportedMessage.getMessage()).isEqualTo("Add a 'package-info.java' file to document the 'nopackageinfo' package");
  }

  @Test
  public void testFileWithoutPackageInfo() throws Exception {
    PackageInfoCheck check = new PackageInfoCheck();
    File testFile = new File("src/test/files/checks/packageInfo/nopackageinfo/HelloWorld.java");
    JavaAstScanner.scanSingleFileForTests(testFile, visitorsBridgeForTests(check, testFile, InputFile.Type.TEST));
    assertThat(check.directoriesWithoutPackageFile).isEmpty();
    assertThat(reportedMessage).isNull();
  }

  private static VisitorsBridge visitorsBridgeForTests(PackageInfoCheck check, File testFile, InputFile.Type typeOfFile) {
    return new VisitorsBridge(Collections.singleton(check), Collections.emptyList(), createSonarComponentsMock(testFile, typeOfFile));
  }

  private static SonarComponents createSonarComponentsMock(File testfile, InputFile.Type typeOfFile) {
    SonarComponents sonarComponents = mock(SonarComponents.class);

    InputFile inputFile = mock(InputFile.class);
    when(inputFile.type()).thenReturn(typeOfFile);
    when(sonarComponents.inputFromIOFile(eq(testfile))).thenReturn(inputFile);

    when(sonarComponents.isSonarLintContext()).thenReturn(true);

    doAnswer(invocation -> {
      reportedMessage = new AnalyzerMessage(invocation.getArgument(1),
        // check
        invocation.getArgument(0),
        // textspan
        null,
        // message
        invocation.getArgument(3),
        // cost
        0);
      return null;
    }).when(sonarComponents).addIssue(any(File.class), any(PackageInfoCheck.class), anyInt(), anyString(), any());

    return sonarComponents;
  }

}
