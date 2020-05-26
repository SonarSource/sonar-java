package org.sonar.java.regex.ast;

public abstract class RegexTree extends RegexSyntaxElement {
  public enum Kind {
    PLAIN_TEXT, SEQUENCE, DISJUNCTION, GROUP, REPETITION
  }

  protected RegexTree(RegexSource source, IndexRange range) {
    super(source, range);
  }

  public abstract void accept(RegexVisitor visitor);

  public abstract Kind kind();

  public boolean is(Kind... kinds) {
    Kind thisKind = kind();
    for (Kind kind : kinds) {
      if (thisKind == kind) {
        return true;
      }
    }
    return false;
  }

}
