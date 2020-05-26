package org.sonar.java.regex.ast;

import javax.annotation.CheckForNull;

public class SimpleQuantifier extends Quantifier {

  enum Kind {
    STAR, PLUS, QUESTION_MARK
  }

  private final Kind kind;

  public SimpleQuantifier(RegexSource source, IndexRange range, Modifier modifier, Kind kind) {
    super(source, range, modifier);
    this.kind = kind;
  }

  @Override
  public int getMinimumRepetitions() {
    if (kind == Kind.PLUS) {
      return 1;
    } else {
      return 0;
    }
  }

  @CheckForNull
  @Override
  public Integer getMaximumRepetitions() {
    if (kind == Kind.QUESTION_MARK) {
      return 1;
    } else {
      return null;
    }
  }

}
