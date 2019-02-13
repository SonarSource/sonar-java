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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageInfoCheckTest {

  @Test
  public void test() throws Exception {
    PackageInfoCheck check = new PackageInfoCheck();
    File file = new File("src/test/files/checks/packageInfo/HelloWorld.java");
    JavaAstScanner.scanSingleFileForTests(file, new VisitorsBridge(Collections.singletonList(check), Collections.emptyList(), sonarComponents(file)));
    assertThat(check.directoriesWithoutPackageFile).isEmpty();
  }

  @Test
  public void testNoPackageInfo() throws Exception {
    PackageInfoCheck check = new PackageInfoCheck();
    File file = new File("src/test/files/checks/packageInfo/nopackageinfo/nopackageinfo.java");
    JavaAstScanner.scanSingleFileForTests(file, new VisitorsBridge(Collections.singletonList(check), Collections.emptyList(), sonarComponents(file)));
    file = new File("src/test/files/checks/packageInfo/nopackageinfo/HelloWorld.java");
    JavaAstScanner.scanSingleFileForTests(file, new VisitorsBridge(Collections.singletonList(check), Collections.emptyList(), sonarComponents(file)));
    Set<File> set = check.directoriesWithoutPackageFile;
    assertThat(set).hasSize(1);
    assertThat(set.iterator().next().getName()).isEqualTo("nopackageinfo");
  }

  static SonarComponents sonarComponents(File file) throws IOException {
    File moduleBaseDir = new File("");
    SensorContextTester context = SensorContextTester.create(moduleBaseDir);
    context.fileSystem().setWorkDir(moduleBaseDir.toPath());
    SonarComponents sonarComponents = new SonarComponents(null, context.fileSystem(), null, null, null);
    sonarComponents.setSensorContext(context);

    DefaultInputFile inputFile = new TestInputFileBuilder("", moduleBaseDir.getAbsoluteFile(), file.getAbsoluteFile()).setCharset(StandardCharsets.UTF_8)
      .setContents(new String(Files.readAllBytes(file.toPath()))).build();
    context.fileSystem().add(inputFile);
    return sonarComponents;
  }

}
