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
import com.google.protobuf.TextFormat;
import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.semsitter.wire.Document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.semsitter.TranslationStateFixtures.kind;
import static org.sonar.java.semsitter.TranslationStateFixtures.mutate;
import static org.sonar.java.semsitter.TranslationStateFixtures.recorded;
import static org.sonar.java.semsitter.TranslationStateFixtures.where;

/**
 * Translates documents a real SemSitter server answered for the sources next to them in the test resources, with the
 * type declarations it answered for them.
 */
class SemSitterTranslatorTest {

  private static final Function<Set<String>, JsonObject> TYPES_ALREADY_SENT = names -> new JsonObject();

  @Test
  void enum_constant_calls_the_enum_constructor() throws Exception {
    List<EnumConstantTree> constants = enumConstants(translate("Outer", recordedTypes("Outer")));

    assertThat(constants).extracting(constant -> constant.initializer().methodSymbol().name()).containsExactly("<init>", "<init>");
    assertThat(constants).allSatisfy(constant -> assertThat(((VariableTree) constant).symbol().isVariableSymbol()).isTrue());
  }

  @Test
  void enum_constant_of_an_unknown_enum_calls_an_unknown_constructor() throws Exception {
    List<EnumConstantTree> constants = enumConstants(translate("Outer", TYPES_ALREADY_SENT));

    assertThat(constants).extracting(constant -> constant.initializer().methodSymbol()).containsOnly(Symbol.MethodSymbol.UNKNOWN_METHOD);
    assertThat(constants).allSatisfy(constant -> assertThat(((VariableTree) constant).symbol().isVariableSymbol()).isTrue());
  }

  @Test
  void an_unresolved_method_is_owned_by_the_unknown_type() {
    JMethodSymbol method = new JMethodSymbol(new JSema(), null, "log");
    method.unknown(true);

    assertThat(method.owner()).isSameAs(Symbol.TypeSymbol.UNKNOWN_TYPE);
    assertThat(((Symbol.TypeSymbol) method.owner()).memberSymbols()).isEmpty();
  }

  @Test
  void a_broken_document_is_rejected_at_its_first_missing_node() {
    Document missingSemicolon = mutate(recorded("Imports"), where(kind(";"), node -> node.toBuilder().setMissing(true).build())).toBuilder().setHasError(true).build();
    Document noProblemNode = recorded("Imports").toBuilder().setHasError(true).build();

    assertThatThrownBy(() -> TranslationStateFixtures.translate(missingSemicolon, 21)).isInstanceOf(RecognitionException.class)
      .hasMessage("Parse error at line 1 column 25: Syntax error");
    assertThatThrownBy(() -> TranslationStateFixtures.translate(noProblemNode, 21)).isInstanceOf(RecognitionException.class)
      .hasMessage("Parse error at line 1 column 1: Syntax error");
  }

  private static ClassTree implicitClass(Document document) {
    return (ClassTree) TranslationStateFixtures.translate(document, 25).types().get(0);
  }

  private static CompilationUnitTree translate(String name, Function<Set<String>, JsonObject> types) throws Exception {
    Document.Builder document = Document.newBuilder();
    try (InputStream input = resource(name + ".textproto")) {
      TextFormat.merge(new InputStreamReader(input, StandardCharsets.UTF_8), document);
    }
    return SemSitterTranslator.translate(document.build(), types, pkg -> null, true, 17).get();
  }

  private static Function<Set<String>, JsonObject> recordedTypes(String name) throws IOException {
    JsonObject recorded;
    try (InputStream input = resource(name + ".types.json")) {
      recorded = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
    }
    return new SessionTypes(names -> {
      JsonObject answer = new JsonObject();
      names.stream().filter(recorded::has).forEach(type -> answer.add(type, recorded.get(type)));
      return answer;
    })::get;
  }

  private static InputStream resource(String name) {
    return Objects.requireNonNull(SemSitterTranslatorTest.class.getResourceAsStream("translator/" + name), name);
  }

  private static List<EnumConstantTree> enumConstants(CompilationUnitTree unit) {
    List<EnumConstantTree> constants = new ArrayList<>();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitEnumConstant(EnumConstantTree tree) {
        constants.add(tree);
        super.visitEnumConstant(tree);
      }
    });
    return constants;
  }

  private static Map<String, Object> annotationValues(CompilationUnitTree unit) {
    List<MethodTree> methods = new ArrayList<>();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethod(MethodTree tree) {
        methods.add(tree);
        super.visitMethod(tree);
      }
    });
    return methods.stream()
      .filter(method -> !method.symbol().metadata().annotations().isEmpty())
      .collect(Collectors.toMap(method -> method.simpleName().name(), SemSitterTranslatorTest::firstValue));
  }

  private static Object firstValue(MethodTree method) {
    SymbolMetadata.AnnotationInstance annotation = method.symbol().metadata().annotations().get(0);
    Object value = annotation.values().get(0).value();
    return value instanceof Object[] array ? Arrays.asList(array) : value;
  }
}
