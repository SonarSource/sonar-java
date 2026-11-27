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
import com.google.gson.JsonParser;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonarsource.semsitter.wire.Document;

import static org.assertj.core.api.Assertions.assertThat;

class JSemaEdgesTest {

  @TempDir
  Path baseDir;

  private final JSema sema = new JSema();

  @Test
  void local_classes_are_numbered_per_enclosing_class() {
    Document document = Document.newBuilder()
      .addSymbols(type("p.C#f()$Local"))
      .addSymbols(type("p.C#g()$Local"))
      .addSymbols(type("p.C#f()$Local$Inner"))
      .addSymbols(type("p.C#f()$"))
      .addSymbols(type("p.C#h"))
      .addSymbols(org.sonarsource.semsitter.wire.Symbol.newBuilder().setKind("type").setName("empty"))
      .build();
    sema.ingestDocument(document);
    sema.ingestDocument(document);

    assertThat(sema.sourceName("p.C$1Local")).isIn("p.C#f()$Local", "p.C#g()$Local");
    assertThat(sema.sourceName("p.C$2Local")).isIn("p.C#f()$Local", "p.C#g()$Local").isNotEqualTo(sema.sourceName("p.C$1Local"));
    assertThat(sema.sourceName("p.C#f()$")).isEqualTo("p.C#f()$");
    assertThat(sema.sourceName("p.C#h")).isEqualTo("p.C#h");
    assertThat(sema.symbols).doesNotContainKey("");
  }

  @Test
  void class_types_by_special_names() {
    assertThat(sema.getClassType("<nulltype>").isNullType()).isTrue();
    assertThat(sema.getClassType("!Unknown![]").fullyQualifiedName()).endsWith("[]");
    assertThat(sema.getClassType("int[]").isArray()).isTrue();
    assertThat(sema.asJType(Type.UNKNOWN).isUnknown()).isTrue();
  }

  @Test
  void empty_ids_and_labels_resolve_to_the_unknown_symbol() {
    sema.ingestDocument(Document.newBuilder()
      .addSymbols(org.sonarsource.semsitter.wire.Symbol.newBuilder().setId("p.A#m()#label").setKind("label").setName("label"))
      .build());

    assertThat(sema.resolve(null)).isSameAs(sema.unknownSymbol);
    assertThat(sema.resolve("")).isSameAs(sema.unknownSymbol);
    assertThat(sema.resolve("p.A#m()#label")).isSameAs(sema.unknownSymbol);
  }

  @Test
  void types_the_loader_cannot_answer_stay_known() {
    sema.loadTypes(names -> new JsonObject());
    sema.typeLoader = names -> null;

    assertThat(sema.typeSymbol("p.Late").isUnknown()).isFalse();
    assertThat(sema.typeSymbol("p.Late")).isSameAs(sema.typeSymbol("p.Late"));
  }

  @Test
  void packages_are_known_from_the_loader_or_from_loaded_types() {
    assertThat(sema.knowsPackage("p")).isFalse();
    sema.loadPackages(name -> "yes".equals(name) ? Boolean.TRUE : null);
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(JsonParser.parseString("{\"p.A\": {\"kind\": \"class\"}, \"q.r.B\": {\"kind\": \"class\"}}").getAsJsonObject());

    assertThat(sema.knowsPackage("yes")).isTrue();
    assertThat(sema.knowsPackage("p")).isTrue();
    assertThat(sema.knowsPackage("q")).isFalse();
    assertThat(sema.knowsPackage("q.r")).isTrue();
  }

  @Test
  void uses_move_between_overloads() throws Exception {
    CompilationUnitTree unit = (CompilationUnitTree) Replays.parseWithEcj(baseDir, "A", "class A {}").get();
    IdentifierTree use = ((ClassTree) unit.types().get(0)).simpleName();
    sema.use("p.A#m(int)", use);

    sema.moveUse("p.A#m(int)", "p.A#m(long)", use);
    assertThat(sema.usages.get("p.A#m(int)")).isEmpty();
    assertThat(sema.usages.get("p.A#m(long)")).containsExactly(use);

    sema.moveUse(null, null, use);
    sema.moveUse("p.A#none()", null, use);
    assertThat(sema.usages).containsOnlyKeys("p.A#m(int)", "p.A#m(long)");
  }

  @Test
  void the_environment_cleaner_holds_nothing() {
    Runnable cleaner = sema.getEnvironmentCleaner();
    cleaner.run();

    assertThat(cleaner).isNotSameAs(new JSema().getEnvironmentCleaner());
  }

  private static org.sonarsource.semsitter.wire.Symbol.Builder type(String id) {
    return org.sonarsource.semsitter.wire.Symbol.newBuilder().setId(id).setKind("type").setName("T");
  }
}
