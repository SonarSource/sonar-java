package org.sonar.java.regex.ast;

public class IndexRange {

  private final int beginningOffset;
  private final int endingOffset;

  public IndexRange(int beginningOffset, int endingOffset) {
    this.beginningOffset = beginningOffset;
    this.endingOffset = endingOffset;
  }

  public int getBeginningOffset() {
    return beginningOffset;
  }

  public int getEndingOffset() {
    return endingOffset;
  }

}
