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
package org.sonar.java.model.springcontext;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeToBeansIndexTest {

  private static final String TYPE = "com.example.MyService";
  private static final String MODULE_A = "moduleA";
  private static final String MODULE_B = "moduleB";

  // ---- same-module visibility -----------------------------------------------

  @Test
  void bean_in_same_module_is_visible() {
    var index = new TypeToBeansIndex();
    index.addBeanForType(TYPE, "myBean", MODULE_A, "com.example");

    assertThat(index.getNamesForType(TYPE, MODULE_A, Set.of())).containsExactly("myBean");
  }

  // ---- cross-module visibility via @ComponentScan ---------------------------

  @Test
  void bean_in_different_module_is_not_visible_without_component_scan() {
    var index = new TypeToBeansIndex();
    index.addBeanForType(TYPE, "myBean", MODULE_B, "com.example");

    assertThat(index.getNamesForType(TYPE, MODULE_A, Set.of())).isEmpty();
  }

  @Test
  void bean_is_visible_when_its_package_is_exactly_the_scanned_package() {
    var index = new TypeToBeansIndex();
    index.addBeanForType(TYPE, "myBean", MODULE_B, "com.example");

    assertThat(index.getNamesForType(TYPE, MODULE_A, Set.of("com.example"))).containsExactly("myBean");
  }

  @Test
  void bean_is_visible_when_its_package_is_a_subpackage_of_the_scanned_package() {
    var index = new TypeToBeansIndex();
    index.addBeanForType(TYPE, "myBean", MODULE_B, "com.example.service");

    assertThat(index.getNamesForType(TYPE, MODULE_A, Set.of("com.example"))).containsExactly("myBean");
  }

  @Test
  void bean_is_not_visible_when_its_package_is_a_prefix_but_not_a_subpackage() {
    // "com.exampleextended" starts with "com.example" as a string but is NOT within it
    var index = new TypeToBeansIndex();
    index.addBeanForType(TYPE, "myBean", MODULE_B, "com.exampleextended");

    assertThat(index.getNamesForType(TYPE, MODULE_A, Set.of("com.example"))).isEmpty();
  }

  @Test
  void bean_is_visible_when_matching_one_of_multiple_scanned_packages() {
    var index = new TypeToBeansIndex();
    index.addBeanForType(TYPE, "myBean", MODULE_B, "org.other.service");

    assertThat(index.getNamesForType(TYPE, MODULE_A, Set.of("com.example", "org.other"))).containsExactly("myBean");
  }

  // ---- unknown type ---------------------------------------------------------

  @Test
  void unknown_type_returns_empty_set() {
    var index = new TypeToBeansIndex();

    assertThat(index.getNamesForType("com.example.Unknown", MODULE_A, Set.of())).isEmpty();
  }

  // ---- getKeys --------------------------------------------------------------

  @Test
  void getKeys_reflects_all_registered_types() {
    var index = new TypeToBeansIndex();
    index.addBeanForType("com.example.Foo", "foo", MODULE_A, "com.example");
    index.addBeanForType("com.example.Bar", "bar", MODULE_A, "com.example");

    assertThat(index.getKeys()).containsExactlyInAnyOrder("com.example.Foo", "com.example.Bar");
  }
}
