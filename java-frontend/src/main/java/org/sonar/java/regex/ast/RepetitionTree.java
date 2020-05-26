package org.sonar.java.regex.ast;

public class RepetitionTree extends RegexTree {

  private final RegexTree element;

  private final Quantifier quantifier;

  protected RepetitionTree(RegexSource source, IndexRange range, RegexTree element, Quantifier quantifier) {
    super(source, range);
    this.element = element;
    this.quantifier = quantifier;
  }

  public RegexTree getElement() {
    return element;
  }

  public Quantifier getQuantifier() {
    return quantifier;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitRepetition(this);
  }

  @Override
  public Kind kind() {
    return Kind.REPETITION;
  }

}
