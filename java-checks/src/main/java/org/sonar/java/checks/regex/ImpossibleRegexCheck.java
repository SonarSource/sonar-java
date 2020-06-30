package org.sonar.java.checks.regex;

import java.util.Collections;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.BackReferenceTree;
import org.sonar.java.regex.ast.CapturingGroupTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5840")
public class ImpossibleRegexCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    new ImpossiblePatternFinder().visit(regexForLiterals);
  }

  private class ImpossiblePatternFinder extends RegexBaseVisitor {

    int groupCount = 0;

    @Override
    public void visitCapturingGroup(CapturingGroupTree tree) {
      groupCount++;
      super.visitCapturingGroup(tree);
    }

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      if (tree.isNumerical() && tree.groupNumber() > groupCount) {
        reportIssue(tree, "Remove this illegal back reference or rewrite the regex.", null, Collections.emptyList());
      }
      super.visitBackReference(tree);
    }

  }

}
