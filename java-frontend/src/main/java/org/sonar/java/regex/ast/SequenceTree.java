package org.sonar.java.regex.ast;

import java.util.Collections;
import java.util.List;

public class SequenceTree extends RegexTree {

  private final List<RegexTree> items;

  public SequenceTree(RegexSource source, IndexRange range, List<RegexTree> items) {
    super(source, range);
    this.items = items;
  }

  public List<RegexTree> getItems() {
    return Collections.unmodifiableList(items);
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitSequence(this);
  }

  @Override
  public Kind kind() {
    return Kind.SEQUENCE;
  }

}
