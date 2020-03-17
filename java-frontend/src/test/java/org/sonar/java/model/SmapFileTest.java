/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class SmapFileTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void test() throws Exception {
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
    Path path = temporaryFolder.newFile("test_jsp.class.smap").toPath();
    Files.write(path, sourceMap.getBytes(StandardCharsets.UTF_8));
    SmapFile smap = SmapFile.fromPath(path);
    assertThat(smap.getGeneratedFile()).isEqualTo(path.resolveSibling("test_jsp.java"));
    assertThat(smap.getFileSection()).containsExactly(
      entry(0, new SmapFile.FileInfo(0, "test.jsp", "WEB-INF/test.jsp")),
      entry(2, new SmapFile.FileInfo(2, "Incl.xyz", null))
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
  public void invalid_file() {
    Path p = Paths.get("file.class.smap");
    assertThatThrownBy(() -> new SmapFile(p, new Scanner("not a smap file")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not a source map");

    assertThatThrownBy(() -> new SmapFile(p, new Scanner("SMAP\ntest.groovy\nGroovy\n\n")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Not a JSP source map");

    assertThatThrownBy(() -> new SmapFile(p, new Scanner("SMAP\ntest.jsp\nJSP\n*E")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Section *S JSP not found");

    assertThatThrownBy(() -> new SmapFile(p, new Scanner("SMAP\ntest.jsp\nJSP\n*S JSP\n*E")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Section *F not found");

    assertThatThrownBy(() -> new SmapFile(p, new Scanner("SMAP\ntest.jsp\nJSP\n*S JSP\n*F\n")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Section *L not found");
  }

  @Test
  public void invalid_line_info() {
    Path p = Paths.get("file.class.smap");

    new SmapFile(p, new Scanner("SMAP\ntest.jsp\nJSP\n*S JSP\n*F\n*L\n" +
      "invalid line info\n"
      ));

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Invalid line info invalid line info");
  }

  @Test
  public void nonexisting_path() {
    assertThatThrownBy(() -> SmapFile.fromPath(Paths.get("nonexisting.java")))
      .isInstanceOf(IllegalStateException.class)
      .hasCauseInstanceOf(FileNotFoundException.class)
      .hasMessage("Error reading sourcemap nonexisting.java");
  }

  @Test
  public void lineinfo_hashcode_equals_tostring() {
    SmapFile.LineInfo lineInfo = new SmapFile.LineInfo(1, 0, 1, 1, 1);
    assertThat(lineInfo.toString()).isEqualTo("LineInfo{inputStartLine=1, lineFileId=0, repeatCount=1, outputStartLine=1, outputLineIncrement=1}");

    Set<SmapFile.LineInfo> set = new HashSet<>();
    set.add(lineInfo);
    SmapFile.LineInfo lineInfo2 = new SmapFile.LineInfo(1, 0, 1, 1, 1);
    assertThat(set.add(lineInfo2)).isFalse();
    assertThat(lineInfo).isEqualTo(lineInfo);
    assertThat(lineInfo).isEqualTo(lineInfo2);
    assertThat(lineInfo).isNotEqualTo(null);
    assertThat(lineInfo).isNotEqualTo(0);
    assertThat(lineInfo).isNotEqualTo(new SmapFile.LineInfo(2, 0, 1, 1, 1));
    assertThat(lineInfo).isNotEqualTo(new SmapFile.LineInfo(1, 2, 1, 1, 1));
    assertThat(lineInfo).isNotEqualTo(new SmapFile.LineInfo(1, 0, 2, 1, 1));
    assertThat(lineInfo).isNotEqualTo(new SmapFile.LineInfo(1, 0, 1, 2, 1));
    assertThat(lineInfo).isNotEqualTo(new SmapFile.LineInfo(1, 0, 1, 1, 2));
  }

  @Test
  public void fileinfo_hashcode_equals_tostring() {
    SmapFile.FileInfo fileInfo = new SmapFile.FileInfo(0, "file.jsp", "path/file.jsp");
    SmapFile.FileInfo fileInfo2 = new SmapFile.FileInfo(0, "file.jsp", "path/file.jsp");
    assertThat(fileInfo).isEqualTo(fileInfo);
    assertThat(fileInfo).isEqualTo(fileInfo2);
    assertThat(fileInfo).isNotEqualTo(null);
    assertThat(fileInfo).isNotEqualTo(0);
    assertThat(fileInfo).isNotEqualTo(new SmapFile.FileInfo(1, "file.jsp", "path/file.jsp"));
    assertThat(fileInfo).isNotEqualTo(new SmapFile.FileInfo(0, "file2.jsp", "path/file.jsp"));
    assertThat(fileInfo).isNotEqualTo(new SmapFile.FileInfo(0, "file.jsp", "path/file2.jsp"));
    Set<SmapFile.FileInfo> set = new HashSet<>();
    set.add(fileInfo);
    assertThat(set.add(fileInfo2)).isFalse();
    assertThat(fileInfo.toString()).isEqualTo("FileInfo{fileId=0, sourceName='file.jsp', sourcePath='path/file.jsp'}");
  }

  @Test
  public void smapfile_tostring() {
    SmapFile smapFile = new SmapFile(Paths.get("nonexisting.java"), new Scanner("SMAP\ntest.jsp\nJSP\n*S JSP\n*F\n*L\n"));
    assertThat(smapFile.toString()).isEqualTo("test.jsp");
  }

}
