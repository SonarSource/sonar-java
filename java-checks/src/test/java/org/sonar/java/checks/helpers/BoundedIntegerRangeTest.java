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

import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Most of {@link BoundedIntegerRange}'s behaviour is exercised end-to-end through
 * {@code IntegerSubtractionInComparisonCheckSample}, matching how the rest of this codebase tests checks. This class
 * only covers the one scenario that cannot be expressed as a real {@code compare()}/{@code compareTo()} method body:
 * {@link BoundedIntegerRange#subtractionCannotOverflow} is only ever called by the check on {@code int}-typed
 * operands, so its {@code long} constant handling has no reachable call site and needs a direct test.
 */
class BoundedIntegerRangeTest {

  @Test
  void private_constructor() throws Exception {
    Constructor<BoundedIntegerRange> constructor = BoundedIntegerRange.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void long_constant_operands_cannot_overflow() {
    List<StatementTree> statements = JParserTestUtils.methodBody(JParserTestUtils.newCode("void m() { long x = 5L - 3L; }"));
    ExpressionTree initializer = JParserTestUtils.initializerFromVariableDeclarationStatement(statements.get(0));
    BinaryExpressionTree binary = (BinaryExpressionTree) initializer;
    assertThat(BoundedIntegerRange.subtractionCannotOverflow(binary.leftOperand(), binary.rightOperand())).isTrue();
  }

}
