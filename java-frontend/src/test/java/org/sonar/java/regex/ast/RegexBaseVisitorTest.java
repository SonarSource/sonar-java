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

  @Test
  void before() {
    class Visitor extends RegexBaseVisitor {
      boolean visitedBefore = false;
      boolean visitedSequence = false;

      @Override
      protected void before(RegexParseResult regexParseResult) {
        visitedBefore = true;
        assertThat(visitedSequence).isFalse();
        super.before(regexParseResult);
      }

      @Override
      public void visitSequence(SequenceTree tree) {
        visitedSequence = true;
        assertThat(visitedBefore).isTrue();
        super.visitSequence(tree);
      }
    }

    Visitor visitor = new Visitor();
    visitor.visit(RegexParserTestUtils.assertSuccessfulParseResult("abc"));
    assertThat(visitor.visitedBefore).isTrue();
    assertThat(visitor.visitedSequence).isTrue();
  }

  @Test
  void after() {
    class Visitor extends RegexBaseVisitor {
      boolean visitedAfter = false;
      boolean visitedSequence = false;

      @Override
      public void visitSequence(SequenceTree tree) {
        visitedSequence = true;
        assertThat(visitedAfter).isFalse();
        super.visitSequence(tree);
      }

      @Override
      protected void after(RegexParseResult regexParseResult) {
        visitedAfter = true;
        assertThat(visitedSequence).isTrue();
        super.before(regexParseResult);
      }
    }

    Visitor visitor = new Visitor();
    visitor.visit(RegexParserTestUtils.assertSuccessfulParseResult("abc"));
    assertThat(visitor.visitedAfter).isTrue();
    assertThat(visitor.visitedSequence).isTrue();
  }

  @Nested
  class VisitTest {
    @Test
    void visitDisjunction() {
      List<RegexTree> items = new ArrayList<>();

      RegexBaseVisitor visitor = new RegexBaseVisitor() {
        @Override
        public void visitCharacter(CharacterTree tree) {
          items.add(tree);
          // does nothing
          super.visitCharacter(tree);
        }
      };

      RegexParseResult result = RegexParserTestUtils.assertSuccessfulParseResult("a|b|c");
      visitor.visit(result);

      RegexTree tree = result.getResult();
      assertThat(tree).isInstanceOf(DisjunctionTree.class);
      assertThat(((DisjunctionTree) tree).getAlternatives())
        .hasSize(3)
        .allMatch(CharacterTree.class::isInstance);
      assertThat(items).hasSize(3);
    }

    @Test
    void visitSequence() {
      List<RegexTree> items = new ArrayList<>();

      RegexBaseVisitor visitor = new RegexBaseVisitor() {
        @Override
        public void visitCharacter(CharacterTree tree) {
          items.add(tree);
          // does nothing
          super.visitCharacter(tree);
        }
      };

      RegexParseResult result = RegexParserTestUtils.assertSuccessfulParseResult("abc");
      visitor.visit(result);

      RegexTree tree = result.getResult();
      assertThat(tree).isInstanceOf(SequenceTree.class);
      assertThat(((SequenceTree) tree).getItems())
        .hasSize(3)
        .allMatch(CharacterTree.class::isInstance);
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
      LeafCollector visitor = new LeafCollector();
      visitor.visit(RegexParserTestUtils.assertSuccessfulParseResult(
        "^[ab&&[^c]]+|(?<x>d)[e-f][\\\\x01-\\\\x02].\\\\1\\\\k<x>\\\\w\\\\x0A\\\\R$")
      );
      assertThat(visitor.visitedCharacters()).isEqualTo(
        "<boundary:^>abcd<range:e-f><range:\u0001-\u0002><dot><backref:1><backref:x><char-class-escape:\\\\w>\n<boundary:$>"
      );
    }

    @Test
    void notVisitingRegularExpressionsWithErrors() {
      LeafCollector visitor = new LeafCollector();
      visitor.visit(RegexParserTestUtils.parseRegex("abcd("));
      assertThat(visitor.visitedCharacters()).isEmpty();
    }

    private void testFlags(String regex) {
      FlagChecker visitor = new FlagChecker();
      RegexParseResult parseResult = RegexParserTestUtils.assertSuccessfulParseResult(regex);
      visitor.visit(parseResult);
      assertThat(visitor.visitedCharacters()).isEqualTo("abcdefghi");
      List<RegexTree> items = ((SequenceTree) ((DisjunctionTree) parseResult.getResult()).getAlternatives().get(1)).getItems();
      FlagSet activeFlags = items.get(items.size() - 1).activeFlags();
      SourceCharacter iFlag = activeFlags.getJavaCharacterForFlag(Pattern.CASE_INSENSITIVE);
      assertThat(iFlag).isNotNull();
      assertThat(iFlag.getCharacter()).isEqualTo('i');
      assertThat(activeFlags.getMask()).isEqualTo(Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
      SourceCharacter uFlag = activeFlags.getJavaCharacterForFlag(Pattern.UNICODE_CASE);
      assertThat(uFlag).isNull();
    }

    private class LeafCollector extends RegexBaseVisitor {

      StringBuilder characters = new StringBuilder();

      @Override
      public void visitCharacter(CharacterTree tree) {
        characters.append(tree.characterAsString());
        super.visitCharacter(tree);
      }

      @Override
      public void visitDot(DotTree tree) {
        characters.append("<dot>");
        super.visitDot(tree);
      }

      @Override
      public void visitCharacterRange(CharacterRangeTree tree) {
        characters.append("<range:");
        visit(tree.getLowerBound());
        characters.append("-");
        visit(tree.getUpperBound());
        characters.append(">");
        super.visitCharacterRange(tree);
      }

      @Override
      public void visitBackReference(BackReferenceTree tree) {
        characters.append("<backref:");
        characters.append(tree.groupName());
        characters.append(">");
        super.visitBackReference(tree);
      }

      @Override
      public void visitEscapedCharacterClass(EscapedCharacterClassTree tree) {
        characters.append("<char-class-escape:");
        characters.append(tree.getText());
        characters.append(">");
        super.visitEscapedCharacterClass(tree);
      }

      @Override
      public void visitBoundary(BoundaryTree boundaryTree) {
        characters.append("<boundary:");
        characters.append(boundaryTree.getText());
        characters.append(">");
        super.visitBoundary(boundaryTree);
      }

      String visitedCharacters() {
        return characters.toString();
      }
    }

    private class FlagChecker extends LeafCollector {

      @Override
      public void visitCharacter(CharacterTree tree) {
        switch (tree.characterAsString()) {
          case "a": case "c": case "f":
            assertActiveFlags(tree, true, false, false);
            break;
          case "b": case "e": case "g":
            assertActiveFlags(tree, true, true, false);
            break;
          case "d":
            assertActiveFlags(tree, false, false, false);
            break;
          case "h":
            assertActiveFlags(tree, true, true, true);
            break;
          case "i":
            assertActiveFlags(tree, true, false, true);
            break;
          default:
            fail("Uncovered character in regex");
        }
        super.visitCharacter(tree);
      }

      void assertActiveFlags(CharacterTree tree, boolean i, boolean u, boolean U) {
        assertThat(tree.activeFlags().contains(Pattern.CASE_INSENSITIVE)).isEqualTo(i);
        assertThat(tree.activeFlags().contains(Pattern.UNICODE_CASE)).isEqualTo(u);
        assertThat(tree.activeFlags().contains(Pattern.UNICODE_CHARACTER_CLASS)).isEqualTo(U);
      }

    }

  }

}
