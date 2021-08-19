/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import static org.assertj.core.api.Assertions.assertThat;

class QuickFixHelperTest {

  @Test
  void nextToken() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A { void foo() {} }");
    ClassTree a = (ClassTree) cut.types().get(0);
    MethodTree foo = (MethodTree) a.members().get(0);

    assertThat(QuickFixHelper.nextToken(foo.simpleName())).isEqualTo(foo.openParenToken());

    // through non-existing nodes (modifiers of method)
    assertThat(QuickFixHelper.nextToken(a.openBraceToken()))
      .isEqualTo(QuickFixHelper.nextToken(foo.modifiers()))
      .isEqualTo(foo.returnType().firstToken());

    // need to go through parent
    assertThat(QuickFixHelper.nextToken(foo.block().lastToken())).isEqualTo(a.closeBraceToken());

    // end of file
    assertThat(QuickFixHelper.nextToken(a.closeBraceToken()))
      .isEqualTo(QuickFixHelper.nextToken(cut))
      .isEqualTo(cut.lastToken());
    assertThat(((InternalSyntaxToken) cut.lastToken()).isEOF()).isTrue();
  }

  @Test
  void previousToken() {
    CompilationUnitTree cut = JParserTestUtils.parse("class A { void foo() {} }");
    ClassTree a = (ClassTree) cut.types().get(0);
    MethodTree foo = (MethodTree) a.members().get(0);

    assertThat(QuickFixHelper.previousToken(foo.simpleName())).isEqualTo(foo.returnType().lastToken());

    // through non-existing nodes (modifiers of method)
    assertThat(QuickFixHelper.previousToken(a.openBraceToken())).isEqualTo(a.simpleName().lastToken());

    // need to go through parent
    assertThat(QuickFixHelper.previousToken(foo.returnType())).isEqualTo(a.openBraceToken());

    // start of file
    assertThat(QuickFixHelper.previousToken(a.declarationKeyword())).isEqualTo(a.declarationKeyword());
  }

}
