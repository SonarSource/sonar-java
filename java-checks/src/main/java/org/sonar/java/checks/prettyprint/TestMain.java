package org.sonar.java.checks.prettyprint;

import org.sonar.plugins.java.api.tree.SwitchStatementTree;

import static org.sonar.java.checks.prettyprint.Template.caseGroupTemplate;
import static org.sonar.java.checks.prettyprint.Template.expressionTemplate;
import static org.sonar.java.checks.prettyprint.Template.statementTemplate;

public final class TestMain {

  private TestMain() {
  }

  public static void main(String[] args) {
    var repl = expressionTemplate("bar(42)").apply();
    var ast = (SwitchStatementTree) statementTemplate("switch ($0()) { }").apply(repl);
    ast.cases().add(caseGroupTemplate("case 0 -> {}").apply());
    ast.cases().add(caseGroupTemplate("case 1 -> throw new IllegalArgumentException();").apply());
    var ppsb = new PrettyPrintStringBuilder(FileConfig.DEFAULT_FILE_CONFIG, null, false);
    ast.accept(new Prettyprinter(ppsb));
    System.out.println(ppsb);
  }

}
