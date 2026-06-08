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
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import static org.assertj.core.api.Assertions.assertThat;


class TryCatchUtilsTest {

  @Test
  void testGetCaughtTypes() {
    assertThat(TryCatchUtils.getCaughtTypes(
      parseTry("try {} catch (ExceptionABCD e) {} ").catches().get(0)
    )).first().matches(type -> type.name().equals("ExceptionABCD"));
  }

  private TryStatementTree parseTry(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse("void main() { %s }".formatted(code));
    ClassTree classTree = (ClassTree) compilationUnitTree.types().get(0);
    MethodTree methodTree = (MethodTree) classTree.members().get(0);
    return (TryStatementTree) methodTree.block().body().get(0);
  }
}

