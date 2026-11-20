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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTypesTest {

  private static final Map<String, String> DECLARATIONS = Map.of(
    "p.Child", "{\"kind\":\"class\",\"super\":\"p.Parent`1\",\"interfaces\":[\"p.Marker\"]}",
    "p.Parent", "{\"kind\":\"class\",\"super\":\"java.lang.Object\",\"interfaces\":null}",
    "p.Marker", "{\"kind\":\"interface\"}",
    "java.lang.Object", "{\"kind\":\"class\"}");

  private final List<Set<String>> asked = new ArrayList<>();
  private final Set<String> sent = new HashSet<>();

  private final SessionTypes types = new SessionTypes(names -> {
    asked.add(Set.copyOf(names));
    JsonObject answer = new JsonObject();
    for (String name : names) {
      if (DECLARATIONS.containsKey(name) && sent.add(name)) {
        answer.add(name, JsonParser.parseString(DECLARATIONS.get(name)));
        supertypesOf(name).forEach(supertype -> {
          if (sent.add(supertype)) {
            answer.add(supertype, JsonParser.parseString(DECLARATIONS.get(supertype)));
          }
        });
      }
    }
    return answer;
  });

  @Test
  void a_declaration_sent_once_is_given_again_with_its_supertypes() {
    assertThat(types.get(Set.of("p.Child")).keySet()).containsExactlyInAnyOrder("p.Child", "p.Parent", "p.Marker", "java.lang.Object");
    assertThat(types.get(Set.of("p.Child")).keySet()).containsExactlyInAnyOrder("p.Child", "p.Parent", "p.Marker", "java.lang.Object");
    assertThat(types.get(Set.of("p.Parent")).keySet()).containsExactlyInAnyOrder("p.Parent", "java.lang.Object");
    assertThat(asked).containsExactly(Set.of("p.Child"));
  }

  @Test
  void a_name_the_server_does_not_declare_is_asked_once() {
    assertThat(types.get(Set.of("x.Missing")).keySet()).isEmpty();
    assertThat(types.get(Set.of("x.Missing", "p.Marker")).keySet()).containsExactly("p.Marker");
    assertThat(asked).containsExactly(Set.of("x.Missing"), Set.of("p.Marker"));
  }

  @Test
  void the_generic_arity_suffix_is_not_part_of_the_name() {
    assertThat(SessionTypes.erase("p.Map`2.Entry`2")).isEqualTo("p.Map.Entry");
    assertThat(types.get(Set.of("p.Parent`1")).keySet()).containsExactlyInAnyOrder("p.Parent", "java.lang.Object");
  }

  private static List<String> supertypesOf(String name) {
    return switch (name) {
      case "p.Child" -> List.of("p.Parent", "p.Marker", "java.lang.Object");
      case "p.Parent" -> List.of("java.lang.Object");
      default -> List.of();
    };
  }
}
