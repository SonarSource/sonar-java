/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.filters;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.CommentRegularExpressionCheck;
import org.sonar.java.checks.naming.BadClassNameCheck;
import org.sonar.java.checks.naming.BadLocalVariableNameCheck;
import org.sonar.java.checks.naming.BadMethodNameCheck;

class GeneratedCodeFilterTest {

  @Test
  void test() {
    CommentRegularExpressionCheck commentRegularExpressionCheck = new CommentRegularExpressionCheck();
    commentRegularExpressionCheck.regularExpression = ".*alpha.*";
    FilterVerifier.newInstance().verify("src/test/files/filters/GeneratedCodeFilter.java", new GeneratedCodeFilter(),
      // activated rules
      commentRegularExpressionCheck,
      new BadClassNameCheck(),
      new BadMethodNameCheck(),
      new BadLocalVariableNameCheck());
  }
}
