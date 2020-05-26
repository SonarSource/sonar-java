package org.sonar.java.regex.ast;

import org.sonar.plugins.java.api.tree.Tree;

public class Location {

  private final Tree javaTree;

  private final IndexRange indexRange;

  public Location(Tree javaTree, int beginningOffset, int endingOffset) {
    this.javaTree = javaTree;
    this.indexRange = new IndexRange(beginningOffset, endingOffset);
  }

  public Tree getJavaTree() {
    return javaTree;
  }

  public IndexRange getIndexRange() {
    return indexRange;
  }

  public int getBeginningOffset() {
    return indexRange.getBeginningOffset();
  }

  public int getEndingOffset() {
    return indexRange.getEndingOffset();
  }

}
