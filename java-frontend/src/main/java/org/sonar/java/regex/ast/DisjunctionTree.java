package org.sonar.java.regex.ast;

import java.util.Collections;
import java.util.List;

public class DisjunctionTree extends RegexTree {

  private final List<RegexTree> alternatives;

  public DisjunctionTree(RegexSource source, IndexRange range, List<RegexTree> alternatives) {
    super(source, range);
    this.alternatives = alternatives;
  }

  public List<RegexTree> getAlternatives() {
    return Collections.unmodifiableList(alternatives);
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitDisjunction(this);
  }

  @Override
  public Kind kind() {
    return Kind.DISJUNCTION;
  }

}
