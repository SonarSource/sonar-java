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
package org.sonar.java.semsitter;

import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.jdt.JDTParser;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parses the same sources with the default frontend and with a real SemSitter server, and expects the same trees and
 * the same semantic information. It runs only when {@value #EXECUTABLE} names a SemSitter server executable.
 */
class SemSitterParityTest {

  private static final String EXECUTABLE = "semsitter.executable";

  @TempDir
  Path project;

  @BeforeAll
  static void locateServer() {
    String path = System.getProperty(EXECUTABLE);
    Assumptions.assumeTrue(path != null && !path.isBlank(), EXECUTABLE + " is not set");
  }

  @Test
  void class_with_fields_and_methods() throws Exception {
    assertSameTree("""
      package p;

      import java.util.List;

      public class A {
        private final int count = 3;
        private List<String> names;

        public int twice(int value) {
          return value * 2 + count;
        }

        String first() {
          return names.get(0);
        }
      }
      """);
  }

  @Test
  void generics_lambdas_and_streams() throws Exception {
    assertSameTree("""
      import java.util.List;
      import java.util.stream.Collectors;

      class A {
        List<Integer> lengths(List<String> words) {
          return words.stream().map(w -> w.length()).filter(n -> n > 2).collect(Collectors.toList());
        }
      }
      """);
  }

  @Test
  void lambda_result_types_the_generic_call() throws Exception {
    assertSameTree("""
      import java.util.List;
      import java.util.stream.Collectors;

      class A {
        List<Integer> lengths(List<String> words) {
          return words.stream().map(w -> w.length()).collect(Collectors.toList());
        }
      }
      """);
  }

  @Test
  void array_length_has_an_unknown_owner() throws Exception {
    assertSameTree("""
      class A {
        int size(int[] values) {
          return values.length;
        }
      }
      """);
  }

  @Test
  void enums_records_and_interfaces() throws Exception {
    assertSameTree("""
      interface Shape { double area(); }

      record Circle(double radius) implements Shape {
        public double area() { return Math.PI * radius * radius; }
      }

      enum Color { RED, GREEN; Color next() { return values()[(ordinal() + 1) % values().length]; } }
      """);
  }

  @Test
  void control_flow_and_exceptions() throws Exception {
    assertSameTree("""
      class A {
        int run(String s) {
          try {
            for (int i = 0; i < s.length(); i++) {
              if (s.charAt(i) == 'x') {
                return i;
              }
            }
            switch (s) {
              case "a": return 1;
              default: break;
            }
          } catch (IllegalStateException | IllegalArgumentException e) {
            throw new RuntimeException(e);
          } finally {
            System.out.println("done");
          }
          return -1;
        }
      }
      """);
  }

  @Test
  void syntax_error_is_reported_as_a_parse_failure() throws IOException {
    InputFile broken = file("Broken.java", "class Broken { int x = ; }");

    JParseResult result = parseAll(semSitter(), broken).get(0);

    assertThatThrownBy(result::get).isInstanceOf(com.sonar.sslr.api.RecognitionException.class);
  }

  private void assertSameTree(String source) throws Exception {
    InputFile input = file("A.java", source);

    List<String> expected = describe(parseAll(JDTParser.create(JParser.Mode.BATCH, new JavaVersionImpl(21), List.of(), false, true), input).get(0));
    List<String> actual = describe(parseAll(semSitter(), input).get(0));

    int common = Math.min(expected.size(), actual.size());
    int mismatch = 0;
    while (mismatch < common && expected.get(mismatch).equals(actual.get(mismatch))) {
      mismatch++;
    }
    assertThat(actual.subList(Math.max(0, mismatch - 2), Math.min(actual.size(), mismatch + 4)))
      .as("first difference at node %d of %d", mismatch, expected.size())
      .containsExactlyElementsOf(expected.subList(Math.max(0, mismatch - 2), Math.min(expected.size(), mismatch + 4)));
    assertThat(actual).hasSameSizeAs(expected);
  }

  private JParser semSitter() {
    return new SemSitterParser(JParser.Mode.BATCH, new JavaVersionImpl(21), List.of(), root -> SemSitterClient.start(Path.of(System.getProperty(EXECUTABLE)), root),
      SemSitterTranslator::translate);
  }

  private static List<JParseResult> parseAll(JParser parser, InputFile input) {
    List<JParseResult> results = new ArrayList<>();
    parser.parse(List.of(input), () -> false, new AnalysisProgress(1), (file, result) -> results.add(result));
    return results;
  }

  private InputFile file(String name, String source) throws IOException {
    Files.writeString(project.resolve(name), source, StandardCharsets.UTF_8);
    return new TestInputFileBuilder("module", name)
      .setModuleBaseDir(project)
      .setCharset(StandardCharsets.UTF_8)
      .setLanguage("java")
      .setContents(source)
      .build();
  }

  private static List<String> describe(JParseResult result) throws Exception {
    List<String> lines = new ArrayList<>();
    describe((Tree) result.get(), lines);
    return lines;
  }

  private static void describe(Tree tree, List<String> lines) {
    StringBuilder line = new StringBuilder(tree.kind().name());
    SyntaxToken first = tree.firstToken();
    if (first != null) {
      line.append(" @").append(first.range().start().line()).append(':').append(first.range().start().column());
    }
    if (tree instanceof IdentifierTree identifier) {
      line.append(" id ").append(identifier.name()).append(" : ").append(identifier.symbolType().fullyQualifiedName()).append(' ').append(symbol(identifier.symbol()));
    } else if (tree instanceof MethodInvocationTree invocation) {
      line.append(" call ").append(symbol(invocation.methodSymbol())).append(" : ").append(invocation.symbolType().fullyQualifiedName());
    }
    lines.add(line.toString());
    JavaTree javaTree = (JavaTree) tree;
    if (javaTree.isLeaf()) {
      return;
    }
    for (Tree child : javaTree.getChildren()) {
      if (child != null) {
        describe(child, lines);
      }
    }
  }

  private static String symbol(Symbol symbol) {
    if (symbol.isUnknown()) {
      return "unknown";
    }
    String owner = symbol.owner() == null ? "" : symbol.owner().name() + ".";
    String kinds = String.valueOf(symbol.isMethodSymbol()) + symbol.isVariableSymbol() + symbol.isTypeSymbol();
    return owner + symbol.name() + " " + kinds;
  }
}
