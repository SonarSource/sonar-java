package org.sonar.java.regex.ast;

import javax.annotation.CheckForNull;

public abstract class Quantifier extends RegexSyntaxElement {

  enum Modifier {
    GREEDY, LAZY, POSSESSIVE
  }

  private final Modifier modifier;

  protected Quantifier(RegexSource source, IndexRange range, Modifier modifier) {
    super(source, range);
    this.modifier = modifier;
  }

  public abstract int getMinimumRepetitions();

  @CheckForNull
  public abstract Integer getMaximumRepetitions();

  public Modifier getModifier() {
    return modifier;
  }

}
