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
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.sonar.plugins.java.api.tree.LiteralTree;

/**
 * The source code of a regular expression, made up out of 1 or more string literals
 */
public class RegexSource {

  private final List<LiteralTree> stringLiterals;
  private final String sourceText;
  private final TreeMap<Integer, Integer> indices;

  public RegexSource(List<LiteralTree> stringLiterals) {
    this.stringLiterals = stringLiterals;
    sourceText = stringLiterals.stream().map(RegexSource::getString).collect(Collectors.joining());
    indices = new TreeMap<>();
    int currentSourceIndex = 0;
    int currentLiteralIndex = 0;
    for (LiteralTree string : stringLiterals) {
      indices.put(currentSourceIndex, currentLiteralIndex);
      currentSourceIndex += string.value().length() - 2;
      currentLiteralIndex++;
    }
  }

  public Iterable<LiteralTree> getStringLiterals() {
    return stringLiterals;
  }

  public String substringAt(IndexRange range) {
    return sourceText.substring(range.getBeginningOffset(), range.getEndingOffset());
  }

  public String getSourceText() {
    return sourceText;
  }

  public List<Location> locationsFor(IndexRange range) {
    List<Location> result = new ArrayList<>();
    Position startPosition = findPosition(range.getBeginningOffset());
    Position endPosition = findPosition(range.getEndingOffset());
    for (int i = startPosition.indexOfLiteral; i <= endPosition.indexOfLiteral; i++) {
      LiteralTree literal = stringLiterals.get(i);
      int length = literal.value().length() - 2;
      int startIndex = startPosition.indexOfLiteral == i ? startPosition.indexInsideLiteral : 0;
      int endIndex = endPosition.indexOfLiteral == i ? endPosition.indexInsideLiteral : length;
      result.add(new Location(literal, startIndex, endIndex));
    }
    return result;
  }

  private Position findPosition(int sourceIndex) {
    Map.Entry<Integer, Integer> entry = indices.floorEntry(sourceIndex);
    return new Position(entry.getKey(), entry.getValue());
  }

  private static String getString(LiteralTree literal) {
    Optional<String> string = literal.asConstant(String.class);
    if (string.isPresent()) {
      return string.get();
    } else {
      throw new IllegalArgumentException("Only string literals allowed");
    }
  }

  private static class Position {
    int indexOfLiteral;
    int indexInsideLiteral;

    public Position(int indexOfLiteral, int indexInsideLiteral) {
      this.indexOfLiteral = indexOfLiteral;
      this.indexInsideLiteral = indexInsideLiteral;
    }
  }

}
