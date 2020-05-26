package org.sonar.java.regex.ast;

public class GroupTree extends RegexTree {

  private final RegexTree element;

  public GroupTree(RegexSource source, IndexRange range, RegexTree element) {
    super(source, range);
    this.element = element;
  }

  public RegexTree getElement() {
    return element;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitGroup(this);
  }

  @Override
  public Kind kind() {
    return Kind.GROUP;
  }

}
