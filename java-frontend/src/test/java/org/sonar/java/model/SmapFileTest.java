/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class SmapFileTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @TempDir
  public Path temporaryFolder;

  @Test
  void test() throws Exception {
    String sourceMap = "SMAP\n" +
      "test_jsp.java\n" +
      "JSP\n" +
      "*S JSP\n" +
      "*F\n" +
      "+ 0 test.jsp\n" +
      "WEB-INF/test.jsp\n" +
      "2 Incl.xyz\n" +
      "*L\n" +
      "1,5:116,0\n" +
      "123:207\n" +
      "130,3:210\n" +
      "140:250,7\n" +
      "160,3:300,2\n" +
      "160#2,3:300,2\n" +
      "160,3:300,2\n" +
      "*E\n";
    DefaultFileSystem fs = new DefaultFileSystem(temporaryFolder);
    DefaultInputFile inputFile = TestInputFileBuilder.create("module", temporaryFolder.toFile(), temporaryFolder.resolve("WEB-INF/test.jsp").toFile()).build();
    fs.add(inputFile);
    SmapFile smap = new SmapFile(temporaryFolder, sourceMap, temporaryFolder, fs);
    assertThat(smap.getGeneratedFile()).isEqualTo(temporaryFolder.resolve("test_jsp.java"));
    assertThat(smap.getFileSection()).containsExactly(
      entry(0, new SmapFile.FileInfo(0, "test.jsp", "WEB-INF/test.jsp", null)),
      entry(2, new SmapFile.FileInfo(2, "Incl.xyz", null, null))
      );
    assertThat(smap.getLineSection()).containsExactly(
      new SmapFile.LineInfo(1, 0, 5, 116, 0),
      new SmapFile.LineInfo(123, 0, 1, 207, 1),
      new SmapFile.LineInfo(130, 0, 3, 210, 1),
      new SmapFile.LineInfo(140, 0, 1, 250, 7),
      new SmapFile.LineInfo(160, 0, 3, 300, 2),
      new SmapFile.LineInfo(160, 2, 3, 300, 2),
      new SmapFile.LineInfo(160, 2, 3, 300, 2)
    );
  }

  @Test
  void invalid_file() {
    Path uriRoot = Paths.get("");
    Path p = Paths.get("file.class.smap");
    assertThatThrownBy(() -> new SmapFile(p, "not a smap file", null, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not a source map");

    assertThatThrownBy(() -> new SmapFile(p, "SMAP\ntest.groovy\nGroovy\n\n", null, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not a JSP source map");

    assertThatThrownBy(() -> new SmapFile(p, "SMAP\ntest.jsp\nJSP\n*E", null, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Section *S JSP not found");

    assertThatThrownBy(() -> new SmapFile(p, "SMAP\ntest.jsp\nJSP\n*S JSP\n*E", null, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Section *F not found");

    assertThatThrownBy(() -> new SmapFile(p, "SMAP\ntest.jsp\nJSP\n*S JSP\n*F\n", null, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Section *L not found");
  }

  @Test
  void invalid_line_info() {
    Path p = Paths.get("file.class.smap");

    new SmapFile(p, "SMAP\ntest.jsp\nJSP\n*S JSP\n*F\n*L\n" +
      "invalid line info\n",
      null, null);

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Invalid line info invalid line info");
  }

  @Test
  void lineinfo_hashcode_equals_tostring() {
    SmapFile.LineInfo lineInfo = new SmapFile.LineInfo(1, 0, 1, 1, 1);
    assertThat(lineInfo).hasToString("LineInfo{inputStartLine=1, lineFileId=0, repeatCount=1, outputStartLine=1, outputLineIncrement=1}");

    Set<SmapFile.LineInfo> set = new HashSet<>();
    set.add(lineInfo);
    SmapFile.LineInfo lineInfo2 = new SmapFile.LineInfo(1, 0, 1, 1, 1);
    assertThat(set.add(lineInfo2)).isFalse();
    assertThat(lineInfo)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isEqualTo(lineInfo)
      .isEqualTo(lineInfo2)
      .isNotEqualTo(new SmapFile.LineInfo(2, 0, 1, 1, 1))
      .isNotEqualTo(new SmapFile.LineInfo(1, 2, 1, 1, 1))
      .isNotEqualTo(new SmapFile.LineInfo(1, 0, 2, 1, 1))
      .isNotEqualTo(new SmapFile.LineInfo(1, 0, 1, 2, 1))
      .isNotEqualTo(new SmapFile.LineInfo(1, 0, 1, 1, 2));
  }

  @Test
  void fileinfo_hashcode_equals_tostring() {
    SmapFile.FileInfo fileInfo = new SmapFile.FileInfo(0, "file.jsp", "path/file.jsp", null);
    SmapFile.FileInfo fileInfo2 = new SmapFile.FileInfo(0, "file.jsp", "path/file.jsp", null);
    assertThat(fileInfo)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isEqualTo(fileInfo)
      .isEqualTo(fileInfo2)
      .isNotEqualTo(new SmapFile.FileInfo(1, "file.jsp", "path/file.jsp", null))
      .isNotEqualTo(new SmapFile.FileInfo(0, "file2.jsp", "path/file.jsp", null))
      .isNotEqualTo(new SmapFile.FileInfo(0, "file.jsp", "path/file2.jsp", null));
    Set<SmapFile.FileInfo> set = new HashSet<>();
    set.add(fileInfo);
    assertThat(set.add(fileInfo2)).isFalse();
    assertThat(fileInfo).hasToString("FileInfo{fileId=0, sourceName='file.jsp', sourcePath='path/file.jsp'}");
  }

  @Test
  void smapfile_tostring() {
    SmapFile smapFile = new SmapFile(Paths.get("dir"), "SMAP\ntest.jsp\nJSP\n*S JSP\n*F\n*L\n", null, null);
    assertThat(smapFile).hasToString(Paths.get("dir/test.jsp").toString());
  }

}
