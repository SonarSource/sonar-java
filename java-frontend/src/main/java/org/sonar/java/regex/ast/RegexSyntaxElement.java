package org.sonar.java.regex.ast;

import java.util.List;

public class RegexSyntaxElement {

  protected final RegexSource source;

  protected final IndexRange range;

  protected RegexSyntaxElement(RegexSource source, IndexRange range) {
    this.source = source;
    this.range = range;
  }

  public List<Location> getLocations() {
    throw new UnsupportedOperationException();
  }

}
