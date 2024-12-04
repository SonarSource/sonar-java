package org.javac.api;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.SourcePositions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.sonar.java.model.InternalSyntaxToken;

public class TokenManager {

  private static final char LINE_SEPARATOR = '\u2028';

  private CharSequence source;
  private SourcePositions sourcePositions;
  private CompilationUnitTree compilationUnitTree;
  private Map<Long, Long> lineStarts = new HashMap<>();

  public TokenManager(CharSequence source, SourcePositions sourcePositions, CompilationUnitTree compilationUnitTree) {
    this.source = source;
    this.sourcePositions = sourcePositions;
    this.compilationUnitTree = compilationUnitTree;
    computeLineStarts();
  }

  private void computeLineStarts() {
    long line = 0L;
    lineStarts.put(line, 0L);
    for (int i = 0; i < source.length(); i++) {
      if (source.charAt(i) == LINE_SEPARATOR) {
        line++;
        lineStarts.put(line, (long) (i + 1));
      }
    }
  }

  public InternalSyntaxToken getEOFToken() {
    long endTokenIndex = sourcePositions.getEndPosition(compilationUnitTree, compilationUnitTree);
    while (source.charAt((int) endTokenIndex) == '\u0000') {
      endTokenIndex--;
    }
    return new InternalSyntaxToken(0, (int) endTokenIndex, "", Collections.emptyList(), true);
  }

  private int findChar(char c, int startPos) {
    for (int i = startPos; i < source.length(); i++) {
      if (source.charAt(i) == c) {
        return i;
      }
    }
    return -1;
  }

}
