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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.semsitter.TranslationStateFixtures.all;
import static org.sonar.java.semsitter.TranslationStateFixtures.kind;
import static org.sonar.java.semsitter.TranslationStateFixtures.leaf;
import static org.sonar.java.semsitter.TranslationStateFixtures.mutate;
import static org.sonar.java.semsitter.TranslationStateFixtures.recorded;
import static org.sonar.java.semsitter.TranslationStateFixtures.text;
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

  @Test
  void top_level_semicolons_are_empty_statements() {
    Document imports = recorded("Imports");
    Node program = imports.getTree().toBuilder()
      .addChildren(12, leaf(";", ";", 13, 0))
      .addChildren(leaf(";", ";", 200, 0))
      .build();
    Node implicitProgram = recorded("java25/ImplicitClass").getTree().toBuilder().addChildren(leaf(";", ";", 19, 0)).build();

    CompilationUnitTree unit = TranslationStateFixtures.translate(imports.toBuilder().setTree(program).build(), 21);
    CompilationUnitTree implicit = TranslationStateFixtures.translate(recorded("java25/ImplicitClass").toBuilder().setTree(implicitProgram).build(), 25);

    assertThat(unit.imports()).hasSize(13).last().matches(tree -> tree.is(Tree.Kind.EMPTY_STATEMENT));
    assertThat(unit.types()).extracting(Tree::kind).containsExactly(Tree.Kind.CLASS, Tree.Kind.EMPTY_STATEMENT);
    assertThat(((ClassTree) implicit.types().get(0)).members()).last().matches(tree -> tree.is(Tree.Kind.EMPTY_STATEMENT));
  }

  @Test
  void records_are_classes_before_java_16() {
    CompilationUnitTree before = TranslationStateFixtures.translate(recorded("Declarations"), 15);
    CompilationUnitTree after = TranslationStateFixtures.translate(recorded("Declarations"), 16);

    assertThat(all(before, ClassTree.class)).noneMatch(tree -> tree.is(Tree.Kind.RECORD));
    assertThat(all(after, ClassTree.class)).anyMatch(tree -> tree.is(Tree.Kind.RECORD));
  }

  @Test
  void the_implicit_class_is_the_one_the_scan_declared_whatever_the_path() {
    Document document = recorded("java25/ImplicitClass");

    assertThat(implicitClass(document.toBuilder().setPath("dir/ImplicitClass").build()).symbol().name()).isEqualTo("ImplicitClass");
    assertThat(implicitClass(document).symbol().name()).isEqualTo("ImplicitClass");
    assertThat(implicitClass(document.toBuilder().clearPath().build()).symbol().name()).isEqualTo("ImplicitClass");
  }

  @Test
  void a_line_comment_ending_with_a_carriage_return_drops_it() {
    Document document = mutate(recorded("java25/ImplicitClass"), where(text("/// Greets `name`."), node -> node.toBuilder().setText("/// Greets `name`.\r").build()));

    MethodTree greet = (MethodTree) implicitClass(document).members().get(2);

    assertThat(greet.firstToken().trivias()).extracting(SyntaxTrivia::comment).containsExactly("/// Greets `name`.");
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
}
