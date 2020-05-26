package org.sonar.java.regex.ast;

public class RegexToken extends RegexSyntaxElement {

  private final String value;

  public RegexToken(RegexSource source, IndexRange range) {
    super(source, range);
    value = source.getSubstringAt(range);
  }

  public String getValue() {
    return value;
  }

}
