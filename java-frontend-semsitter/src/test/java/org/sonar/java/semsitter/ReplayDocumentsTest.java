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
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.model.JParseResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Every recorded document is the one of its sample: the server and ECJ agree on whether it has a syntax error. */
class ReplayDocumentsTest {

  @TempDir
  Path baseDir;

  static List<String> samples() throws IOException {
    return Replays.samples();
  }

  @ParameterizedTest
  @MethodSource("samples")
  void recorded_document_agrees_with_ecj_on_syntax_errors(String sample) throws Exception {
    String source = Replays.source(sample);
    JParseResult ecj = Replays.parseWithEcj(baseDir, sample, source);

    if (Replays.document(sample).getHasError()) {
      assertThatThrownBy(ecj::get).isNotNull();
    } else {
      assertThatCode(ecj::get).doesNotThrowAnyException();
    }
    assertThat(Replays.document(sample).getTree().getChildrenCount()).isPositive();
  }

  @ParameterizedTest
  @MethodSource("samples")
  void recorded_document_is_read_by_the_semantic_model(String sample) {
    JSema sema = new JSema();
    sema.loadPackages(name -> null);
    sema.loadTypes(names -> new JsonObject());

    assertThatCode(() -> sema.ingestDocument(Replays.document(sample))).doesNotThrowAnyException();
  }
}
