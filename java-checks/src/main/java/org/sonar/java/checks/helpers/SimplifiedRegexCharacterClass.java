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
package org.sonar.java.checks.helpers;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.sonar.java.regex.ast.CharacterClassElementTree;
import org.sonar.java.regex.ast.CharacterClassIntersectionTree;
import org.sonar.java.regex.ast.CharacterClassTree;
import org.sonar.java.regex.ast.CharacterRangeTree;
import org.sonar.java.regex.ast.CharacterTree;
import org.sonar.java.regex.ast.EscapedCharacterClassTree;
import org.sonar.java.regex.ast.MiscEscapeSequenceTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;

public class SimplifiedRegexCharacterClass {

  /**
   * This map defines the contents of the character class in the following way:<br>
   * For any entry {@code codepoint -> tree}, all the codepoints from {@code codepoint} up to (and excluding) the next
   * entry are in the character class and belong to the given tree.<br>
   * For any entry {@code codepoint -> null}, all the codepoints from {@code codepoint} up to (and excluding) the next
   * entry are not part of the character class.<br>
   * So a codepoint is contained in this class if and only if {@code contents.floorEntry(codePoint).getValue()} is
   * non-null and the tree returned by {@code getValue} will be the element of the character class which matches that
   * code point.
   */
  private TreeMap<Integer, CharacterClassElementTree> contents = new TreeMap<>();

  private boolean containsUnknownCharacters = false;

  public SimplifiedRegexCharacterClass() {
  }

  public SimplifiedRegexCharacterClass(CharacterClassElementTree tree) {
    add(tree);
  }

  public boolean isEmpty() {
    return contents.isEmpty() && !containsUnknownCharacters;
  }

  public void add(CharacterClassElementTree tree) {
    new Builder(this).visitInCharClass(tree);
  }

  public boolean intersects(SimplifiedRegexCharacterClass that, boolean defaultAnswer) {
    if (defaultAnswer && ((containsUnknownCharacters && !that.isEmpty()) || (!isEmpty() && that.containsUnknownCharacters))) {
      return true;
    }
    Iterator<Map.Entry<Integer, CharacterClassElementTree>> iter = that.contents.entrySet().iterator();
    if (!iter.hasNext()) {
      return false;
    }
    Map.Entry<Integer, CharacterClassElementTree> entry = iter.next();
    while (iter.hasNext()) {
      Map.Entry<Integer, CharacterClassElementTree> nextEntry = iter.next();
      if (entry.getValue() != null && hasEntryBetween(entry.getKey(), nextEntry.getKey())) {
        return true;
      }
      entry = nextEntry;
    }
    return entry.getValue() != null && hasEntryBetween(entry.getKey(), Character.MAX_CODE_POINT);
  }

  private boolean hasEntryBetween(int from, int to) {
    Map.Entry<Integer, CharacterClassElementTree> before = contents.floorEntry(from);
    return ((before != null && before.getValue() != null) || !contents.subMap(from, false, to, false).isEmpty());
  }

  public void addRange(int from, int to, CharacterClassElementTree tree) {
    Map.Entry<Integer, CharacterClassElementTree> oldEntry = contents.floorEntry(to);
    Integer oldEnd = oldEntry == null ? null : contents.higherKey(oldEntry.getKey());
    contents.put(from, tree);
    for (Map.Entry<Integer, CharacterClassElementTree> entry : contents.subMap(from, false, to, true).entrySet()) {
      if (entry.getValue() == null) {
        entry.setValue(tree);
      }
    }
    int next = to + 1;
    if (next <= Character.MAX_CODE_POINT) {
      if (oldEntry != null && oldEntry.getValue() != null && (oldEnd == null || oldEnd > next)) {
        contents.put(next, oldEntry.getValue());
      } else if (!contents.containsKey(next)) {
        contents.put(next, null);
      }
    }
  }

  private static class Builder extends RegexBaseVisitor {

    private SimplifiedRegexCharacterClass characters;

    public Builder(SimplifiedRegexCharacterClass characters) {
      this.characters = characters;
    }

    @Override
    protected void visitCharacter(CharacterTree tree) {
      addRange(tree.codePointOrUnit(), tree.codePointOrUnit(), tree);
    }

    @Override
    public void visitCharacterRange(CharacterRangeTree tree) {
      addRange(tree.getLowerBound().codePointOrUnit(), tree.getUpperBound().codePointOrUnit(), tree);
    }

    @Override
    public void visitMiscEscapeSequence(MiscEscapeSequenceTree tree) {
      characters.containsUnknownCharacters = true;
    }

    @Override
    public void visitCharacterClass(CharacterClassTree tree) {
      if (tree.isNegated()) {
        SimplifiedRegexCharacterClass old = characters;
        SimplifiedRegexCharacterClass inner = new SimplifiedRegexCharacterClass();
        characters = inner;
        super.visitCharacterClass(tree);
        characters = old;
        if (inner.containsUnknownCharacters) {
          // When negating a class that contains unknown characters, we can't know for sure whether any character is in the
          // class, so we don't add any known characters to it
          characters.containsUnknownCharacters = true;
          characters.contents = new TreeMap<>();
          return;
        }
        if (inner.contents.get(0) == null) {
          characters.contents.put(0, tree);
        }
        for (Map.Entry<Integer, CharacterClassElementTree> entry : inner.contents.entrySet()) {
          if (entry.getValue() == null) {
            characters.contents.put(entry.getKey(), tree);
          } else {
            characters.contents.put(entry.getKey(), null);
          }
        }
      } else {
        super.visitCharacterClass(tree);
      }
    }

    @Override
    public void visitCharacterClassIntersection(CharacterClassIntersectionTree tree) {
      characters.containsUnknownCharacters = true;
    }

    @Override
    public void visitEscapedCharacterClass(EscapedCharacterClassTree tree) {
      switch (tree.getType()) {
        case 'd':
          characters.addRange('0', '9', tree);
          if (tree.activeFlags().contains(Pattern.UNICODE_CHARACTER_CLASS)) {
            characters.containsUnknownCharacters = true;
          }
          break;
        case 'D':
          characters.addRange(0x00, '0' - 1, tree);
          if (tree.activeFlags().contains(Pattern.UNICODE_CHARACTER_CLASS)) {
            characters.addRange('9' + 1, 0xff, tree);
            characters.containsUnknownCharacters = true;
          } else {
            characters.addRange('9' + 1, Character.MAX_CODE_POINT, tree);
          }
          break;
        case 'w':
          characters.addRange('0', '9', tree);
          characters.addRange('A', 'Z', tree);
          characters.addRange('_', '_', tree);
          characters.addRange('a', 'z', tree);
          if (tree.activeFlags().contains(Pattern.UNICODE_CHARACTER_CLASS)) {
            characters.containsUnknownCharacters = true;
          }
          break;
        case 'W':
          characters.addRange(0x00, '0' - 1, tree);
          characters.addRange('9' + 1, 'A' - 1, tree);
          characters.addRange('Z'+1, '_' - 1, tree);
          characters.addRange('`', '`', tree);
          if (tree.activeFlags().contains(Pattern.UNICODE_CHARACTER_CLASS)) {
            characters.addRange('z' + 1, 'µ' - 1, tree);
            characters.containsUnknownCharacters = true;
          } else {
            characters.addRange('z' + 1, Character.MAX_CODE_POINT, tree);
          }
          break;
        case 's':
          characters.addRange('\t', '\r', tree);
          characters.addRange(' ', ' ', tree);
          if (tree.activeFlags().contains(Pattern.UNICODE_CHARACTER_CLASS)) {
            characters.addRange(0x85, 0x85, tree);
            characters.addRange(0xA0, 0xA0, tree);
            characters.addRange(0x1680, 0x1680, tree);
            characters.addRange(0x2000, 0x200A, tree);
            characters.addRange(0x2028, 0x2029, tree);
            characters.addRange(0x202F, 0x202F, tree);
            characters.addRange(0x205F, 0x205F, tree);
            characters.addRange(0x3000, 0x3000, tree);
          }
          break;
        case 'S':
          characters.addRange(0x00, '\t' - 1, tree);
          characters.addRange('\r' + 1, ' ' - 1, tree);
          if (tree.activeFlags().contains(Pattern.UNICODE_CHARACTER_CLASS)) {
            characters.addRange(' ' + 1, 0x84, tree);
            characters.addRange(0x86, 0x9F, tree);
            characters.addRange(0xA1, 0x167F, tree);
            characters.addRange(0x1681, 0x1FFF, tree);
            characters.addRange(0x200B, 0x2027, tree);
            characters.addRange(0x202A, 0x202E, tree);
            characters.addRange(0x2030, 0x205E, tree);
            characters.addRange(0x2060, 0x2FFF, tree);
            characters.addRange(0x3001, Character.MAX_CODE_POINT, tree);
          } else {
            characters.addRange(' ' + 1, Character.MAX_CODE_POINT, tree);
          }
          break;
        default:
          characters.containsUnknownCharacters = true;
          break;
      }
    }

    private void addRange(int from, int to, CharacterClassElementTree tree) {
      int upperCaseFrom = Character.toUpperCase(from);
      int upperCaseTo = Character.toUpperCase(to);
      int lowerCaseFrom = Character.toLowerCase(upperCaseFrom);
      int lowerCaseTo = Character.toLowerCase(upperCaseTo);
      if (tree.activeFlags().contains(Pattern.CASE_INSENSITIVE) && lowerCaseFrom != upperCaseFrom && lowerCaseTo != upperCaseTo
        && ((isAscii(from) && isAscii(to)) || tree.activeFlags().contains(Pattern.UNICODE_CASE))) {
        characters.addRange(upperCaseFrom, upperCaseTo, tree);
        characters.addRange(lowerCaseFrom, lowerCaseTo, tree);
      } else {
        characters.addRange(from, to, tree);
      }
    }

    private static boolean isAscii(int c) {
      return c < 128;
    }

  }

}
