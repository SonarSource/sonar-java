package org.sonar.java.regex.ast;

public class PlainTextTree extends RegexTree {

  private final RegexToken contents;

  public PlainTextTree(RegexToken contents) {
    super(contents.source, contents.range);
    this.contents = contents;
  }

  public String getContents() {
    return contents.getValue();
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitPlainText(this);
  }

  @Override
  public Kind kind() {
    return Kind.PLAIN_TEXT;
  }

}
