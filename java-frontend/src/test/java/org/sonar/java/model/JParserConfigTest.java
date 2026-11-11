/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.TestUtils;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.JParserConfig.shouldEnablePreviewFlag;

class JParserConfigTest {
  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.INFO);

  @Test
  void should_enable_preview() {
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl())).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(8))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(11))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(16))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(17))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(18))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(19))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(20))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(42))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(42, true))).isTrue();

    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8"))).isFalse();
    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8", "True"))).isTrue();
  }

  @Test
  void a_debug_message_is_logged_when_shouldIgnoreUnnamedModuleForSplitPackage_is_set() {
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList());
    assertThat(logTester.logs()).isEmpty();
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), false);
    assertThat(logTester.logs()).isEmpty();
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), true);
    assertThat(logTester.logs()).containsExactly("The Java analyzer will ignore the unnamed module for split packages.");
  }

  @Test
  void files_to_scan_are_the_analyzed_files_when_there_is_no_sourcepath(@TempDir Path root) throws IOException {
    Path analyzed = Files.writeString(root.resolve("A.java"), "class A {}");
    assertThat(JParserConfig.filesToScan(List.of(analyzed), List.of())).containsExactly(analyzed);
  }

  @Test
  void files_to_scan_add_the_java_files_of_the_sourcepath_once(@TempDir Path root) throws IOException {
    Path sources = Files.createDirectories(root.resolve("src/main/java/p"));
    Path analyzed = Files.writeString(sources.resolve("A.java"), "package p; class A {}");
    Path other = Files.writeString(sources.resolve("B.java"), "package p; class B {}");
    Path nested = Files.writeString(Files.createDirectories(sources.resolve("q")).resolve("C.java"), "package p.q; class C {}");
    Files.writeString(sources.resolve("notes.txt"), "not java");
    File sourceRoot = root.resolve("src/main/java").toFile();

    List<Path> files = JParserConfig.filesToScan(List.of(analyzed), List.of(sourceRoot, sourceRoot, root.resolve("missing").toFile()));

    assertThat(files.get(0)).isEqualTo(analyzed);
    assertThat(files).containsExactlyInAnyOrder(analyzed, other.toAbsolutePath(), nested.toAbsolutePath());
  }

  @Test
  void sourcepath_files_resolve_types_but_are_not_analyzed(@TempDir Path root) throws IOException {
    Path sources = root.resolve("src/main/java");
    Files.writeString(Files.createDirectories(sources.resolve("com/a")).resolve("Util.java"), """
      package com.a;
      public class Util {
        public static String make() { return ""; }
      }
      """);
    Path example = Files.writeString(Files.createDirectories(sources.resolve("com/b")).resolve("Example.java"), """
      package com.b;
      import com.a.Util;
      class Example {
        int m() { return Util.make().length(); }
      }
      """);
    InputFile inputFile = TestUtils.inputFile(example.toFile());
    List<InputFile> parsed = new ArrayList<>();
    List<Symbol.MethodSymbol> invoked = new ArrayList<>();

    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList())
      .withSourcepath(List.of(sources.toFile()))
      .parse(List.of(inputFile), () -> false, new AnalysisProgress(1), (file, result) -> {
        parsed.add(file);
        try {
          result.get().accept(new BaseTreeVisitor() {
            @Override
            public void visitMethodInvocation(MethodInvocationTree tree) {
              invoked.add(tree.methodSymbol());
              super.visitMethodInvocation(tree);
            }
          });
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      });

    assertThat(parsed).containsExactly(inputFile);
    assertThat(invoked).extracting(symbol -> symbol.owner().type().fullyQualifiedName() + "#" + symbol.name())
      .containsExactlyInAnyOrder("java.lang.String#length", "com.a.Util#make");
  }
}
