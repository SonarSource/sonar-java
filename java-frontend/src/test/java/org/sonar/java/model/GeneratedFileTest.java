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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.plugins.java.api.SourceMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableRuleMigrationSupport
class GeneratedFileTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  Path tmp;
  Path expected;
  private GeneratedFile actual;

  @Rule
  public LogTester logTester = new LogTester();
  private DefaultInputFile inputFile;

  @BeforeEach
  public void setUp() throws Exception {
    tmp = temporaryFolder.newFolder().toPath();
    expected = tmp.resolve("file.jsp");
    Files.write(expected, "content".getBytes(StandardCharsets.UTF_8));
    inputFile = TestInputFileBuilder.create("", "file.jsp").build();
    actual = new GeneratedFile(expected, inputFile);
  }

  @Test
  void test() throws Exception {
    assertEquals(expected.toAbsolutePath().toString(), actual.absolutePath());
    assertEquals(expected.toString(), actual.relativePath());
    assertEquals(expected, actual.path());
    assertEquals(expected.toFile(), actual.file());
    assertEquals(expected.toFile(), actual.file());
    assertEquals(expected.toUri(), actual.uri());
    assertEquals("file.jsp", actual.filename());
    assertEquals("content", actual.contents());
    try (InputStream is = actual.inputStream()) {
      assertEquals("content", IOUtils.toString(is));
    }
    assertFalse(actual.isEmpty());
    assertEquals(StandardCharsets.UTF_8, actual.charset());
    assertEquals(expected.toString(), actual.key());
    assertTrue(actual.isFile());
    assertEquals("java", actual.language());
    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  void test_not_implemented() throws Exception {
    assertThrows(UnsupportedOperationException.class, () -> actual.type());
    assertThrows(UnsupportedOperationException.class, () -> actual.status());
    assertThrows(UnsupportedOperationException.class, () -> actual.lines());
    assertThrows(UnsupportedOperationException.class, () -> actual.newPointer(0, 0));
    assertThrows(UnsupportedOperationException.class, () -> actual.newRange(null, null));
    assertThrows(UnsupportedOperationException.class, () -> actual.newRange(0, 0, 0, 0));
    assertThrows(UnsupportedOperationException.class, () -> actual.selectLine(0));

  }

  @Test
  void test_source_map() {
    String smap = "SMAP\n" +
      "index_jsp.java\n" +
      "JSP\n" +
      "*S JSP\n" +
      "*F\n" +
      "+ 0 index.jsp\n" +
      "index.jsp\n" +
      "*L\n" +
      "1,6:116,0\n" +
      "123:207\n" +
      "130,3:210\n" +
      "140:250,7\n" +
      "160,3:300,2\n" +
      "*E\n";

    SmapFile smapFile = new SmapFile(tmp, smap);
    InputFile inputFile = inputFileFromPath(Paths.get("src/main/webapp/index.jsp"));
    GeneratedFile generatedFile = new GeneratedFile(tmp.resolve("index_jsp.java"), inputFile);
    generatedFile.addSmap(smapFile);

    GeneratedFile.SourceMapImpl sourceMap = ((GeneratedFile.SourceMapImpl) generatedFile.sourceMap());
    SourceMap.Location location = sourceMap.getLocation(116, 116).get();
    assertThat(location.file()).isEqualTo(inputFile);
    assertThat(location.inputFile()).isEqualTo(inputFile.path());

    assertLocation(sourceMap.getLocation(116, 116), 1, 6);
    assertLocation(sourceMap.getLocation(207, 207), 123, 123);
    assertLocation(sourceMap.getLocation(210, 212), 130, 132);
    assertLocation(sourceMap.getLocation(250, 256), 140, 140);
    assertLocation(sourceMap.getLocation(300, 301), 160, 160);
    assertLocation(sourceMap.getLocation(302, 303), 161, 161);
    assertLocation(sourceMap.getLocation(304, 305), 162, 162);

    // start is not mapped
    assertThat(sourceMap.getLocation(100, 207)).isEmpty();
    // end is not mapped
    assertThat(sourceMap.getLocation(207, 209)).isEmpty();
  }

  private void assertLocation(Optional<SourceMap.Location> location, int expectedStart, int expectedEnd) {
    assertThat(location).isPresent();
    SourceMap.Location loc = location.get();
    assertThat(loc.startLine()).isEqualTo(expectedStart);
    assertThat(loc.endLine()).isEqualTo(expectedEnd);
  }

  @Test
  void sourcemap_should_be_instantiated_lazily() throws Exception {
    String smap = "SMAP\n" +
      "index_jsp.java\n" +
      "JSP\n" +
      "*S JSP\n" +
      "*F\n" +
      "+ 0 index.jsp\n" +
      "index.jsp\n" +
      "*L\n" +
      "1,6:116,0\n" +
      "*E\n";

    SmapFile smapFile = new SmapFile(tmp, smap);
    GeneratedFile generatedFile = new GeneratedFile(tmp.resolve("index_jsp.java"), inputFile);
    generatedFile.addSmap(smapFile);

    SourceMap actual = generatedFile.sourceMap();
    assertThat(actual).isSameAs(generatedFile.sourceMap());
  }

  private static InputFile inputFileFromPath(Path path) {
    return new TestInputFileBuilder("", path.toString()).build();
  }
}
