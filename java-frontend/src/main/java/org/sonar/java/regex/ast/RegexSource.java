package org.sonar.java.regex.ast;

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
  private final List<String> strings;
  private final TreeMap<Integer, Integer> indices;

  public RegexSource(List<LiteralTree> stringLiterals) {
    this.stringLiterals = stringLiterals;
    strings = stringLiterals.stream().map(RegexSource::getString).collect(Collectors.toList());
    indices = new TreeMap<>();
    int currentSourceIndex = 0;
    int currentLiteralIndex = 0;
    for (String string : strings) {
      indices.put(currentSourceIndex, currentLiteralIndex);
      currentSourceIndex += string.length();
      currentLiteralIndex++;
    }
  }

  public Iterable<LiteralTree> getStringLiterals() {
    return stringLiterals;
  }

  public String getSubstringAt(IndexRange range) {
    StringBuilder sb = new StringBuilder();

    Position startPosition = findPosition(range.getBeginningOffset());
    Position endPosition = findPosition(range.getEndingOffset());
    for (int i = startPosition.indexOfLiteral; i <= endPosition.indexOfLiteral; i++) {
      String string = strings.get(i);
      int startIndex = startPosition.indexOfLiteral == i ? startPosition.indexInsideLiteral : 0;
      int endIndex = endPosition.indexOfLiteral == i ? endPosition.indexInsideLiteral : string.length();
      sb.append(string, startIndex, endIndex);
    }
    return sb.toString();
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
