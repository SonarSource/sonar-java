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

import com.google.gson.JsonObject;
import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.model.JParseResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Translates every recorded replay sample. The tree must have the shape ECJ gives for the same source, and the semantic
 * information must be the approved one in {@code replay/X.expected.txt}. Run with {@code -Dsemsitter.approve=true}
 * to approve the current translation.
 */
class ReplayTest {

  private static JsonObject types;
  private static JsonObject packages;

  @TempDir
  Path baseDir;

  @BeforeAll
  static void loadRecordings() throws IOException {
    types = Replays.readJson(Replays.TYPES);
    packages = Replays.readJson(Replays.PACKAGES);
  }

  static List<String> samples() throws IOException {
    return Replays.samples();
  }

  @ParameterizedTest
  @MethodSource("samples")
  void same_tree_shape_as_ecj(String sample) throws Exception {
    String source = Replays.source(sample);
    JParseResult ecj = Replays.parseWithEcj(baseDir, sample, source);
    JParseResult semSitter = translate(sample);
    assumeFalse(unsupported(semSitter), "parsed by the default frontend");

    if (Replays.document(sample).getHasError()) {
      assertThatThrownBy(ecj::get).isInstanceOf(RecognitionException.class);
      assertThatThrownBy(semSitter::get).isInstanceOf(RecognitionException.class);
    } else {
      assertThat(TreeDescription.shape(semSitter.get())).containsExactlyElementsOf(TreeDescription.shape(ecj.get()));
    }
  }

  @ParameterizedTest
  @MethodSource("samples")
  void approved_semantics(String sample) throws Exception {
    List<String> actual = describe(translate(sample));

    Path approved = Replays.approved(sample);
    if (Boolean.getBoolean("semsitter.approve")) {
      Replays.write(approved, String.join("\n", actual) + "\n");
    }
    assertThat(actual).containsExactlyElementsOf(Replays.read(approved).lines().toList());
  }

  private static List<String> describe(JParseResult result) {
    try {
      return TreeDescription.describe(result.get());
    } catch (Exception failure) {
      return List.of("FAILURE " + failure.getMessage());
    }
  }

  private static JParseResult translate(String sample) throws IOException {
    SessionTypes session = new SessionTypes(names -> {
      JsonObject answer = new JsonObject();
      names.stream().filter(types::has).forEach(name -> answer.add(name, types.get(name)));
      return answer;
    });
    try {
      return SemSitterTranslator.translate(Replays.document(sample), session::get, ReplayTest::hasPackage, true, Replays.javaVersion(sample));
    } catch (RecognitionException | UnsupportedConstructException failure) {
      return JParseResult.failure(failure);
    }
  }

  private static boolean unsupported(JParseResult result) {
    try {
      result.get();
      return false;
    } catch (Exception failure) {
      return failure instanceof UnsupportedConstructException;
    }
  }

  private static Boolean hasPackage(String name) {
    if (!packages.has(name) || packages.get(name).isJsonNull()) {
      return null;
    }
    return packages.get(name).getAsBoolean();
  }
}
