/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.SourceMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedFileTest {

  @TempDir
  Path tmp;
  Path expected;
  private GeneratedFile actual;

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.DEBUG);
  private DefaultFileSystem fs;

  @BeforeEach
  void setUp() throws Exception {
    expected = tmp.resolve("file.jsp");
    Files.write(expected, "content".getBytes(UTF_8));
    fs = new DefaultFileSystem(tmp);
    actual = new GeneratedFile(expected);
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
    String computedContent = actual.contents();
    String cachedContent = actual.contents();
    assertEquals("content", computedContent);
    assertSame(computedContent, cachedContent);
    String computedMd5Hash = actual.md5Hash();
    String cachedMd5Hash = actual.md5Hash();
    assertEquals("9a0364b9e99bb480dd25e1f0284c8555", computedMd5Hash);
    assertSame(computedMd5Hash, cachedMd5Hash);
    try (InputStream is = actual.inputStream()) {
      assertEquals("content", IOUtils.toString(is, UTF_8));
    }
    assertFalse(actual.isEmpty());
    assertEquals(UTF_8, actual.charset());
    assertEquals(expected.toString(), actual.key());
    assertTrue(actual.isFile());
    assertEquals("java", actual.language());
    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  void test_not_implemented() {
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
    String smap = """
      SMAP
      index_jsp.java
      JSP
      *S JSP
      *F
      + 0 index.jsp
      index.jsp
      *L
      1,6:116,0
      123:207
      130,3:210
      140:250,7
      160,3:300,2
      *E
      """;

    InputFile inputFile = inputFileFromPath(tmp.resolve("src/main/webapp/index.jsp"));
    fs.add(inputFile);
    SmapFile smapFile = new SmapFile(tmp, smap, tmp.resolve("src/main/webapp"), fs);
    GeneratedFile generatedFile = new GeneratedFile(tmp.resolve("index_jsp.java"));
    generatedFile.addSmap(smapFile);

    GeneratedFile.SourceMapImpl sourceMap = ((GeneratedFile.SourceMapImpl) generatedFile.sourceMap());
    SourceMap.Location location = sourceMap.getLocation(116, 116).get();
    assertThat(location.file()).isEqualTo(inputFile);

    assertLocation(sourceMap.getLocation(116, 116), 1, 6);
    assertLocation(sourceMap.getLocation(207, 207), 123, 123);
    assertLocation(sourceMap.getLocation(210, 212), 130, 132);
    assertLocation(sourceMap.getLocation(250, 256), 140, 140);
    assertLocation(sourceMap.getLocation(300, 301), 160, 160);
    assertLocation(sourceMap.getLocation(302, 303), 161, 161);
    assertLocation(sourceMap.getLocation(304, 305), 162, 162);

    InternalSyntaxToken token = new InternalSyntaxToken(116, 0, "something", Collections.emptyList(), false);
    assertLocation(sourceMap.sourceMapLocationFor(token), 1, 6);

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
  void sourcemap_should_be_instantiated_lazily() {
    String smap = """
      SMAP
      index_jsp.java
      JSP
      *S JSP
      *F
      + 0 index.jsp
      index.jsp
      *L
      1,6:116,0
      *E
      """;

    SmapFile smapFile = new SmapFile(tmp, smap, tmp, fs);
    GeneratedFile generatedFile = new GeneratedFile(tmp.resolve("index_jsp.java"));
    generatedFile.addSmap(smapFile);

    SourceMap actualSourceMap = generatedFile.sourceMap();
    assertThat(actualSourceMap).isSameAs(generatedFile.sourceMap());
  }

  @Test
  void test_multiple_files() {
    String smap = """
      SMAP
      index_jsp.java
      JSP
      *S JSP
      *F
      + 0 index.jsp
      index.jsp
      + 1 index2.jsp
      index2.jsp
      *L
      1:1
      2#1:2
      *E
      """;

    Path uriRoot = tmp.resolve("src/main/webapp");
    InputFile indexJsp = inputFileFromPath(uriRoot.resolve("index.jsp"));
    InputFile index2Jsp = inputFileFromPath(uriRoot.resolve("index2.jsp"));
    fs.add(indexJsp);
    fs.add(index2Jsp);
    SmapFile smapFile = new SmapFile(tmp, smap, uriRoot, fs);
    GeneratedFile generatedFile = new GeneratedFile(tmp.resolve("index_jsp.java"));
    generatedFile.addSmap(smapFile);

    GeneratedFile.SourceMapImpl sourceMap = ((GeneratedFile.SourceMapImpl) generatedFile.sourceMap());

    Optional<SourceMap.Location> loc1 = sourceMap.getLocation(1, 1);
    assertThat(loc1.get().file()).isEqualTo(indexJsp);
    assertLocation(loc1, 1, 1);

    Optional<SourceMap.Location> loc2 = sourceMap.getLocation(2, 2);
    assertThat(loc2.get().file()).isEqualTo(index2Jsp);
    assertLocation(loc2, 2, 2);

    // spanning two input files would return start file
    assertThat(sourceMap.getLocation(1, 2).get().file()).isEqualTo(indexJsp);
  }

  private InputFile inputFileFromPath(Path path) {
    return new TestInputFileBuilder("", tmp.toFile(), path.toFile()).build();
  }
}
