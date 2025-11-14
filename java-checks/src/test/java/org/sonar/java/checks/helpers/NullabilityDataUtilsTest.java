/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.helpers.NullabilityDataUtils.nullabilityAsString;

class NullabilityDataUtilsTest {

  @Test
  void test_no_annotation_nullability_data() {
    JavaTree.CompilationUnitTreeImpl cut = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse("" +
      "class A {" +
      " Object o;" +
      "}");
    SymbolMetadata.NullabilityData nullabilityData = getNullabilityDataOfFirstMember(cut);
    assertThat(nullabilityAsString(nullabilityData)).isEmpty();
  }

  @Test
  void test_unknown_nullability_data() {
    JavaTree.CompilationUnitTreeImpl cut = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse("" +
      "class A {" +
      " @Unknown" +
      " Object o;" +
      "}");
    SymbolMetadata.NullabilityData nullabilityData = getNullabilityDataOfFirstMember(cut);
    assertThat(nullabilityAsString(nullabilityData)).isEmpty();
  }

  private static SymbolMetadata.NullabilityData getNullabilityDataOfFirstMember(JavaTree.CompilationUnitTreeImpl cut) {
    VariableTree o = (VariableTree) ((ClassTree) cut.types().get(0)).members().get(0);
    return o.symbol().metadata().nullabilityData();
  }

}
