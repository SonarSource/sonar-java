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
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.RegexParserTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

      RegexParseResult result = RegexParserTestUtils.assertSuccessfulParseResult("a|b|c");
      visitor.visit(result);

      RegexTree tree = result.getResult();
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

      RegexParseResult result = RegexParserTestUtils.assertSuccessfulParseResult("abc");
      visitor.visit(result);

      RegexTree tree = result.getResult();
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
    void trackingFlagsInRegex() {
      testFlags("(?i)a(?u:b)|[c](?-i:d)(?u)e((?-U)f)g(?U)h(?-u)i");
    }

    @Test
    void trackingFlagsInRegexWithDifferentTypesOfGroups() {
      testFlags("(?i)a(?:(?u)b)|[c](?>(?-i)d)(?u)e(?=(?-U)f)g(?U)h(?-u)i");
    }

    @Test
    void visitingRegexWithVariousFeatures() {
      PlainCharCollector visitor = new PlainCharCollector();
      visitor.visit(RegexParserTestUtils.assertSuccessfulParseResult("[ab&&[^c]]+|d"));
      assertThat(visitor.visitedCharacters()).isEqualTo("abcd");
    }

    @Test
    void notVisitingRegularExpressionsWithErrors() {
      PlainCharCollector visitor = new PlainCharCollector();
      visitor.visit(RegexParserTestUtils.parseRegex("abcd("));
      assertThat(visitor.visitedCharacters()).isEmpty();
    }

    private void testFlags(String regex) {
      FlagChecker visitor = new FlagChecker();
      visitor.visit(RegexParserTestUtils.assertSuccessfulParseResult(regex));
      assertThat(visitor.visitedCharacters()).isEqualTo("abcdefghi");
      JavaCharacter iFlag = visitor.getJavaCharacterForFlag(Pattern.CASE_INSENSITIVE);
      assertThat(iFlag).isNotNull();
      assertThat(iFlag.getCharacter()).isEqualTo('i');
      assertThat(visitor.getActiveFlags()).isEqualTo(Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
      JavaCharacter uFlag = visitor.getJavaCharacterForFlag(Pattern.UNICODE_CASE);
      assertThat(uFlag).isNull();
    }

    private class PlainCharCollector extends RegexBaseVisitor {

      StringBuilder characters = new StringBuilder();

      @Override
      public void visitPlainCharacter(PlainCharacterTree tree) {
        characters.append(tree.getCharacter());
        super.visitPlainCharacter(tree);
      }

      String visitedCharacters() {
        return characters.toString();
      }
    }

    private class FlagChecker extends PlainCharCollector {

      @Override
      public void visitPlainCharacter(PlainCharacterTree tree) {
        switch (tree.getCharacter()) {
          case 'a': case 'c': case 'f':
            assertActiveFlags(true, false, false);
            break;
          case 'b': case 'e': case 'g':
            assertActiveFlags(true, true, false);
            break;
          case 'd':
            assertActiveFlags(false, false, false);
            break;
          case 'h':
            assertActiveFlags(true, true, true);
            break;
          case 'i':
            assertActiveFlags(true, false, true);
            break;
          default:
            fail("Uncovered character in regex");
        }
        super.visitPlainCharacter(tree);
      }

      void assertActiveFlags(boolean i, boolean u, boolean U) {
        assertThat(flagActive(Pattern.CASE_INSENSITIVE)).isEqualTo(i);
        assertThat(flagActive(Pattern.UNICODE_CASE)).isEqualTo(u);
        assertThat(flagActive(Pattern.UNICODE_CHARACTER_CLASS)).isEqualTo(U);
      }

    }

  }

}
