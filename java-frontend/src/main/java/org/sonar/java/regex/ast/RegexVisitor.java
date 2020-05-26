package org.sonar.java.regex.ast;

public interface RegexVisitor {

  default void visit(RegexTree tree) {
    tree.accept(this);
  }

  void visitPlainText(PlainTextTree tree);

  void visitSequence(SequenceTree tree);

  void visitDisjunction(DisjunctionTree tree);

  void visitGroup(GroupTree tree);

  void visitRepetition(RepetitionTree tree);

}
