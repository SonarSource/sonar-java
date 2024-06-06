package org.sonar.plugins.java.api.query.ruleexamples;

import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.Tree;

class IssuableJavaCheck implements JavaCheck {
  public void report(Tree tree, String message) {
    throw new UnsupportedOperationException();
  }
}
