package org.sonar.java.regex.ast;

public class JavaCharacter extends RegexSyntaxElement {

  private final char character;

  public JavaCharacter(RegexSource source, IndexRange range, char character) {
    super(source, range);
    this.character = character;
  }

  public char getCharacter() {
    return character;
  }

}
