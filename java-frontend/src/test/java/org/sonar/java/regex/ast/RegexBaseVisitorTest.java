/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.regex.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.RegexParserTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RegexBaseVisitorTest {

  @Nested
  class VisitTest {
    @Test
    void visitDisjunction() {
      List<RegexTree> items = new ArrayList<>();

      RegexBaseVisitor visitor = new RegexBaseVisitor() {
        @Override
        public void visitPlainCharacter(PlainCharacterTree tree) {
          items.add(tree);
          // does nothing
          super.visitPlainCharacter(tree);
        }
      };

      RegexTree tree = RegexParserTestUtils.assertSuccessfulParse("a|b|c");
      tree.accept(visitor);

      assertThat(tree).isInstanceOf(DisjunctionTree.class);
      assertThat(((DisjunctionTree) tree).getAlternatives())
        .hasSize(3)
        .allMatch(PlainCharacterTree.class::isInstance);
      assertThat(items).hasSize(3);
    }

    @Test
    void visitSequence() {
      List<RegexTree> items = new ArrayList<>();

      RegexBaseVisitor visitor = new RegexBaseVisitor() {
        @Override
        public void visitPlainCharacter(PlainCharacterTree tree) {
          items.add(tree);
          // does nothing
          super.visitPlainCharacter(tree);
        }
      };

      RegexTree tree = RegexParserTestUtils.assertSuccessfulParse("abc");
      tree.accept(visitor);

      assertThat(tree).isInstanceOf(SequenceTree.class);
      assertThat(((SequenceTree) tree).getItems())
        .hasSize(3)
        .allMatch(PlainCharacterTree.class::isInstance);
      assertThat(items).hasSize(3);
    }
  }

  @Nested
  class FlagsTest {
    @Test
    void getSetActiveFlags() {
      RegexBaseVisitor visitor = new RegexBaseVisitor();
      assertThat(visitor.getActiveFlags()).isZero();

      visitor.setActiveFlags(Pattern.LITERAL);
      assertThat(visitor.getActiveFlags()).isEqualTo(Pattern.LITERAL);
    }

    @Test
    void flagActive() {
      RegexBaseVisitor visitor = new RegexBaseVisitor();
      assertThat(visitor.flagActive(Pattern.LITERAL)).isFalse();

      visitor.setActiveFlags(Pattern.LITERAL);
      assertThat(visitor.flagActive(Pattern.LITERAL)).isTrue();
    }

  }

}
