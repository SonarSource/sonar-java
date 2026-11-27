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

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonarsource.semsitter.wire.Node;
import org.sonarsource.semsitter.wire.Ref;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.semsitter.TranslationStateFixtures.all;
import static org.sonar.java.semsitter.TranslationStateFixtures.mutate;
import static org.sonar.java.semsitter.TranslationStateFixtures.recorded;
import static org.sonar.java.semsitter.TranslationStateFixtures.text;
import static org.sonar.java.semsitter.TranslationStateFixtures.translate;
import static org.sonar.java.semsitter.TranslationStateFixtures.unresolved;
import static org.sonar.java.semsitter.TranslationStateFixtures.where;

class NameTranslatorTest {

  private static final Predicate<Node> REFERENCE = node -> node.getRefsCount() > 0;

  @Test
  void a_static_import_of_a_missing_type_brings_in_nothing() {
    Predicate<Node> argument = REFERENCE.and(text("argument"));
    CompilationUnitTree unit = translate(mutate(recorded("Unresolved"), where(argument, node -> unresolved(node.toBuilder().setText("help").build()))), 21);

    assertThat(describe(unit, "help")).isNotEmpty().containsOnly(".!unknown! !Unknown!");
  }

  @Test
  void a_type_name_denotes_its_single_type_import() {
    Predicate<Node> list = REFERENCE.and(text("List"));
    CompilationUnitTree unit = translate(mutate(recorded("Imports"), where(list, node -> node.toBuilder().clearRefs()
      .addRefs(Ref.newBuilder().setAccess("ref").setOutcome("bound").setTarget("java.util.Collection`1")).build())), 21);

    assertThat(describe(unit, "List")).isNotEmpty().allMatch(description -> description.endsWith(" java.util.List"));
  }

  private static List<String> describe(CompilationUnitTree unit, String name) {
    return all(unit, IdentifierTree.class).stream()
      .filter(identifier -> name.equals(identifier.name()) && identifier.identifierToken().range().start().line() > 4)
      .map(identifier -> (identifier.symbol().owner() == null ? "" : identifier.symbol().owner().name()) + "." + identifier.symbol().name() + " " + identifier.symbolType().fullyQualifiedName())
      .toList();
  }
}
