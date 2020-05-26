package org.sonar.java.regex.ast;

public class RegexBaseVisitor implements RegexVisitor {

  @Override
  public void visitPlainText(PlainTextTree tree) {
    // No children to visit
  }

  @Override
  public void visitSequence(SequenceTree tree) {
    for (RegexTree item : tree.getItems()) {
      visit(item);
    }
  }

  @Override
  public void visitDisjunction(DisjunctionTree tree) {
    for (RegexTree alternative : tree.getAlternatives()) {
      visit(alternative);
    }
  }

  @Override
  public void visitGroup(GroupTree tree) {
    visit(tree.getElement());
  }

  @Override
  public void visitRepetition(RepetitionTree tree) {
    visit(tree.getElement());
  }

}
