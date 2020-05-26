package org.sonar.java.regex.ast;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class CurlyBraceQuantifier extends Quantifier {

  private final int minimumRepetitions;

  private final Integer maximumRepetitions;

  public CurlyBraceQuantifier(
    RegexSource source,
    IndexRange range,
    Modifier modifier,
    int minimumRepetitions,
    @Nullable Integer maximumRepetitions
  ) {
    super(source, range, modifier);
    this.minimumRepetitions = minimumRepetitions;
    this.maximumRepetitions = maximumRepetitions;
  }

  @Override
  public int getMinimumRepetitions() {
    return minimumRepetitions;
  }

  @CheckForNull
  @Override
  public Integer getMaximumRepetitions() {
    return maximumRepetitions;
  }

}
